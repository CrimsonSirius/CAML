package org.chs.camlauncher.language.generator.lang;

import org.chs.camlauncher.language.generator.LanguageTranslationGenerator;
import org.chs.camlauncher.language.generator.TranslationTable;

/** བོད་ཡིག་གི་བསྒྱུར་ཡིག ། */
public final class BOCNGenerator extends LanguageTranslationGenerator {
    @Override
    protected String locale() {
        return "bo_cn";
    }

    @Override
    protected void populate(TranslationTable t) {
        // སྤྱི་ཡོངས
        t.put("common.back", "ཕྱིར་ལོག");
        t.put("common.cancel", "རྩིས་མེད");
        t.put("common.browse", "བཤེར་བ");
        t.put("common.save", "ཉར་ཚགས");

        // གོ་རིམ་དང་མཇུག་རིམ
        t.put("nav.home", "མ་ཤོག");
        t.put("nav.downloads", "ཕབ་ལེན");
        t.put("nav.settings", "སྒྲིག་བཀོད");
        t.put("app.subtitle", "Minecraft འགོ་འཛུགས་ཆས");
        t.put("footer.status", "CAML %s · རྒྱབ་སྟེགས %s · གཞི་གྲངས་སྣེའུ: %s");
        t.put("footer.backend.ready", "གྲ་སྒྲིག་ཟིན");
        t.put("footer.backend.notReady", "གྲ་སྒྲིག་མིན");

        // འགོ་འཛུགས་ཀྱི་ཤོག་ལེབ
        t.put("home.username", "དྲ་མེད་བེད་སྤྱོད་མིང");
        t.put("home.launch", "རྩེད་མོ་འགོ་འཛུགས");
        t.put("home.launching", "འགོ་འཛུགས་བཞིན...");
        t.put("home.browseInstances", "བཟོ་བཅོས་བཤེར་བ");
        t.put("home.manageInstances", "བཟོ་བཅོས་དོ་དམ");
        t.put("home.instance.current", "ད་ལྟའི་བཟོ་བཅོས: %s");
        t.put("home.instance.current.none", "ད་ལྟའི་བཟོ་བཅོས: མ་འདེམས");
        t.put("home.gameDirectory.none", "རྩེད་མོའི་སྣེའུ: མ་སྒྲིགས");
        t.put("home.gameDirectory.missing", "རྩེད་མོའི་སྣེའུ: མི་འདུག · %s");
        t.put("home.gameDirectory.value", "རྩེད་མོའི་སྣེའུ: %s");
        t.put("home.saveLocation.none", "ཉར་ཚགས་ཀྱི་གནས: མ་སྒྲིགས");
        t.put("home.saveLocation.fixDirectory", "ཉར་ཚགས་ཀྱི་གནས: སྔོན་ལ་རྩེད་མོའི་སྣེའུ་བཟོ་ཐབས་བྱོས");
        t.put("home.saveLocation.selectInstance", "ཉར་ཚགས་ཀྱི་གནས: སྔོན་ལ་བཟོ་བཅོས་ཤིག་འདེམས");
        t.put("home.saveLocation.value", "ཉར་ཚགས་ཀྱི་གནས: %s");
        t.put("home.saveLocation.isolated", "（བཟོ་བཅོས་ཆ་ཚང་ལོགས་བཀར）");
        t.put("home.error.inspectDirectory", "རྩེད་མོའི་སྣེའུ་ཞིབ་བཤེར་བྱས་མ་ཐུབ: %s");
        t.put("home.error.noGameDirectory", "སྔོན་ལ་སྒྲིག་བཀོད་ནང་རྩེད་མོའི་སྣེའུ་ཉར་ཚགས་བྱོས");
        t.put("home.error.missingGameDirectory", "རྩེད་མོའི་སྣེའུ་མི་འདུག སྒྲིག་བཀོད་ནང་ཡང་དག་པའི་སྣེའུ་ཞིག་ཉར་ཚགས་བྱོས");
        t.put("home.error.noInstance", "སྔོན་ལ་བཟོ་བཅོས་ཤིག་བཤེར་ཞིབ་བྱས་ཏེ་འདེམས");
        t.put("home.error.noUsername", "དྲ་མེད་བེད་སྤྱོད་མིང་འབྲིས");

        // དྲན་ཐོའི་པར
        t.put("log.title", "ལག་བསྟར་དྲན་ཐོ");
        t.put("log.subtitle", "ཕབ་ལེན་གྱི་འཕེལ་རིམ་དང་རྩེད་མོའི་བྱ་རིམ་གྱི་ཕྱིར་ཐོན");
        t.put("log.accessible", "ལག་བསྟར་དྲན་ཐོ");
        t.put("log.download.installed.vanilla", "[ཕབ་ལེན] བཟོ་བཅོས %s སྒྲིག་ཟིན（སྲོལ་རྒྱུན་ %s）");
        t.put("log.download.installed.fabric", "[ཕབ་ལེན] Fabric བཟོ་བཅོས %s སྒྲིག་ཟིན（Loader %s）");
        t.put("log.download.installed.loader", "[ཕབ་ལེན] %s བཟོ་བཅོས %s སྒྲིག་ཟིན");
        t.put("log.download.progress", "[ཕབ་ལེན] %s (%d/%d)");
        t.put("log.download.progressNoTotal", "[ཕབ་ལེན] %s (%d)");
        t.put("log.gameProcess.exit", "རྩེད་མོའི་བྱ་རིམ་མཇུག་སྒྲིལ་ཟིན། ཨང་ཀ: %d");
        t.put("log.gameProcess.exitError", "རྩེད་མོའི་བྱ་རིམ་རྒྱུན་མིན་གྱིས་མཇུག་སྒྲིལ་ཟིན། ཨང་ཀ: %d");
        t.put("log.error.operationFailed", "[%s] ཕམ་ཉེས: %s");

        // འགོ་འཛུགས་ཀྱི་གནས་སྟངས
        t.put("status.starting", "%s འགོ་འཛུགས་བཞིན...");
        t.put("status.started", "འགོ་འཛུགས་ཀྱི་བཀའ་ཚིག་བཏང་ཟིན");
        t.put("status.launchFailed", "འགོ་འཛུགས་ཕམ་ཉེས!");
        t.put("status.launchFailedDetail", "འགོ་འཛུགས་ཕམ་ཉེས: %s");
        t.put("error.unknown", "རྒྱུ་མཚན་མེད་པའི་ནོར་འཁྲུལ་བྱུང་། དྲན་ཐོ་ལ་གཟིགས།");
        t.put("download.error.failed", "%s ཕབ་ལེན་ཐེངས་ %s རྗེས་ཕམ་ཉེས（མཐའ་མའི་ཁ་བྱང: %s）");
        t.put("network.requestFailed", "%s ལ་རེ་འདུན་བཏང་མ་ཐུབ（མཐའ་མའི་ཁ་བྱང: %s）");
        t.put("network.timeout", "རེ་འདུན་གྱི་དུས་ཚོད་ཟད་སོང（%s མི་ལི་སྐར）: %s");
        t.put("network.httpError", "HTTP %s: %s");
        t.put("network.responseInvalid", "ལན་གདམ་ཞིབ་བཤེར་བྱས་མ་ཐུབ: %s");
        t.put("network.redirectLocationMissing", "ལོག་ཕྱོགས་ལ་ Location མི་འདུག: %s");
        t.put("network.tooManyRedirects", "ལོག་ཕྱོགས་མང་དྲགས་སོང: %s");
        t.put("network.noSource", "ཕབ་ལེན་གྱི་འབྱུང་ཁུངས་མི་འདུག: %s");
        t.put("metadata.manifestEmpty", "པར་གཞིའི་ཐོ་གཞུང་སྟོང་པ་ཡིན");
        t.put("metadata.versionJsonEmpty", "པར་གཞིའི་ JSON སྟོང་པ་ཡིན");
        t.put("metadata.versionJsonHashMismatch", "པར་གཞིའི་ JSON གྱི་ SHA-1 ཞིབ་བཤེར་ཕམ་ཉེས: %s");
        t.put("launch.permissionDenied", "Java ལག་བསྟར་དང་རྩེད་མོའི་སྣེའུ་ལ་འཛུལ་བའི་དབང་ཚད་མི་འདུག ཡིག་ཆའི་དབང་ཚད་ལ་གཟིགས");
        t.put("launch.executableNotFound", "Java ལག་བསྟར་ཡིག་ཆ་མ་རྙེད། ཡང་བསྐྱར་ Java འདེམས");
        t.put("launch.commandTooLong", "འགོ་འཛུགས་ཀྱི་བཀའ་ཚིག་རིང་དྲགས། རྩེད་མོའི་ལམ་བུ་ཡང་ན་ classpath ཐུང་དུ་གཏོང་");
        t.put("launch.commandTooLongAfterFallback", "argfile དང་ manifest བེད་སྤྱད་རྗེས་ཀྱང་བཀའ་ཚིག་རིང་དྲགས། རྩེད་མོའི་ལམ་བུ་ཡང་ན་ classpath ཐུང་དུ་གཏོང་");
        t.put("launch.processStartFailed", "རྩེད་མོའི་བྱ་རིམ་འགོ་འཛུགས་མ་ཐུབ། ཞིབ་ཕྲ་དྲན་ཐོ་ནང་གཟིགས");
        t.put("launch.manualJavaNotConfigured", "ལག་ཐོགས་ Java འདེམས་སྒྲུག་སྒོ་ཕྱེ་ཡོད་ཀྱང་ Java ལམ་བུ་མ་འདེམས");
        t.put("launch.manualJavaUnavailable", "འདེམས་པའི་ Java ད་ལྟ་བེད་མི་སྤྱོད་ཐུབ: %s");
        t.put("launch.javaRequirementNotFound", "པར་གཞིའི་དགོས་མཁོ %s ལ་མཐུན་པའི་ Java མ་རྙེད། Java སྒྲིག་འཛུགས་ཡང་ན་འདེམས");

        // པར་གཞིའི་རིགས་དང་འབྱུང་ཁུངས
        t.put("download.category.release", "གཏན་འབེབས་པར་གཞི");
        t.put("download.category.snapshot", "མྱུར་པར");
        t.put("download.category.old", "རྙིང་པའི་པར་གཞི");
        t.put("download.category.unknown", "མ་ཤེས་པའི་པར་གཞི");
        t.put("download.source.mirror", "མེ་ལོང་འབྱུང་ཁུངས");
        t.put("download.source.official", "གཞུང་ཕྱོགས་འབྱུང་ཁུངས");
        t.put("download.source.custom", "རང་འདོད");

        // པར་གཞིའི་ཐོ་གཞུང
        t.put("download.versions.loading", "%1$s ནས་ %2$s ཐོ་གཞུང་ལེན་བཞིན...");
        t.put("download.versions.empty", "ཕབ་ལེན་འབྱུང་ཁུངས་ཀྱིས་ཕབ་ལེན་རུང་བའི་ %s མ་སྤྲད");
        t.put("download.versions.count", "སྤྱི་ %d གི་ %s། སྤེལ་བའི་དུས་ཚོད་གསར་པ་ནས་རྙིང་པར");
        t.put("download.versions.error", "%s ཐོ་གཞུང་ལེན་མ་ཐུབ: %s");
        t.put("download.versions.accessible", "%s, %s");

        // ནང་དོན་གྱི་འགྲེལ་བཤད
        t.put("download.description.optifine", "Forge %s + OptiFine %s");
        t.put("download.description.forge", "Forge %s");
        t.put("download.description.fabric", "Fabric Loader %s");
        t.put("download.description.fabricWithApi", "Fabric Loader %s + Fabric API %s");
        t.put("download.description.neoforge", "NeoForge %s");
        t.put("download.description.vanilla", "Minecraft %s");

        // ཕབ་ལེན་སྒྲིག་སྟངས་ཤོག་ལེབ
        t.put("download.setup.status.idle", "འཇུག་ཆས་མ་འདེམས་ན་སྲོལ་རྒྱུན་རྩེད་མོ་ཕབ་ལེན་བྱེད");
        t.put("download.setup.instanceName", "བཟོ་བཅོས་ཀྱི་མིང");
        t.put("download.setup.download", "བཟོ་བཅོས་ཕབ་ལེན");
        t.put("download.setup.option.versionInfo", "པར་གཞིའི་གནས་ཚུལ");
        t.put("download.setup.option.forge", "Forge པར་གཞི་འདེམས");
        t.put("download.setup.option.optifine", "OptiFine པར་གཞི་འདེམས");
        t.put("download.setup.option.fabric", "Fabric པར་གཞི་འདེམས");
        t.put("download.setup.option.fabricApi", "Fabric API པར་གཞི་འདེམས");
        t.put("download.setup.option.neoforge", "NeoForge པར་གཞི་འདེམས");
        t.put("download.setup.versionInfo", "Minecraft %s · %s");
        t.put("download.setup.versionInfoWithDate", "Minecraft %s · %s · %s");
        t.put("download.setup.optifineSwitched", "Forge དང་ OptiFine མི་མཐུན། Forge %s ལ་རང་བཞིན་བརྗེས");
        t.put("download.setup.optifineIncompatibleForge", "ད་ལྟའི་ Forge དང་ OptiFine མི་མཐུན");
        t.put("download.setup.noCompatibleOptiFine", "ད་ལྟའི་ Forge དང་མཐུན་པའི་ OptiFine པར་གཞི་མི་འདུག");
        t.put("download.setup.optifineForgeConflict", "Forge %s དང་ OptiFine %s མི་མཐུན། Forge %s ཡང་ན་སྔ་མའི་པར་གཞི་འདེམས");
        t.put("download.setup.nameEmpty", "བཟོ་བཅོས་ཀྱི་མིང་སྟོང་པ་ཡིན་མི་ཆོག");
        t.put("download.setup.nameDotSegments", "བཟོ་བཅོས་ཀྱི་མིང་ . ཡང་ན་ .. ཡིན་མི་ཆོག");
        t.put("download.setup.nameTrailing", "བཟོ་བཅོས་ཀྱི་མིང་ཐིག་རྟགས་སམ་བར་སྟོང་གིས་མཇུག་སྒྲིལ་མི་ཆོག");
        t.put("download.setup.nameIllegalCharacter", "བཟོ་བཅོས་ཀྱི་མིང་ནང་ཆོག་མཆན་མེད་པའི་ཡིག་རྟགས་འདུག: %s");
        t.put("download.setup.nameExists", "བཟོ་བཅོས་ཀྱི་མིང་དེ་ཡོད་ཟིན: %s། མིང་གཞན་ཞིག་འདེམས");
        t.put("download.setup.noGameDirectory", "རྩེད་མོའི་སྣེའུ་མ་སྒྲིགས");
        t.put("download.setup.checkingName", "བཟོ་བཅོས་ཀྱི་མིང་ཞིབ་བཤེར་བཞིན...");
        t.put("download.setup.nameSameAsLoader", "འཇུག་ཆས་སྒྲིག་སྐབས་བཟོ་བཅོས་ཀྱི་མིང་དང་རྩེད་མོའི་པར་གཞི་མཐུན་མི་ཆོག");
        t.put("download.setup.nameAvailable", "བཟོ་བཅོས་ཀྱི་མིང་བེད་སྤྱོད་རུང");
        t.put("download.setup.checkNameFailed", "བཟོ་བཅོས་ཀྱི་མིང་ཞིབ་བཤེར་ཕམ་ཉེས: %s");
        t.put("download.setup.nameNotChecked", "བཟོ་བཅོས་ཀྱི་མིང་ད་དུང་ཞིབ་བཤེར་མ་བྱུང་། ཏིང་ངེར་ཡང་བསྐྱར་ཚོད་ལྟ་བྱོས");
        t.put("download.setup.noGameDirectoryLong", "རྩེད་མོའི་སྣེའུ་མ་སྒྲིགས། སྔོན་ལ་སྒྲིག་བཀོད་ནང་ཉར་ཚགས་བྱོས");
        t.put("download.setup.optifineRequiresForge", "OptiFine ནི་ Forge དང་མཉམ་དུ་ལས་སྒྲིག་བྱེད་ཐུབ");

        // འཇུག་ཆས་འདེམས་སྐབས་ཀྱི་བརྡ་སྟོན
        t.put("download.prompt.loading.fabric", "Fabric པར་གཞི་ལེན་བཞིན...");
        t.put("download.prompt.loading.forge", "Forge པར་གཞི་ལེན་བཞིན...");
        t.put("download.prompt.loading.neoforge", "NeoForge པར་གཞི་ལེན་བཞིན...");
        t.put("download.prompt.fabricFirst", "སྔོན་ལ་ Fabric པར་གཞི་འདེམས");
        t.put("download.prompt.forgeFirst", "སྔོན་ལ་ Forge པར་གཞི་འདེམས");
        t.put("download.prompt.fabricApiFirst", "སྔོན་ལ་ Fabric པར་གཞི་འདེམས");
        t.put("download.prompt.select.fabric", "Fabric པར་གཞི་འདེམས");
        t.put("download.prompt.select.forge", "Forge པར་གཞི་འདེམས");
        t.put("download.prompt.select.neoforge", "NeoForge པར་གཞི་འདེམས");
        t.put("download.prompt.select.optifine", "OptiFine པར་གཞི་འདེམས");
        t.put("download.prompt.select.fabricApi", "Fabric API པར་གཞི་འདེམས");
        t.put("download.prompt.empty.fabric", "Fabric པར་གཞི་བེད་སྤྱོད་རུང་བ་མི་འདུག");
        t.put("download.prompt.empty.fabricApi", "Fabric API པར་གཞི་བེད་སྤྱོད་རུང་བ་མི་འདུག");
        t.put("download.prompt.empty.forge", "Forge པར་གཞི་བེད་སྤྱོད་རུང་བ་མི་འདུག");
        t.put("download.prompt.empty.neoforge", "NeoForge པར་གཞི་བེད་སྤྱོད་རུང་བ་མི་འདུག");
        t.put("download.prompt.empty.optifine", "OptiFine པར་གཞི་བེད་སྤྱོད་རུང་བ་མི་འདུག");
        t.put("download.prompt.error.fabric", "Fabric པར་གཞི་ལེན་མ་ཐུབ");
        t.put("download.prompt.error.fabricApi", "Fabric API པར་གཞི་ལེན་མ་ཐུབ");
        t.put("download.prompt.error.forge", "Forge པར་གཞི་ལེན་མ་ཐུབ");
        t.put("download.prompt.error.neoforge", "NeoForge པར་གཞི་ལེན་མ་ཐུབ");
        t.put("download.prompt.error.optifine", "OptiFine པར་གཞི་ལེན་མ་ཐུབ");

        // འཕེལ་རིམ་གྱི་ཤོག་ལེབ
        t.put("download.progress.title", "%s ཕབ་ལེན་བཞིན");
        t.put("download.progress.subtitle", "%s · Minecraft %s");
        t.put("download.progress.back", "ཕབ་ལེན་ཐོ་གཞུང་ལ་ཕྱིར་ལོག");
        t.put("download.progress.speed", "དྲ་རྒྱའི་མགྱོགས་ཚད");
        t.put("download.progress.speedValue", "0 B/s");
        t.put("download.progress.progress", "ཕབ་ལེན་གྱི་འཕེལ་རིམ");
        t.put("download.progress.preparing", "ཕབ་ལེན་གྲ་སྒྲིག་བཞིན");
        t.put("download.progress.waiting", "ཕབ་ལེན་ལས་འགན་སྒུག་བཞིན...");
        t.put("download.progress.completed", "ཕབ་ལེན་མཇུག་སྒྲིལ་ཟིན");
        t.put("download.progress.completedTotal", "ཕབ་ལེན་མཇུག་སྒྲིལ་ཟིན");
        t.put("download.progress.total", "བསྡོམས་ %s");
        t.put("download.progress.totalWithMax", "བསྡོམས་ %s / %s");
        t.put("download.progress.failed", "ཕབ་ལེན་ཕམ་ཉེས: %s");
        t.put("download.progress.currentFile", "ད་ལྟའི་ཡིག་ཆ: %s");
        t.put("download.progress.currentFileWithBytes", "ད་ལྟའི་ཡིག་ཆ: %s · %s / %s");
        t.put("download.progress.finishedHint", "བཟོ་བཅོས་སྒྲིག་ཟིན། ཐོ་གཞུང་ལ་ཕྱིར་ལོག་ཆོག");
        t.put("download.progress.failedHint", "ཕབ་ལེན་མཚམས་བཅད་ཟིན། ཐོ་གཞུང་ལ་ཕྱིར་ལོག་ནས་ཡང་བསྐྱར་ཚོད་ལྟ་བྱོས");
        t.put("download.progress.bytesDetail", "%.1f%% · %s / %s · ཡིག་ཆ %d / %d");
        t.put("download.progress.filesDetail", "ཡིག་ཆ %d / %d");
        t.put("download.progress.planning", "ཕབ་ལེན་གྱི་འཆར་གཞི་གྲ་སྒྲིག་བཞིན...");

        // ཕབ་ལེན་གྱི་གོ་རིམ
        t.put("download.stage.preparing", "ཕབ་ལེན་གྲ་སྒྲིག་བཞིན");
        t.put("download.stage.completed", "མཇུག་སྒྲིལ");
        t.put("download.stage.failed", "ཕབ་ལེན་ཕམ་ཉེས");
        t.put("download.stage.unknownError", "མ་ཤེས་པའི་ནོར་འཁྲུལ");
        t.put("download.stage.version.fetch", "པར་གཞིའི་གནས་ཚུལ་ལེན་བཞིན");
        t.put("download.stage.version.checkLocal", "ས་གནས་པར་གཞི་ཞིབ་བཤེར་བཞིན");
        t.put("download.stage.version.downloadFiles", "རྩེད་མོའི་ཡིག་ཆ་དང་མཛོད་ཁང་ཕབ་ལེན་བཞིན");
        t.put("download.stage.version.downloadAssets", "ཐོན་ཁུངས་ཕབ་ལེན་བཞིན");
        t.put("download.stage.version.writeJson", "པར་གཞིའི་གནས་ཚུལ་འབྲི་བཞིན");
        t.put("download.stage.loader.installer.prepare", "%s སྒྲིག་ཆས་གྲ་སྒྲིག་བཞིན");
        t.put("download.stage.loader.installer.download", "%s སྒྲིག་ཆས་ཕབ་ལེན་བཞིན");
        t.put("download.stage.loader.installer.run", "%s སྒྲིག་ཆས་ལག་བསྟར་བཞིན");
        t.put("download.stage.loader.libraries.download", "%s མཛོད་ཁང་ཕབ་ལེན་བཞིན");
        t.put("download.stage.loader.files.merge", "%s ཡིག་ཆ་སྦྲེལ་བཞིན");
        t.put("download.stage.loader.verify", "%s སྒྲིག་སྟངས་ཞིབ་བཤེར་བཞིན");
        t.put("download.stage.fabric.config.fetch", "Fabric སྒྲིག་བཀོད་ལེན་བཞིན");
        t.put("download.stage.fabric.loader.install", "Fabric Loader སྒྲིག་བཞིན");
        t.put("download.stage.fabric.api.download", "Fabric API ཕབ་ལེན་བཞིན");
        t.put("download.stage.fabric.verify", "Fabric སྒྲིག་སྟངས་ཞིབ་བཤེར་བཞིན");
        t.put("download.stage.optifine.installer.download", "OptiFine སྒྲིག་ཆས་ཕབ་ལེན་བཞིན");
        t.put("download.stage.optifine.installer.run", "OptiFine སྒྲིག་ཆས་ལག་བསྟར་བཞིན");
        t.put("download.stage.optifine.files.merge", "OptiFine ཡིག་ཆ་སྦྲེལ་བཞིན");

        // ཕབ་ལེན་སྒྲིག་བཀོད
        t.put("download.settings.source", "ཕབ་ལེན་འབྱུང་ཁུངས");
        t.put("download.settings.source.description", "པར་གཞི་དང་མཛོད་ཁང་། ཐོན་ཁུངས་བཅས་ཀྱི་སྔོན་འགྲོའི་ཕབ་ལེན་ཁ་བྱང་འདེམས");
        t.put("download.settings.source.mirror", "མེ་ལོང་འབྱུང་ཁུངས་གཙོ་བོར་བཟུང");
        t.put("download.settings.source.official", "གཞུང་ཕྱོགས་འབྱུང་ཁུངས་གཙོ་བོར་བཟུང");
        t.put("download.settings.source.custom", "རང་འདོད་ API ནང་འདྲེན");
        t.put("download.settings.customApi.placeholder", "དཔེར་ན https://bmclapi2.bangbang93.com");
        t.put("download.settings.concurrency", "ཕབ་ལེན་མཉམ་སྒྲུབ་ཨང་གྲངས");
        t.put("download.settings.concurrency.description", "མཉམ་དུ་ཕབ་ལེན་བྱེད་པའི་ཡིག་ཆའི་ཨང་གྲངས། 1–256 བར");
        t.put("download.settings.concurrency.label", "ཕབ་ལེན་མཉམ་སྒྲུབ་ཨང་གྲངས");
        t.put("download.settings.concurrency.value", "ད་ལྟའི་ཨང་གྲངས: %d");
        t.put("download.settings.concurrency.accessible", "ཕབ་ལེན་མཉམ་སྒྲུབ་ཨང་གྲངས");

        // སྒྲིག་བཀོད་ཀྱི་གོ་རིམ
        t.put("settings.section.game", "རྩེད་མོའི་སྒྲིག་བཀོད");
        t.put("settings.section.download", "ཕབ་ལེན་སྒྲིག་བཀོད");
        t.put("settings.section.appearance", "ཕྱི་ཚུལ");

        // རྩེད་མོའི་སྣེའུ
        t.put("settings.game.location.title", "རྩེད་མོའི་སྣེའུ");
        t.put("settings.game.location.description", "Minecraft གཞི་གྲངས་སྣེའུ་སྒྲིག ལག་ཐོགས་ཀྱིས་འབྲིས་ན་ཉར་ཚགས་མནན་རྗེས་གཏན་ནས་འཇུག");
        t.put("settings.game.location.browse", "བཤེར་བ");
        t.put("settings.game.location.save", "ཉར་ཚགས");
        t.put("settings.game.location.chooserTitle", "རྩེད་མོའི་སྣེའུ་འདེམས");
        t.put("settings.game.location.notSaved", "མ་ཉར");
        t.put("settings.game.location.empty", "རྩེད་མོའི་སྣེའུ་སྟོང་པ་ཡིན་མི་ཆོག");
        t.put("settings.game.location.invalid", "རྩེད་མོའི་སྣེའུའི་རྣམ་པ་མི་འགྲིག");
        t.put("settings.game.location.checking", "སྣེའུ་ཞིབ་བཤེར་བཞིན...");
        t.put("settings.game.location.missing", "སྣེའུ་མི་འདུག: %s");
        t.put("settings.game.location.saved", "རྩེད་མོའི་སྣེའུ་ཉར་ཚགས་ཟིན");
        t.put("settings.game.location.checkFailed", "སྣེའུ་ཞིབ་བཤེར་ཕམ་ཉེས: %s");

        // དྲན་ཤེས
        t.put("settings.game.memory.title", "དྲན་ཤེས");
        t.put("settings.game.memory.accessible", "དྲན་ཤེས་མང་ཤོས");
        t.put("settings.game.memory.description", "རྩེད་མོ་འགོ་འཛུགས་སྐབས་བེད་སྤྱོད་བྱེད་པའི་དྲན་ཤེས་སྒྲིག");
        t.put("settings.game.memory.label", "དྲན་ཤེས་བགོ་སྐལ");
        t.put("settings.game.memory.hint", "ལག་འཛིན་འཐེན་ནས་རྩེད་མོའི་བགོ་སྐལ་སྒྲིག སྐྱ་མདོག་གི་སྒྲོམ་ནི་བེད་སྤྱོད་ཟིན་+རྩེད་མོའི་དྲན་ཤེས། གཡས་ཀྱི་སྐྱ་སྲིད་ནི་བེད་མ་སྤྱད་པའི་དྲན་ཤེས");
        t.put("settings.game.memory.summary", "བེད་སྤྱོད་ཟིན %d MB / རྩེད་མོ %d MB / ཐུབ་རུང %d MB / སྤྱི %d MB");

        // པར་གཞི་ལོགས་བཀར
        // JVM ཚད་སྒྲིག
        t.put("settings.game.jvm.title", "JVM ཚད་སྒྲིག");
        t.put("settings.game.jvm.description", "རྩེད་མོ་འགོ་འཛུགས་སྐབས་ JVM ཚད་སྒྲིག་ཁ་སྣོན་བྱེད། དྲན་ཤེས་བགོ་སྐལ་ནི་སྟེང་གི་འཐེན་ཆས་ཀྱིས་དོ་དམ་བྱེད། ཁྱེད་ཀྱིས་དེའི་བེད་སྤྱོད་ཤེས་མེད་ན་མ་བསྒྱུར།");
        t.put("settings.game.jvm.label", "ཁ་སྣོན་ཚད་སྒྲིག");
        t.put("settings.game.jvm.hint", "ཚད་སྒྲིག་རྣམས་བར་སྟོང་གིས་ཁ་བྲལ་བྱེད། བར་སྟོང་ཡོད་པའི་རིན་ཐང་ལ་ཡིག་སྒྲོམ་གཉིས་བེད་སྤྱོད་བྱེད། དཔེར་ན་ -Dfoo=\"bar baz\"། ${game_directory} དང་ ${library_directory} ལ་རྒྱབ་སྐྱོར་ཡོད།");
        t.put("settings.game.jvm.placeholder", "-XX:+UseG1GC -Dexample=value");
        t.put("settings.game.jvm.save", "ཉར་ཚགས");
        t.put("settings.game.jvm.saved", "JVM ཚད་སྒྲིག་ཉར་ཚགས་ཟིན");
        t.put("settings.game.jvm.saveFailed", "JVM ཚད་སྒྲིག་ཉར་ཚགས་བྱེད་མ་ཐུབ། %s");

        t.put("settings.game.isolation.title", "པར་གཞི་ལོགས་བཀར");
        t.put("settings.game.isolation.full", "པར་གཞི་ཆ་ཚང་ལོགས་བཀར");
        t.put("settings.game.isolation.none", "པར་གཞི་ལོགས་མི་བཀར");

        // རྩེད་མོའི་སྐད་ཡིག
        t.put("settings.game.language.title", "རྩེད་མོའི་སྐད་ཡིག");
        t.put("settings.game.language.description", "བཟོ་བཅོས་གསར་པ་ཐེངས་དང་པོ་འགོ་འཛུགས་སྐབས་རྩེད་མོའི་སྐད་ཡིག་འགོ་འཛུགས་ཆས་ཀྱི་སྐད་ཡིག་ལ་རང་བཞིན་སྒྲིག");
        t.put("settings.game.language.enabled", "རང་བཞིན་བརྗེ");
        t.put("settings.game.language.disabled", "སྒོ་བརྒྱབ");
        t.put("settings.game.language.hint", "བཟོ་བཅོས་ལ་སྐད་ཡིག་མ་སྒྲིགས་ན་ལག་བསྟར་བྱེད། options.txt ནང་སྒྲིག་ཟིན་པའི་སྐད་ཡིག་ལ་བརྗེ་མི་བྱེད། རང་འདོད་རྒྱ་ཡིག་ནི་རྒྱ་ཡིག་སྟབས་བདེར་བསྒྱུར");

        // Java
        t.put("settings.game.java.title", "Java");
        t.put("settings.game.java.description", "རྩེད་མོ་འགོ་འཛུགས་སྐབས་བེད་སྤྱོད་བྱེད་པའི་ Java ཁོར་ཡུག་འདེམས། རང་བཞིན་འདེམས་སྒྲུག་གིས་རྩེད་མོའི་པར་གཞིའི་དགོས་མཁོ་དང་མཐུན་པའི་ Java འདེམས");
        t.put("settings.game.java.auto", "རང་བཞིན་འདེམས");
        t.put("settings.game.java.manual", "ལག་ཐོགས་འདེམས");
        t.put("settings.game.java.rescan", "ཡང་བསྐྱར་བཤེར");
        t.put("settings.game.java.add", "Java སྣོན");
        t.put("settings.game.java.scanning", "Java བཤེར་བཞིན...");
        t.put("settings.game.java.scanningAll", "རིགས་ཀུན་གྱི་ Java བཤེར་བཞིན...");
        t.put("settings.game.java.scanFailedPrompt", "Java བཤེར་མ་ཐུབ");
        t.put("settings.game.java.scanFailed", "Java བཤེར་མ་ཐུབ: %s");
        t.put("settings.game.java.noneFound", "Java མ་རྙེད");
        t.put("settings.game.java.select", "Java འདེམས");
        t.put("settings.game.java.autoSelected", "རང་བཞིན་འདེམས");
        t.put("settings.game.java.chooserTitle", "Java ལག་བསྟར་ཡིག་ཆ་འདེམས");
        t.put("settings.game.java.checking", "Java ཞིབ་བཤེར་བཞིན...");
        t.put("settings.game.java.addFailed", "Java སྣོན་མ་ཐུབ: %s");
        t.put("settings.game.java.autoHint", "རང་བཞིན་འདེམས་སྒྲུག་གིས་བཤེར་འབྲས་ནང་ནས་ད་ལྟའི་རྩེད་མོའི་པར་གཞིའི་དགོས་མཁོ་དང་མཐུན་པའི་ Java འདེམས");
        t.put("settings.game.java.selectOrAdd", "Java ཞིག་འདེམས་ཡང་ན་「Java སྣོན」མནན");
        t.put("settings.game.java.unavailable", "Java དེ་ད་ལྟ་བེད་མི་སྤྱོད་ཐུབ: %s");
        t.put("settings.game.java.detail", "པར་གཞི %s · %s · %s · %d བིཊ%s");
        t.put("settings.game.java.jdk", " · JDK");
        t.put("settings.game.java.jre", " · JRE");

        // ཕྱི་ཚུལ
        t.put("settings.appearance.title", "ཕྱི་ཚུལ");
        t.put("settings.appearance.description", "འགོ་འཛུགས་ཆས་ཀྱི་ཕྱི་ཚུལ་སྒྲིག");
        t.put("settings.appearance.themeHint", "བརྗོད་གཞི་ནི་ ~/.caml/themes ནས་ཡོང་བ་དང་། བརྗེས་རྗེས་མ་ཐག་ལག་བསྟར་བྱེད");
        t.put("settings.appearance.theme", "བརྗོད་གཞི");
        t.put("settings.appearance.theme.accessible", "བརྗོད་གཞི");
        t.put("settings.appearance.language.title", "སྐད་ཡིག");
        t.put("settings.appearance.language.description", "འགོ་འཛུགས་ཆས་ཀྱི་ངོས་ཀའི་སྐད་ཡིག་འདེམས་སྟངས་སྒྲིག");
        t.put("settings.appearance.language.auto", "རང་བཞིན");
        t.put("settings.appearance.language.manual", "ལག་ཐོགས");
        t.put("settings.appearance.language.label", "ངོས་ཀའི་སྐད་ཡིག");
        t.put("settings.appearance.language.hint", "རང་བཞིན་ནི་རིགས་ཀྱི་སྐད་ཡིག་ལ་རྗེས་སུ་འབྲང་། ལག་ཐོགས་ནི་འོག་གི་སྐད་ཡིག་བེད་སྤྱོད་བྱེད");
        t.put("settings.appearance.language.accessible", "ངོས་ཀའི་སྐད་ཡིག");
        t.put("settings.appearance.languageFallback.title", "སྐད་ཡིག་གི་རྒྱབ་སྐྱོར");
        t.put("settings.appearance.languageFallback.description", "བསྒྱུར་ཡིག་མི་འདུག་སྐབས་སྐད་ཡིག་གང་གིས་ཁ་སྐོང་བྱེད་པ་སྒྲིག");
        t.put("settings.appearance.languageFallback.system", "རིགས་ཀྱི་སྐད་ཡིག");
        t.put("settings.appearance.languageFallback.default", "སྔོན་སྒྲིག་སྐད་ཡིག");
        t.put("settings.appearance.languageFallback.key", "རྩ་བའི་ལྡེ་ཡིག་སྟོན");
        t.put("settings.appearance.languageFallback.custom", "རང་འདོད་སྐད་ཡིག");
        t.put("settings.appearance.languageFallback.hint", "རིགས་ཀྱི་སྐད་ཡིག་གིས་ཤེས་འཕེར་བྱུང་བའི་སྐད་ཡིག་ཁོ་ན་བེད་སྤྱོད་བྱེད། སྔོན་སྒྲིག་སྐད་ཡིག་གིས་རྟག་ཏུ་དབྱིན་ཡིག་བེད་སྤྱོད་བྱེད། རྩ་བའི་ལྡེ་ཡིག་གིས་ཁ་སྐོང་མི་བྱེད། རང་འདོད་སྐད་ཡིག་གིས་འོག་གི་སྐད་ཡིག་ཁོ་ན་བེད་སྤྱོད་བྱེད། སྔོན་སྒྲིག་སྐད་ཡིག་མ་གཏོགས་པའི་ནང་དོན་རྩ་བའི་ལྡེ་ཡིག་ཏུ་སྟོན");
        t.put("settings.appearance.languageFallback.accessible", "སྐད་ཡིག་གི་རྒྱབ་སྐྱོར");

        // བཟོ་བཅོས་འདེམས་སྒྲུག
        t.put("instances.picker.scanning", "རྩེད་མོའི་བཟོ་བཅོས་བཤེར་བཞིན...");
        t.put("instances.picker.title", "རྩེད་མོའི་བཟོ་བཅོས་འདེམས");
        t.put("instances.picker.noGameDirectory", "རྩེད་མོའི་སྣེའུ་མ་སྒྲིགས");
        t.put("instances.picker.gameDirectory", "རྩེད་མོའི་སྣེའུ: %s");
        t.put("instances.picker.noGameDirectoryHint", "རྩེད་མོའི་སྣེའུ་མ་སྒྲིགས། སྔོན་ལ་སྒྲིག་བཀོད་ནང་ཉར་ཚགས་བྱོས");
        t.put("instances.picker.missing", "རྩེད་མོའི་སྣེའུ་མི་འདུག: %s");
        t.put("instances.picker.empty", "ད་ལྟའི་རྩེད་མོའི་སྣེའུ་ནང་སྒྲིག་ཟིན་པའི་བཟོ་བཅོས་མི་འདུག");
        t.put("instances.picker.count", "སྤྱི་ %d གི་རྩེད་མོའི་བཟོ་བཅོས");
        t.put("instances.picker.scanFailed", "བཟོ་བཅོས་བཤེར་མ་ཐུབ: %s");
        t.put("instances.manage.title", "བཟོ་བཅོས་དོ་དམ");
        t.put("instances.manage.instance", "Instance: %s");
        t.put("instances.manage.folder", "Instance folder: %s");
        t.put("instances.manage.openFolder", "Open instance folder");
        t.put("instances.manage.openFolder.opening", "Opening instance folder...");
        t.put("instances.manage.openFolder.failed", "Failed to open instance folder: %s");
        t.put("instances.manage.noGameDirectory", "No game directory configured");
        t.put("instances.manage.noInstance", "No instance selected");
        t.put("instances.manage.mods.title", "Mods");
        t.put("instances.manage.mods.scanning", "Reading mod list...");
        t.put("instances.manage.mods.count", "%d mods");
        t.put("instances.manage.mods.empty", "This instance has no mods");
        t.put("instances.manage.mods.scanFailed", "Failed to read mod list: %s");
        t.put("instances.manage.mods.toggleFailed", "Failed to toggle mod state: %s");
        t.put("instances.manage.mods.update", "Update");
        t.put("instances.manage.mods.updating", "Updating %s...");
        t.put("instances.manage.mods.updateFailed", "Failed to update mod: %s");
        t.put("instances.manage.mods.enabled", "Enabled");
        t.put("instances.manage.mods.disabled", "Disabled");
        t.put("instances.manage.mods.pageTitle", "Manage mods");
        t.put("instances.manage.scanning", "Reading instance information...");
        t.put("instances.manage.scanFailed", "Failed to read instance information: %s");
        t.put("instances.manage.summary", "%d mods, %d resource packs");
        t.put("instances.manage.manageMods", "Manage mods");
        t.put("instances.manage.manageResourcePacks", "Manage resource packs");
        t.put("instances.manage.resourcePacks.pageTitle", "Manage resource packs");
        t.put("instances.manage.resourcePacks.scanning", "Reading resource pack list...");
        t.put("instances.manage.resourcePacks.count", "%d resource packs");
        t.put("instances.manage.resourcePacks.empty", "This instance has no resource packs");
        t.put("instances.manage.resourcePacks.scanFailed", "Failed to read resource pack list: %s");
        t.put("instances.manage.resourcePacks.toggleFailed", "Failed to toggle resource pack state: %s");
        t.put("instances.manage.resourcePacks.enabled", "Enabled");
        t.put("instances.manage.resourcePacks.disabled", "Disabled");
        t.put("instances.manage.resourcePacks.version", "Version: %s");

        // MOD ཕབ་ལེན
        t.put("mods.nav", "MOD");
        t.put("shaders.nav", "Shaders");
        t.put("shaders.search.title", "Search shaders");
        t.put("shaders.search.nameHint", "Enter a shader pack name");
        t.put("shaders.search.loading", "Searching shaders...");
        t.put("shaders.search.empty", "No matching shader packs found");
        t.put("shaders.search.failed", "Failed to load shader packs: %s");
        t.put("shaders.list.meta", "Versions: %s · Downloads %s · Updated %s");
        t.put("shaders.detail.loading", "Loading shader pack details...");
        t.put("resourcepacks.nav", "Resource packs");
        t.put("resourcepacks.search.title", "Search resource packs");
        t.put("resourcepacks.search.nameHint", "Enter a resource pack name");
        t.put("resourcepacks.search.loading", "Searching resource packs...");
        t.put("resourcepacks.search.empty", "No matching resource packs found");
        t.put("resourcepacks.search.failed", "Failed to load resource packs: %s");
        t.put("resourcepacks.list.meta", "Versions: %s · Downloads %s · Updated %s");
        t.put("resourcepacks.detail.loading", "Loading resource pack details...");
        t.put("mods.search.title", "MOD འཚོལ");
        t.put("mods.search.name", "མིང");
        t.put("mods.search.nameHint", "MOD ཀྱི་མིང་འབྲིས");
        t.put("mods.search.source", "འབྱུང་ཁུངས");
        t.put("mods.search.gameVersion", "རྩེད་མོའི་པར་གཞི");
        t.put("mods.search.versionHint", "དཔེར་ན 1.20.1། སྟོང་པ་ན་ཚང་མ");
        t.put("mods.search.loader", "འཇུག་ཆས");
        t.put("mods.search.allSources", "འབྱུང་ཁུངས་ཚང་མ");
        t.put("mods.search.allLoaders", "འཇུག་ཆས་ཚང་མ");
        t.put("mods.search.button", "འཚོལ");
        t.put("mods.search.reset", "ཆ་རྐྱེན་བསྐྱར་སྒྲིག");
        t.put("mods.search.loading", "MOD འཚོལ་བཞིན...");
        t.put("mods.search.empty", "མཐུན་པའི་ MOD མ་རྙེད");
        t.put("mods.search.count", "སྤྱི་ %d གི་འབྲས་བུ");
        t.put("mods.search.failed", "MOD ལེན་མ་ཐུབ: %s");
        t.put("mods.page.previous", "སྔོན་མ");
        t.put("mods.page.next", "རྗེས་མ");
        t.put("mods.page.info", "ཤོག་ངོས %d");
        t.put("mods.source.modrinth", "Modrinth");
        t.put("mods.source.curseforge", "CurseForge");
        t.put("mods.loader.fabric", "Fabric");
        t.put("mods.loader.forge", "Forge");
        t.put("mods.loader.neoforge", "NeoForge");
        t.put("mods.loader.quilt", "Quilt");
        t.put("mods.value.unknown", "མ་ཤེས");
        t.put("mods.list.meta", "འཇུག་ཆས: %s · པར་གཞི: %s · ཕབ་ལེན %s · གསར་སྒྱུར %s");
        t.put("mods.detail.loading", "MOD ཀྱི་ཞིབ་ཕྲ་ལེན་བཞིན...");
        t.put("mods.detail.count", "སྤྱི་ %d གི་ཡིག་ཆའི་པར་གཞི");
        t.put("mods.detail.meta", "ཕབ་ལེན %s · གསར་སྒྱུར %s");
        t.put("mods.detail.versionFilter", "རྩེད་མོའི་པར་གཞི");
        t.put("mods.detail.loaderFilter", "འཇུག་ཆས");
        t.put("mods.detail.allVersions", "པར་གཞི་ཚང་མ");
        t.put("mods.detail.allLoaders", "འཇུག་ཆས་ཚང་མ");
        t.put("mods.detail.empty", "ད་ལྟའི་ཆ་རྐྱེན་དང་མཐུན་པའི་པར་གཞི་མི་འདུག");
        t.put("mods.detail.versionMeta", "སྤེལ་བ %s · ཕབ་ལེན %s · ཡིག་ཆ %s");
        t.put("mods.detail.download", "ཕབ་ལེན");
        t.put("mods.detail.downloading", "%s ཕབ་ལེན་བཞིན...");
        t.put("mods.detail.downloaded", "ཕབ་ལེན་གནས: %s");
        t.put("mods.detail.noGameDirectory", "སྔོན་ལ་རྩེད་མོའི་སྣེའུ་སྒྲིག");
        t.put("mods.detail.noInstance", "སྔོན་ལ་བཟོ་བཅོས་ཤིག་འདེམས");
        t.put("mods.detail.failed", "བྱ་བ་ཕམ་ཉེས: %s");
        t.put("mods.detail.dependencies", "བརྟེན་གཞི:");
        t.put("mods.detail.noDependencies", "བརྟེན་གཞི་མི་འདུག");
        t.put("mods.dependency.required", "དགོས་ངེས");
        t.put("mods.dependency.optional", "འདེམས་རུང");
        t.put("mods.dependency.incompatible", "མི་མཐུན");
        t.put("mods.dependency.embedded", "ནང་འདྲེས");
        t.put("mods.dependency.unknown", "མ་ཤེས");
        t.put("mod.error.curseforgeKeyMissing", "CurseForge API ལྡེ་མིག་མ་སྒྲིགས། CURSEFORGE_API_KEY ཡང་ན་ -Dcaml.curseforge.apiKey སྒྲིག");
        t.put("mod.error.downloadUrlMissing", "MOD པར་གཞི་དེ་ལ་གཞུང་ཕྱོགས་ཀྱི་ཕབ་ལེན་ཁ་བྱང་མི་འདུག: %s");
        t.put("mod.error.projectNotFound", "MOD ལས་གཞི་མ་རྙེད: %s");
        t.put("mod.error.iconUrlInvalid", "MOD རྟགས་མའི་ཁ་བྱང་མི་འགྲིག: %s");
        t.put("mod.error.iconEmpty", "MOD རྟགས་མའི་ཡིག་ཆ་སྟོང་པ་ཡིན: %s");

        // རྒྱབ་ལྗོངས་ཀྱི་བྱ་བའི་མིང
        t.put("operation.inspectGameDirectory", "རྩེད་མོའི་སྣེའུ་ཞིབ་བཤེར");
        t.put("operation.inspectInstance", "Read instance information");
        t.put("operation.openInstanceFolder", "Open instance folder");
        t.put("operation.loadInstanceModIcon", "Load mod icon");
        t.put("operation.setModDisabled", "Toggle mod state");
        t.put("operation.setResourcePackDisabled", "Toggle resource pack state");
        t.put("operation.checkModUpdates", "Check mod updates");
        t.put("operation.updateMod", "Update mod %s");
        t.put("operation.checkInstanceName", "བཟོ་བཅོས་ཀྱི་མིང་ཞིབ་བཤེར");
        t.put("operation.fetchVersionList", "གཞུང་ཕྱོགས་ %s ཐོ་གཞུང་ལེན");
        t.put("operation.installVanilla", "སྲོལ་རྒྱུན་བཟོ་བཅོས %s སྒྲིག");
        t.put("operation.fetchFabricLoader", "Fabric Loader པར་གཞི་ལེན");
        t.put("operation.fetchFabricApi", "Fabric API པར་གཞི་ལེན");
        t.put("operation.installFabric", "Fabric བཟོ་བཅོས %s སྒྲིག");
        t.put("operation.fetchForge", "Forge པར་གཞི་ལེན");
        t.put("operation.fetchNeoForge", "NeoForge པར་གཞི་ལེན");
        t.put("operation.fetchOptiFine", "OptiFine པར་གཞི་ལེན");
        t.put("operation.installLoader", "%s བཟོ་བཅོས %s སྒྲིག");
        t.put("operation.scanJava", "Java བཤེར");
        t.put("operation.addManualJava", "ལག་ཐོགས་ Java སྣོན");
        t.put("operation.saveJavaSelection", "Java འདེམས་སྒྲུག་ཉར་ཚགས");
        t.put("operation.saveMaxMemory", "དྲན་ཤེས་མང་ཤོས་ཀྱི་སྒྲིག་བཀོད་ཉར་ཚགས");
        t.put("operation.saveJvmArguments", "JVM ཚད་སྒྲིག་ཉར་ཚགས");
        t.put("operation.saveGameDirectory", "རྩེད་མོའི་སྣེའུ་ཉར་ཚགས");
        t.put("operation.saveWindowState", "སྒེའུ་ཁུང་གི་གནས་ཚུལ་ཉར་ཚགས");
        t.put("operation.saveVersionIsolation", "པར་གཞི་ལོགས་བཀར་གྱི་སྒྲིག་བཀོད་ཉར་ཚགས");
        t.put("operation.saveAutoGameLanguage", "རྩེད་མོའི་སྐད་ཡིག་རང་བཞིན་སྒྲིག་བཀོད་ཉར་ཚགས");
        t.put("operation.saveDownloadConcurrency", "ཕབ་ལེན་མཉམ་སྒྲུབ་ཨང་གྲངས་ཉར་ཚགས");
        t.put("operation.saveDownloadSource", "ཕབ་ལེན་འབྱུང་ཁུངས་ཀྱི་སྒྲིག་བཀོད་ཉར་ཚགས");
        t.put("operation.saveTheme", "བརྗོད་གཞིའི་འདེམས་སྒྲུག་ཉར་ཚགས");
        t.put("operation.saveLanguage", "སྐད་ཡིག་གི་སྒྲིག་བཀོད་ཉར་ཚགས");
        t.put("operation.saveLanguageFallback", "སྐད་ཡིག་གི་རྒྱབ་སྐྱོར་སྒྲིག་བཀོད་ཉར་ཚགས");
        t.put("operation.searchMods", "MOD འཚོལ: %s");
        t.put("operation.fetchModProject", "MOD ལས་གཞི་ལེན: %s");
        t.put("operation.downloadMod", "MOD ཕབ་ལེན: %s");
        t.put("operation.searchShaders", "Search shaders: %s");
        t.put("operation.fetchShaderProject", "Fetch shader project: %s");
        t.put("operation.downloadShader", "Download shader pack: %s");
        t.put("operation.searchResourcePacks", "Search resource packs: %s");
        t.put("operation.fetchResourcePackProject", "Fetch resource pack project: %s");
        t.put("operation.downloadResourcePack", "Download resource pack: %s");
        t.put("operation.loadModIcon", "MOD རྟགས་མ་ལེན");
        t.put("operation.saveLastInstance", "མཐའ་མའི་འགོ་འཛུགས་བཟོ་བཅོས་ཉར་ཚགས");
        t.put("operation.launchGame", "རྩེད་མོ %s འགོ་འཛུགས");

        // Quilt འཇུག་ཆས
        t.put("download.description.quilt", "Quilt Loader %s");
        t.put("download.description.quiltWithApi", "Quilt Loader %s + Quilted Fabric API %s");
        t.put("download.setup.option.quilt", "Quilt པར་གཞི་འདེམས");
        t.put("download.setup.option.quiltApi", "Quilted Fabric API པར་གཞི་འདེམས");
        t.put("download.prompt.loading.quilt", "Quilt པར་གཞི་ལེན་བཞིན་པ...");
        t.put("download.prompt.quiltFirst", "སྔོན་ལ Quilt པར་གཞི་འདེམས་རོགས");
        t.put("download.prompt.select.quilt", "Quilt པར་གཞི་འདེམས་རོགས");
        t.put("download.prompt.select.quiltApi", "Quilted Fabric API པར་གཞི་འདེམས་རོགས");
        t.put("download.prompt.empty.quilt", "Quilt པར་གཞི་མེད");
        t.put("download.prompt.empty.quiltApi", "Quilted Fabric API པར་གཞི་མེད");
        t.put("download.prompt.error.quilt", "Quilt པར་གཞི་ལེན་མ་ཐུབ");
        t.put("download.prompt.error.quiltApi", "Quilted Fabric API པར་གཞི་ལེན་མ་ཐུབ");
        t.put("download.stage.quilt.config.fetch", "Quilt སྒྲིག་བཀོད་ལེན་བཞིན་པ");
        t.put("download.stage.quilt.loader.install", "Quilt Loader སྒྲིག་བཞིན་པ");
        t.put("download.stage.quilt.api.download", "Quilted Fabric API ཕབ་ལེན་བཞིན་པ");
        t.put("download.stage.quilt.verify", "Quilt སྒྲིག་བཀོད་དཔྱད་བཞིན་པ");
        t.put("log.download.installed.quilt", "[ཕབ་ལེན] Quilt མིང་ཚིག %s སྒྲིག་ཟིན (Loader %s)");
        t.put("operation.fetchQuiltLoader", "Quilt Loader པར་གཞི་ལེན");
        t.put("operation.fetchQuiltApi", "Quilted Fabric API པར་གཞི་ལེན");
        t.put("operation.installQuilt", "Quilt མིང་ཚིག %s སྒྲིག");
    }
}

