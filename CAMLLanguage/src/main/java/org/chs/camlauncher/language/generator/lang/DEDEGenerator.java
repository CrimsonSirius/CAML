package org.chs.camlauncher.language.generator.lang;

import org.chs.camlauncher.language.generator.LanguageTranslationGenerator;
import org.chs.camlauncher.language.generator.TranslationTable;

/** Deutsche Übersetzungen. */
public final class DEDEGenerator extends LanguageTranslationGenerator {
    @Override
    protected String locale() {
        return "de_de";
    }

    @Override
    protected void populate(TranslationTable t) {
        // Allgemein
        t.put("common.back", "Zurück");
        t.put("common.cancel", "Abbrechen");
        t.put("common.retry", "Erneut versuchen");
        t.put("common.browse", "Durchsuchen");
        t.put("common.save", "Speichern");
        t.put("common.copy", "Befehle kopieren");
        t.put("common.copied", "Kopiert");

        // Navigation und Fußzeile
        t.put("nav.home", "Start");
        t.put("nav.downloads", "Downloads");
        t.put("nav.settings", "Einstellungen");
        t.put("app.subtitle", "Minecraft-Launcher");
        t.put("footer.status", "CAML %s · Backend %s · Datenverzeichnis: %s");
        t.put("footer.backend.ready", "Bereit");
        t.put("footer.backend.notReady", "Nicht bereit");

        // Startformular
        t.put("home.username", "Offline-Benutzername");
        t.put("home.launch", "Spiel starten");
        t.put("home.launching", "Wird gestartet...");
        t.put("home.running", "Spiel läuft");
        t.put("home.launchFailedTitle", "Spielstart fehlgeschlagen");
        t.put("home.browseInstances", "Instanzen durchsuchen");
        t.put("home.manageInstances", "Instanzen verwalten");
        t.put("home.instance.current", "Aktuelle Instanz: %s");
        t.put("home.instance.current.none", "Aktuelle Instanz: nicht ausgewählt");
        t.put("home.gameDirectory.none", "Spielverzeichnis: nicht festgelegt");
        t.put("home.gameDirectory.missing", "Spielverzeichnis: fehlt · %s");
        t.put("home.gameDirectory.value", "Spielverzeichnis: %s");
        t.put("home.saveLocation.none", "Speicherort: nicht festgelegt");
        t.put("home.saveLocation.fixDirectory", "Speicherort: zuerst das Spielverzeichnis reparieren");
        t.put("home.saveLocation.selectInstance", "Speicherort: zuerst eine Spielinstanz auswählen");
        t.put("home.saveLocation.value", "Speicherort: %s");
        t.put("home.saveLocation.isolated", " (vollständige Versionsisolierung)");
        t.put("home.error.inspectDirectory", "Prüfung des Spielverzeichnisses fehlgeschlagen: %s");
        t.put("home.error.noGameDirectory", "Speichere zuerst das Spielverzeichnis in den Einstellungen");
        t.put("home.error.missingGameDirectory", "Das Spielverzeichnis existiert nicht; speichere in den Einstellungen ein gültiges Verzeichnis");
        t.put("home.error.noInstance", "Durchsuche und wähle zuerst eine Spielinstanz");
        t.put("home.error.noUsername", "Gib einen Offline-Benutzernamen ein");

        // Protokoll
        t.put("log.title", "Ausführungsprotokoll");
        t.put("log.subtitle", "Download-Fortschritt und Ausgabe des Spielprozesses");
        t.put("log.accessible", "Ausführungsprotokoll");
        t.put("log.download.installed.vanilla", "[Download] Instanz %s installiert (Vanilla %s)");
        t.put("log.download.installed.fabric", "[Download] Fabric-Instanz %s installiert (Loader %s)");
        t.put("log.download.installed.loader", "[Download] %s-Instanz %s installiert");
        t.put("log.download.progress", "[Download] %s (%d/%d)");
        t.put("log.download.progressNoTotal", "[Download] %s (%d)");
        t.put("log.gameProcess.exit", "Spielprozess beendet mit Code: %d");
        t.put("log.gameProcess.exitError", "Spielprozess unerwartet beendet mit Code: %d");
        t.put("log.error.operationFailed", "[%s] fehlgeschlagen: %s");

        // Startstatus
        t.put("status.starting", "Starte %s...");
        t.put("status.started", "Startbefehl gesendet");
        t.put("status.launchFailed", "Start fehlgeschlagen!");
        t.put("status.launchFailedDetail", "Start fehlgeschlagen: %s");
        t.put("error.unknown", "Ein unbekannter Fehler ist aufgetreten. Prüfe die Protokolle.");
        t.put("download.error.failed", "Download von %s nach %s Versuchen fehlgeschlagen (letzte Adresse: %s)");
        t.put("network.requestFailed", "Anfrage an %s fehlgeschlagen (letzte Adresse: %s)");
        t.put("network.timeout", "Zeitüberschreitung der Anfrage (%s ms): %s");
        t.put("network.httpError", "HTTP %s: %s");
        t.put("network.responseInvalid", "Antwort konnte nicht verarbeitet werden: %s");
        t.put("network.redirectLocationMissing", "Weiterleitung ohne Location: %s");
        t.put("network.tooManyRedirects", "Zu viele Weiterleitungen: %s");
        t.put("network.noSource", "Keine Download-Quelle verfügbar: %s");
        t.put("metadata.manifestEmpty", "Das Versionsmanifest ist leer");
        t.put("metadata.versionJsonEmpty", "Das Versions-JSON ist leer");
        t.put("metadata.versionJsonHashMismatch", "SHA-1-Prüfung des Versions-JSON fehlgeschlagen: %s");
        t.put("launch.permissionDenied", "Keine Berechtigung zum Ausführen von Java oder zum Zugriff auf das Spielverzeichnis. Prüfe die Dateiberechtigungen.");
        t.put("launch.executableNotFound", "Java-Programm nicht gefunden. Wähle erneut eine Java-Installation.");
        t.put("launch.commandTooLong", "Der Startbefehl ist zu lang. Kürze den Spielpfad oder den Classpath.");
        t.put("launch.commandTooLongAfterFallback", "Der Startbefehl ist auch nach Argfile- und Manifest-Fallback zu lang. Kürze den Spielpfad oder den Classpath.");
        t.put("launch.processStartFailed", "Der Spielprozess konnte nicht gestartet werden. Details findest du im Protokoll.");
        t.put("launch.manualJavaNotConfigured", "Die manuelle Java-Auswahl ist aktiviert, aber kein Java-Pfad ausgewählt.");
        t.put("launch.manualJavaUnavailable", "Das ausgewählte Java ist nicht verfügbar: %s");
        t.put("launch.javaRequirementNotFound", "Kein Java gefunden, das die Versionsanforderung %s erfüllt. Installiere oder wähle Java.");
        t.put("launch.warning.xrandr.title", "xrandr fehlt");
        t.put("launch.warning.xrandr.message", "Diese Instanz verwendet LWJGL 2, aber xrandr wurde im System nicht gefunden. Das Spiel wird trotzdem gestartet, kann aber beim Initialisieren des Anzeigemodus beendet werden. Installiere xrandr und starte das Spiel erneut.");
        t.put("launch.warning.xrandr.commands", "Arch Linux: sudo pacman -S xorg-xrandr\nDebian/Ubuntu: sudo apt install x11-xserver-utils\nFedora/RHEL: sudo dnf install xrandr");

        // Kategorien und Quellen
        t.put("download.category.release", "Release");
        t.put("download.category.snapshot", "Snapshot");
        t.put("download.category.old", "Alt");
        t.put("download.category.unknown", "Unbekannte Version");
        t.put("download.source.mirror", "Spiegelquelle");
        t.put("download.source.official", "Offizielle Quelle");
        t.put("download.source.custom", "Benutzerdefiniert");

        // Versionsliste
        t.put("download.versions.loading", "Lade %2$s-Liste von %1$s...");
        t.put("download.versions.empty", "Die Download-Quelle hat keine herunterladbaren %s zurückgegeben");
        t.put("download.versions.count", "%d %s, neueste zuerst nach Veröffentlichungsdatum");
        t.put("download.versions.error", "Laden der %s-Liste fehlgeschlagen: %s");
        t.put("download.versions.accessible", "%s, %s");

        // Beschreibungen
        t.put("download.description.optifine", "Forge %s + OptiFine %s");
        t.put("download.description.forge", "Forge %s");
        t.put("download.description.fabric", "Fabric Loader %s");
        t.put("download.description.fabricWithApi", "Fabric Loader %s + Fabric API %s");
        t.put("download.description.neoforge", "NeoForge %s");
        t.put("download.description.vanilla", "Minecraft %s");

        // Download-Einrichtung
        t.put("download.setup.status.idle", "Lädt das Vanilla-Spiel herunter, wenn kein Loader ausgewählt ist.");
        t.put("download.setup.instanceName", "Instanzname");
        t.put("download.setup.download", "Instanz herunterladen");
        t.put("download.setup.option.versionInfo", "Versionsinformationen");
        t.put("download.setup.option.forge", "Forge-Version auswählen");
        t.put("download.setup.option.optifine", "OptiFine-Version auswählen");
        t.put("download.setup.option.fabric", "Fabric-Version auswählen");
        t.put("download.setup.option.fabricApi", "Fabric-API-Version auswählen");
        t.put("download.setup.option.neoforge", "NeoForge-Version auswählen");
        t.put("download.setup.versionInfo", "Minecraft %s · %s");
        t.put("download.setup.versionInfoWithDate", "Minecraft %s · %s · %s");
        t.put("download.setup.optifineSwitched", "Forge und OptiFine sind inkompatibel; zu Forge %s gewechselt");
        t.put("download.setup.optifineIncompatibleForge", "Das aktuelle Forge ist mit OptiFine inkompatibel");
        t.put("download.setup.noCompatibleOptiFine", "Keine OptiFine-Version ist mit dem aktuellen Forge kompatibel");
        t.put("download.setup.optifineForgeConflict", "Forge %s ist nicht mit OptiFine %s kompatibel. Wähle Forge %s oder älter.");
        t.put("download.setup.nameEmpty", "Der Instanzname darf nicht leer sein");
        t.put("download.setup.nameDotSegments", "Der Instanzname darf nicht . oder .. sein");
        t.put("download.setup.nameTrailing", "Der Instanzname darf nicht mit einem Punkt oder Leerzeichen enden");
        t.put("download.setup.nameIllegalCharacter", "Der Instanzname enthält ein ungültiges Zeichen: %s");
        t.put("download.setup.nameExists", "Dieser Instanzname existiert bereits: %s. Wähle einen anderen Namen.");
        t.put("download.setup.noGameDirectory", "Das Spielverzeichnis ist nicht festgelegt");
        t.put("download.setup.checkingName", "Instanzname wird geprüft...");
        t.put("download.setup.nameSameAsLoader", "Beim Installieren eines Loaders darf der Instanzname nicht der Spielversion entsprechen");
        t.put("download.setup.nameAvailable", "Der Instanzname ist verfügbar");
        t.put("download.setup.checkNameFailed", "Prüfung des Instanznamens fehlgeschlagen: %s");
        t.put("download.setup.nameNotChecked", "Der Instanzname wurde noch nicht geprüft; versuche es gleich noch einmal.");
        t.put("download.setup.noGameDirectoryLong", "Das Spielverzeichnis ist nicht festgelegt; speichere es zuerst in den Einstellungen.");
        t.put("download.setup.optifineRequiresForge", "OptiFine kann nur zusammen mit Forge installiert werden.");

        // Loader-Hinweise
        t.put("download.prompt.loading.fabric", "Lade Fabric-Versionen...");
        t.put("download.prompt.loading.forge", "Lade Forge-Versionen...");
        t.put("download.prompt.loading.neoforge", "Lade NeoForge-Versionen...");
        t.put("download.prompt.fabricFirst", "Wähle zuerst eine Fabric-Version");
        t.put("download.prompt.forgeFirst", "Wähle zuerst eine Forge-Version");
        t.put("download.prompt.fabricApiFirst", "Wähle zuerst eine Fabric-Version");
        t.put("download.prompt.select.fabric", "Wähle eine Fabric-Version");
        t.put("download.prompt.select.forge", "Wähle eine Forge-Version");
        t.put("download.prompt.select.neoforge", "Wähle eine NeoForge-Version");
        t.put("download.prompt.select.optifine", "Wähle eine OptiFine-Version");
        t.put("download.prompt.select.fabricApi", "Wähle eine Fabric-API-Version");
        t.put("download.prompt.empty.fabric", "Keine Fabric-Versionen verfügbar");
        t.put("download.prompt.empty.fabricApi", "Keine Fabric-API-Versionen verfügbar");
        t.put("download.prompt.empty.forge", "Keine Forge-Versionen verfügbar");
        t.put("download.prompt.empty.neoforge", "Keine NeoForge-Versionen verfügbar");
        t.put("download.prompt.empty.optifine", "Keine OptiFine-Versionen verfügbar");
        t.put("download.prompt.error.fabric", "Laden der Fabric-Versionen fehlgeschlagen");
        t.put("download.prompt.error.fabricApi", "Laden der Fabric-API-Versionen fehlgeschlagen");
        t.put("download.prompt.error.forge", "Laden der Forge-Versionen fehlgeschlagen");
        t.put("download.prompt.error.neoforge", "Laden der NeoForge-Versionen fehlgeschlagen");
        t.put("download.prompt.error.optifine", "Laden der OptiFine-Versionen fehlgeschlagen");

        // Fortschrittsseite
        t.put("download.progress.title", "Lade %s herunter");
        t.put("download.progress.subtitle", "%s · Minecraft %s");
        t.put("download.progress.back", "Zurück zu den Downloads");
        t.put("download.progress.speed", "Netzwerkgeschwindigkeit");
        t.put("download.progress.speedValue", "0 B/s");
        t.put("download.progress.progress", "Download-Fortschritt");
        t.put("download.progress.preparing", "Download wird vorbereitet");
        t.put("download.progress.waiting", "Warte auf Download-Aufgabe...");
        t.put("download.progress.completed", "Download abgeschlossen");
        t.put("download.progress.completedTotal", "Download abgeschlossen");
        t.put("download.progress.total", "Gesamt %s");
        t.put("download.progress.totalWithMax", "Gesamt %s / %s");
        t.put("download.progress.failed", "Download fehlgeschlagen: %s");
        t.put("download.progress.currentFile", "Aktuelle Datei: %s");
        t.put("download.progress.currentFileWithBytes", "Aktuelle Datei: %s · %s / %s");
        t.put("download.progress.finishedHint", "Die Instanz ist installiert; du kannst zur Liste zurückkehren.");
        t.put("download.progress.failedHint", "Download gestoppt; kehre zur Liste zurück und versuche es erneut.");
        t.put("download.progress.bytesDetail", "%.1f%% · %s / %s · Dateien %d / %d");
        t.put("download.progress.filesDetail", "Dateien %d / %d");
        t.put("download.progress.planning", "Download-Plan wird vorbereitet...");

        // Download-Phasen
        t.put("download.stage.preparing", "Download wird vorbereitet");
        t.put("download.stage.completed", "Abgeschlossen");
        t.put("download.stage.failed", "Download fehlgeschlagen");
        t.put("download.stage.unknownError", "Unbekannter Fehler");
        t.put("download.stage.version.fetch", "Versionsmetadaten werden abgerufen");
        t.put("download.stage.version.checkLocal", "Lokale Version wird geprüft");
        t.put("download.stage.version.downloadFiles", "Spieldateien und Bibliotheken werden heruntergeladen");
        t.put("download.stage.version.downloadAssets", "Ressourcen werden heruntergeladen");
        t.put("download.stage.version.writeJson", "Versionsmetadaten werden geschrieben");
        t.put("download.stage.loader.installer.prepare", "%s-Installer wird vorbereitet");
        t.put("download.stage.loader.installer.download", "%s-Installer wird heruntergeladen");
        t.put("download.stage.loader.installer.run", "%s-Installer wird ausgeführt");
        t.put("download.stage.loader.libraries.download", "%s-Bibliotheken werden heruntergeladen");
        t.put("download.stage.loader.files.merge", "%s-Dateien werden zusammengeführt");
        t.put("download.stage.loader.verify", "%s-Installation wird überprüft");
        t.put("download.stage.fabric.config.fetch", "Fabric-Konfiguration wird abgerufen");
        t.put("download.stage.fabric.loader.install", "Fabric Loader wird installiert");
        t.put("download.stage.fabric.api.download", "Fabric API wird heruntergeladen");
        t.put("download.stage.fabric.verify", "Fabric-Installation wird überprüft");
        t.put("download.stage.optifine.installer.download", "OptiFine-Installer wird heruntergeladen");
        t.put("download.stage.optifine.installer.run", "OptiFine-Installer wird ausgeführt");
        t.put("download.stage.optifine.files.merge", "OptiFine-Dateien werden zusammengeführt");

        // Download-Einstellungen
        t.put("download.settings.source", "Download-Quelle");
        t.put("download.settings.source.description", "Wähle die bevorzugte Download-Adresse für Versionen, Bibliotheken und Ressourcen.");
        t.put("download.settings.source.mirror", "Spiegelquelle bevorzugen");
        t.put("download.settings.source.official", "Offizielle Quelle bevorzugen");
        t.put("download.settings.source.custom", "Benutzerdefinierte API importieren");
        t.put("download.settings.customApi.placeholder", "z. B. https://bmclapi2.bangbang93.com");
        t.put("download.settings.concurrency", "Download-Parallelität");
        t.put("download.settings.concurrency.description", "Anzahl gleichzeitig heruntergeladener Dateien, Bereich 1–256.");
        t.put("download.settings.concurrency.label", "Download-Parallelität");
        t.put("download.settings.concurrency.value", "Aktuelle Parallelität: %d");
        t.put("download.settings.concurrency.accessible", "Download-Parallelität");

        // Einstellungsnavigation
        t.put("settings.section.game", "Spieleinstellungen");
        t.put("settings.section.download", "Download-Einstellungen");
        t.put("settings.section.appearance", "Darstellung");

        // Spielverzeichnis
        t.put("settings.game.location.title", "Spielverzeichnis");
        t.put("settings.game.location.description", "Lege das Minecraft-Datenverzeichnis fest. Änderungen gelten erst nach dem Speichern.");
        t.put("settings.game.location.browse", "Durchsuchen");
        t.put("settings.game.location.save", "Speichern");
        t.put("settings.game.location.chooserTitle", "Spielverzeichnis wählen");
        t.put("settings.game.location.notSaved", "Nicht gespeichert");
        t.put("settings.game.location.empty", "Das Spielverzeichnis darf nicht leer sein");
        t.put("settings.game.location.invalid", "Ungültiges Format des Spielverzeichnisses");
        t.put("settings.game.location.checking", "Verzeichnis wird geprüft...");
        t.put("settings.game.location.missing", "Verzeichnis existiert nicht: %s");
        t.put("settings.game.location.saved", "Spielverzeichnis gespeichert");
        t.put("settings.game.location.checkFailed", "Prüfung des Verzeichnisses fehlgeschlagen: %s");

        // Arbeitsspeicher
        t.put("settings.game.memory.title", "Arbeitsspeicher");
        t.put("settings.game.memory.accessible", "Maximaler Arbeitsspeicher");
        t.put("settings.game.memory.description", "Passe den Arbeitsspeicher an, der beim Start des Spiels verwendet wird.");
        t.put("settings.game.memory.label", "Speicherzuweisung");
        t.put("settings.game.memory.hint", "Ziehe den Griff, um die Spielzuweisung anzupassen; der graue Balken zeigt belegten + Spielspeicher, der hellgraue Balken rechts den freien Speicher.");
        t.put("settings.game.memory.summary", "Belegt %d MB / Spiel %d MB / Verfügbar %d MB / Gesamt %d MB");

        // Versionsisolierung
        // JVM-Argumente
        t.put("settings.game.jvm.title", "JVM-Argumente");
        t.put("settings.game.jvm.description", "Fügt beim Start zusätzliche JVM-Argumente hinzu. Die Speicherzuweisung wird weiterhin über den Schieberegler oben gesteuert; ändere diese Argumente nur, wenn du ihre Wirkung kennst.");
        t.put("settings.game.jvm.label", "Zusätzliche Argumente");
        t.put("settings.game.jvm.hint", "Argumente werden durch Leerzeichen getrennt; Werte mit Leerzeichen in Anführungszeichen setzen, z. B. -Dfoo=\"bar baz\". Platzhalter wie ${game_directory} und ${library_directory} werden unterstützt.");
        t.put("settings.game.jvm.placeholder", "-XX:+UseG1GC -Dexample=value");
        t.put("settings.game.jvm.save", "Speichern");
        t.put("settings.game.jvm.saved", "JVM-Argumente gespeichert");
        t.put("settings.game.jvm.saveFailed", "JVM-Argumente konnten nicht gespeichert werden: %s");

        t.put("settings.game.isolation.title", "Versionsisolierung");
        t.put("settings.game.isolation.full", "Vollständige Versionsisolierung");
        t.put("settings.game.isolation.none", "Keine Versionsisolierung");

        // Spielsprache
        t.put("settings.game.language.title", "Spielsprache");
        t.put("settings.game.language.description", "Setzt die Spielsprache beim ersten Start einer Instanz automatisch auf die Sprache des Launchers.");
        t.put("settings.game.language.enabled", "Automatisch wechseln");
        t.put("settings.game.language.disabled", "Aus");
        t.put("settings.game.language.hint", "Gilt nur, wenn für die Instanz noch keine Sprache festgelegt ist; eine bereits in options.txt gesetzte Sprache bleibt erhalten. Benutzerdefiniertes Chinesisch wird auf vereinfachtes Chinesisch abgebildet.");

        // Java
        t.put("settings.game.java.title", "Java");
        t.put("settings.game.java.description", "Wähle die Java-Laufzeit zum Starten des Spiels. Der automatische Modus wählt ein Java, das den Anforderungen der Spielversion entspricht.");
        t.put("settings.game.java.auto", "Automatisch");
        t.put("settings.game.java.manual", "Manuell");
        t.put("settings.game.java.rescan", "Neu scannen");
        t.put("settings.game.java.add", "Java hinzufügen");
        t.put("settings.game.java.scanning", "Java wird gesucht...");
        t.put("settings.game.java.scanningAll", "System wird nach Java durchsucht...");
        t.put("settings.game.java.scanFailedPrompt", "Suche nach Java fehlgeschlagen");
        t.put("settings.game.java.scanFailed", "Suche nach Java fehlgeschlagen: %s");
        t.put("settings.game.java.noneFound", "Kein Java gefunden");
        t.put("settings.game.java.select", "Wähle ein Java");
        t.put("settings.game.java.autoSelected", "Automatisch");
        t.put("settings.game.java.chooserTitle", "Java-Programm wählen");
        t.put("settings.game.java.checking", "Java wird geprüft...");
        t.put("settings.game.java.addFailed", "Java konnte nicht hinzugefügt werden: %s");
        t.put("settings.game.java.autoHint", "Der automatische Modus wählt aus den Scanergebnissen ein Java, das den Anforderungen der aktuellen Spielversion entspricht.");
        t.put("settings.game.java.selectOrAdd", "Wähle ein Java oder klicke auf „Java hinzufügen“.");
        t.put("settings.game.java.unavailable", "Dieses Java ist derzeit nicht verfügbar: %s");
        t.put("settings.game.java.detail", "Version %s · %s · %s · %d Bit%s");
        t.put("settings.game.java.jdk", " · JDK");
        t.put("settings.game.java.jre", " · JRE");

        // Darstellung
        t.put("settings.appearance.title", "Darstellung");
        t.put("settings.appearance.description", "Passe das Erscheinungsbild des Launchers an.");
        t.put("settings.appearance.themeHint", "Themes stammen aus ~/.caml/themes; Änderungen gelten sofort");
        t.put("settings.appearance.theme", "Theme");
        t.put("settings.appearance.theme.accessible", "Theme");
        t.put("settings.appearance.language.title", "Sprache");
        t.put("settings.appearance.language.description", "Wähle, wie die Sprache der Launcher-Oberfläche ausgewählt wird.");
        t.put("settings.appearance.language.auto", "Automatisch");
        t.put("settings.appearance.language.manual", "Manuell");
        t.put("settings.appearance.language.label", "Oberflächensprache");
        t.put("settings.appearance.language.hint", "Automatisch folgt der Systemsprache; manuell verwendet die unten gewählte Sprache.");
        t.put("settings.appearance.language.accessible", "Oberflächensprache");
        t.put("settings.appearance.languageFallback.title", "Sprach-Fallback");
        t.put("settings.appearance.languageFallback.description", "Wähle, welche Sprache fehlende Übersetzungen ergänzt.");
        t.put("settings.appearance.languageFallback.system", "Systemsprache");
        t.put("settings.appearance.languageFallback.default", "Standardsprache");
        t.put("settings.appearance.languageFallback.key", "Rohe Schlüssel anzeigen");
        t.put("settings.appearance.languageFallback.custom", "Benutzerdefinierte Sprache");
        t.put("settings.appearance.languageFallback.hint", "Die Systemsprache nutzt nur die erkannte Sprache; die Standardsprache nutzt immer Englisch; rohe Schlüssel deaktivieren den Fallback; die benutzerdefinierte Sprache nutzt nur die ausgewählte Sprache. Außer bei der Standardsprache werden fehlende Texte als rohe Schlüssel angezeigt.");
        t.put("settings.appearance.languageFallback.accessible", "Sprach-Fallback");

        // Instanzauswahl
        t.put("instances.picker.scanning", "Spielinstanzen werden gesucht...");
        t.put("instances.picker.title", "Spielinstanz wählen");
        t.put("instances.picker.noGameDirectory", "Das Spielverzeichnis ist nicht festgelegt");
        t.put("instances.picker.gameDirectory", "Spielverzeichnis: %s");
        t.put("instances.picker.noGameDirectoryHint", "Das Spielverzeichnis ist nicht festgelegt; speichere es zuerst in den Einstellungen");
        t.put("instances.picker.missing", "Das Spielverzeichnis existiert nicht: %s");
        t.put("instances.picker.empty", "Keine installierten Instanzen im aktuellen Spielverzeichnis");
        t.put("instances.picker.count", "%d Spielinstanzen");
        t.put("instances.picker.scanFailed", "Suche nach Spielinstanzen fehlgeschlagen: %s");
        t.put("instances.manage.title", "Instanzen verwalten");
        t.put("instances.manage.instance", "Instanz: %s");
        t.put("instances.manage.folder", "Instanzordner: %s");
        t.put("instances.manage.openFolder", "Instanzordner öffnen");
        t.put("instances.manage.openFolder.opening", "Instanzordner wird geöffnet...");
        t.put("instances.manage.openFolder.failed", "Instanzordner konnte nicht geöffnet werden: %s");
        t.put("instances.manage.noGameDirectory", "Kein Spielverzeichnis konfiguriert");
        t.put("instances.manage.noInstance", "Keine Instanz ausgewählt");
        t.put("instances.manage.mods.title", "Mod-Liste");
        t.put("instances.manage.mods.scanning", "Mod-Liste wird gelesen...");
        t.put("instances.manage.mods.count", "%d Mods");
        t.put("instances.manage.mods.empty", "Diese Instanz enthält keine Mods");
        t.put("instances.manage.mods.scanFailed", "Mod-Liste konnte nicht gelesen werden: %s");
        t.put("instances.manage.mods.toggleFailed", "Mod-Status konnte nicht geändert werden: %s");
        t.put("instances.manage.mods.update", "Aktualisieren");
        t.put("instances.manage.mods.updating", "%s wird aktualisiert...");
        t.put("instances.manage.mods.updateFailed", "Mod konnte nicht aktualisiert werden: %s");
        t.put("instances.manage.mods.enabled", "Aktiviert");
        t.put("instances.manage.mods.disabled", "Deaktiviert");
        t.put("instances.manage.mods.pageTitle", "Mods verwalten");
        t.put("instances.manage.scanning", "Instanzinformationen werden gelesen...");
        t.put("instances.manage.scanFailed", "Instanzinformationen konnten nicht gelesen werden: %s");
        t.put("instances.manage.summary", "%d Mods, %d Ressourcenpakete");
        t.put("instances.manage.manageMods", "Mods verwalten");
        t.put("instances.manage.manageResourcePacks", "Ressourcenpakete verwalten");
        t.put("instances.manage.resourcePacks.pageTitle", "Ressourcenpakete verwalten");
        t.put("instances.manage.resourcePacks.scanning", "Ressourcenpaketliste wird gelesen...");
        t.put("instances.manage.resourcePacks.count", "%d Ressourcenpakete");
        t.put("instances.manage.resourcePacks.empty", "Diese Instanz hat keine Ressourcenpakete");
        t.put("instances.manage.resourcePacks.scanFailed", "Ressourcenpaketliste konnte nicht gelesen werden: %s");
        t.put("instances.manage.resourcePacks.toggleFailed", "Status des Ressourcenpakets konnte nicht geändert werden: %s");
        t.put("instances.manage.resourcePacks.enabled", "Aktiviert");
        t.put("instances.manage.resourcePacks.disabled", "Deaktiviert");
        t.put("instances.manage.resourcePacks.version", "Version: %s");

        // Mod-Downloads
        t.put("downloads.section.instances", "Spielinstanzen");
        t.put("downloads.section.content", "Spielinhalte");
        t.put("maps.nav", "Karten");
        t.put("maps.search.title", "Karten suchen");
        t.put("maps.search.nameHint", "Kartenname eingeben");
        t.put("maps.search.loading", "Karten werden gesucht...");
        t.put("maps.search.empty", "Keine passenden Karten gefunden");
        t.put("maps.search.failed", "Karten konnten nicht geladen werden: %s");
        t.put("maps.list.meta", "Version: %s · Downloads %s · Aktualisiert %s");
        t.put("maps.detail.loading", "Kartendetails werden geladen...");
        t.put("operation.searchMaps", "Karten suchen %s");
        t.put("operation.fetchMapProject", "Kartenprojekt abrufen %s");
        t.put("operation.downloadMap", "Karte herunterladen %s");
        t.put("mod.error.worldArchiveInvalid", "Ungültiges Kartenarchiv: %s");
        t.put("mods.nav", "Mods");
        t.put("shaders.nav", "Shader");
        t.put("shaders.search.title", "Shader suchen");
        t.put("shaders.search.nameHint", "Shader-Namen eingeben");
        t.put("shaders.search.loading", "Shader werden gesucht...");
        t.put("shaders.search.empty", "Keine passenden Shader gefunden");
        t.put("shaders.search.failed", "Shader konnten nicht geladen werden: %s");
        t.put("shaders.list.meta", "Versionen: %s · Downloads %s · Aktualisiert %s");
        t.put("shaders.detail.loading", "Shader-Details werden geladen...");
        t.put("resourcepacks.nav", "Ressourcenpakete");
        t.put("resourcepacks.search.title", "Ressourcenpakete suchen");
        t.put("resourcepacks.search.nameHint", "Ressourcenpaketnamen eingeben");
        t.put("resourcepacks.search.loading", "Ressourcenpakete werden gesucht...");
        t.put("resourcepacks.search.empty", "Keine passenden Ressourcenpakete gefunden");
        t.put("resourcepacks.search.failed", "Ressourcenpakete konnten nicht geladen werden: %s");
        t.put("resourcepacks.list.meta", "Versionen: %s · Downloads %s · Aktualisiert %s");
        t.put("resourcepacks.detail.loading", "Ressourcenpaket-Details werden geladen...");
        t.put("mods.search.title", "Mods suchen");
        t.put("mods.search.name", "Name");
        t.put("mods.search.nameHint", "Mod-Namen eingeben");
        t.put("mods.search.source", "Quelle");
        t.put("mods.search.gameVersion", "Spielversion");
        t.put("mods.search.versionHint", "z. B. 1.20.1; leer lassen für alle");
        t.put("mods.search.loader", "Loader");
        t.put("mods.search.allSources", "Alle Quellen");
        t.put("mods.search.allLoaders", "Alle Loader");
        t.put("mods.search.button", "Suchen");
        t.put("mods.search.reset", "Filter zurücksetzen");
        t.put("mods.search.loading", "Mods werden gesucht...");
        t.put("mods.search.empty", "Keine passenden Mods gefunden");
        t.put("mods.search.count", "%d Ergebnisse");
        t.put("mods.search.failed", "Laden der Mods fehlgeschlagen: %s");
        t.put("mods.page.previous", "Zurück");
        t.put("mods.page.next", "Weiter");
        t.put("mods.page.info", "Seite %d");
        t.put("mods.source.modrinth", "Modrinth");
        t.put("mods.source.curseforge", "CurseForge");
        t.put("mods.loader.fabric", "Fabric");
        t.put("mods.loader.forge", "Forge");
        t.put("mods.loader.neoforge", "NeoForge");
        t.put("mods.loader.quilt", "Quilt");
        t.put("mods.value.unknown", "Unbekannt");
        t.put("mods.list.meta", "Loader: %s · Versionen: %s · Downloads %s · Aktualisiert %s");
        t.put("mods.detail.loading", "Mod-Details werden geladen...");
        t.put("mods.detail.count", "%d Dateiversionen");
        t.put("mods.detail.meta", "Downloads %s · Aktualisiert %s");
        t.put("mods.detail.versionFilter", "Spielversion");
        t.put("mods.detail.loaderFilter", "Loader");
        t.put("mods.detail.allVersions", "Alle Versionen");
        t.put("mods.detail.allLoaders", "Alle Loader");
        t.put("mods.detail.empty", "Keine Versionen entsprechen den aktuellen Filtern");
        t.put("mods.detail.versionMeta", "Veröffentlicht %s · Downloads %s · Datei %s");
        t.put("mods.detail.download", "Herunterladen");
        t.put("mods.detail.downloading", "Lade %s herunter...");
        t.put("mods.detail.downloaded", "Heruntergeladen nach: %s");
        t.put("mods.detail.noGameDirectory", "Lege zuerst das Spielverzeichnis fest");
        t.put("mods.detail.noInstance", "Wähle zuerst eine Spielinstanz");
        t.put("mods.detail.failed", "Vorgang fehlgeschlagen: %s");
        t.put("mods.detail.dependencies", "Abhängigkeiten:");
        t.put("mods.detail.noDependencies", "Keine Abhängigkeiten");
        t.put("mods.dependency.required", "Erforderlich");
        t.put("mods.dependency.optional", "Optional");
        t.put("mods.dependency.incompatible", "Inkompatibel");
        t.put("mods.dependency.embedded", "Eingebettet");
        t.put("mods.dependency.unknown", "Unbekannt");
        t.put("mod.error.curseforgeKeyMissing", "Der CurseForge-API-Schlüssel ist nicht konfiguriert. Setze CURSEFORGE_API_KEY oder -Dcaml.curseforge.apiKey.");
        t.put("mod.error.downloadUrlMissing", "Für diese Mod-Version ist keine offizielle Download-URL verfügbar: %s");
        t.put("mod.error.projectNotFound", "Mod-Projekt nicht gefunden: %s");
        t.put("mod.error.iconUrlInvalid", "Ungültige Mod-Symbol-URL: %s");
        t.put("mod.error.iconEmpty", "Das Mod-Symbol ist leer: %s");

        // Hintergrundvorgänge
        t.put("operation.inspectGameDirectory", "Spielverzeichnis prüfen");
        t.put("operation.inspectInstance", "Instanzinformationen lesen");
        t.put("operation.openInstanceFolder", "Instanzordner öffnen");
        t.put("operation.loadInstanceModIcon", "Mod-Symbol laden");
        t.put("operation.setModDisabled", "Mod-Status ändern");
        t.put("operation.setResourcePackDisabled", "Ressourcenpaketstatus ändern");
        t.put("operation.checkModUpdates", "Mod-Updates prüfen");
        t.put("operation.updateMod", "Mod %s aktualisieren");
        t.put("operation.checkInstanceName", "Instanznamen prüfen");
        t.put("operation.fetchVersionList", "Offizielle %s-Liste abrufen");
        t.put("operation.installVanilla", "Vanilla-Instanz %s installieren");
        t.put("operation.fetchFabricLoader", "Fabric-Loader-Versionen abrufen");
        t.put("operation.fetchFabricApi", "Fabric-API-Versionen abrufen");
        t.put("operation.installFabric", "Fabric-Instanz %s installieren");
        t.put("operation.fetchForge", "Forge-Versionen abrufen");
        t.put("operation.fetchNeoForge", "NeoForge-Versionen abrufen");
        t.put("operation.fetchOptiFine", "OptiFine-Versionen abrufen");
        t.put("operation.installLoader", "%s-Instanz %s installieren");
        t.put("operation.scanJava", "Java scannen");
        t.put("operation.addManualJava", "Manuelles Java hinzufügen");
        t.put("operation.saveJavaSelection", "Java-Auswahl speichern");
        t.put("operation.saveMaxMemory", "Maximalen Arbeitsspeicher speichern");
        t.put("operation.saveJvmArguments", "JVM-Argumente speichern");
        t.put("operation.saveGameDirectory", "Spielverzeichnis speichern");
        t.put("operation.saveWindowState", "Fensterstatus speichern");
        t.put("operation.saveVersionIsolation", "Versionsisolierung speichern");
        t.put("operation.saveAutoGameLanguage", "Automatische Spielsprache speichern");
        t.put("operation.saveDownloadConcurrency", "Download-Parallelität speichern");
        t.put("operation.saveDownloadSource", "Download-Quelle speichern");
        t.put("operation.saveTheme", "Theme-Auswahl speichern");
        t.put("operation.saveLanguage", "Spracheinstellung speichern");
        t.put("operation.saveLanguageFallback", "Sprach-Fallback speichern");
        t.put("operation.searchMods", "Mods suchen: %s");
        t.put("operation.fetchModProject", "Mod-Projekt abrufen: %s");
        t.put("operation.downloadMod", "Mod herunterladen: %s");
        t.put("operation.searchShaders", "Shader suchen: %s");
        t.put("operation.fetchShaderProject", "Shader-Projekt abrufen: %s");
        t.put("operation.downloadShader", "Shader herunterladen: %s");
        t.put("operation.searchResourcePacks", "Ressourcenpakete suchen: %s");
        t.put("operation.fetchResourcePackProject", "Ressourcenpaket-Projekt abrufen: %s");
        t.put("operation.downloadResourcePack", "Ressourcenpaket herunterladen: %s");
        t.put("operation.loadModIcon", "Mod-Symbol laden");
        t.put("operation.saveLastInstance", "Zuletzt gestartete Instanz speichern");
        t.put("operation.launchGame", "Spiel %s starten");

        // Quilt-Loader
        t.put("download.description.quilt", "Quilt Loader %s");
        t.put("download.description.quiltWithApi", "Quilt Loader %s + Quilted Fabric API %s");
        t.put("download.setup.option.quilt", "Quilt-Version auswählen");
        t.put("download.setup.option.quiltApi", "Quilted-Fabric-API-Version auswählen");
        t.put("download.prompt.loading.quilt", "Quilt-Versionen werden geladen...");
        t.put("download.prompt.quiltFirst", "Zuerst eine Quilt-Version auswählen");
        t.put("download.prompt.select.quilt", "Quilt-Version auswählen");
        t.put("download.prompt.select.quiltApi", "Quilted-Fabric-API-Version auswählen");
        t.put("download.prompt.empty.quilt", "Keine Quilt-Versionen verfügbar");
        t.put("download.prompt.empty.quiltApi", "Keine Quilted-Fabric-API-Versionen verfügbar");
        t.put("download.prompt.error.quilt", "Quilt-Versionen konnten nicht geladen werden");
        t.put("download.prompt.error.quiltApi", "Quilted-Fabric-API-Versionen konnten nicht geladen werden");
        t.put("download.stage.quilt.config.fetch", "Quilt-Konfiguration wird abgerufen");
        t.put("download.stage.quilt.loader.install", "Quilt Loader wird installiert");
        t.put("download.stage.quilt.api.download", "Quilted Fabric API wird heruntergeladen");
        t.put("download.stage.quilt.verify", "Quilt-Installation wird geprüft");
        t.put("log.download.installed.quilt", "[Download] Quilt-Instanz %s installiert (Loader %s)");
        t.put("operation.fetchQuiltLoader", "Quilt-Loader-Versionen abrufen");
        t.put("operation.fetchQuiltApi", "Quilted-Fabric-API-Versionen abrufen");
        t.put("operation.installQuilt", "Quilt-Instanz %s installieren");
    }
}

