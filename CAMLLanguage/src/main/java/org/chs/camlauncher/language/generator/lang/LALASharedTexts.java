package org.chs.camlauncher.language.generator.lang;

import org.chs.camlauncher.language.generator.TranslationTable;

/** Textus Latini communes: rationes, optiones, munera. */
final class LALASharedTexts {
    private LALASharedTexts() {
    }

    static void populate(TranslationTable t) {
        // Rationes
        t.put("account.choice.default", "Rationem praedefinitam sequi");
        t.put("account.choice.offline", "Nomen usoris sine rete");
        t.put("account.default.saved", "%s nunc est ratio praedefinita");
        t.put("account.error.passwordRequired", "Tesseram inscribe");
        t.put("account.error.serverRequired", "Inscribe domicilium servi authentications");
        t.put("account.error.usernameRequired", "Inscribe nomen usoris");
        t.put("account.injector.builtin", "Versione inclusa uti (si opus est, deprompta)");
        t.put("account.injector.chooserTitle", "Elige authlib-injector jar");
        t.put("account.injector.custom", "Documentum proprium: %s");
        t.put("account.injector.reset", "Versione inclusa uti");
        t.put("account.injector.saved", "Configuratio authlib-injector servata");
        t.put("account.instance.count",
                "%d instantiae sunt; elige rationem qua quaeque incipiat");
        t.put("account.instance.loading", "Rationes leguntur...");
        t.put("account.instance.mismatch",
                "Quaedam rationes instantiarum ad %s adligatae erant et hic non iam valent (%d instantiae)");
        t.put("account.instance.mismatchApplied",
                "Adligationes rationum instantiarum (%d) huic loco ludi impositae");
        t.put("account.instance.mismatchApply", "Huic loco ludi imponere");
        t.put("account.instance.saved", "Ratio instantiae %s servata");
        t.put("account.instance.title", "Rationes instantiarum");
        t.put("account.list.default", "Praedefinita");
        t.put("account.list.empty",
                "Nullae adhuc rationes. Adde rationem sine rete, intra per Microsoft, aut intra in servo "
                        + "alieno.");
        t.put("account.list.needsLogin", "Introitus exspiravit");
        t.put("account.list.relogin", "Iterum intra");
        t.put("account.list.remove", "Tollere");
        t.put("account.login.customServer", "Domicilium servi proprium");
        t.put("account.login.customServerOption", "Domicilium proprium");
        t.put("account.login.hint", "Tessera tantum huic introitui inservit nec umquam servatur.");
        t.put("account.login.password", "Tessera");
        t.put("account.login.reloginHint",
                "Tesseram pro %s iterum inscribe. Inceptor tesseris numquam servat.");
        t.put("account.login.remember", "Me meminisse");
        t.put("account.login.running", "Intratur...");
        t.put("account.login.server", "Servus authentications");
        t.put("account.login.submit", "Intrare");
        t.put("account.login.success", "Intrasti ut %s");
        t.put("account.login.username", "Inscriptio cursualis aut nomen usoris");
        t.put("account.microsoft.codeReady",
                "Aperi paginam confirmationis in navigatro et codicem descriptum inscribe.");
        t.put("account.microsoft.copyCode", "Codicem describere");
        t.put("account.microsoft.copyUrl", "Paginam confirmationis describere");
        t.put("account.microsoft.login", "Per Microsoft intrare");
        t.put("account.microsoft.running", "Codex instrumenti Microsoft petitur...");
        t.put("account.microsoft.userCode", "Codex instrumenti: %s");
        t.put("account.microsoft.verificationUri", "Pagina confirmationis: %s");
        t.put("account.offline.add", "Addere");
        t.put("account.offline.added", "Ratio sine rete %s addita");
        t.put("account.offline.label", "Rationem sine rete addere");
        t.put("account.offline.placeholder", "Nomen usoris sine rete");
        t.put("account.profile.label", "Persona");
        t.put("account.profile.switch", "Mutare");
        t.put("account.profile.switched", "Persona mutata in %s");
        t.put("account.removed", "%s sublata");

        // Errores authentications
        t.put("auth.error.accountMissing", "Ratio non inventa: %s");
        t.put("auth.error.accountRequired", "Primum elige rationem");
        t.put("auth.error.injectorChecksum", "Probata authlib-injector defecit: %s");
        t.put("auth.error.injectorNotFound", "authlib-injector non inventus: %s");
        t.put("auth.error.invalidCredentials", "Nomen usoris aut tessera pro %s falsa");
        t.put("auth.error.loginExpired", "Introitus %s (%s) exspiravit, iterum intra");
        t.put("auth.error.loginRequired",
                "Ratio %s (%s) nullas tesseras servatas habet, iterum intra");
        t.put("auth.error.metadataUnavailable",
                "Metadata servi authentications legi non possunt: %s (HTTP %s)");
        t.put("auth.error.noAccount",
                "Nulla ratio configurata est. Adde in optionibus aut utere nomine usoris sine rete.");
        t.put("auth.error.noSelectedProfile", "Haec ratio personam Minecraft non habet: %s");
        t.put("auth.error.passwordRequired", "Tessera requiritur");
        t.put("auth.error.profileMissing", "Illa persona non iam praesto est: %s");
        t.put("auth.error.requestFailed", "Petitio authentications defecit: %s (HTTP %s)");
        t.put("auth.error.responseInvalid", "Servus authentications responsum inopinatum reddidit: %s");
        t.put("auth.error.tooManyRequests",
                "Nimis multae petitiones ad %s, paulisper morare et retempta");
        t.put("auth.error.usernameRequired", "Nomen usoris requiritur");

        // Errores Microsoft
        t.put("auth.microsoft.accountSuspended", "Haec ratio Microsoft suspensa esse videtur.");
        t.put("auth.microsoft.clientIdMissing",
                "Introitus Microsoft non est configuratus: constitue client ID OAuth Microsoft CAML.");
        t.put("auth.microsoft.forbidden",
                "Rete praesens a Minecraft Services reprobatum est. Utere alio rete.");
        t.put("auth.microsoft.loginDeclined", "Introitus Microsoft reprobatus est.");
        t.put("auth.microsoft.loginExpired", "Introitus Microsoft exspiravit. Iterum intra.");
        t.put("auth.microsoft.loginFailed", "Introitus Microsoft defecit: %s");
        t.put("auth.microsoft.loginTimeout", "Codex instrumenti Microsoft exspiravit. Retempta.");
        t.put("auth.microsoft.missingField", "In responsione authentications Microsoft deest %s.");
        t.put("auth.microsoft.noLicense",
                "Haec ratio Minecraft: Java Edition non possidet aut beneficium Game Pass exspiravit.");
        t.put("auth.microsoft.noProfile",
                "Haec ratio personam Minecraft non habet. Fac personam et iterum intra.");
        t.put("auth.microsoft.profileChanged",
                "Persona Minecraft mutata est dum ratio refricatur. Iterum intra.");
        t.put("auth.microsoft.requestFailed",
                "Petitio authentications Microsoft ad %s defecit (HTTP %s).");
        t.put("auth.microsoft.responseInvalid",
                "Responsum authentications Microsoft inopinatum a %s.");
        t.put("auth.microsoft.serviceUnavailable",
                "Minecraft Services ad tempus non praesto est. Retempta postea.");
        t.put("auth.microsoft.tooManyRequests",
                "Nimis multi conatus intrandi. Paucis minutis morare et retempta.");
        t.put("auth.microsoft.wrongVerifyMethod",
                "In pagina introitus Microsoft tessera utere, non alia via intrandi.");
        t.put("auth.microsoft.xboxBanned", "Haec ratio Microsoft ab Xbox Live interdicta est.");
        t.put("auth.microsoft.xboxFamily",
                "Haec ratio puerilis primum familiae Microsoft addenda est.");
        t.put("auth.microsoft.xboxNoAccount",
                "Haec ratio Microsoft nullam rationem Xbox habet. Fac unam et retempta.");
        t.put("auth.microsoft.xboxRegion",
                "Xbox Live in hac terra aut regione non praesto est.");
        t.put("auth.microsoft.xboxUnknown", "Xbox Live introitum repudiavit (XErr %s).");

        // Communia
        t.put("common.close", "Claudere");
        t.put("common.reset", "Restituere");

        // Fines extractionis
        t.put("download.settings.archive.description",
                "Fines adhibiti cum sarcinae modorum et mundi explicantur.");
        t.put("download.settings.archive.entries", "Numerus maximus elementorum");
        t.put("download.settings.archive.entrySize", "Magnitudo maxima elementi (MiB)");
        t.put("download.settings.archive.invalid",
                "Fines validos inscribe. Magnitudo tota non minor esse debet quam finis elementi.");
        t.put("download.settings.archive.saved", "Fines extractionis servati.");
        t.put("download.settings.archive.title", "Fines extractionis");
        t.put("download.settings.archive.totalSize", "Magnitudo tota maxima explicata (MiB)");

        // Domus
        t.put("home.account", "Ratio");
        t.put("home.error.loadAccounts", "Rationes legi non potuerunt: %s");
        t.put("home.manageAccounts", "Rationes instantiarum");
        t.put("home.username.managed", "Nomen usoris ex ratione selecta venit: %s");

        // Monita incepti
        t.put("launch.warning.java.force", "Nihilo minus incipere");
        t.put("launch.warning.java.message",
                "Hic ludus Java %s postulat, sed Java %s selecta est. Iniectus coactus deficere potest.");
        t.put("launch.warning.java.stop", "Desistere");
        t.put("launch.warning.java.title", "Versio Java fortasse non congruit");

        // Ephemeris
        t.put("log.collapse", "Complicare");
        t.put("log.empty", "Nulla adhuc actio");
        t.put("log.expand", "Explicare");

        // Sarcinae modorum
        t.put("modpack.error.invalidArchive", "Archivum sarcinae invalidum: %s");
        t.put("modpack.error.invalidFile", "Semita documenti sarcinae invalida: %s");
        t.put("modpack.error.missingFileUrl", "Documento sarcinae deest nexus depromendi: %s");
        t.put("modpack.error.missingMinecraft", "Sarcina versionem Minecraft non declarat");
        t.put("modpack.error.unsupportedLoader", "Onus sarcinae non adiutum: %s");
        t.put("modpacks.detail.install", "Sarcinam instituere");
        t.put("modpacks.detail.installed", "Sarcina instituta: %s");
        t.put("modpacks.detail.installing", "Sarcina %s instituitur...");
        t.put("modpacks.detail.loading", "Singula sarcinae leguntur...");
        t.put("modpacks.install.instanceName", "Nomen instantiae");
        t.put("modpacks.install.instanceNameEmpty", "Nomen instantiae vacuum esse non potest");
        t.put("modpacks.list.meta", "Versio: %s · Depromptiones %s · Renovata %s");
        t.put("modpacks.nav", "Sarcinae modorum");
        t.put("modpacks.search.empty", "Nullae sarcinae congruentes inventae");
        t.put("modpacks.search.failed", "Sarcinae legi non potuerunt: %s");
        t.put("modpacks.search.loading", "Sarcinae quaeruntur...");
        t.put("modpacks.search.nameHint", "Inscribe nomen sarcinae");
        t.put("modpacks.search.title", "Sarcinas modorum quaerere");

        // Nomina operationum
        t.put("operation.addOfflineAccount", "Rationem sine rete addere");
        t.put("operation.downloadJava", "Java %s depromere et instituere");
        t.put("operation.fetchAuthServer", "Servum authentications petere");
        t.put("operation.fetchJavaDownloadList", "Catalogum depromendarum Java petere: %s");
        t.put("operation.fetchModpackProject", "Propositum sarcinae petere %s");
        t.put("operation.installModpack", "Sarcinam %s instituere");
        t.put("operation.loadAccounts", "Rationes legere");
        t.put("operation.loadInstanceAccount", "Rationem instantiae legere");
        t.put("operation.loginAccount", "In rationem intrare");
        t.put("operation.loginMicrosoftAccount", "Per Microsoft intrare");
        t.put("operation.openJavaFolder", "Scrinium Java aperire");
        t.put("operation.removeAccount", "Rationem tollere");
        t.put("operation.saveArchiveExtractionLimits", "Fines extractionis servare");
        t.put("operation.saveDefaultAccount", "Rationem praedefinitam servare");
        t.put("operation.saveInjectorPath", "Semitam authlib-injector servare");
        t.put("operation.saveInstanceAccount", "Rationem instantiae servare");
        t.put("operation.searchModpacks", "Sarcinas quaerere %s");
        t.put("operation.switchProfile", "Personam rationis mutare");

        // Optiones: de hoc programmate
        t.put("settings.about.application.title", "Inceptor");
        t.put("settings.about.architecture", "Architectura");
        t.put("settings.about.backend", "Posticum");
        t.put("settings.about.copyright", "Ius auctoris");
        t.put("settings.about.dataDirectory", "Scrinium datorum");
        t.put("settings.about.dependencies.title", "Dependentiae propositi");
        t.put("settings.about.dependency.author", "Auctor: %s");
        t.put("settings.about.dependency.license", "Licentia: %s");
        t.put("settings.about.device.title", "Singula instrumenti");
        t.put("settings.about.directories.title", "Scrinia");
        t.put("settings.about.gameDirectory", "Scrinium ludi");
        t.put("settings.about.javaVendor", "Venditor Java");
        t.put("settings.about.javaVersion", "Versio Java");
        t.put("settings.about.kernelVersion", "Versio nuclei");
        t.put("settings.about.license", "Licentia");
        t.put("settings.about.memory", "Memoria systematis");
        t.put("settings.about.notDetected", "Non detecta");
        t.put("settings.about.os", "Systema operandi");
        t.put("settings.about.osVersion", "Versio systematis");
        t.put("settings.about.project.title", "Propositum");
        t.put("settings.about.projectName", "Nomen");
        t.put("settings.about.runtime.title", "Ambientia inceptoris");
        t.put("settings.about.system.title", "Systema");
        t.put("settings.about.unknown", "Ignotum");
        t.put("settings.about.version", "Versio");

        // Optiones: rationes
        t.put("settings.account.description",
                "Rationes ab inceptore servatae. Tessera accessus ante scriptionem xcriptantur; "
                        + "tesserae numquam servantur.");
        t.put("settings.account.injector.description",
                "Inceptor authlib-injector semel depromit et deinde utitur; documentum omni inceptu SHA-256 "
                        + "probatur.");
        t.put("settings.account.login.description",
                "Intra ratione servi authentications Yggdrasil (exempli gratia LittleSkin).");
        t.put("settings.account.login.title", "Introitus alienus");
        t.put("settings.account.microsoft.description",
                "Per Microsoft intra codice instrumenti. Tessera in inceptore numquam inscribitur.");
        t.put("settings.account.microsoft.title", "Ratio Microsoft");
        t.put("settings.account.title", "Rationes");

        // Optiones: administratio Java
        t.put("settings.javaManagement.count", "%d ambientiae Java");
        t.put("settings.javaManagement.distribution", "Distributio");
        t.put("settings.javaManagement.download", "Java depromere");
        t.put("settings.javaManagement.downloadAndInstall", "Depromere et instituere");
        t.put("settings.javaManagement.downloading", "Java depromitur...");
        t.put("settings.javaManagement.downloadingProgress", "Java depromitur... %d%%");
        t.put("settings.javaManagement.empty", "Nullae ambientiae Java inventae");
        t.put("settings.javaManagement.emptyVersions", "Nullae versiones Java depromendae inventae");
        t.put("settings.javaManagement.installed", "Java instituta: %s");
        t.put("settings.javaManagement.installFailed", "Java institui non potuit: %s");
        t.put("settings.javaManagement.linuxHint",
                "In Linux institue JDK aut JRE per administratorem fasciculorum distributionis (ut apt, dnf "
                        + "aut pacman). Java ab CAML administrata non commendatur.");
        t.put("settings.javaManagement.loadingVersions", "Versiones Java leguntur...");
        t.put("settings.javaManagement.loadVersionsFailed",
                "Versiones Java legi non potuerunt: %s");
        t.put("settings.javaManagement.manual", "Manu addita");
        t.put("settings.javaManagement.meta", "%s · %d bit%s");
        t.put("settings.javaManagement.openFolder", "Scrinium continens aperire");
        t.put("settings.javaManagement.openFolder.failed", "Scrinium Java aperiri non potuit: %s");
        t.put("settings.javaManagement.openFolder.opening", "Scrinium aperitur...");
        t.put("settings.javaManagement.packageType", "Genus fasciculi");
        t.put("settings.javaManagement.unavailable", "Non praesto");
        t.put("settings.javaManagement.version", "Versio");
        t.put("settings.javaManagement.versionCount", "%d versiones");

        // Optiones: sectiones
        t.put("settings.section.about", "De hoc programmate");
        t.put("settings.section.account", "Rationes");
        t.put("settings.section.java", "Administratio Java");

        // Munera
        t.put("tasks.back", "Ad munera redire");
        t.put("tasks.empty", "Nulla adhuc munera");
        t.put("tasks.open", "Munus %s aperire · %s");
        t.put("tasks.status.cancelled", "Abrogatum");
        t.put("tasks.status.completed", "Perfectum");
        t.put("tasks.status.failed", "Defecit");
        t.put("tasks.status.running", "Currit");
        t.put("tasks.subtitle", "Depromptiones et institutiones");
        t.put("tasks.title", "Munera");

        // Navigatio et tabulae
    }
}

