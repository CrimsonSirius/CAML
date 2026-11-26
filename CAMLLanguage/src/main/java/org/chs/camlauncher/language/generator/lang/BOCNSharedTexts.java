package org.chs.camlauncher.language.generator.lang;

import org.chs.camlauncher.language.generator.TranslationTable;

/** རྩིས་ཐོ་དང་། སྒྲིག་བཀོད། ལས་དོན་བཅས་ཀྱི་བོད་ཡིག་སྤྱི་ཡོངས་ཀྱི་ཚིག་སྦྱོར། */
final class BOCNSharedTexts {
    private BOCNSharedTexts() {
    }

    static void populate(TranslationTable t) {
        // རྩིས་ཐོ
        t.put("account.choice.default", "སྔོན་གཏན་འབེབས་རྩིས་ཐོ་ལ་རྗེས་སུ་འབྲང་བ");
        t.put("account.choice.offline", "དྲ་མེད་བེད་སྤྱོད་མིང");
        t.put("account.default.saved", "%s ད་ནས་སྔོན་གཏན་འབེབས་རྩིས་ཐོ་རུ་བསྒྱུར་ཟིན");
        t.put("account.error.passwordRequired", "གསང་ཚིག་འཇུག་རོགས");
        t.put("account.error.serverRequired", "ངོས་འཛིན་སར་བར་གྱི་ཁ་བྱང་འཇུག་རོགས");
        t.put("account.error.usernameRequired", "བེད་སྤྱོད་མིང་འཇུག་རོགས");
        t.put("account.injector.builtin", "ནང་བཞག་པའི་ཐོན་རིམ་བེད་སྤྱོད་(དགོས་ན་ཕབ་ལེན)");
        t.put("account.injector.chooserTitle", "authlib-injector གྱི་ jar འདེམས་རོགས");
        t.put("account.injector.custom", "རང་ཉིད་ཀྱི་ཡིག་ཆ: %s");
        t.put("account.injector.reset", "ནང་བཞག་པའི་ཐོན་རིམ་བེད་སྤྱོད");
        t.put("account.injector.saved", "authlib-injector གྱི་སྒྲིག་བཀོད་ཉར་ཚགས་བྱས་ཟིན");
        t.put("account.instance.count", "དཔེ་གཞི་ %d ཡོད། རེ་རེར་འགོ་འཛུགས་སྐབས་བེད་སྤྱོད་བྱེད་པའི་རྩིས་ཐོ་འདེམས་རོགས");
        t.put("account.instance.loading", "རྩིས་ཐོ་ཀློག་བཞིན་པ...");
        t.put("account.instance.mismatch",
                "དཔེ་གཞི་འགའི་རྩིས་ཐོ་ %s ལ་སྦྲེལ་ཡོད་པས་ལུད་གནས་འདིར་མི་འཁོར་ཐུབ（དཔེ་གཞི་ %d）");
        t.put("account.instance.mismatchApplied",
                "དཔེ་གཞིའི་རྩིས་ཐོ་སྦྲེལ་བ་ %d ལུད་གནས་འདིར་བཀོལ་ཟིན");
        t.put("account.instance.mismatchApply", "ལུད་གནས་འདིར་བཀོལ་བ");
        t.put("account.instance.saved", "དཔེ་གཞི་ %s གི་རྩིས་ཐོ་ཉར་ཚགས་བྱས་ཟིན");
        t.put("account.instance.title", "དཔེ་གཞིའི་རྩིས་ཐོ");
        t.put("account.list.default", "སྔོན་གཏན་འབེབས");
        t.put("account.list.empty",
                "ད་དུང་རྩིས་ཐོ་མེད། དྲ་མེད་རྩིས་ཐོ་གསར་དུ་བཀོལ་ནམ་ཡང་ན Microsoft བརྒྱུད་ནས་ནང་འཛུལ་བྱེད་པའམ་"
                        + "ཕྱི་ཕྱོགས་ཀྱི་སར་བར་ལ་ནང་འཛུལ་བྱེད་རོགས།");
        t.put("account.list.needsLogin", "ནང་འཛུལ་དུས་ཚོད་ལས་བརྒལ་ཟིན");
        t.put("account.list.relogin", "ཡང་བསྐྱར་ནང་འཛུལ");
        t.put("account.list.remove", "བསུབ་པ");
        t.put("account.login.customServer", "རང་ཉིད་ཀྱི་སར་བར་ཁ་བྱང");
        t.put("account.login.customServerOption", "རང་ཉིད་ཀྱི་ཁ་བྱང");
        t.put("account.login.hint", "གསང་ཚིག་དེ་ནང་འཛུལ་ཐེངས་འདིར་ཁོ་ན་བེད་སྤྱོད་བྱེད་པ་དང་ནམ་ཡང་ཉར་ཚགས་མི་བྱེད།");
        t.put("account.login.password", "གསང་ཚིག");
        t.put("account.login.reloginHint",
                "%s གི་གསང་ཚིག་ཡང་བསྐྱར་འཇུག་རོགས། འགོ་འཛུགས་ཆས་ཀྱིས་གསང་ཚིག་ནམ་ཡང་ཉར་ཚགས་མི་བྱེད།");
        t.put("account.login.remember", "ནང་འཛུལ་གནས་ཚུལ་དྲན་པ");
        t.put("account.login.running", "ནང་འཛུལ་བཞིན་པ...");
        t.put("account.login.server", "ངོས་འཛིན་སར་བར");
        t.put("account.login.submit", "ནང་འཛུལ");
        t.put("account.login.success", "%s ཡི་ཚུལ་དུ་ནང་འཛུལ་བྱས་ཟིན");
        t.put("account.login.username", "གློག་འཕྲིན་ནམ་བེད་སྤྱོད་མིང");
        t.put("account.microsoft.codeReady",
                "ཚོལ་ཆས་ནང་ངོས་འཛིན་ཤོག་ངོས་ཁ་ཕྱེ་ནས་བཤུས་ཟིན་པའི་ཨང་རྟགས་འཇུག་རོགས།");
        t.put("account.microsoft.copyCode", "ཨང་རྟགས་བཤུས");
        t.put("account.microsoft.copyUrl", "ངོས་འཛིན་ཤོག་ངོས་བཤུས");
        t.put("account.microsoft.login", "Microsoft བརྒྱུད་ནས་ནང་འཛུལ");
        t.put("account.microsoft.running", "Microsoft གྱི་ཆས་ཨང་རྟགས་ཞུ་བཞིན་པ...");
        t.put("account.microsoft.userCode", "ཆས་ཨང་རྟགས: %s");
        t.put("account.microsoft.verificationUri", "ངོས་འཛིན་ཤོག་ངོས: %s");
        t.put("account.offline.add", "གསར་དུ་བཀོལ་བ");
        t.put("account.offline.added", "དྲ་མེད་རྩིས་ཐོ་ %s གསར་དུ་བཀོལ་ཟིན");
        t.put("account.offline.label", "དྲ་མེད་རྩིས་ཐོ་གསར་དུ་བཀོལ་བ");
        t.put("account.offline.placeholder", "དྲ་མེད་བེད་སྤྱོད་མིང");
        t.put("account.profile.label", "མིང་ཐོ");
        t.put("account.profile.switch", "བརྗེ་བ");
        t.put("account.profile.switched", "མིང་ཐོ་ %s ལ་བརྗེས་ཟིན");
        t.put("account.removed", "%s བསུབས་ཟིན");

        // ངོས་འཛིན་ནོར་འཁྲུལ
        t.put("auth.error.accountMissing", "རྩིས་ཐོ་རྙེད་མ་སོང: %s");
        t.put("auth.error.accountRequired", "སྔ་ལ་རྩིས་ཐོ་ཞིག་འདེམས་རོགས");
        t.put("auth.error.injectorChecksum", "authlib-injector གྱི་བརྟག་དཔྱད་ཕམ་སོང: %s");
        t.put("auth.error.injectorNotFound", "authlib-injector རྙེད་མ་སོང: %s");
        t.put("auth.error.invalidCredentials", "%s གི་བེད་སྤྱོད་མིང་ནམ་གསང་ཚིག་ནོར་འདུག");
        t.put("auth.error.loginExpired", "%s（%s）གི་ནང་འཛུལ་དུས་ཚོད་ལས་བརྒལ་ཟིན། ཡང་བསྐྱར་ནང་འཛུལ་རོགས");
        t.put("auth.error.loginRequired",
                "རྩིས་ཐོ་ %s（%s）ལ་ཉར་ཚགས་བྱས་པའི་དཔང་ཡིག་མེད། ཡང་བསྐྱར་ནང་འཛུལ་རོགས");
        t.put("auth.error.metadataUnavailable",
                "ངོས་འཛིན་སར་བར་གྱི་གནས་ཚུལ་ཀློག་མ་ཐུབ: %s（HTTP %s）");
        t.put("auth.error.noAccount",
                "རྩིས་ཐོ་སྒྲིག་མེད། སྒྲིག་བཀོད་ནང་གསར་དུ་བཀོལ་ནམ་ཡང་ན་དྲ་མེད་བེད་སྤྱོད་མིང་བེད་སྤྱོད་བྱེད་རོགས།");
        t.put("auth.error.noSelectedProfile", "རྩིས་ཐོ་འདིར་ Minecraft མིང་ཐོ་མེད: %s");
        t.put("auth.error.passwordRequired", "གསང་ཚིག་དགོས");
        t.put("auth.error.profileMissing", "མིང་ཐོ་དེ་བེད་སྤྱོད་མི་ཐུབ་སོང: %s");
        t.put("auth.error.requestFailed", "ངོས་འཛིན་ཞུ་འདུན་ཕམ་སོང: %s（HTTP %s）");
        t.put("auth.error.responseInvalid", "ངོས་འཛིན་སར་བར་གྱིས་ཚོད་མ་བཅོལ་བའི་ལན་སྤྲོད་བྱས་སོང: %s");
        t.put("auth.error.tooManyRequests",
                "%s ལ་ཞུ་འདུན་མང་དྲགས་སོང་། ཙམ་ཞིག་སྒུག་ནས་ཡང་བསྐྱར་ཚོད་ལྟ་བྱེད་རོགས");
        t.put("auth.error.usernameRequired", "བེད་སྤྱོད་མིང་དགོས");

        // Microsoft ངོས་འཛིན་ནོར་འཁྲུལ
        t.put("auth.microsoft.accountSuspended", "Microsoft རྩིས་ཐོ་འདི་བཀག་ཡོད་སྲིད།");
        t.put("auth.microsoft.clientIdMissing",
                "Microsoft ནང་འཛུལ་སྒྲིག་མེད། CAML གི་ Microsoft OAuth client ID གཏན་འབེབས་བྱེད་རོགས།");
        t.put("auth.microsoft.forbidden",
                "ད་ལྟའི་དྲ་རྒྱ་ Minecraft Services ཀྱིས་བཀག་སོང་། དྲ་རྒྱ་གཞན་ཞིག་བེད་སྤྱོད་བྱེད་རོགས།");
        t.put("auth.microsoft.loginDeclined", "Microsoft ནང་འཛུལ་བཀག་སོང་།");
        t.put("auth.microsoft.loginExpired", "Microsoft ནང་འཛུལ་དུས་ཚོད་ལས་བརྒལ་སོང་། ཡང་བསྐྱར་ནང་འཛུལ་རོགས།");
        t.put("auth.microsoft.loginFailed", "Microsoft ནང་འཛུལ་ཕམ་སོང: %s");
        t.put("auth.microsoft.loginTimeout", "Microsoft ཆས་ཨང་རྟགས་དུས་ཚོད་ལས་བརྒལ་སོང་། ཡང་བསྐྱར་ཚོད་ལྟ་བྱེད་རོགས།");
        t.put("auth.microsoft.missingField", "Microsoft ངོས་འཛིན་ལན་ནང་ %s མེད།");
        t.put("auth.microsoft.noLicense",
                "རྩིས་ཐོ་འདིར་ Minecraft: Java Edition མེད་པའམ Game Pass གི་ཐོབ་ཐང་དུས་ཚོད་ལས་བརྒལ་སོང་།");
        t.put("auth.microsoft.noProfile",
                "རྩིས་ཐོ་འདིར་ Minecraft མིང་ཐོ་མེད། མིང་ཐོ་བཟོས་ནས་ཡང་བསྐྱར་ནང་འཛུལ་རོགས།");
        t.put("auth.microsoft.profileChanged",
                "རྩིས་ཐོ་གསར་སྒྱུར་སྐབས་ Minecraft མིང་ཐོ་འགྱུར་སོང་། ཡང་བསྐྱར་ནང་འཛུལ་རོགས།");
        t.put("auth.microsoft.requestFailed", "%s ལ་ Microsoft ངོས་འཛིན་ཞུ་འདུན་ཕམ་སོང（HTTP %s）།");
        t.put("auth.microsoft.responseInvalid", "%s ནས་ Microsoft ངོས་འཛིན་གྱི་ལན་སྤྲོད་ཚོད་མ་བཅོལ་བ་བྱུང་སོང་།");
        t.put("auth.microsoft.serviceUnavailable",
                "Minecraft Services ད་ལྟ་བེད་སྤྱོད་མི་ཐུབ། ཙམ་ཞིག་རྗེས་ཡང་བསྐྱར་ཚོད་ལྟ་བྱེད་རོགས།");
        t.put("auth.microsoft.tooManyRequests",
                "ནང་འཛུལ་ཚོད་ལྟ་མང་དྲགས་སོང་། སྐར་མ་འགའ་སྒུག་ནས་ཡང་བསྐྱར་ཚོད་ལྟ་བྱེད་རོགས།");
        t.put("auth.microsoft.wrongVerifyMethod",
                "Microsoft ནང་འཛུལ་ཤོག་ངོས་སུ་གསང་ཚིག་བེད་སྤྱོད་བྱེད་རོགས། ཐབས་ལམ་གཞན་མི་བེད་སྤྱོད་བྱེད།");
        t.put("auth.microsoft.xboxBanned", "Microsoft རྩིས་ཐོ་འདི་ Xbox Live ཀྱིས་བཀག་ཡོད།");
        t.put("auth.microsoft.xboxFamily",
                "བྱིས་པའི་རྩིས་ཐོ་འདི་སྔ་ལ་ Microsoft ཁྱིམ་ཚང་ནང་བཙུགས་དགོས།");
        t.put("auth.microsoft.xboxNoAccount",
                "Microsoft རྩིས་ཐོ་འདིར་ Xbox རྩིས་ཐོ་མེད། གཅིག་བཟོས་ནས་ཡང་བསྐྱར་ཚོད་ལྟ་བྱེད་རོགས།");
        t.put("auth.microsoft.xboxRegion", "ད་ལྟའི་རྒྱལ་ཁབ་ནམ་ས་ཁུལ་དུ་ Xbox Live བེད་སྤྱོད་མི་ཐུབ།");
        t.put("auth.microsoft.xboxUnknown", "Xbox Live ཀྱིས་ནང་འཛུལ་བཀག་སོང（XErr %s）།");

        // སྤྱི་ཡོངས
        t.put("common.close", "བརྒྱབ་པ");
        t.put("common.reset", "སླར་གསོ");

        // ཕྱིར་བཏོན་གྱི་ཚད་བཀག
        t.put("download.settings.archive.description", "Mod ཚོགས་པ་དང་སྒྱུ་ལུད་ཕྱིར་བཏོན་སྐབས་བཀོལ་བའི་ཚད་བཀག");
        t.put("download.settings.archive.entries", "ནང་དོན་གྱི་ཆེ་བའི་ཚད");
        t.put("download.settings.archive.entrySize", "ནང་དོན་གཅིག་གི་ཆེ་ཆུང་ཚད་བཀག（MiB）");
        t.put("download.settings.archive.invalid",
                "ཚད་བཀག་ཡང་དག་པ་འཇུག་རོགས། སྤྱིའི་ཆེ་ཆུང་ནང་དོན་གྱི་ཚད་བཀག་ལས་ཆུང་མི་རུང་།");
        t.put("download.settings.archive.saved", "ཕྱིར་བཏོན་གྱི་ཚད་བཀག་ཉར་ཚགས་བྱས་ཟིན།");
        t.put("download.settings.archive.title", "ཕྱིར་བཏོན་གྱི་ཚད་བཀག");
        t.put("download.settings.archive.totalSize", "ཕྱིར་བཏོན་རྗེས་ཀྱི་སྤྱིའི་ཆེ་ཆུང་ཚད་བཀག（MiB）");

        // མ་ཤོག
        t.put("home.account", "རྩིས་ཐོ");
        t.put("home.error.loadAccounts", "རྩིས་ཐོ་ཀློག་མ་ཐུབ: %s");
        t.put("home.manageAccounts", "དཔེ་གཞིའི་རྩིས་ཐོ");
        t.put("home.username.managed", "བེད་སྤྱོད་མིང་དེ་འདེམས་ཟིན་པའི་རྩིས་ཐོ་ལས་ཐོབ: %s");

        // འགོ་འཛུགས་ཉེན་བརྡ
        t.put("launch.warning.java.force", "བཙན་སྐུལ་འགོ་འཛུགས");
        t.put("launch.warning.java.message",
                "ལུད་འདིར་ Java %s དགོས་ཀྱང་ Java %s འདེམས་ཡོད། བཙན་སྐུལ་འགོ་འཛུགས་ན་ཕམ་སྲིད།");
        t.put("launch.warning.java.stop", "མཚམས་འཇོག");
        t.put("launch.warning.java.title", "Java ཐོན་རིམ་མི་མཐུན་སྲིད");

        // ཐོ་དེབ
        t.put("log.collapse", "བསྡུ་བ");
        t.put("log.empty", "ད་དུང་བྱ་བ་མེད");
        t.put("log.expand", "ཁ་འབྱེད");

        // Mod ཚོགས་པ
        t.put("modpack.error.invalidArchive", "Mod ཚོགས་པའི་བཙིར་ཡིག་ཡང་དག་མིན: %s");
        t.put("modpack.error.invalidFile", "Mod ཚོགས་པའི་ཡིག་ཆའི་ལམ་ཡང་དག་མིན: %s");
        t.put("modpack.error.missingFileUrl", "Mod ཚོགས་པའི་ཡིག་ཆར་ཕབ་ལེན་ཁ་བྱང་མེད: %s");
        t.put("modpack.error.missingMinecraft", "Mod ཚོགས་པས་ Minecraft ཐོན་རིམ་བཤད་མེད");
        t.put("modpack.error.unsupportedLoader", "Mod ཚོགས་པའི་འཇུག་ཆས་རྒྱབ་སྐྱོར་མེད: %s");
        t.put("modpacks.detail.install", "Mod ཚོགས་པ་འཇུག་པ");
        t.put("modpacks.detail.installed", "Mod ཚོགས་པ་བཙུགས་ཟིན: %s");
        t.put("modpacks.detail.installing", "Mod ཚོགས་པ་ %s བཙུགས་བཞིན་པ...");
        t.put("modpacks.detail.loading", "Mod ཚོགས་པའི་ཞིབ་ཕྲ་ཀློག་བཞིན་པ...");
        t.put("modpacks.install.instanceName", "དཔེ་གཞིའི་མིང");
        t.put("modpacks.install.instanceNameEmpty", "དཔེ་གཞིའི་མིང་སྟོང་བ་མི་རུང་");
        t.put("modpacks.list.meta", "ཐོན་རིམ: %s · ཕབ་ལེན %s · གསར་སྒྱུར %s");
        t.put("modpacks.nav", "Mod ཚོགས་པ");
        t.put("modpacks.search.empty", "མཐུན་པའི་ Mod ཚོགས་པ་རྙེད་མ་སོང");
        t.put("modpacks.search.failed", "Mod ཚོགས་པ་ཀློག་མ་ཐུབ: %s");
        t.put("modpacks.search.loading", "Mod ཚོགས་པ་འཚོལ་བཞིན་པ...");
        t.put("modpacks.search.nameHint", "Mod ཚོགས་པའི་མིང་འཇུག་རོགས");
        t.put("modpacks.search.title", "Mod ཚོགས་པ་འཚོལ་བ");

        // རྒྱབ་སྐྱོར་ལས་དོན་གྱི་མིང
        t.put("operation.addOfflineAccount", "དྲ་མེད་རྩིས་ཐོ་གསར་དུ་བཀོལ་བ");
        t.put("operation.downloadJava", "Java %s ཕབ་ལེན་དང་འཇུག་པ");
        t.put("operation.fetchAuthServer", "ངོས་འཛིན་སར་བར་གྱི་གནས་ཚུལ་ལེན་པ");
        t.put("operation.fetchJavaDownloadList", "Java ཕབ་ལེན་ཐོ་ཡིག་ལེན་པ: %s");
        t.put("operation.fetchModpackProject", "Mod ཚོགས་པའི་ལས་འཆར་ལེན་པ %s");
        t.put("operation.installModpack", "Mod ཚོགས་པ་ %s འཇུག་པ");
        t.put("operation.loadAccounts", "རྩིས་ཐོ་ཀློག་པ");
        t.put("operation.loadInstanceAccount", "དཔེ་གཞིའི་རྩིས་ཐོ་ཀློག་པ");
        t.put("operation.loginAccount", "རྩིས་ཐོར་ནང་འཛུལ");
        t.put("operation.loginMicrosoftAccount", "Microsoft བརྒྱུད་ནས་ནང་འཛུལ");
        t.put("operation.openJavaFolder", "Java ཡིག་སྣོད་ཁ་ཕྱེ་བ");
        t.put("operation.removeAccount", "རྩིས་ཐོ་བསུབ་པ");
        t.put("operation.saveArchiveExtractionLimits", "ཕྱིར་བཏོན་གྱི་ཚད་བཀག་ཉར་ཚགས");
        t.put("operation.saveDefaultAccount", "སྔོན་གཏན་འབེབས་རྩིས་ཐོ་ཉར་ཚགས");
        t.put("operation.saveInjectorPath", "authlib-injector གྱི་ལམ་ཉར་ཚགས");
        t.put("operation.saveInstanceAccount", "དཔེ་གཞིའི་རྩིས་ཐོ་ཉར་ཚགས");
        t.put("operation.searchModpacks", "Mod ཚོགས་པ་འཚོལ་བ %s");
        t.put("operation.switchProfile", "རྩིས་ཐོའི་མིང་ཐོ་བརྗེ་བ");

        // སྒྲིག་བཀོད: སྐོར
        t.put("settings.about.application.title", "འགོ་འཛུགས་ཆས");
        t.put("settings.about.architecture", "གཞི་རྩའི་བཟོ་སྟངས");
        t.put("settings.about.backend", "རྒྱབ་ངོས");
        t.put("settings.about.copyright", "བཟོ་དབང");
        t.put("settings.about.dataDirectory", "གནས་ཚུལ་ཡིག་སྣོད");
        t.put("settings.about.dependencies.title", "ལས་འཆར་གྱི་རག་ལས");
        t.put("settings.about.dependency.author", "རྩོམ་པ་པོ: %s");
        t.put("settings.about.dependency.license", "ཆོག་མཆན: %s");
        t.put("settings.about.device.title", "ཆས་ཀྱི་གནས་ཚུལ");
        t.put("settings.about.directories.title", "ཡིག་སྣོད");
        t.put("settings.about.gameDirectory", "ལུད་ཀྱི་ཡིག་སྣོད");
        t.put("settings.about.javaVendor", "Java མཁོ་སྤྲོད་པ");
        t.put("settings.about.javaVersion", "Java ཐོན་རིམ");
        t.put("settings.about.kernelVersion", "སྙིང་པོའི་ཐོན་རིམ");
        t.put("settings.about.license", "ཆོག་མཆན");
        t.put("settings.about.memory", "ལག་ལེན་དྲན་པ");
        t.put("settings.about.notDetected", "རྙེད་མ་སོང");
        t.put("settings.about.os", "ལག་ལེན་མ་ལག");
        t.put("settings.about.osVersion", "མ་ལག་ཐོན་རིམ");
        t.put("settings.about.project.title", "ལས་འཆར");
        t.put("settings.about.projectName", "མིང");
        t.put("settings.about.runtime.title", "འགོ་འཛུགས་ཆས་ཀྱི་ལག་ལེན་གནས་སྟངས");
        t.put("settings.about.system.title", "མ་ལག");
        t.put("settings.about.unknown", "མི་ཤེས");
        t.put("settings.about.version", "ཐོན་རིམ");

        // སྒྲིག་བཀོད: རྩིས་ཐོ
        t.put("settings.account.description",
                "འགོ་འཛུགས་ཆས་ཀྱིས་ཉར་ཚགས་བྱས་པའི་རྩིས་ཐོ། accessToken གསང་སྦས་བྱས་རྗེས་ཀྱི་ས་མཚམས་སུ་"
                        + "བྲིས་པ་དང་། གསང་ཚིག་ནམ་ཡང་ཉར་ཚགས་མི་བྱེད།");
        t.put("settings.account.injector.description",
                "authlib-injector ཐེངས་དང་པོར་ཁོ་ན་ཕབ་ལེན་བྱས་ནས་རྟག་ཏུ་བེད་སྤྱོད་བྱེད་པ་དང་། འགོ་འཛུགས་ཐེངས་རེར་"
                        + "SHA-256 ཀྱིས་བརྟག་དཔྱད་བྱེད།");
        t.put("settings.account.login.description",
                "Yggdrasil ངོས་འཛིན་སར་བར（དཔེར་ན LittleSkin）གི་རྩིས་ཐོ་བེད་སྤྱོད་བྱས་ནས་ནང་འཛུལ་བྱེད་པ།");
        t.put("settings.account.login.title", "ཕྱི་ཕྱོགས་ནང་འཛུལ");
        t.put("settings.account.microsoft.description",
                "ཆས་ཨང་རྟགས་བེད་སྤྱོད་བྱས་ནས་ Microsoft ལ་ནང་འཛུལ་བྱེད་པ། འགོ་འཛུགས་ཆས་ཀྱིས་གསང་ཚིག་མི་ཞུ།");
        t.put("settings.account.microsoft.title", "Microsoft རྩིས་ཐོ");
        t.put("settings.account.title", "རྩིས་ཐོ");

        // སྒྲིག་བཀོད: Java དོ་དམ
        t.put("settings.javaManagement.count", "Java ལག་ལེན་ %d");
        t.put("settings.javaManagement.distribution", "ཁྱབ་བསྒྲགས");
        t.put("settings.javaManagement.download", "Java ཕབ་ལེན");
        t.put("settings.javaManagement.downloadAndInstall", "ཕབ་ལེན་དང་འཇུག་པ");
        t.put("settings.javaManagement.downloading", "Java ཕབ་ལེན་བཞིན་པ...");
        t.put("settings.javaManagement.downloadingProgress", "Java ཕབ་ལེན་བཞིན་པ... %d%%");
        t.put("settings.javaManagement.empty", "Java ལག་ལེན་རྙེད་མ་སོང");
        t.put("settings.javaManagement.emptyVersions", "ཕབ་ལེན་བྱེད་ཆོག་པའི་ Java ཐོན་རིམ་མེད");
        t.put("settings.javaManagement.installed", "Java བཙུགས་ཟིན: %s");
        t.put("settings.javaManagement.installFailed", "Java འཇུག་མ་ཐུབ: %s");
        t.put("settings.javaManagement.linuxHint",
                "Linux ཐོག་ཁྱབ་བསྒྲགས་ཀྱི་ཐུམ་སྒྲིག་དོ་དམ་（apt、dnf ནམ་ pacman）བེད་སྤྱོད་བྱས་ནས་"
                        + "JDK ནམ་ JRE འཇུག་པ་ལེགས། CAML ཀྱིས་དོ་དམ་བྱེད་པའི་ Java བེད་སྤྱོད་མི་འོས།");
        t.put("settings.javaManagement.loadingVersions", "Java ཐོན་རིམ་ཀློག་བཞིན་པ...");
        t.put("settings.javaManagement.loadVersionsFailed", "Java ཐོན་རིམ་ཀློག་མ་ཐུབ: %s");
        t.put("settings.javaManagement.manual", "ལག་ཐོག་ནས་བཀོལ་བ");
        t.put("settings.javaManagement.meta", "%s · %d བིཊ%s");
        t.put("settings.javaManagement.openFolder", "ཡིག་སྣོད་ཁ་ཕྱེ་བ");
        t.put("settings.javaManagement.openFolder.failed", "Java ཡིག་སྣོད་ཁ་ཕྱེ་མ་ཐུབ: %s");
        t.put("settings.javaManagement.openFolder.opening", "ཡིག་སྣོད་ཁ་ཕྱེ་བཞིན་པ...");
        t.put("settings.javaManagement.packageType", "ཐུམ་སྒྲིག་གི་རིགས");
        t.put("settings.javaManagement.unavailable", "བེད་སྤྱོད་མི་ཐུབ");
        t.put("settings.javaManagement.version", "ཐོན་རིམ");
        t.put("settings.javaManagement.versionCount", "ཐོན་རིམ་ %d");

        // སྒྲིག་བཀོད: ཚན་པ
        t.put("settings.section.about", "སྐོར");
        t.put("settings.section.account", "རྩིས་ཐོ");
        t.put("settings.section.java", "Java དོ་དམ");

        // ལས་དོན
        t.put("tasks.back", "ལས་དོན་ཐོ་ཡིག་ལ་ཕྱིར་ལོག");
        t.put("tasks.empty", "ད་དུང་ལས་དོན་མེད");
        t.put("tasks.open", "ལས་དོན %s ཁ་ཕྱེ་བ · %s");
        t.put("tasks.status.cancelled", "རྩིས་མེད་བྱས་ཟིན");
        t.put("tasks.status.completed", "ལེགས་གྲུབ");
        t.put("tasks.status.failed", "ཕམ་སོང");
        t.put("tasks.status.running", "བྱེད་བཞིན་པ");
        t.put("tasks.subtitle", "ཕབ་ལེན་དང་འཇུག་པའི་ལས་དོན");
        t.put("tasks.title", "ལས་དོན");

        // ཕབ་ལེན་ཚན་པ

        // དཔེ་གཞི་དོ་དམ

        // ས་ཁྲ

        // འོད་གཟུགས

        // གཏེར་ཁ

        // ལས་དོན་གྱི་མིང་གཞན
    }
}

