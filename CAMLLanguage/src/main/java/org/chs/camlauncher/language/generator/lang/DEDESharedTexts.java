package org.chs.camlauncher.language.generator.lang;

import org.chs.camlauncher.language.generator.TranslationTable;

/** Deutsche gemeinsame Texte für Konten, Einstellungen und Aufgaben. */
final class DEDESharedTexts {
    private DEDESharedTexts() {
    }

    static void populate(TranslationTable t) {
        // Konten
        t.put("account.choice.default", "Standardkonto folgen");
        t.put("account.choice.offline", "Offline-Benutzername");
        t.put("account.default.saved", "%s ist jetzt das Standardkonto");
        t.put("account.error.passwordRequired", "Bitte das Passwort eingeben");
        t.put("account.error.serverRequired", "Bitte die Adresse des Authentifizierungsservers eingeben");
        t.put("account.error.usernameRequired", "Bitte den Benutzernamen eingeben");
        t.put("account.injector.builtin", "Mitgelieferte Version verwenden (bei Bedarf heruntergeladen)");
        t.put("account.injector.chooserTitle", "authlib-injector-JAR auswählen");
        t.put("account.injector.custom", "Eigene Datei: %s");
        t.put("account.injector.reset", "Mitgelieferte Version verwenden");
        t.put("account.injector.saved", "authlib-injector-Einstellung gespeichert");
        t.put("account.instance.count",
                "%d Instanzen; für jede Instanz das Konto zum Starten auswählen");
        t.put("account.instance.loading", "Konten werden geladen ...");
        t.put("account.instance.mismatch",
                "Einige Instanzkonten waren an %s gebunden und gelten hier nicht mehr (%d Instanzen)");
        t.put("account.instance.mismatchApplied",
                "%d Instanzkonto-Zuordnungen auf diesen Spielort angewendet");
        t.put("account.instance.mismatchApply", "Auf diesen Spielort anwenden");
        t.put("account.instance.saved", "Konto für Instanz %s gespeichert");
        t.put("account.instance.title", "Instanzkonten");
        t.put("account.list.default", "Standard");
        t.put("account.list.empty",
                "Noch keine Konten. Füge ein Offline-Konto hinzu, melde dich bei Microsoft an oder melde dich "
                        + "bei einem Drittanbieterserver an.");
        t.put("account.list.needsLogin", "Anmeldung abgelaufen");
        t.put("account.list.relogin", "Erneut anmelden");
        t.put("account.list.remove", "Entfernen");
        t.put("account.login.customServer", "Eigene Serveradresse");
        t.put("account.login.customServerOption", "Eigene Adresse");
        t.put("account.login.hint", "Das Passwort wird nur für diese Anmeldung verwendet und nie gespeichert.");
        t.put("account.login.password", "Passwort");
        t.put("account.login.reloginHint",
                "Gib das Passwort für %s erneut ein. Der Launcher speichert niemals Passwörter.");
        t.put("account.login.remember", "Angemeldet bleiben");
        t.put("account.login.running", "Anmeldung läuft ...");
        t.put("account.login.server", "Authentifizierungsserver");
        t.put("account.login.submit", "Anmelden");
        t.put("account.login.success", "Angemeldet als %s");
        t.put("account.login.username", "E-Mail oder Benutzername");
        t.put("account.microsoft.codeReady",
                "Öffne die Bestätigungsseite im Browser und gib den kopierten Code ein.");
        t.put("account.microsoft.copyCode", "Code kopieren");
        t.put("account.microsoft.copyUrl", "Bestätigungsseite kopieren");
        t.put("account.microsoft.login", "Mit Microsoft anmelden");
        t.put("account.microsoft.running", "Microsoft-Gerätecode wird angefordert ...");
        t.put("account.microsoft.userCode", "Gerätecode: %s");
        t.put("account.microsoft.verificationUri", "Bestätigungsseite: %s");
        t.put("account.offline.add", "Hinzufügen");
        t.put("account.offline.added", "Offline-Konto %s hinzugefügt");
        t.put("account.offline.label", "Offline-Konto hinzufügen");
        t.put("account.offline.placeholder", "Offline-Benutzername");
        t.put("account.profile.label", "Profil");
        t.put("account.profile.switch", "Wechseln");
        t.put("account.profile.switched", "Profil gewechselt zu %s");
        t.put("account.removed", "%s entfernt");

        // Anmeldefehler
        t.put("auth.error.accountMissing", "Konto nicht gefunden: %s");
        t.put("auth.error.accountRequired", "Bitte zuerst ein Konto auswählen");
        t.put("auth.error.injectorChecksum", "authlib-injector-Prüfung fehlgeschlagen: %s");
        t.put("auth.error.injectorNotFound", "authlib-injector nicht gefunden: %s");
        t.put("auth.error.invalidCredentials", "Benutzername oder Passwort für %s ist falsch");
        t.put("auth.error.loginExpired",
                "Die Anmeldung von %s (%s) ist abgelaufen, bitte erneut anmelden");
        t.put("auth.error.loginRequired",
                "Konto %s (%s) hat keine gespeicherten Zugangsdaten, bitte erneut anmelden");
        t.put("auth.error.metadataUnavailable",
                "Die Metadaten des Authentifizierungsservers können nicht gelesen werden: %s (HTTP %s)");
        t.put("auth.error.noAccount",
                "Es ist kein Konto konfiguriert. Füge eines in den Einstellungen hinzu oder verwende einen "
                        + "Offline-Benutzernamen.");
        t.put("auth.error.noSelectedProfile", "Dieses Konto hat kein Minecraft-Profil: %s");
        t.put("auth.error.passwordRequired", "Passwort ist erforderlich");
        t.put("auth.error.profileMissing", "Dieses Profil ist nicht mehr verfügbar: %s");
        t.put("auth.error.requestFailed", "Authentifizierungsanfrage fehlgeschlagen: %s (HTTP %s)");
        t.put("auth.error.responseInvalid",
                "Der Authentifizierungsserver hat eine unerwartete Antwort geliefert: %s");
        t.put("auth.error.tooManyRequests",
                "Zu viele Anfragen an %s, bitte kurz warten und erneut versuchen");
        t.put("auth.error.usernameRequired", "Benutzername ist erforderlich");

        // Microsoft-Anmeldefehler
        t.put("auth.microsoft.accountSuspended", "Dieses Microsoft-Konto scheint gesperrt zu sein.");
        t.put("auth.microsoft.clientIdMissing",
                "Microsoft-Anmeldung ist nicht konfiguriert: CAML Microsoft-OAuth-Client-ID festlegen.");
        t.put("auth.microsoft.forbidden",
                "Das aktuelle Netzwerk wurde von Minecraft Services abgelehnt. Versuche ein anderes Netzwerk.");
        t.put("auth.microsoft.loginDeclined", "Die Microsoft-Anmeldung wurde abgelehnt.");
        t.put("auth.microsoft.loginExpired", "Die Microsoft-Anmeldung ist abgelaufen. Bitte erneut anmelden.");
        t.put("auth.microsoft.loginFailed", "Microsoft-Anmeldung fehlgeschlagen: %s");
        t.put("auth.microsoft.loginTimeout", "Der Microsoft-Gerätecode ist abgelaufen. Bitte erneut versuchen.");
        t.put("auth.microsoft.missingField", "In der Microsoft-Antwort fehlt %s.");
        t.put("auth.microsoft.noLicense",
                "Dieses Konto besitzt Minecraft: Java Edition nicht, oder die Game-Pass-Berechtigung ist "
                        + "abgelaufen.");
        t.put("auth.microsoft.noProfile",
                "Dieses Konto hat kein Minecraft-Profil. Erstelle eines und melde dich erneut an.");
        t.put("auth.microsoft.profileChanged",
                "Das Minecraft-Profil hat sich beim Aktualisieren des Kontos geändert. Bitte erneut anmelden.");
        t.put("auth.microsoft.requestFailed",
                "Microsoft-Authentifizierungsanfrage bei %s fehlgeschlagen (HTTP %s).");
        t.put("auth.microsoft.responseInvalid",
                "Unerwartete Microsoft-Authentifizierungsantwort von %s.");
        t.put("auth.microsoft.serviceUnavailable",
                "Minecraft Services ist vorübergehend nicht verfügbar. Versuche es später erneut.");
        t.put("auth.microsoft.tooManyRequests",
                "Zu viele Anmeldeversuche. Warte einige Minuten und versuche es erneut.");
        t.put("auth.microsoft.wrongVerifyMethod",
                "Verwende auf der Microsoft-Anmeldeseite dein Passwort statt einer anderen Anmeldemethode.");
        t.put("auth.microsoft.xboxBanned", "Dieses Microsoft-Konto ist bei Xbox Live gesperrt.");
        t.put("auth.microsoft.xboxFamily",
                "Dieses Kinderkonto muss zuerst einer Microsoft-Familie hinzugefügt werden.");
        t.put("auth.microsoft.xboxNoAccount",
                "Dieses Microsoft-Konto hat kein Xbox-Konto. Erstelle eines und versuche es erneut.");
        t.put("auth.microsoft.xboxRegion",
                "Xbox Live ist im aktuellen Land oder in der aktuellen Region nicht verfügbar.");
        t.put("auth.microsoft.xboxUnknown", "Xbox Live hat die Anmeldung abgelehnt (XErr %s).");

        // Allgemein
        t.put("common.close", "Schließen");
        t.put("common.reset", "Zurücksetzen");

        // Grenzen beim Entpacken
        t.put("download.settings.archive.description",
                "Grenzen, die beim Entpacken von Modpacks und Welten gelten.");
        t.put("download.settings.archive.entries", "Maximale Anzahl Einträge");
        t.put("download.settings.archive.entrySize", "Maximale Größe eines Eintrags (MiB)");
        t.put("download.settings.archive.invalid",
                "Gültige Grenzen eingeben. Die Gesamtgröße muss mindestens dem Eintragslimit entsprechen.");
        t.put("download.settings.archive.saved", "Grenzen für das Entpacken gespeichert.");
        t.put("download.settings.archive.title", "Grenzen beim Entpacken");
        t.put("download.settings.archive.totalSize", "Maximale Gesamtgröße entpackt (MiB)");

        // Startseite
        t.put("home.account", "Konto");
        t.put("home.error.loadAccounts", "Konten konnten nicht geladen werden: %s");
        t.put("home.manageAccounts", "Instanzkonten");
        t.put("home.username.managed", "Der Benutzername stammt aus dem gewählten Konto: %s");

        // Startwarnungen
        t.put("launch.warning.java.force", "Trotzdem starten");
        t.put("launch.warning.java.message",
                "Dieses Spiel benötigt Java %s, ausgewählt ist jedoch Java %s. Ein erzwungener Start kann "
                        + "fehlschlagen.");
        t.put("launch.warning.java.stop", "Abbrechen");
        t.put("launch.warning.java.title", "Java-Version ist möglicherweise inkompatibel");

        // Protokoll
        t.put("log.collapse", "Einklappen");
        t.put("log.empty", "Noch keine Aktivität");
        t.put("log.expand", "Ausklappen");

        // Modpacks
        t.put("modpack.error.invalidArchive", "Ungültiges Modpack-Archiv: %s");
        t.put("modpack.error.invalidFile", "Ungültiger Modpack-Dateipfad: %s");
        t.put("modpack.error.missingFileUrl", "Modpack-Datei hat keine Download-URL: %s");
        t.put("modpack.error.missingMinecraft", "Das Modpack gibt keine Minecraft-Version an");
        t.put("modpack.error.unsupportedLoader", "Nicht unterstützter Modpack-Loader: %s");
        t.put("modpacks.detail.install", "Modpack installieren");
        t.put("modpacks.detail.installed", "Modpack installiert: %s");
        t.put("modpacks.detail.installing", "Modpack %s wird installiert ...");
        t.put("modpacks.detail.loading", "Modpack-Details werden geladen ...");
        t.put("modpacks.install.instanceName", "Instanzname");
        t.put("modpacks.install.instanceNameEmpty", "Der Instanzname darf nicht leer sein");
        t.put("modpacks.list.meta", "Version: %s · Downloads %s · Aktualisiert %s");
        t.put("modpacks.nav", "Modpacks");
        t.put("modpacks.search.empty", "Keine passenden Modpacks gefunden");
        t.put("modpacks.search.failed", "Modpacks konnten nicht geladen werden: %s");
        t.put("modpacks.search.loading", "Modpacks werden gesucht ...");
        t.put("modpacks.search.nameHint", "Modpack-Namen eingeben");
        t.put("modpacks.search.title", "Modpacks suchen");

        // Vorgangsnamen
        t.put("operation.addOfflineAccount", "Offline-Konto hinzufügen");
        t.put("operation.downloadJava", "Java %s herunterladen und installieren");
        t.put("operation.fetchAuthServer", "Authentifizierungsserver abrufen");
        t.put("operation.fetchJavaDownloadList", "Java-Downloadliste abrufen: %s");
        t.put("operation.fetchModpackProject", "Modpack-Projekt abrufen %s");
        t.put("operation.installModpack", "Modpack %s installieren");
        t.put("operation.loadAccounts", "Konten laden");
        t.put("operation.loadInstanceAccount", "Instanzkonto laden");
        t.put("operation.loginAccount", "Bei Konto anmelden");
        t.put("operation.loginMicrosoftAccount", "Mit Microsoft anmelden");
        t.put("operation.openJavaFolder", "Java-Ordner öffnen");
        t.put("operation.removeAccount", "Konto entfernen");
        t.put("operation.saveArchiveExtractionLimits", "Grenzen beim Entpacken speichern");
        t.put("operation.saveDefaultAccount", "Standardkonto speichern");
        t.put("operation.saveInjectorPath", "authlib-injector-Pfad speichern");
        t.put("operation.saveInstanceAccount", "Instanzkonto speichern");
        t.put("operation.searchModpacks", "Modpacks suchen %s");
        t.put("operation.switchProfile", "Kontoprofil wechseln");

        // Einstellungen: Über
        t.put("settings.about.application.title", "Launcher");
        t.put("settings.about.architecture", "Architektur");
        t.put("settings.about.backend", "Backend");
        t.put("settings.about.copyright", "Copyright");
        t.put("settings.about.dataDirectory", "Datenverzeichnis");
        t.put("settings.about.dependencies.title", "Projektabhängigkeiten");
        t.put("settings.about.dependency.author", "Autor: %s");
        t.put("settings.about.dependency.license", "Lizenz: %s");
        t.put("settings.about.device.title", "Geräteinformationen");
        t.put("settings.about.directories.title", "Verzeichnisse");
        t.put("settings.about.gameDirectory", "Spielverzeichnis");
        t.put("settings.about.javaVendor", "Java-Anbieter");
        t.put("settings.about.javaVersion", "Java-Version");
        t.put("settings.about.kernelVersion", "Kernelversion");
        t.put("settings.about.license", "Lizenz");
        t.put("settings.about.memory", "Arbeitsspeicher");
        t.put("settings.about.notDetected", "Nicht erkannt");
        t.put("settings.about.os", "Betriebssystem");
        t.put("settings.about.osVersion", "Systemversion");
        t.put("settings.about.project.title", "Projekt");
        t.put("settings.about.projectName", "Name");
        t.put("settings.about.runtime.title", "Launcher-Laufzeit");
        t.put("settings.about.system.title", "System");
        t.put("settings.about.unknown", "Unbekannt");
        t.put("settings.about.version", "Version");

        // Einstellungen: Konten
        t.put("settings.account.description",
                "Vom Launcher gespeicherte Konten. Access-Token werden vor dem Schreiben verschlüsselt; "
                        + "Passwörter werden nie gespeichert.");
        t.put("settings.account.injector.description",
                "Der Launcher lädt authlib-injector einmal herunter und verwendet ihn weiter; bei jedem Start "
                        + "wird die Datei per SHA-256 geprüft.");
        t.put("settings.account.login.description",
                "Melde dich mit einem Konto eines Yggdrasil-Authentifizierungsservers an (zum Beispiel "
                        + "LittleSkin).");
        t.put("settings.account.login.title", "Anmeldung über Drittanbieter");
        t.put("settings.account.microsoft.description",
                "Melde dich mit einem Gerätecode bei Microsoft an. Das Passwort wird nie im Launcher eingegeben.");
        t.put("settings.account.microsoft.title", "Microsoft-Konto");
        t.put("settings.account.title", "Konten");

        // Einstellungen: Java-Verwaltung
        t.put("settings.javaManagement.count", "%d Java-Laufzeiten");
        t.put("settings.javaManagement.distribution", "Distribution");
        t.put("settings.javaManagement.download", "Java herunterladen");
        t.put("settings.javaManagement.downloadAndInstall", "Herunterladen und installieren");
        t.put("settings.javaManagement.downloading", "Java wird heruntergeladen ...");
        t.put("settings.javaManagement.downloadingProgress", "Java wird heruntergeladen ... %d%%");
        t.put("settings.javaManagement.empty", "Keine Java-Laufzeiten gefunden");
        t.put("settings.javaManagement.emptyVersions", "Keine herunterladbaren Java-Versionen gefunden");
        t.put("settings.javaManagement.installed", "Java installiert: %s");
        t.put("settings.javaManagement.installFailed", "Java konnte nicht installiert werden: %s");
        t.put("settings.javaManagement.linuxHint",
                "Installiere JDK oder JRE unter Linux mit dem Paketmanager der Distribution (etwa apt, dnf "
                        + "oder pacman). Von CAML verwaltetes Java wird nicht empfohlen.");
        t.put("settings.javaManagement.loadingVersions", "Java-Versionen werden geladen ...");
        t.put("settings.javaManagement.loadVersionsFailed",
                "Java-Versionen konnten nicht geladen werden: %s");
        t.put("settings.javaManagement.manual", "Manuell hinzugefügt");
        t.put("settings.javaManagement.meta", "%s · %d Bit%s");
        t.put("settings.javaManagement.openFolder", "Enthaltenden Ordner öffnen");
        t.put("settings.javaManagement.openFolder.failed", "Java-Ordner konnte nicht geöffnet werden: %s");
        t.put("settings.javaManagement.openFolder.opening", "Ordner wird geöffnet ...");
        t.put("settings.javaManagement.packageType", "Pakettyp");
        t.put("settings.javaManagement.unavailable", "Nicht verfügbar");
        t.put("settings.javaManagement.version", "Version");
        t.put("settings.javaManagement.versionCount", "%d Versionen");

        // Einstellungen: Abschnitte
        t.put("settings.section.about", "Über");
        t.put("settings.section.account", "Konten");
        t.put("settings.section.java", "Java-Verwaltung");

        // Aufgaben
        t.put("tasks.back", "Zurück zu den Aufgaben");
        t.put("tasks.empty", "Noch keine Aufgaben");
        t.put("tasks.open", "Aufgabe %s öffnen · %s");
        t.put("tasks.status.cancelled", "Abgebrochen");
        t.put("tasks.status.completed", "Abgeschlossen");
        t.put("tasks.status.failed", "Fehlgeschlagen");
        t.put("tasks.status.running", "Läuft");
        t.put("tasks.subtitle", "Downloads und Installationen");
        t.put("tasks.title", "Aufgaben");

        // Navigation und Listen
    }
}
