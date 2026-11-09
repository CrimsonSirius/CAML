package org.chs.camlauncher.front.view;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.chs.camlauncher.backend.runtime.JavaSelectionMode;

import java.nio.file.Path;

/** 主页和设置页共享的启动参数。 */
final class LaunchSettings {
    private final ObjectProperty<Path> gameDirectory = new SimpleObjectProperty<>(this, "gameDirectory");
    private final StringProperty selectedVersion = new SimpleStringProperty(this, "selectedVersion");
    private final BooleanProperty fullVersionIsolation =
            new SimpleBooleanProperty(this, "fullVersionIsolation");
    private final ObjectProperty<JavaSelectionMode> javaSelectionMode =
            new SimpleObjectProperty<>(this, "javaSelectionMode");
    private final ObjectProperty<Path> selectedJavaPath =
            new SimpleObjectProperty<>(this, "selectedJavaPath");
    private int maxMemoryMb;

    LaunchSettings(int maxMemoryMb, Path gameDirectory, boolean fullVersionIsolation,
                   String selectedVersion, JavaSelectionMode javaSelectionMode,
                   Path selectedJavaPath) {
        this.maxMemoryMb = maxMemoryMb;
        this.gameDirectory.set(gameDirectory);
        this.fullVersionIsolation.set(fullVersionIsolation);
        this.javaSelectionMode.set(javaSelectionMode == null
                ? JavaSelectionMode.AUTO
                : javaSelectionMode);
        this.selectedJavaPath.set(selectedJavaPath);
        this.selectedVersion.set(selectedVersion == null || selectedVersion.isBlank()
                ? "1.21.4"
                : selectedVersion);
    }

    int maxMemoryMb() {
        return maxMemoryMb;
    }

    void setMaxMemoryMb(int maxMemoryMb) {
        this.maxMemoryMb = maxMemoryMb;
    }

    Path gameDirectory() {
        return gameDirectory.get();
    }

    void setGameDirectory(Path gameDirectory) {
        this.gameDirectory.set(gameDirectory);
    }

    ObjectProperty<Path> gameDirectoryProperty() {
        return gameDirectory;
    }

    String selectedVersion() {
        return selectedVersion.get();
    }

    void setSelectedVersion(String selectedVersion) {
        this.selectedVersion.set(selectedVersion);
    }

    StringProperty selectedVersionProperty() {
        return selectedVersion;
    }

    boolean fullVersionIsolation() {
        return fullVersionIsolation.get();
    }

    void setFullVersionIsolation(boolean fullVersionIsolation) {
        this.fullVersionIsolation.set(fullVersionIsolation);
    }

    BooleanProperty fullVersionIsolationProperty() {
        return fullVersionIsolation;
    }

    JavaSelectionMode javaSelectionMode() {
        return javaSelectionMode.get();
    }

    void setJavaSelectionMode(JavaSelectionMode mode) {
        javaSelectionMode.set(mode == null ? JavaSelectionMode.AUTO : mode);
    }

    ObjectProperty<JavaSelectionMode> javaSelectionModeProperty() {
        return javaSelectionMode;
    }

    Path selectedJavaPath() {
        return selectedJavaPath.get();
    }

    void setSelectedJavaPath(Path path) {
        selectedJavaPath.set(path);
    }

    ObjectProperty<Path> selectedJavaPathProperty() {
        return selectedJavaPath;
    }
}
