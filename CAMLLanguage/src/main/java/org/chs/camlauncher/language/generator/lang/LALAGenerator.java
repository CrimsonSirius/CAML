package org.chs.camlauncher.language.generator.lang;

import org.chs.camlauncher.language.generator.LanguageTranslationGenerator;
import org.chs.camlauncher.language.generator.TranslationTable;

/** Translationes Latinae. */
public final class LALAGenerator extends LanguageTranslationGenerator {
    @Override
    protected String locale() {
        return "la_la";
    }

    @Override
    protected void populate(TranslationTable t) {
        // Communia
        t.put("common.back", "Retro");
        t.put("common.cancel", "Abrogare");
        t.put("common.browse", "Percurrere");
        t.put("common.save", "Servare");

        // Navigatio et pedis
        t.put("nav.home", "Domus");
        t.put("nav.downloads", "Deposita");
        t.put("nav.settings", "Optiones");
        t.put("app.subtitle", "Deductor Minecraft");
        t.put("footer.status", "CAML %s · Tergum %s · Directorium datorum: %s");
        t.put("footer.backend.ready", "Paratus");
        t.put("footer.backend.notReady", "Non paratus");

        // Forma deducendi
        t.put("home.username", "Nomen usoris extra rete");
        t.put("home.launch", "Ludum deducere");
        t.put("home.launching", "Deducitur...");
        t.put("home.browseInstances", "Instantias percurrere");
        t.put("home.manageInstances", "Instantias administrare");
        t.put("home.instance.current", "Instantia praesens: %s");
        t.put("home.instance.current.none", "Instantia praesens: non electa");
        t.put("home.gameDirectory.none", "Directorium ludi: non positum");
        t.put("home.gameDirectory.missing", "Directorium ludi: deest · %s");
        t.put("home.gameDirectory.value", "Directorium ludi: %s");
        t.put("home.saveLocation.none", "Locus servatorum: non positus");
        t.put("home.saveLocation.fixDirectory", "Locus servatorum: primum directorium ludi corrige");
        t.put("home.saveLocation.selectInstance", "Locus servatorum: primum instantiam elige");
        t.put("home.saveLocation.value", "Locus servatorum: %s");
        t.put("home.saveLocation.isolated", " (plena versio segregata)");
        t.put("home.error.inspectDirectory", "Inspectio directorii ludi defecit: %s");
        t.put("home.error.noGameDirectory", "Primum directorium ludi in optionibus serva");
        t.put("home.error.missingGameDirectory", "Directorium ludi non exstat; directorium validum in optionibus serva");
        t.put("home.error.noInstance", "Primum instantiam percurre et elige");
        t.put("home.error.noUsername", "Nomen usoris extra rete scribe");

        // Commentarium
        t.put("log.title", "Commentarium executionis");
        t.put("log.subtitle", "Progressus depositorum et exitus processus ludi");
        t.put("log.accessible", "Commentarium executionis");
        t.put("log.download.installed.vanilla", "[Depositum] Instantia %s installata (vanilla %s)");
        t.put("log.download.installed.fabric", "[Depositum] Instantia Fabric %s installata (Loader %s)");
        t.put("log.download.installed.loader", "[Depositum] Instantia %s %s installata");
        t.put("log.download.progress", "[Depositum] %s (%d/%d)");
        t.put("log.download.progressNoTotal", "[Depositum] %s (%d)");
        t.put("log.gameProcess.exit", "Processus ludi exiit cum codice: %d");
        t.put("log.gameProcess.exitError", "Processus ludi abnormaliter exiit cum codice: %d");
        t.put("log.error.operationFailed", "[%s] defecit: %s");

        // Status deductionis
        t.put("status.starting", "Deducitur %s...");
        t.put("status.started", "Mandatum deducendi missum");
        t.put("status.launchFailed", "Deductio defecit!");
        t.put("status.launchFailedDetail", "Deductio defecit: %s");
        t.put("error.unknown", "Error incognitus accidit. Commentarium inspice.");
        t.put("download.error.failed", "Depositum %s post %s conatus defecit (ultima inscriptio: %s)");
        t.put("network.requestFailed", "Petitio %s defecit (ultima inscriptio: %s)");
        t.put("network.timeout", "Petitio tempus excessit (%s ms): %s");
        t.put("network.httpError", "HTTP %s: %s");
        t.put("network.responseInvalid", "Responsum interpretari non potuit: %s");
        t.put("network.redirectLocationMissing", "Redirectioni deest Location: %s");
        t.put("network.tooManyRedirects", "Nimis multae redirectiones: %s");
        t.put("network.noSource", "Fons depositi nullus praesto est: %s");
        t.put("metadata.manifestEmpty", "Manifestum versionum vacuum est");
        t.put("metadata.versionJsonEmpty", "JSON versionis vacuum est");
        t.put("metadata.versionJsonHashMismatch", "Comprobatio SHA-1 JSON versionis defecit: %s");
        t.put("launch.permissionDenied", "Licentia negata ad Java exsequendum vel ad directorium ludi accedendum. Licentias fasciculorum inspice.");
        t.put("launch.executableNotFound", "Fasciculus exsecutabilis Java non inventus. Iterum Java elige.");
        t.put("launch.commandTooLong", "Mandatum deducendi nimis longum est. Semitam ludi vel classpath abbrevia.");
        t.put("launch.commandTooLongAfterFallback", "Mandatum deducendi etiam post argfile et manifestum nimis longum est. Semitam ludi vel classpath abbrevia.");
        t.put("launch.processStartFailed", "Processus ludi incipere non potuit. Commentarium inspice.");
        t.put("launch.manualJavaNotConfigured", "Electio manualis Java activa est, sed semita Java non electa est.");
        t.put("launch.manualJavaUnavailable", "Java electa praesto non est: %s");
        t.put("launch.javaRequirementNotFound", "Nulla Java requisitum versionis %s implens inventa est. Java installa vel elige.");

        // Genera et fontes
        t.put("download.category.release", "Editio");
        t.put("download.category.snapshot", "Specimen");
        t.put("download.category.old", "Vetusta");
        t.put("download.category.unknown", "Versio incognita");
        t.put("download.source.mirror", "Speculum");
        t.put("download.source.official", "Officialis");
        t.put("download.source.custom", "Proprium");

        // Index versionum
        t.put("download.versions.loading", "Index %2$s ex %1$s legitur...");
        t.put("download.versions.empty", "Fons depositi nullas versiones %s deponibiles reddidit");
        t.put("download.versions.count", "%d versiones %s, recentissimae primae");
        t.put("download.versions.error", "Index %s legi non potuit: %s");
        t.put("download.versions.accessible", "%s, %s");

        // Descriptiones
        t.put("download.description.optifine", "Forge %s + OptiFine %s");
        t.put("download.description.forge", "Forge %s");
        t.put("download.description.fabric", "Fabric Loader %s");
        t.put("download.description.fabricWithApi", "Fabric Loader %s + Fabric API %s");
        t.put("download.description.neoforge", "NeoForge %s");
        t.put("download.description.vanilla", "Minecraft %s");

        // Pagina dispositionis depositi
        t.put("download.setup.status.idle", "Ludum vanilla deponit si nullus loader electus est.");
        t.put("download.setup.instanceName", "Nomen instantiae");
        t.put("download.setup.download", "Instantiam deponere");
        t.put("download.setup.option.versionInfo", "Informationes versionis");
        t.put("download.setup.option.forge", "Versionem Forge eligere");
        t.put("download.setup.option.optifine", "Versionem OptiFine eligere");
        t.put("download.setup.option.fabric", "Versionem Fabric eligere");
        t.put("download.setup.option.fabricApi", "Versionem Fabric API eligere");
        t.put("download.setup.option.neoforge", "Versionem NeoForge eligere");
        t.put("download.setup.versionInfo", "Minecraft %s · %s");
        t.put("download.setup.versionInfoWithDate", "Minecraft %s · %s · %s");
        t.put("download.setup.optifineSwitched", "Forge et OptiFine inter se non congruunt; ad Forge %s transmissum est");
        t.put("download.setup.optifineIncompatibleForge", "Forge praesens cum OptiFine non congruit");
        t.put("download.setup.noCompatibleOptiFine", "Nulla versio OptiFine cum Forge praesente congruit");
        t.put("download.setup.optifineForgeConflict", "Forge %s cum OptiFine %s non congruit. Forge %s vel vetustiorem elige.");
        t.put("download.setup.nameEmpty", "Nomen instantiae vacuum esse non potest");
        t.put("download.setup.nameDotSegments", "Nomen instantiae . vel .. esse non potest");
        t.put("download.setup.nameTrailing", "Nomen instantiae puncto vel spatio finiri non potest");
        t.put("download.setup.nameIllegalCharacter", "Nomen instantiae litteram illicitam continet: %s");
        t.put("download.setup.nameExists", "Nomen instantiae iam exstat: %s. Aliud nomen elige.");
        t.put("download.setup.noGameDirectory", "Directorium ludi non positum est");
        t.put("download.setup.checkingName", "Nomen instantiae inspicitur...");
        t.put("download.setup.nameSameAsLoader", "Cum loader installatur, nomen instantiae eidem atque versio ludi esse non potest");
        t.put("download.setup.nameAvailable", "Nomen instantiae praesto est");
        t.put("download.setup.checkNameFailed", "Inspectio nominis instantiae defecit: %s");
        t.put("download.setup.nameNotChecked", "Nomen instantiae nondum probationem transiit; mox rursus tenta.");
        t.put("download.setup.noGameDirectoryLong", "Directorium ludi non positum est; primum in optionibus serva.");
        t.put("download.setup.optifineRequiresForge", "OptiFine solum cum Forge installari potest.");

        // Monita loader
        t.put("download.prompt.loading.fabric", "Versiones Fabric leguntur...");
        t.put("download.prompt.loading.forge", "Versiones Forge leguntur...");
        t.put("download.prompt.loading.neoforge", "Versiones NeoForge leguntur...");
        t.put("download.prompt.fabricFirst", "Primum versionem Fabric elige");
        t.put("download.prompt.forgeFirst", "Primum versionem Forge elige");
        t.put("download.prompt.fabricApiFirst", "Primum versionem Fabric elige");
        t.put("download.prompt.select.fabric", "Versionem Fabric elige");
        t.put("download.prompt.select.forge", "Versionem Forge elige");
        t.put("download.prompt.select.neoforge", "Versionem NeoForge elige");
        t.put("download.prompt.select.optifine", "Versionem OptiFine elige");
        t.put("download.prompt.select.fabricApi", "Versionem Fabric API elige");
        t.put("download.prompt.empty.fabric", "Nullae versiones Fabric praesto sunt");
        t.put("download.prompt.empty.fabricApi", "Nullae versiones Fabric API praesto sunt");
        t.put("download.prompt.empty.forge", "Nullae versiones Forge praesto sunt");
        t.put("download.prompt.empty.neoforge", "Nullae versiones NeoForge praesto sunt");
        t.put("download.prompt.empty.optifine", "Nullae versiones OptiFine praesto sunt");
        t.put("download.prompt.error.fabric", "Versiones Fabric legi non potuerunt");
        t.put("download.prompt.error.fabricApi", "Versiones Fabric API legi non potuerunt");
        t.put("download.prompt.error.forge", "Versiones Forge legi non potuerunt");
        t.put("download.prompt.error.neoforge", "Versiones NeoForge legi non potuerunt");
        t.put("download.prompt.error.optifine", "Versiones OptiFine legi non potuerunt");

        // Pagina progressus
        t.put("download.progress.title", "%s deponitur");
        t.put("download.progress.subtitle", "%s · Minecraft %s");
        t.put("download.progress.back", "Ad indicem depositorum redire");
        t.put("download.progress.speed", "Celeritas retis");
        t.put("download.progress.speedValue", "0 B/s");
        t.put("download.progress.progress", "Progressus depositi");
        t.put("download.progress.preparing", "Depositum praeparatur");
        t.put("download.progress.waiting", "Exspectatur munus depositi...");
        t.put("download.progress.completed", "Depositum perfectum");
        t.put("download.progress.completedTotal", "Depositum perfectum est");
        t.put("download.progress.total", "Summa %s");
        t.put("download.progress.totalWithMax", "Summa %s / %s");
        t.put("download.progress.failed", "Depositum defecit: %s");
        t.put("download.progress.currentFile", "Fasciculus praesens: %s");
        t.put("download.progress.currentFileWithBytes", "Fasciculus praesens: %s · %s / %s");
        t.put("download.progress.finishedHint", "Instantia installata est; ad indicem redire potes.");
        t.put("download.progress.failedHint", "Depositum substitit; ad indicem redi et rursus tenta.");
        t.put("download.progress.bytesDetail", "%.1f%% · %s / %s · fasciculi %d / %d");
        t.put("download.progress.filesDetail", "Fasciculi %d / %d");
        t.put("download.progress.planning", "Consilium depositi praeparatur...");

        // Gradus depositi
        t.put("download.stage.preparing", "Depositum praeparatur");
        t.put("download.stage.completed", "Perfectum");
        t.put("download.stage.failed", "Depositum defecit");
        t.put("download.stage.unknownError", "Error incognitus");
        t.put("download.stage.version.fetch", "Metadada versionis petuntur");
        t.put("download.stage.version.checkLocal", "Versio localis inspicitur");
        t.put("download.stage.version.downloadFiles", "Fasciculi ludi et bibliothecae deponuntur");
        t.put("download.stage.version.downloadAssets", "Facultates deponuntur");
        t.put("download.stage.version.writeJson", "Metadada versionis scribuntur");
        t.put("download.stage.loader.installer.prepare", "Installer %s praeparatur");
        t.put("download.stage.loader.installer.download", "Installer %s deponitur");
        t.put("download.stage.loader.installer.run", "Installer %s exsequitur");
        t.put("download.stage.loader.libraries.download", "Bibliothecae %s deponuntur");
        t.put("download.stage.loader.files.merge", "Fasciculi %s coniunguntur");
        t.put("download.stage.loader.verify", "Installatio %s comprobatur");
        t.put("download.stage.fabric.config.fetch", "Configuratio Fabric petitur");
        t.put("download.stage.fabric.loader.install", "Fabric Loader installatur");
        t.put("download.stage.fabric.api.download", "Fabric API deponitur");
        t.put("download.stage.fabric.verify", "Installatio Fabric comprobatur");
        t.put("download.stage.optifine.installer.download", "Installer OptiFine deponitur");
        t.put("download.stage.optifine.installer.run", "Installer OptiFine exsequitur");
        t.put("download.stage.optifine.files.merge", "Fasciculi OptiFine coniunguntur");

        // Optiones depositi
        t.put("download.settings.source", "Fons depositi");
        t.put("download.settings.source.description", "Elige inscriptionem depositi praeferendam pro versionibus, bibliothecis et facultatibus.");
        t.put("download.settings.source.mirror", "Speculum praeferre");
        t.put("download.settings.source.official", "Fontem officialem praeferre");
        t.put("download.settings.source.custom", "API proprium importare");
        t.put("download.settings.customApi.placeholder", "ex. https://bmclapi2.bangbang93.com");
        t.put("download.settings.concurrency", "Concurrentia depositi");
        t.put("download.settings.concurrency.description", "Numerus fasciculorum simul depositorum, ab 1 ad 256.");
        t.put("download.settings.concurrency.label", "Concurrentia depositi");
        t.put("download.settings.concurrency.value", "Concurrentia praesens: %d");
        t.put("download.settings.concurrency.accessible", "Concurrentia depositi");

        // Navigatio optionum
        t.put("settings.section.game", "Optiones ludi");
        t.put("settings.section.download", "Optiones depositi");
        t.put("settings.section.appearance", "Aspectus");

        // Directorium ludi
        t.put("settings.game.location.title", "Directorium ludi");
        t.put("settings.game.location.description", "Directorium datorum Minecraft constitue. Mutationes solum post servandum valent.");
        t.put("settings.game.location.browse", "Percurrere");
        t.put("settings.game.location.save", "Servare");
        t.put("settings.game.location.chooserTitle", "Directorium ludi elige");
        t.put("settings.game.location.notSaved", "Non servatum");
        t.put("settings.game.location.empty", "Directorium ludi vacuum esse non potest");
        t.put("settings.game.location.invalid", "Forma directorii ludi invalida");
        t.put("settings.game.location.checking", "Directorium inspicitur...");
        t.put("settings.game.location.missing", "Directorium non exstat: %s");
        t.put("settings.game.location.saved", "Directorium ludi servatum");
        t.put("settings.game.location.checkFailed", "Inspectio directorii defecit: %s");

        // Memoria
        t.put("settings.game.memory.title", "Memoria");
        t.put("settings.game.memory.accessible", "Memoria maxima");
        t.put("settings.game.memory.description", "Memoriam ad ludum deducendum adhibendam accommoda.");
        t.put("settings.game.memory.label", "Attributio memoriae");
        t.put("settings.game.memory.hint", "Manubrium trahe ut attributionem ludi accommodes; virga cinerea memoriam adhibitam una cum ludo ostendit, virga pallide cinerea ad dextram memoriam liberam.");
        t.put("settings.game.memory.summary", "Adhibita %d MB / Ludus %d MB / Praesto %d MB / Summa %d MB");

        // Segregatio versionum
        t.put("settings.game.isolation.title", "Segregatio versionum");
        t.put("settings.game.isolation.full", "Plena segregatio versionum");
        t.put("settings.game.isolation.none", "Nulla segregatio versionum");

        // Lingua ludi
        t.put("settings.game.language.title", "Lingua ludi");
        t.put("settings.game.language.description", "Cum instantia primum deducitur, linguam ludi ad linguam deductoris praesentem sponte constituit.");
        t.put("settings.game.language.enabled", "Commutatio automatica");
        t.put("settings.game.language.disabled", "Exclusa");
        t.put("settings.game.language.hint", "Solum valet si instantiae nondum lingua constituta est; lingua in options.txt iam posita non mutatur. Lingua Sinica propria ad Sinicam simplificatam convertitur.");

        // Java
        t.put("settings.game.java.title", "Java");
        t.put("settings.game.java.description", "Elige ambitum Java ad ludum deducendum. Modus automaticus Java eligit quae requisitis versionis ludi satisfacit.");
        t.put("settings.game.java.auto", "Automaticus");
        t.put("settings.game.java.manual", "Manualis");
        t.put("settings.game.java.rescan", "Iterum scrutari");
        t.put("settings.game.java.add", "Java addere");
        t.put("settings.game.java.scanning", "Java scrutatur...");
        t.put("settings.game.java.scanningAll", "Systema pro Java scrutatur...");
        t.put("settings.game.java.scanFailedPrompt", "Scrutatio Java defecit");
        t.put("settings.game.java.scanFailed", "Scrutatio Java defecit: %s");
        t.put("settings.game.java.noneFound", "Nulla Java inventa");
        t.put("settings.game.java.select", "Java elige");
        t.put("settings.game.java.autoSelected", "Automaticus");
        t.put("settings.game.java.chooserTitle", "Fasciculum exsecutabilem Java elige");
        t.put("settings.game.java.checking", "Java inspicitur...");
        t.put("settings.game.java.addFailed", "Additio Java defecit: %s");
        t.put("settings.game.java.autoHint", "Modus automaticus ex eventibus scrutatiae Java eligit quae requisitis versionis ludi praesentis satisfacit.");
        t.put("settings.game.java.selectOrAdd", "Java elige vel «Java addere» preme.");
        t.put("settings.game.java.unavailable", "Haec Java nunc praesto non est: %s");
        t.put("settings.game.java.detail", "Versio %s · %s · %s · %d bit%s");
        t.put("settings.game.java.jdk", " · JDK");
        t.put("settings.game.java.jre", " · JRE");

        // Aspectus
        t.put("settings.appearance.title", "Aspectus");
        t.put("settings.appearance.description", "Aspectum deductoris accommoda.");
        t.put("settings.appearance.themeHint", "Themata ex ~/.caml/themes veniunt; mutationes statim valent");
        t.put("settings.appearance.theme", "Thema");
        t.put("settings.appearance.theme.accessible", "Thema");
        t.put("settings.appearance.language.title", "Lingua");
        t.put("settings.appearance.language.description", "Elige quomodo lingua interfaciei deductoris eligatur.");
        t.put("settings.appearance.language.auto", "Automatica");
        t.put("settings.appearance.language.manual", "Manualis");
        t.put("settings.appearance.language.label", "Lingua interfaciei");
        t.put("settings.appearance.language.hint", "Modus automaticus linguam systematis sequitur; manualis lingua infra electa utitur.");
        t.put("settings.appearance.language.accessible", "Lingua interfaciei");
        t.put("settings.appearance.languageFallback.title", "Subsidium linguae");
        t.put("settings.appearance.languageFallback.description", "Elige qua lingua translationes deficientes suppleantur.");
        t.put("settings.appearance.languageFallback.system", "Lingua systematis");
        t.put("settings.appearance.languageFallback.default", "Lingua praedefinita");
        t.put("settings.appearance.languageFallback.key", "Claves crudas ostendere");
        t.put("settings.appearance.languageFallback.custom", "Lingua propria");
        t.put("settings.appearance.languageFallback.hint", "Lingua systematis solum linguam detectam adhibet; lingua praedefinita semper Anglice utitur; claves crudae nullum subsidium praebent; lingua propria solum linguam electam adhibet. Praeter linguam praedefinitam, textus deficiens ut claves crudae ostenditur.");
        t.put("settings.appearance.languageFallback.accessible", "Subsidium linguae");

        // Electio instantiae
        t.put("instances.picker.scanning", "Instantiae ludi scrutantur...");
        t.put("instances.picker.title", "Instantiam ludi elige");
        t.put("instances.picker.noGameDirectory", "Directorium ludi non positum est");
        t.put("instances.picker.gameDirectory", "Directorium ludi: %s");
        t.put("instances.picker.noGameDirectoryHint", "Directorium ludi non positum est; primum in optionibus serva");
        t.put("instances.picker.missing", "Directorium ludi non exstat: %s");
        t.put("instances.picker.empty", "In directorio ludi praesenti nullae instantiae installatae sunt");
        t.put("instances.picker.count", "%d instantiae ludi");
        t.put("instances.picker.scanFailed", "Scrutatio instantiarum ludi defecit: %s");
        t.put("instances.manage.title", "Instantias administrare");
        t.put("instances.manage.instance", "Instantia: %s");
        t.put("instances.manage.folder", "Directorium instantiae: %s");
        t.put("instances.manage.openFolder", "Directorium instantiae aperire");
        t.put("instances.manage.openFolder.opening", "Directorium instantiae aperitur...");
        t.put("instances.manage.openFolder.failed", "Directorium instantiae aperire non potuit: %s");
        t.put("instances.manage.noGameDirectory", "Directorium ludi non positum est");
        t.put("instances.manage.noInstance", "Instantia non selecta est");
        t.put("instances.manage.mods.title", "Index modorum");
        t.put("instances.manage.mods.scanning", "Index modorum legitur...");
        t.put("instances.manage.mods.count", "%d modi");
        t.put("instances.manage.mods.empty", "Haec instantia modos non habet");
        t.put("instances.manage.mods.scanFailed", "Index modorum legi non potuit: %s");
        t.put("instances.manage.mods.enabled", "Activatus");
        t.put("instances.manage.mods.disabled", "Inactivatus");

        // Deposita modorum
        t.put("mods.nav", "Modi");
        t.put("mods.search.title", "Modos quaerere");
        t.put("mods.search.name", "Nomen");
        t.put("mods.search.nameHint", "Nomen modi scribe");
        t.put("mods.search.source", "Fons");
        t.put("mods.search.gameVersion", "Versio ludi");
        t.put("mods.search.versionHint", "ex. 1.20.1; vacuum omnes significat");
        t.put("mods.search.loader", "Loader");
        t.put("mods.search.allSources", "Omnes fontes");
        t.put("mods.search.allLoaders", "Omnes loaders");
        t.put("mods.search.button", "Quaerere");
        t.put("mods.search.reset", "Condiciones reserare");
        t.put("mods.search.loading", "Modi quaeruntur...");
        t.put("mods.search.empty", "Nulli modi congruentes inventi");
        t.put("mods.search.count", "%d eventus");
        t.put("mods.search.failed", "Modi legi non potuerunt: %s");
        t.put("mods.page.previous", "Prior");
        t.put("mods.page.next", "Posterior");
        t.put("mods.page.info", "Pagina %d");
        t.put("mods.source.modrinth", "Modrinth");
        t.put("mods.source.curseforge", "CurseForge");
        t.put("mods.loader.fabric", "Fabric");
        t.put("mods.loader.forge", "Forge");
        t.put("mods.loader.neoforge", "NeoForge");
        t.put("mods.loader.quilt", "Quilt");
        t.put("mods.value.unknown", "Incognitum");
        t.put("mods.list.meta", "Loaders: %s · Versiones: %s · Deposita %s · Renovatum %s");
        t.put("mods.detail.loading", "Detalia modi leguntur...");
        t.put("mods.detail.count", "%d versiones fasciculorum");
        t.put("mods.detail.meta", "Deposita %s · Renovatum %s");
        t.put("mods.detail.versionFilter", "Versio ludi");
        t.put("mods.detail.loaderFilter", "Loader");
        t.put("mods.detail.allVersions", "Omnes versiones");
        t.put("mods.detail.allLoaders", "Omnes loaders");
        t.put("mods.detail.empty", "Nullae versiones condicionibus praesentibus congruunt");
        t.put("mods.detail.versionMeta", "Publicatum %s · Deposita %s · Fasciculus %s");
        t.put("mods.detail.download", "Deponere");
        t.put("mods.detail.downloading", "%s deponitur...");
        t.put("mods.detail.downloaded", "Depositum in: %s");
        t.put("mods.detail.noGameDirectory", "Primum directorium ludi constitue");
        t.put("mods.detail.noInstance", "Primum instantiam ludi elige");
        t.put("mods.detail.failed", "Operatio defecit: %s");
        t.put("mods.detail.dependencies", "Dependentiae:");
        t.put("mods.detail.noDependencies", "Nullae dependentiae");
        t.put("mods.dependency.required", "Necessaria");
        t.put("mods.dependency.optional", "Optionalis");
        t.put("mods.dependency.incompatible", "Incompatibilis");
        t.put("mods.dependency.embedded", "Inclusa");
        t.put("mods.dependency.unknown", "Incognita");
        t.put("mod.error.curseforgeKeyMissing", "Clavis API CurseForge non constituta est. CURSEFORGE_API_KEY vel -Dcaml.curseforge.apiKey constitue.");
        t.put("mod.error.downloadUrlMissing", "Nulla inscriptio depositi officialis huic versioni modi praesto est: %s");
        t.put("mod.error.projectNotFound", "Propositum modi non inventum: %s");
        t.put("mod.error.iconUrlInvalid", "Inscriptio iconis modi invalida: %s");
        t.put("mod.error.iconEmpty", "Fasciculus iconis modi vacuus est: %s");

        // Nomina operationum
        t.put("operation.inspectGameDirectory", "Directorium ludi inspicere");
        t.put("operation.inspectInstance", "Informationes instantiae legere");
        t.put("operation.openInstanceFolder", "Directorium instantiae aperire");
        t.put("operation.checkInstanceName", "Nomen instantiae inspicere");
        t.put("operation.fetchVersionList", "Indicem officialem %s petere");
        t.put("operation.installVanilla", "Instantiam vanilla %s installare");
        t.put("operation.fetchFabricLoader", "Versiones Fabric Loader petere");
        t.put("operation.fetchFabricApi", "Versiones Fabric API petere");
        t.put("operation.installFabric", "Instantiam Fabric %s installare");
        t.put("operation.fetchForge", "Versiones Forge petere");
        t.put("operation.fetchNeoForge", "Versiones NeoForge petere");
        t.put("operation.fetchOptiFine", "Versiones OptiFine petere");
        t.put("operation.installLoader", "Instantiam %s %s installare");
        t.put("operation.scanJava", "Java scrutari");
        t.put("operation.addManualJava", "Java manualem addere");
        t.put("operation.saveJavaSelection", "Electionem Java servare");
        t.put("operation.saveMaxMemory", "Memoriam maximam servare");
        t.put("operation.saveGameDirectory", "Directorium ludi servare");
        t.put("operation.saveVersionIsolation", "Segregationem versionum servare");
        t.put("operation.saveAutoGameLanguage", "Linguam ludi automaticam servare");
        t.put("operation.saveDownloadConcurrency", "Concurrentiam depositi servare");
        t.put("operation.saveDownloadSource", "Fontem depositi servare");
        t.put("operation.saveTheme", "Electionem thematis servare");
        t.put("operation.saveLanguage", "Optionem linguae servare");
        t.put("operation.saveLanguageFallback", "Subsidium linguae servare");
        t.put("operation.searchMods", "Modos quaerere: %s");
        t.put("operation.fetchModProject", "Propositum modi petere: %s");
        t.put("operation.downloadMod", "Modum deponere: %s");
        t.put("operation.loadModIcon", "Iconem modi legere");
        t.put("operation.saveLastInstance", "Ultimam instantiam deducendam servare");
        t.put("operation.launchGame", "Ludum %s deducere");

        // Quilt onerator
        t.put("download.description.quilt", "Quilt Loader %s");
        t.put("download.description.quiltWithApi", "Quilt Loader %s + Quilted Fabric API %s");
        t.put("download.setup.option.quilt", "Versionem Quilt eligere");
        t.put("download.setup.option.quiltApi", "Versionem Quilted Fabric API eligere");
        t.put("download.prompt.loading.quilt", "Versiones Quilt leguntur...");
        t.put("download.prompt.quiltFirst", "Primum versionem Quilt elige");
        t.put("download.prompt.select.quilt", "Versionem Quilt elige");
        t.put("download.prompt.select.quiltApi", "Versionem Quilted Fabric API elige");
        t.put("download.prompt.empty.quilt", "Nullae versiones Quilt praesto sunt");
        t.put("download.prompt.empty.quiltApi", "Nullae versiones Quilted Fabric API praesto sunt");
        t.put("download.prompt.error.quilt", "Versiones Quilt legere non potuimus");
        t.put("download.prompt.error.quiltApi", "Versiones Quilted Fabric API legere non potuimus");
        t.put("download.stage.quilt.config.fetch", "Configuratio Quilt petenda");
        t.put("download.stage.quilt.loader.install", "Quilt Loader installatur");
        t.put("download.stage.quilt.api.download", "Quilted Fabric API deponitur");
        t.put("download.stage.quilt.verify", "Installatio Quilt probatur");
        t.put("log.download.installed.quilt", "[Depositio] Instantia Quilt %s installata (Loader %s)");
        t.put("operation.fetchQuiltLoader", "Versiones Quilt Loader petere");
        t.put("operation.fetchQuiltApi", "Versiones Quilted Fabric API petere");
        t.put("operation.installQuilt", "Instantiam Quilt %s installare");
    }
}

