package org.chs.camlauncher.language.generator.lang;

import org.chs.camlauncher.language.generator.TranslationTable;

/** 地道中国话的账号、设置与任务文案。 */
final class FNCNSharedTexts {
    private FNCNSharedTexts() {
    }

    static void populate(TranslationTable t) {
        // 账号
        t.put("account.choice.default", "跟着默认账号走");
        t.put("account.choice.offline", "离线大号名儿");
        t.put("account.default.saved", "%s 现在是默认账号了");
        t.put("account.error.passwordRequired", "密码得填上");
        t.put("account.error.serverRequired", "认证服务器地址得填上");
        t.put("account.error.usernameRequired", "用户名得填上");
        t.put("account.injector.builtin", "用自带的（缺了再去下）");
        t.put("account.injector.chooserTitle", "挑一个 authlib-injector 的 jar");
        t.put("account.injector.custom", "自己准备的文件：%s");
        t.put("account.injector.reset", "还是用自带的");
        t.put("account.injector.saved", "authlib-injector 设置收好了");
        t.put("account.instance.count", "一共 %d 个实例，挨个挑启动时用的账号");
        t.put("account.instance.loading", "正翻账号本儿...");
        t.put("account.instance.mismatch",
                "有些实例的账号原来绑在 %s 上，搁这个游戏位置就不灵了（%d 个实例）");
        t.put("account.instance.mismatchApplied", "已经把 %d 个实例的账号绑定搬到这个游戏位置");
        t.put("account.instance.mismatchApply", "搁这个游戏位置使");
        t.put("account.instance.saved", "实例 %s 的账号收好了");
        t.put("account.instance.title", "实例账号");
        t.put("account.list.default", "默认的");
        t.put("account.list.empty",
                "还没账号呢。加个离线的、用微软登录，或者登个第三方认证服务器都成。");
        t.put("account.list.needsLogin", "登录过期了");
        t.put("account.list.relogin", "再登一回");
        t.put("account.list.remove", "撵走");
        t.put("account.login.customServer", "自己填的服务器地址");
        t.put("account.login.customServerOption", "自己填的地址");
        t.put("account.login.hint", "密码就这一回使，绝不保存。");
        t.put("account.login.password", "密码");
        t.put("account.login.reloginHint", "把 %s 的密码再填一遍。启动器从来不存密码。");
        t.put("account.login.remember", "记住登录状态");
        t.put("account.login.running", "正登录呢...");
        t.put("account.login.server", "认证服务器");
        t.put("account.login.submit", "登进去");
        t.put("account.login.success", "已经登成 %s 了");
        t.put("account.login.username", "邮箱或者用户名");
        t.put("account.microsoft.codeReady", "拿浏览器打开验证页面，把抄好的设备码填进去。");
        t.put("account.microsoft.copyCode", "抄设备码");
        t.put("account.microsoft.copyUrl", "抄验证页面");
        t.put("account.microsoft.login", "使微软登录");
        t.put("account.microsoft.running", "正跟微软要设备码...");
        t.put("account.microsoft.userCode", "设备码：%s");
        t.put("account.microsoft.verificationUri", "验证页面：%s");
        t.put("account.offline.add", "加上");
        t.put("account.offline.added", "离线账号 %s 加上了");
        t.put("account.offline.label", "加个离线账号");
        t.put("account.offline.placeholder", "离线大号名儿");
        t.put("account.profile.label", "角色");
        t.put("account.profile.switch", "换个");
        t.put("account.profile.switched", "角色换成 %s 了");
        t.put("account.removed", "把 %s 撵走了");

        // 认证出错
        t.put("auth.error.accountMissing", "找不着这账号：%s");
        t.put("auth.error.accountRequired", "先挑个账号");
        t.put("auth.error.injectorChecksum", "authlib-injector 没验过：%s");
        t.put("auth.error.injectorNotFound", "找不着 authlib-injector：%s");
        t.put("auth.error.invalidCredentials", "%s 的用户名或者密码不对");
        t.put("auth.error.loginExpired", "%s（%s）的登录过期了，再登一回");
        t.put("auth.error.loginRequired", "账号 %s（%s）没存凭据，再登一回");
        t.put("auth.error.metadataUnavailable", "读不了认证服务器的信息：%s（HTTP %s）");
        t.put("auth.error.noAccount", "还没配账号呢。上设置里加一个，或者用离线用户名。");
        t.put("auth.error.noSelectedProfile", "这账号没有 Minecraft 角色：%s");
        t.put("auth.error.passwordRequired", "得填密码");
        t.put("auth.error.profileMissing", "那个角色使不了了：%s");
        t.put("auth.error.requestFailed", "认证没成：%s（HTTP %s）");
        t.put("auth.error.responseInvalid", "认证服务器回的玩意儿看不懂：%s");
        t.put("auth.error.tooManyRequests", "跟 %s 要得太勤了，歇一会儿再试");
        t.put("auth.error.usernameRequired", "得填用户名");

        // 微软认证出错
        t.put("auth.microsoft.accountSuspended", "这微软账号八成被封了。");
        t.put("auth.microsoft.clientIdMissing", "微软登录还没配，先把 CAML 的 Microsoft OAuth 客户端 ID 设上。");
        t.put("auth.microsoft.forbidden", "现在的网络让 Minecraft Services 拦了，换个网再试。");
        t.put("auth.microsoft.loginDeclined", "微软那边把登录拒了。");
        t.put("auth.microsoft.loginExpired", "微软登录过期了，再登一回。");
        t.put("auth.microsoft.loginFailed", "微软登录没成：%s");
        t.put("auth.microsoft.loginTimeout", "微软设备码过期了，再试一回。");
        t.put("auth.microsoft.missingField", "微软回的响应里少了 %s。");
        t.put("auth.microsoft.noLicense", "这账号没有 Minecraft Java 版，或者 Game Pass 权益过期了。");
        t.put("auth.microsoft.noProfile", "这账号还没建 Minecraft 角色，建一个再登。");
        t.put("auth.microsoft.profileChanged", "刷新账号时 Minecraft 角色变了，再登一回。");
        t.put("auth.microsoft.requestFailed", "微软认证在 %s 那儿没成（HTTP %s）。");
        t.put("auth.microsoft.responseInvalid", "从 %s 回来的微软认证响应对不上号。");
        t.put("auth.microsoft.serviceUnavailable", "Minecraft Services 这会儿不顶事，过会儿再试。");
        t.put("auth.microsoft.tooManyRequests", "登得太勤了，歇几分钟再试。");
        t.put("auth.microsoft.wrongVerifyMethod", "微软登录页上使密码进，别选别的登录方式。");
        t.put("auth.microsoft.xboxBanned", "这微软账号让 Xbox Live 封了。");
        t.put("auth.microsoft.xboxFamily", "这小孩账号得先拉进微软家庭。");
        t.put("auth.microsoft.xboxNoAccount", "这微软账号还没有 Xbox 账号，先建一个再试。");
        t.put("auth.microsoft.xboxRegion", "现在这个国家或地区使不了 Xbox Live。");
        t.put("auth.microsoft.xboxUnknown", "Xbox Live 把登录拒了（XErr %s）。");

        // 通用
        t.put("common.close", "关喽");
        t.put("common.reset", "恢复原样");

        // 解压限制
        t.put("download.settings.archive.description", "解整合包和地图时候的门槛。");
        t.put("download.settings.archive.entries", "最多多少个条目");
        t.put("download.settings.archive.entrySize", "单个条目最大多大（MiB）");
        t.put("download.settings.archive.invalid", "填点靠谱的数。总共的大小不能比单条限制还小。");
        t.put("download.settings.archive.saved", "解压门槛收好了。");
        t.put("download.settings.archive.title", "解压门槛");
        t.put("download.settings.archive.totalSize", "解完总共最大多大（MiB）");

        // 主页
        t.put("home.account", "账号");
        t.put("home.error.loadAccounts", "账号翻不出来：%s");
        t.put("home.manageAccounts", "实例账号");
        t.put("home.username.managed", "用户名跟着挑的账号走：%s");

        // 启动提醒
        t.put("launch.warning.java.force", "硬上");
        t.put("launch.warning.java.message", "这游戏要 Java %s，眼下选的是 Java %s。硬上八成要栽。");
        t.put("launch.warning.java.stop", "算了");
        t.put("launch.warning.java.title", "Java 版本可能不对付");

        // 日志
        t.put("log.collapse", "收起来");
        t.put("log.empty", "还没啥动静");
        t.put("log.expand", "摊开");

        // 整合包
        t.put("modpack.error.invalidArchive", "整合包压缩文件不对劲：%s");
        t.put("modpack.error.invalidFile", "整合包里的文件路径不地道：%s");
        t.put("modpack.error.missingFileUrl", "整合包文件没给下载地址：%s");
        t.put("modpack.error.missingMinecraft", "整合包没说使哪个 Minecraft 版本");
        t.put("modpack.error.unsupportedLoader", "整合包用的加载器咱不认：%s");
        t.put("modpacks.detail.install", "装整合包");
        t.put("modpacks.detail.installed", "整合包装好了：%s");
        t.put("modpacks.detail.installing", "正装整合包 %s...");
        t.put("modpacks.detail.loading", "正翻整合包的家底...");
        t.put("modpacks.install.instanceName", "实例名儿");
        t.put("modpacks.install.instanceNameEmpty", "实例名儿不能空着");
        t.put("modpacks.list.meta", "版本：%s · 下载 %s · 更新 %s");
        t.put("modpacks.nav", "整合包");
        t.put("modpacks.search.empty", "没找着对口的整合包");
        t.put("modpacks.search.failed", "整合包翻不出来：%s");
        t.put("modpacks.search.loading", "正翻整合包...");
        t.put("modpacks.search.nameHint", "填个整合包名儿");
        t.put("modpacks.search.title", "翻整合包");

        // 后台操作名
        t.put("operation.addOfflineAccount", "加离线账号");
        t.put("operation.downloadJava", "下载并装上 Java %s");
        t.put("operation.fetchAuthServer", "打听认证服务器");
        t.put("operation.fetchJavaDownloadList", "打听 Java 下载清单：%s");
        t.put("operation.fetchModpackProject", "打听整合包项目 %s");
        t.put("operation.installModpack", "装整合包 %s");
        t.put("operation.loadAccounts", "翻账号");
        t.put("operation.loadInstanceAccount", "翻实例账号");
        t.put("operation.loginAccount", "登账号");
        t.put("operation.loginMicrosoftAccount", "使微软登录");
        t.put("operation.openJavaFolder", "打开 Java 文件夹");
        t.put("operation.removeAccount", "撵走账号");
        t.put("operation.saveArchiveExtractionLimits", "保存解压门槛");
        t.put("operation.saveDefaultAccount", "保存默认账号");
        t.put("operation.saveInjectorPath", "保存 authlib-injector 路径");
        t.put("operation.saveInstanceAccount", "保存实例账号");
        t.put("operation.searchModpacks", "翻整合包 %s");
        t.put("operation.switchProfile", "换账号角色");

        // 设置：关于
        t.put("settings.about.application.title", "启动器");
        t.put("settings.about.architecture", "架构");
        t.put("settings.about.backend", "后台");
        t.put("settings.about.copyright", "版权");
        t.put("settings.about.dataDirectory", "数据窝");
        t.put("settings.about.dependencies.title", "项目使的玩意儿");
        t.put("settings.about.dependency.author", "作者：%s");
        t.put("settings.about.dependency.license", "许可：%s");
        t.put("settings.about.device.title", "机器底细");
        t.put("settings.about.directories.title", "文件夹");
        t.put("settings.about.gameDirectory", "游戏文件夹");
        t.put("settings.about.javaVendor", "Java 供货的");
        t.put("settings.about.javaVersion", "Java 版本");
        t.put("settings.about.kernelVersion", "内核版本");
        t.put("settings.about.license", "许可");
        t.put("settings.about.memory", "运行内存");
        t.put("settings.about.notDetected", "没瞅见");
        t.put("settings.about.os", "操作系统");
        t.put("settings.about.osVersion", "系统版本");
        t.put("settings.about.project.title", "项目");
        t.put("settings.about.projectName", "名儿");
        t.put("settings.about.runtime.title", "启动器运行环境");
        t.put("settings.about.system.title", "系统");
        t.put("settings.about.unknown", "闹不清");
        t.put("settings.about.version", "版本");

        // 设置：账号
        t.put("settings.account.description",
                "启动器存着的账号。accessToken 加密之后才落盘，密码一概不存。");
        t.put("settings.account.injector.description",
                "authlib-injector 头一回使的时候下一下，往后一直用，每回启动都拿 SHA-256 验一遍。");
        t.put("settings.account.login.description", "使 Yggdrasil 认证服务器（比如 LittleSkin）的账号登进去。");
        t.put("settings.account.login.title", "第三方登录");
        t.put("settings.account.microsoft.description", "拿设备码登微软账号，启动器不跟你要密码。");
        t.put("settings.account.microsoft.title", "微软账号");
        t.put("settings.account.title", "账号");

        // 设置：Java 管理
        t.put("settings.javaManagement.count", "一共 %d 个 Java 运行时");
        t.put("settings.javaManagement.distribution", "发行版");
        t.put("settings.javaManagement.download", "下 Java");
        t.put("settings.javaManagement.downloadAndInstall", "下来装上");
        t.put("settings.javaManagement.downloading", "正下 Java...");
        t.put("settings.javaManagement.downloadingProgress", "正下 Java... %d%%");
        t.put("settings.javaManagement.empty", "没瞅见 Java 运行时");
        t.put("settings.javaManagement.emptyVersions", "没得 Java 版本可下");
        t.put("settings.javaManagement.installed", "Java 装上了：%s");
        t.put("settings.javaManagement.installFailed", "Java 没装上：%s");
        t.put("settings.javaManagement.linuxHint",
                "Linux 上头最好使发行版的包管理器（apt、dnf 或 pacman）装 JDK 或 JRE，"
                        + "别使 CAML 管的 Java。");
        t.put("settings.javaManagement.loadingVersions", "正打听 Java 版本...");
        t.put("settings.javaManagement.loadVersionsFailed", "Java 版本打听不来：%s");
        t.put("settings.javaManagement.manual", "自己加的");
        t.put("settings.javaManagement.meta", "%s · %d 位%s");
        t.put("settings.javaManagement.openFolder", "打开它待的文件夹");
        t.put("settings.javaManagement.openFolder.failed", "Java 文件夹打不开：%s");
        t.put("settings.javaManagement.openFolder.opening", "正开文件夹...");
        t.put("settings.javaManagement.packageType", "包类型");
        t.put("settings.javaManagement.unavailable", "使不了");
        t.put("settings.javaManagement.version", "版本");
        t.put("settings.javaManagement.versionCount", "一共 %d 个版本");

        // 设置：分区
        t.put("settings.section.about", "关于");
        t.put("settings.section.account", "账号");
        t.put("settings.section.java", "Java 管理");

        // 任务
        t.put("tasks.back", "回任务列表");
        t.put("tasks.empty", "还没活儿");
        t.put("tasks.open", "打开任务 %s · %s");
        t.put("tasks.status.cancelled", "撂挑子了");
        t.put("tasks.status.completed", "完事了");
        t.put("tasks.status.failed", "栽了");
        t.put("tasks.status.running", "正忙活");
        t.put("tasks.subtitle", "下载与安装的活儿");
        t.put("tasks.title", "任务");
    }
}

