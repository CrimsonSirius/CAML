package org.chs.camlauncher.language.generator.lang;

import org.chs.camlauncher.language.generator.LanguageTranslationGenerator;
import org.chs.camlauncher.language.generator.TranslationTable;

/** Traductions françaises. */
public final class FRFRGenerator extends LanguageTranslationGenerator {
    @Override
    protected String locale() {
        return "fr_fr";
    }

    @Override
    protected void populate(TranslationTable t) {
        // Général
        t.put("common.back", "Retour");
        t.put("common.cancel", "Annuler");
        t.put("common.browse", "Parcourir");
        t.put("common.save", "Enregistrer");

        // Navigation et pied de page
        t.put("nav.home", "Accueil");
        t.put("nav.downloads", "Téléchargements");
        t.put("nav.settings", "Paramètres");
        t.put("app.subtitle", "Lanceur Minecraft");
        t.put("footer.status", "CAML %s · Backend %s · Répertoire de données : %s");
        t.put("footer.backend.ready", "Prêt");
        t.put("footer.backend.notReady", "Non prêt");

        // Formulaire de lancement
        t.put("home.username", "Nom d’utilisateur hors ligne");
        t.put("home.launch", "Lancer le jeu");
        t.put("home.launching", "Lancement...");
        t.put("home.browseInstances", "Parcourir les instances");
        t.put("home.manageInstances", "Gérer les instances");
        t.put("home.instance.current", "Instance actuelle : %s");
        t.put("home.instance.current.none", "Instance actuelle : non sélectionnée");
        t.put("home.gameDirectory.none", "Répertoire de jeu : non défini");
        t.put("home.gameDirectory.missing", "Répertoire de jeu : introuvable · %s");
        t.put("home.gameDirectory.value", "Répertoire de jeu : %s");
        t.put("home.saveLocation.none", "Emplacement des sauvegardes : non défini");
        t.put("home.saveLocation.fixDirectory", "Emplacement des sauvegardes : corrigez d’abord le répertoire de jeu");
        t.put("home.saveLocation.selectInstance", "Emplacement des sauvegardes : sélectionnez d’abord une instance");
        t.put("home.saveLocation.value", "Emplacement des sauvegardes : %s");
        t.put("home.saveLocation.isolated", " (isolation complète de version)");
        t.put("home.error.inspectDirectory", "Échec de l’inspection du répertoire de jeu : %s");
        t.put("home.error.noGameDirectory", "Enregistrez d’abord le répertoire de jeu dans les paramètres");
        t.put("home.error.missingGameDirectory", "Le répertoire de jeu n’existe pas ; enregistrez un répertoire valide dans les paramètres");
        t.put("home.error.noInstance", "Parcourez et sélectionnez d’abord une instance de jeu");
        t.put("home.error.noUsername", "Saisissez un nom d’utilisateur hors ligne");

        // Journal
        t.put("log.title", "Journal d’exécution");
        t.put("log.subtitle", "Progression du téléchargement et sortie du processus de jeu");
        t.put("log.accessible", "Journal d’exécution");
        t.put("log.download.installed.vanilla", "[Téléchargement] Instance %s installée (vanilla %s)");
        t.put("log.download.installed.fabric", "[Téléchargement] Instance Fabric %s installée (Loader %s)");
        t.put("log.download.installed.loader", "[Téléchargement] Instance %s %s installée");
        t.put("log.download.progress", "[Téléchargement] %s (%d/%d)");
        t.put("log.download.progressNoTotal", "[Téléchargement] %s (%d)");
        t.put("log.gameProcess.exit", "Le processus de jeu s’est terminé avec le code : %d");
        t.put("log.gameProcess.exitError", "Le processus de jeu s’est terminé anormalement avec le code : %d");
        t.put("log.error.operationFailed", "[%s] échec : %s");

        // État du lancement
        t.put("status.starting", "Démarrage de %s...");
        t.put("status.started", "Commande de lancement envoyée");
        t.put("status.launchFailed", "Échec du lancement !");
        t.put("status.launchFailedDetail", "Échec du lancement : %s");
        t.put("error.unknown", "Une erreur inconnue s’est produite. Consultez les journaux.");
        t.put("download.error.failed", "Échec du téléchargement de %s après %s tentatives (dernière adresse : %s)");
        t.put("network.requestFailed", "Échec de la requête vers %s (dernière adresse : %s)");
        t.put("network.timeout", "Délai de la requête dépassé (%s ms) : %s");
        t.put("network.httpError", "HTTP %s : %s");
        t.put("network.responseInvalid", "Échec de l’analyse de la réponse : %s");
        t.put("network.redirectLocationMissing", "Redirection sans en-tête Location : %s");
        t.put("network.tooManyRedirects", "Trop de redirections : %s");
        t.put("network.noSource", "Aucune source de téléchargement disponible : %s");
        t.put("metadata.manifestEmpty", "Le manifeste des versions est vide");
        t.put("metadata.versionJsonEmpty", "Le JSON de version est vide");
        t.put("metadata.versionJsonHashMismatch", "Échec de la vérification SHA-1 du JSON de version : %s");
        t.put("launch.permissionDenied", "Permission refusée lors de l’exécution de Java ou de l’accès au répertoire de jeu. Vérifiez les permissions des fichiers.");
        t.put("launch.executableNotFound", "Exécutable Java introuvable. Sélectionnez à nouveau une installation Java.");
        t.put("launch.commandTooLong", "La commande de lancement est trop longue. Raccourcissez le chemin du jeu ou le classpath.");
        t.put("launch.commandTooLongAfterFallback", "La commande de lancement reste trop longue même après le repli sur argfile et manifest. Raccourcissez le chemin du jeu ou le classpath.");
        t.put("launch.processStartFailed", "Échec du démarrage du processus de jeu. Consultez les journaux pour plus de détails.");
        t.put("launch.manualJavaNotConfigured", "La sélection manuelle de Java est activée, mais aucun chemin Java n’est sélectionné.");
        t.put("launch.manualJavaUnavailable", "Le Java sélectionné est indisponible : %s");
        t.put("launch.javaRequirementNotFound", "Aucun Java satisfaisant l’exigence de version %s n’a été trouvé. Installez ou sélectionnez Java.");

        // Catégories et sources
        t.put("download.category.release", "Version finale");
        t.put("download.category.snapshot", "Snapshot");
        t.put("download.category.old", "Ancienne");
        t.put("download.category.unknown", "Version inconnue");
        t.put("download.source.mirror", "Miroir");
        t.put("download.source.official", "Officiel");
        t.put("download.source.custom", "Personnalisé");

        // Liste des versions
        t.put("download.versions.loading", "Chargement de la liste %2$s depuis %1$s...");
        t.put("download.versions.empty", "La source de téléchargement n’a renvoyé aucune version %s téléchargeable");
        t.put("download.versions.count", "%d versions %s, de la plus récente à la plus ancienne");
        t.put("download.versions.error", "Échec du chargement de la liste %s : %s");
        t.put("download.versions.accessible", "%s, %s");

        // Descriptions
        t.put("download.description.optifine", "Forge %s + OptiFine %s");
        t.put("download.description.forge", "Forge %s");
        t.put("download.description.fabric", "Fabric Loader %s");
        t.put("download.description.fabricWithApi", "Fabric Loader %s + Fabric API %s");
        t.put("download.description.neoforge", "NeoForge %s");
        t.put("download.description.vanilla", "Minecraft %s");

        // Page de configuration du téléchargement
        t.put("download.setup.status.idle", "Télécharge le jeu vanilla lorsqu’aucun loader n’est sélectionné.");
        t.put("download.setup.instanceName", "Nom de l’instance");
        t.put("download.setup.download", "Télécharger l’instance");
        t.put("download.setup.option.versionInfo", "Informations de version");
        t.put("download.setup.option.forge", "Sélectionner la version de Forge");
        t.put("download.setup.option.optifine", "Sélectionner la version d’OptiFine");
        t.put("download.setup.option.fabric", "Sélectionner la version de Fabric");
        t.put("download.setup.option.fabricApi", "Sélectionner la version de Fabric API");
        t.put("download.setup.option.neoforge", "Sélectionner la version de NeoForge");
        t.put("download.setup.versionInfo", "Minecraft %s · %s");
        t.put("download.setup.versionInfoWithDate", "Minecraft %s · %s · %s");
        t.put("download.setup.optifineSwitched", "Forge et OptiFine sont incompatibles ; passage à Forge %s");
        t.put("download.setup.optifineIncompatibleForge", "Le Forge actuel est incompatible avec OptiFine");
        t.put("download.setup.noCompatibleOptiFine", "Aucune version d’OptiFine n’est compatible avec le Forge actuel");
        t.put("download.setup.optifineForgeConflict", "Forge %s n’est pas compatible avec OptiFine %s. Choisissez Forge %s ou une version antérieure.");
        t.put("download.setup.nameEmpty", "Le nom de l’instance ne peut pas être vide");
        t.put("download.setup.nameDotSegments", "Le nom de l’instance ne peut pas être . ou ..");
        t.put("download.setup.nameTrailing", "Le nom de l’instance ne peut pas se terminer par un point ou un espace");
        t.put("download.setup.nameIllegalCharacter", "Le nom de l’instance contient un caractère illégal : %s");
        t.put("download.setup.nameExists", "Ce nom d’instance existe déjà : %s. Choisissez un autre nom.");
        t.put("download.setup.noGameDirectory", "Le répertoire de jeu n’est pas défini");
        t.put("download.setup.checkingName", "Vérification du nom de l’instance...");
        t.put("download.setup.nameSameAsLoader", "Lors de l’installation d’un loader, le nom de l’instance ne peut pas être identique à la version du jeu");
        t.put("download.setup.nameAvailable", "Le nom de l’instance est disponible");
        t.put("download.setup.checkNameFailed", "Échec de la vérification du nom de l’instance : %s");
        t.put("download.setup.nameNotChecked", "Le nom de l’instance n’a pas encore été validé ; réessayez dans un instant.");
        t.put("download.setup.noGameDirectoryLong", "Le répertoire de jeu n’est pas défini ; enregistrez-le d’abord dans les paramètres.");
        t.put("download.setup.optifineRequiresForge", "OptiFine ne peut être installé qu’avec Forge.");

        // Invites des loaders
        t.put("download.prompt.loading.fabric", "Chargement des versions de Fabric...");
        t.put("download.prompt.loading.forge", "Chargement des versions de Forge...");
        t.put("download.prompt.loading.neoforge", "Chargement des versions de NeoForge...");
        t.put("download.prompt.fabricFirst", "Sélectionnez d’abord une version de Fabric");
        t.put("download.prompt.forgeFirst", "Sélectionnez d’abord une version de Forge");
        t.put("download.prompt.fabricApiFirst", "Sélectionnez d’abord une version de Fabric");
        t.put("download.prompt.select.fabric", "Sélectionnez une version de Fabric");
        t.put("download.prompt.select.forge", "Sélectionnez une version de Forge");
        t.put("download.prompt.select.neoforge", "Sélectionnez une version de NeoForge");
        t.put("download.prompt.select.optifine", "Sélectionnez une version d’OptiFine");
        t.put("download.prompt.select.fabricApi", "Sélectionnez une version de Fabric API");
        t.put("download.prompt.empty.fabric", "Aucune version de Fabric disponible");
        t.put("download.prompt.empty.fabricApi", "Aucune version de Fabric API disponible");
        t.put("download.prompt.empty.forge", "Aucune version de Forge disponible");
        t.put("download.prompt.empty.neoforge", "Aucune version de NeoForge disponible");
        t.put("download.prompt.empty.optifine", "Aucune version d’OptiFine disponible");
        t.put("download.prompt.error.fabric", "Échec du chargement des versions de Fabric");
        t.put("download.prompt.error.fabricApi", "Échec du chargement des versions de Fabric API");
        t.put("download.prompt.error.forge", "Échec du chargement des versions de Forge");
        t.put("download.prompt.error.neoforge", "Échec du chargement des versions de NeoForge");
        t.put("download.prompt.error.optifine", "Échec du chargement des versions d’OptiFine");

        // Page de progression
        t.put("download.progress.title", "Téléchargement de %s");
        t.put("download.progress.subtitle", "%s · Minecraft %s");
        t.put("download.progress.back", "Retour aux téléchargements");
        t.put("download.progress.speed", "Vitesse du réseau");
        t.put("download.progress.speedValue", "0 B/s");
        t.put("download.progress.progress", "Progression du téléchargement");
        t.put("download.progress.preparing", "Préparation du téléchargement");
        t.put("download.progress.waiting", "En attente de la tâche de téléchargement...");
        t.put("download.progress.completed", "Téléchargement terminé");
        t.put("download.progress.completedTotal", "Téléchargement terminé");
        t.put("download.progress.total", "Total %s");
        t.put("download.progress.totalWithMax", "Total %s / %s");
        t.put("download.progress.failed", "Échec du téléchargement : %s");
        t.put("download.progress.currentFile", "Fichier actuel : %s");
        t.put("download.progress.currentFileWithBytes", "Fichier actuel : %s · %s / %s");
        t.put("download.progress.finishedHint", "L’instance est installée ; vous pouvez revenir à la liste.");
        t.put("download.progress.failedHint", "Téléchargement interrompu ; revenez à la liste et réessayez.");
        t.put("download.progress.bytesDetail", "%.1f%% · %s / %s · fichiers %d / %d");
        t.put("download.progress.filesDetail", "Fichiers %d / %d");
        t.put("download.progress.planning", "Préparation du plan de téléchargement...");

        // Étapes
        t.put("download.stage.preparing", "Préparation du téléchargement");
        t.put("download.stage.completed", "Terminé");
        t.put("download.stage.failed", "Échec du téléchargement");
        t.put("download.stage.unknownError", "Erreur inconnue");
        t.put("download.stage.version.fetch", "Récupération des métadonnées de version");
        t.put("download.stage.version.checkLocal", "Vérification de la version locale");
        t.put("download.stage.version.downloadFiles", "Téléchargement des fichiers de jeu et des bibliothèques");
        t.put("download.stage.version.downloadAssets", "Téléchargement des ressources");
        t.put("download.stage.version.writeJson", "Écriture des métadonnées de version");
        t.put("download.stage.loader.installer.prepare", "Préparation de l’installateur %s");
        t.put("download.stage.loader.installer.download", "Téléchargement de l’installateur %s");
        t.put("download.stage.loader.installer.run", "Exécution de l’installateur %s");
        t.put("download.stage.loader.libraries.download", "Téléchargement des bibliothèques %s");
        t.put("download.stage.loader.files.merge", "Fusion des fichiers %s");
        t.put("download.stage.loader.verify", "Vérification de l’installation %s");
        t.put("download.stage.fabric.config.fetch", "Récupération de la configuration Fabric");
        t.put("download.stage.fabric.loader.install", "Installation de Fabric Loader");
        t.put("download.stage.fabric.api.download", "Téléchargement de Fabric API");
        t.put("download.stage.fabric.verify", "Vérification de l’installation Fabric");
        t.put("download.stage.optifine.installer.download", "Téléchargement de l’installateur OptiFine");
        t.put("download.stage.optifine.installer.run", "Exécution de l’installateur OptiFine");
        t.put("download.stage.optifine.files.merge", "Fusion des fichiers OptiFine");

        // Paramètres de téléchargement
        t.put("download.settings.source", "Source de téléchargement");
        t.put("download.settings.source.description", "Choisissez l’adresse de téléchargement préférée pour les versions, les bibliothèques et les ressources.");
        t.put("download.settings.source.mirror", "Préférer la source miroir");
        t.put("download.settings.source.official", "Préférer la source officielle");
        t.put("download.settings.source.custom", "Importer une API personnalisée");
        t.put("download.settings.customApi.placeholder", "ex. https://bmclapi2.bangbang93.com");
        t.put("download.settings.concurrency", "Parallélisme de téléchargement");
        t.put("download.settings.concurrency.description", "Nombre de fichiers téléchargés simultanément, de 1 à 256.");
        t.put("download.settings.concurrency.label", "Parallélisme de téléchargement");
        t.put("download.settings.concurrency.value", "Parallélisme actuel : %d");
        t.put("download.settings.concurrency.accessible", "Parallélisme de téléchargement");

        // Navigation des paramètres
        t.put("settings.section.game", "Paramètres du jeu");
        t.put("settings.section.download", "Paramètres de téléchargement");
        t.put("settings.section.appearance", "Apparence");

        // Répertoire de jeu
        t.put("settings.game.location.title", "Répertoire de jeu");
        t.put("settings.game.location.description", "Définissez le répertoire de données de Minecraft. Les modifications ne s’appliquent qu’après l’enregistrement.");
        t.put("settings.game.location.browse", "Parcourir");
        t.put("settings.game.location.save", "Enregistrer");
        t.put("settings.game.location.chooserTitle", "Choisir le répertoire de jeu");
        t.put("settings.game.location.notSaved", "Non enregistré");
        t.put("settings.game.location.empty", "Le répertoire de jeu ne peut pas être vide");
        t.put("settings.game.location.invalid", "Format du répertoire de jeu invalide");
        t.put("settings.game.location.checking", "Vérification du répertoire...");
        t.put("settings.game.location.missing", "Le répertoire n’existe pas : %s");
        t.put("settings.game.location.saved", "Répertoire de jeu enregistré");
        t.put("settings.game.location.checkFailed", "Échec de la vérification du répertoire : %s");

        // Mémoire
        t.put("settings.game.memory.title", "Mémoire");
        t.put("settings.game.memory.accessible", "Mémoire maximale");
        t.put("settings.game.memory.description", "Ajustez la mémoire utilisée au lancement du jeu.");
        t.put("settings.game.memory.label", "Allocation mémoire");
        t.put("settings.game.memory.hint", "Faites glisser la poignée pour ajuster l’allocation du jeu ; la barre grise indique la mémoire utilisée + jeu, la barre gris clair à droite la mémoire inutilisée.");
        t.put("settings.game.memory.summary", "Utilisée %d Mo / Jeu %d Mo / Disponible %d Mo / Totale %d Mo");

        // Isolation des versions
        t.put("settings.game.isolation.title", "Isolation des versions");
        t.put("settings.game.isolation.full", "Isolation complète des versions");
        t.put("settings.game.isolation.none", "Aucune isolation des versions");

        // Langue du jeu
        t.put("settings.game.language.title", "Langue du jeu");
        t.put("settings.game.language.description", "Définit automatiquement la langue du jeu sur celle du lanceur lors du premier lancement d’une instance.");
        t.put("settings.game.language.enabled", "Changement auto");
        t.put("settings.game.language.disabled", "Désactivé");
        t.put("settings.game.language.hint", "S’applique uniquement si l’instance n’a pas encore de langue configurée ; une langue déjà définie dans options.txt est conservée. Le chinois personnalisé correspond au chinois simplifié.");

        // Java
        t.put("settings.game.java.title", "Java");
        t.put("settings.game.java.description", "Choisissez l’environnement Java utilisé pour lancer le jeu. Le mode automatique sélectionne un Java conforme aux exigences de la version du jeu.");
        t.put("settings.game.java.auto", "Automatique");
        t.put("settings.game.java.manual", "Manuel");
        t.put("settings.game.java.rescan", "Rescanner");
        t.put("settings.game.java.add", "Ajouter Java");
        t.put("settings.game.java.scanning", "Recherche de Java...");
        t.put("settings.game.java.scanningAll", "Analyse du système à la recherche de Java...");
        t.put("settings.game.java.scanFailedPrompt", "Échec de la recherche de Java");
        t.put("settings.game.java.scanFailed", "Échec de la recherche de Java : %s");
        t.put("settings.game.java.noneFound", "Aucun Java trouvé");
        t.put("settings.game.java.select", "Sélectionnez un Java");
        t.put("settings.game.java.autoSelected", "Automatique");
        t.put("settings.game.java.chooserTitle", "Choisir l’exécutable Java");
        t.put("settings.game.java.checking", "Vérification de Java...");
        t.put("settings.game.java.addFailed", "Échec de l’ajout de Java : %s");
        t.put("settings.game.java.autoHint", "Le mode automatique sélectionne, parmi les résultats de l’analyse, un Java conforme aux exigences de la version du jeu actuelle.");
        t.put("settings.game.java.selectOrAdd", "Sélectionnez un Java ou cliquez sur « Ajouter Java ».");
        t.put("settings.game.java.unavailable", "Ce Java est actuellement indisponible : %s");
        t.put("settings.game.java.detail", "Version %s · %s · %s · %d bits%s");
        t.put("settings.game.java.jdk", " · JDK");
        t.put("settings.game.java.jre", " · JRE");

        // Apparence
        t.put("settings.appearance.title", "Apparence");
        t.put("settings.appearance.description", "Ajustez l’apparence du lanceur.");
        t.put("settings.appearance.themeHint", "Les thèmes proviennent de ~/.caml/themes ; les modifications s’appliquent immédiatement");
        t.put("settings.appearance.theme", "Thème");
        t.put("settings.appearance.theme.accessible", "Thème");
        t.put("settings.appearance.language.title", "Langue");
        t.put("settings.appearance.language.description", "Choisissez comment la langue de l’interface du lanceur est sélectionnée.");
        t.put("settings.appearance.language.auto", "Automatique");
        t.put("settings.appearance.language.manual", "Manuel");
        t.put("settings.appearance.language.label", "Langue de l’interface");
        t.put("settings.appearance.language.hint", "Le mode automatique suit la langue du système ; le mode manuel utilise la langue sélectionnée ci-dessous.");
        t.put("settings.appearance.language.accessible", "Langue de l’interface");
        t.put("settings.appearance.languageFallback.title", "Repli de langue");
        t.put("settings.appearance.languageFallback.description", "Choisissez la langue qui fournit les traductions manquantes.");
        t.put("settings.appearance.languageFallback.system", "Langue du système");
        t.put("settings.appearance.languageFallback.default", "Langue par défaut");
        t.put("settings.appearance.languageFallback.key", "Afficher les clés brutes");
        t.put("settings.appearance.languageFallback.custom", "Langue personnalisée");
        t.put("settings.appearance.languageFallback.hint", "La langue du système n’utilise que la langue détectée ; la langue par défaut utilise toujours l’anglais ; les clés brutes désactivent le repli ; la langue personnalisée n’utilise que la langue sélectionnée. Sauf pour la langue par défaut, le texte manquant s’affiche sous forme de clés brutes.");
        t.put("settings.appearance.languageFallback.accessible", "Repli de langue");

        // Sélection d’instance
        t.put("instances.picker.scanning", "Analyse des instances de jeu...");
        t.put("instances.picker.title", "Sélectionner une instance de jeu");
        t.put("instances.picker.noGameDirectory", "Le répertoire de jeu n’est pas défini");
        t.put("instances.picker.gameDirectory", "Répertoire de jeu : %s");
        t.put("instances.picker.noGameDirectoryHint", "Le répertoire de jeu n’est pas défini ; enregistrez-le d’abord dans les paramètres");
        t.put("instances.picker.missing", "Le répertoire de jeu n’existe pas : %s");
        t.put("instances.picker.empty", "Aucune instance installée dans le répertoire de jeu actuel");
        t.put("instances.picker.count", "%d instances de jeu");
        t.put("instances.picker.scanFailed", "Échec de l’analyse des instances de jeu : %s");
        t.put("instances.manage.title", "Gérer les instances");

        // Téléchargement de mods
        t.put("mods.nav", "Mods");
        t.put("mods.search.title", "Rechercher des mods");
        t.put("mods.search.name", "Nom");
        t.put("mods.search.nameHint", "Saisissez un nom de mod");
        t.put("mods.search.source", "Source");
        t.put("mods.search.gameVersion", "Version du jeu");
        t.put("mods.search.versionHint", "p. ex. 1.20.1 ; laissez vide pour toutes");
        t.put("mods.search.loader", "Loader");
        t.put("mods.search.allSources", "Toutes les sources");
        t.put("mods.search.allLoaders", "Tous les loaders");
        t.put("mods.search.button", "Rechercher");
        t.put("mods.search.reset", "Réinitialiser les filtres");
        t.put("mods.search.loading", "Recherche de mods...");
        t.put("mods.search.empty", "Aucun mod correspondant trouvé");
        t.put("mods.search.count", "%d résultats");
        t.put("mods.search.failed", "Échec du chargement des mods : %s");
        t.put("mods.page.previous", "Précédent");
        t.put("mods.page.next", "Suivant");
        t.put("mods.page.info", "Page %d");
        t.put("mods.source.modrinth", "Modrinth");
        t.put("mods.source.curseforge", "CurseForge");
        t.put("mods.loader.fabric", "Fabric");
        t.put("mods.loader.forge", "Forge");
        t.put("mods.loader.neoforge", "NeoForge");
        t.put("mods.loader.quilt", "Quilt");
        t.put("mods.value.unknown", "Inconnu");
        t.put("mods.list.meta", "Loaders : %s · Versions : %s · Téléchargements %s · Mis à jour %s");
        t.put("mods.detail.loading", "Chargement des détails du mod...");
        t.put("mods.detail.count", "%d versions de fichiers");
        t.put("mods.detail.meta", "Téléchargements %s · Mis à jour %s");
        t.put("mods.detail.versionFilter", "Version du jeu");
        t.put("mods.detail.loaderFilter", "Loader");
        t.put("mods.detail.allVersions", "Toutes les versions");
        t.put("mods.detail.allLoaders", "Tous les loaders");
        t.put("mods.detail.empty", "Aucune version ne correspond aux filtres actuels");
        t.put("mods.detail.versionMeta", "Publié %s · Téléchargements %s · Fichier %s");
        t.put("mods.detail.download", "Télécharger");
        t.put("mods.detail.downloading", "Téléchargement de %s...");
        t.put("mods.detail.downloaded", "Téléchargé dans : %s");
        t.put("mods.detail.noGameDirectory", "Définissez d’abord le répertoire de jeu");
        t.put("mods.detail.noInstance", "Sélectionnez d’abord une instance de jeu");
        t.put("mods.detail.failed", "Échec de l’opération : %s");
        t.put("mods.detail.dependencies", "Dépendances :");
        t.put("mods.detail.noDependencies", "Aucune dépendance");
        t.put("mods.dependency.required", "Obligatoire");
        t.put("mods.dependency.optional", "Facultative");
        t.put("mods.dependency.incompatible", "Incompatible");
        t.put("mods.dependency.embedded", "Intégrée");
        t.put("mods.dependency.unknown", "Inconnue");
        t.put("mod.error.curseforgeKeyMissing", "La clé d’API CurseForge n’est pas configurée. Définissez CURSEFORGE_API_KEY ou -Dcaml.curseforge.apiKey.");
        t.put("mod.error.downloadUrlMissing", "Aucune URL de téléchargement officielle n’est disponible pour cette version du mod : %s");
        t.put("mod.error.projectNotFound", "Projet de mod introuvable : %s");
        t.put("mod.error.iconUrlInvalid", "URL de l’icône du mod invalide : %s");
        t.put("mod.error.iconEmpty", "L’icône du mod est vide : %s");

        // Opérations en arrière-plan
        t.put("operation.inspectGameDirectory", "Inspecter le répertoire de jeu");
        t.put("operation.checkInstanceName", "Vérifier le nom de l’instance");
        t.put("operation.fetchVersionList", "Récupérer la liste officielle %s");
        t.put("operation.installVanilla", "Installer l’instance vanilla %s");
        t.put("operation.fetchFabricLoader", "Récupérer les versions de Fabric Loader");
        t.put("operation.fetchFabricApi", "Récupérer les versions de Fabric API");
        t.put("operation.installFabric", "Installer l’instance Fabric %s");
        t.put("operation.fetchForge", "Récupérer les versions de Forge");
        t.put("operation.fetchNeoForge", "Récupérer les versions de NeoForge");
        t.put("operation.fetchOptiFine", "Récupérer les versions d’OptiFine");
        t.put("operation.installLoader", "Installer l’instance %s %s");
        t.put("operation.scanJava", "Scanner Java");
        t.put("operation.addManualJava", "Ajouter un Java manuel");
        t.put("operation.saveJavaSelection", "Enregistrer la sélection Java");
        t.put("operation.saveMaxMemory", "Enregistrer la mémoire maximale");
        t.put("operation.saveGameDirectory", "Enregistrer le répertoire de jeu");
        t.put("operation.saveVersionIsolation", "Enregistrer le paramètre d’isolation des versions");
        t.put("operation.saveAutoGameLanguage", "Enregistrer le paramètre de langue automatique du jeu");
        t.put("operation.saveDownloadConcurrency", "Enregistrer le parallélisme de téléchargement");
        t.put("operation.saveDownloadSource", "Enregistrer la source de téléchargement");
        t.put("operation.saveTheme", "Enregistrer la sélection de thème");
        t.put("operation.saveLanguage", "Enregistrer le paramètre de langue");
        t.put("operation.saveLanguageFallback", "Enregistrer le paramètre de repli de langue");
        t.put("operation.searchMods", "Rechercher des mods : %s");
        t.put("operation.fetchModProject", "Récupérer le projet de mod : %s");
        t.put("operation.downloadMod", "Télécharger le mod : %s");
        t.put("operation.loadModIcon", "Charger l’icône du mod");
        t.put("operation.saveLastInstance", "Enregistrer la dernière instance lancée");
        t.put("operation.launchGame", "Lancer le jeu %s");

        // Chargeur Quilt
        t.put("download.description.quilt", "Quilt Loader %s");
        t.put("download.description.quiltWithApi", "Quilt Loader %s + Quilted Fabric API %s");
        t.put("download.setup.option.quilt", "Sélectionner une version de Quilt");
        t.put("download.setup.option.quiltApi", "Sélectionner une version de Quilted Fabric API");
        t.put("download.prompt.loading.quilt", "Chargement des versions de Quilt...");
        t.put("download.prompt.quiltFirst", "Sélectionnez d’abord une version de Quilt");
        t.put("download.prompt.select.quilt", "Sélectionner une version de Quilt");
        t.put("download.prompt.select.quiltApi", "Sélectionner une version de Quilted Fabric API");
        t.put("download.prompt.empty.quilt", "Aucune version de Quilt disponible");
        t.put("download.prompt.empty.quiltApi", "Aucune version de Quilted Fabric API disponible");
        t.put("download.prompt.error.quilt", "Échec du chargement des versions de Quilt");
        t.put("download.prompt.error.quiltApi", "Échec du chargement des versions de Quilted Fabric API");
        t.put("download.stage.quilt.config.fetch", "Récupération de la configuration Quilt");
        t.put("download.stage.quilt.loader.install", "Installation de Quilt Loader");
        t.put("download.stage.quilt.api.download", "Téléchargement de Quilted Fabric API");
        t.put("download.stage.quilt.verify", "Vérification de l’installation de Quilt");
        t.put("log.download.installed.quilt", "[Téléchargement] Instance Quilt %s installée (Loader %s)");
        t.put("operation.fetchQuiltLoader", "Récupérer les versions de Quilt Loader");
        t.put("operation.fetchQuiltApi", "Récupérer les versions de Quilted Fabric API");
        t.put("operation.installQuilt", "Installer l’instance Quilt %s");
    }
}

