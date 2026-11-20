import org.chs.camlauncher.backend.api.CAMLBackend;
import org.chs.camlauncher.backend.config.BackendConfig;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 发布包冒烟校验：以安装包完全相同的模块布局启动后端。
 *
 * <p>用法（app 目录指 app-image 中存放 jar 的目录）：
 * <pre>
 * javac -d classes -cp "APP/*" packaging/smoke/BackendSmoke.java
 * java --module-path APP --add-modules=javafx.controls,javafx.fxml \
 *      -cp "classes:APP/*" BackendSmoke DATA_DIR GAME_DIR
 * </pre>
 *
 * <p>为什么不能只跑单元测试：单元测试是类路径布局，jar 处于无名模块，
 * {@code Class.getResourceAsStream} 可以跨 jar 读资源；而 jpackage 产物把 app 目录下
 * 所有 jar 放进 {@code --module-path}，它们成为命名（自动）模块，只能看到自身资源。
 * 1.0.0 安装包因此启动即崩溃（找不到 CAMLLanguage 里的 lang/*.json）。
 *
 * <p>这里刻意断言 CAMLBackend 处于命名模块，否则说明本次运行没有复现安装包的加载形态，
 * 校验也就失去了意义。
 */
public final class BackendSmoke {
    private static final int EXPECTED_LANGUAGES = 11;

    private BackendSmoke() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            throw new IllegalArgumentException("usage: BackendSmoke <dataDir> <gameDir>");
        }
        Module module = CAMLBackend.class.getModule();
        System.out.println("CAMLBackend module: " + module.getName());
        if (!module.isNamed()) {
            throw new IllegalStateException(
                    "CAMLBackend 未以命名模块加载，本次校验没有复现安装包的模块布局；"
                            + "请确认 java 命令带有 --module-path <app>");
        }

        Path dataDirectory = Path.of(args[0]);
        Path gameDirectory = Path.of(args[1]);
        Files.createDirectories(dataDirectory);
        Files.createDirectories(gameDirectory);

        CAMLBackend backend = new CAMLBackend(new BackendConfig(dataDirectory, gameDirectory));
        try {
            backend.start();
            int languages = backend.languageService().availableLocales().size();
            System.out.println("backend started, bundled languages: " + languages);
            if (languages < EXPECTED_LANGUAGES) {
                throw new IllegalStateException(
                        "内置语言数量不足：期望 " + EXPECTED_LANGUAGES + "，实际 " + languages);
            }
            if (!Files.isRegularFile(dataDirectory.resolve("lang").resolve("zh_cn.json"))) {
                throw new IllegalStateException("内置语言文件未同步到数据目录：" + dataDirectory);
            }
        } finally {
            backend.close();
        }
        System.out.println("smoke check passed");
    }
}

