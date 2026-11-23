package org.chs.camlauncher.backend.system;

import java.util.List;

/** 与打包许可证清单保持一致的运行时依赖列表。 */
public final class ProjectDependencyService {

    public List<ProjectDependency> dependencies() {
        return List.of(
                new ProjectDependency(
                        "OpenJDK runtime (Azul Zulu)",
                        String.valueOf(Runtime.version().feature()),
                        "Azul Systems",
                        "GPL-2.0 with Classpath Exception"),
                new ProjectDependency(
                        "OpenJFX (JavaFX)", "25.0.3", "OpenJFX",
                        "GPL-2.0 with Classpath Exception"),
                new ProjectDependency("Gson", "2.15.0", "Google", "Apache-2.0"),
                new ProjectDependency(
                        "Log4j API and Log4j Core", "2.26.1",
                        "Apache Software Foundation", "Apache-2.0"),
                new ProjectDependency(
                        "TwelveMonkeys ImageIO (WebP)", "3.15.3",
                        "Harald Kuhr", "BSD-3-Clause"),
                new ProjectDependency(
                        "Error Prone annotations", "2.50.0", "Google", "Apache-2.0")
        );
    }
}
