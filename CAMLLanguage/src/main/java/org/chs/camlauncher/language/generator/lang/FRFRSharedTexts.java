package org.chs.camlauncher.language.generator.lang;

import org.chs.camlauncher.language.generator.TranslationTable;

/** Textes communs français : comptes, paramètres et tâches. */
final class FRFRSharedTexts {
    private FRFRSharedTexts() {
    }

    static void populate(TranslationTable t) {
        // Comptes
        t.put("account.choice.default", "Suivre le compte par défaut");
        t.put("account.choice.offline", "Nom d'utilisateur hors ligne");
        t.put("account.default.saved", "%s est désormais le compte par défaut");
        t.put("account.error.passwordRequired", "Veuillez saisir le mot de passe");
        t.put("account.error.serverRequired", "Veuillez saisir l'adresse du serveur d'authentification");
        t.put("account.error.usernameRequired", "Veuillez saisir le nom d'utilisateur");
        t.put("account.injector.builtin", "Utiliser la version intégrée (téléchargée si nécessaire)");
        t.put("account.injector.chooserTitle", "Sélectionner le jar authlib-injector");
        t.put("account.injector.custom", "Fichier personnalisé : %s");
        t.put("account.injector.reset", "Utiliser la version intégrée");
        t.put("account.injector.saved", "Paramètre authlib-injector enregistré");
        t.put("account.instance.count",
                "%d instances ; choisissez le compte utilisé au lancement de chacune");
        t.put("account.instance.loading", "Chargement des comptes...");
        t.put("account.instance.mismatch",
                "Certains comptes d'instance étaient liés à %s et ne s'appliquent plus ici (%d instances)");
        t.put("account.instance.mismatchApplied",
                "%d liaisons de comptes d'instance appliquées à cet emplacement de jeu");
        t.put("account.instance.mismatchApply", "Appliquer à cet emplacement de jeu");
        t.put("account.instance.saved", "Compte enregistré pour l'instance %s");
        t.put("account.instance.title", "Comptes des instances");
        t.put("account.list.default", "Par défaut");
        t.put("account.list.empty",
                "Aucun compte pour l'instant. Ajoutez un compte hors ligne, connectez-vous avec Microsoft ou "
                        + "connectez-vous à un serveur tiers.");
        t.put("account.list.needsLogin", "Connexion expirée");
        t.put("account.list.relogin", "Se reconnecter");
        t.put("account.list.remove", "Supprimer");
        t.put("account.login.customServer", "Adresse de serveur personnalisée");
        t.put("account.login.customServerOption", "Adresse personnalisée");
        t.put("account.login.hint",
                "Le mot de passe ne sert qu'à cette connexion et n'est jamais enregistré.");
        t.put("account.login.password", "Mot de passe");
        t.put("account.login.reloginHint",
                "Saisissez à nouveau le mot de passe de %s. Le launcher ne stocke jamais les mots de passe.");
        t.put("account.login.remember", "Rester connecté");
        t.put("account.login.running", "Connexion...");
        t.put("account.login.server", "Serveur d'authentification");
        t.put("account.login.submit", "Se connecter");
        t.put("account.login.success", "Connecté en tant que %s");
        t.put("account.login.username", "E-mail ou nom d'utilisateur");
        t.put("account.microsoft.codeReady",
                "Ouvrez la page de vérification dans un navigateur et saisissez le code copié.");
        t.put("account.microsoft.copyCode", "Copier le code");
        t.put("account.microsoft.copyUrl", "Copier la page de vérification");
        t.put("account.microsoft.login", "Se connecter avec Microsoft");
        t.put("account.microsoft.running", "Demande d'un code d'appareil Microsoft...");
        t.put("account.microsoft.userCode", "Code d'appareil : %s");
        t.put("account.microsoft.verificationUri", "Page de vérification : %s");
        t.put("account.offline.add", "Ajouter");
        t.put("account.offline.added", "Compte hors ligne %s ajouté");
        t.put("account.offline.label", "Ajouter un compte hors ligne");
        t.put("account.offline.placeholder", "Nom d'utilisateur hors ligne");
        t.put("account.profile.label", "Profil");
        t.put("account.profile.switch", "Changer");
        t.put("account.profile.switched", "Profil changé pour %s");
        t.put("account.removed", "%s supprimé");

        // Erreurs d'authentification
        t.put("auth.error.accountMissing", "Compte introuvable : %s");
        t.put("auth.error.accountRequired", "Veuillez d'abord choisir un compte");
        t.put("auth.error.injectorChecksum", "Échec de la vérification d'authlib-injector : %s");
        t.put("auth.error.injectorNotFound", "authlib-injector introuvable : %s");
        t.put("auth.error.invalidCredentials", "Nom d'utilisateur ou mot de passe incorrect pour %s");
        t.put("auth.error.loginExpired",
                "La connexion de %s (%s) a expiré, veuillez vous reconnecter");
        t.put("auth.error.loginRequired",
                "Le compte %s (%s) n'a pas d'identifiants enregistrés, veuillez vous reconnecter");
        t.put("auth.error.metadataUnavailable",
                "Impossible de lire les métadonnées du serveur d'authentification : %s (HTTP %s)");
        t.put("auth.error.noAccount",
                "Aucun compte n'est configuré. Ajoutez-en un dans les paramètres ou utilisez un nom "
                        + "d'utilisateur hors ligne.");
        t.put("auth.error.noSelectedProfile", "Ce compte n'a pas de profil Minecraft : %s");
        t.put("auth.error.passwordRequired", "Le mot de passe est requis");
        t.put("auth.error.profileMissing", "Ce profil n'est plus disponible : %s");
        t.put("auth.error.requestFailed", "Échec de la requête d'authentification : %s (HTTP %s)");
        t.put("auth.error.responseInvalid",
                "Le serveur d'authentification a renvoyé une réponse inattendue : %s");
        t.put("auth.error.tooManyRequests",
                "Trop de requêtes vers %s, veuillez patienter un instant et réessayer");
        t.put("auth.error.usernameRequired", "Le nom d'utilisateur est requis");

        // Erreurs Microsoft
        t.put("auth.microsoft.accountSuspended", "Ce compte Microsoft semble suspendu.");
        t.put("auth.microsoft.clientIdMissing",
                "La connexion Microsoft n'est pas configurée : définissez l'ID client OAuth Microsoft de CAML.");
        t.put("auth.microsoft.forbidden",
                "Le réseau actuel a été refusé par Minecraft Services. Essayez un autre réseau.");
        t.put("auth.microsoft.loginDeclined", "La connexion Microsoft a été refusée.");
        t.put("auth.microsoft.loginExpired", "La connexion Microsoft a expiré. Reconnectez-vous.");
        t.put("auth.microsoft.loginFailed", "Échec de la connexion Microsoft : %s");
        t.put("auth.microsoft.loginTimeout", "Le code d'appareil Microsoft a expiré. Réessayez.");
        t.put("auth.microsoft.missingField", "Il manque %s dans la réponse d'authentification Microsoft.");
        t.put("auth.microsoft.noLicense",
                "Ce compte ne possède pas Minecraft : Java Edition ou son avantage Game Pass a expiré.");
        t.put("auth.microsoft.noProfile",
                "Ce compte n'a pas de profil Minecraft. Créez-en un puis reconnectez-vous.");
        t.put("auth.microsoft.profileChanged",
                "Le profil Minecraft a changé pendant l'actualisation du compte. Reconnectez-vous.");
        t.put("auth.microsoft.requestFailed",
                "Échec de la requête d'authentification Microsoft sur %s (HTTP %s).");
        t.put("auth.microsoft.responseInvalid",
                "Réponse d'authentification Microsoft inattendue de %s.");
        t.put("auth.microsoft.serviceUnavailable",
                "Minecraft Services est temporairement indisponible. Réessayez plus tard.");
        t.put("auth.microsoft.tooManyRequests",
                "Trop de tentatives de connexion. Patientez quelques minutes et réessayez.");
        t.put("auth.microsoft.wrongVerifyMethod",
                "Utilisez votre mot de passe sur la page de connexion Microsoft plutôt qu'une autre méthode.");
        t.put("auth.microsoft.xboxBanned", "Ce compte Microsoft est banni par Xbox Live.");
        t.put("auth.microsoft.xboxFamily",
                "Ce compte enfant doit d'abord être ajouté à une famille Microsoft.");
        t.put("auth.microsoft.xboxNoAccount",
                "Ce compte Microsoft n'a pas de compte Xbox. Créez-en un et réessayez.");
        t.put("auth.microsoft.xboxRegion",
                "Xbox Live n'est pas disponible dans le pays ou la région actuelle.");
        t.put("auth.microsoft.xboxUnknown", "Xbox Live a refusé la connexion (XErr %s).");

        // Commun
        t.put("common.close", "Fermer");
        t.put("common.reset", "Réinitialiser");

        // Limites d'extraction
        t.put("download.settings.archive.description",
                "Limites appliquées lors de l'extraction des modpacks et des mondes.");
        t.put("download.settings.archive.entries", "Nombre maximal d'entrées");
        t.put("download.settings.archive.entrySize", "Taille maximale d'une entrée (MiB)");
        t.put("download.settings.archive.invalid",
                "Saisissez des limites valides. La taille totale doit être au moins égale à la limite "
                        + "par entrée.");
        t.put("download.settings.archive.saved", "Limites d'extraction enregistrées.");
        t.put("download.settings.archive.title", "Limites d'extraction");
        t.put("download.settings.archive.totalSize", "Taille totale extraite maximale (MiB)");

        // Accueil
        t.put("home.account", "Compte");
        t.put("home.error.loadAccounts", "Échec du chargement des comptes : %s");
        t.put("home.manageAccounts", "Comptes des instances");
        t.put("home.username.managed", "Le nom d'utilisateur vient du compte sélectionné : %s");

        // Avertissements de lancement
        t.put("launch.warning.java.force", "Forcer le lancement");
        t.put("launch.warning.java.message",
                "Ce jeu nécessite Java %s, mais Java %s est sélectionné. Forcer le lancement peut échouer.");
        t.put("launch.warning.java.stop", "Arrêter");
        t.put("launch.warning.java.title", "La version de Java peut être incompatible");

        // Journal
        t.put("log.collapse", "Réduire");
        t.put("log.empty", "Aucune activité pour l'instant");
        t.put("log.expand", "Développer");

        // Modpacks
        t.put("modpack.error.invalidArchive", "Archive de modpack invalide : %s");
        t.put("modpack.error.invalidFile", "Chemin de fichier de modpack invalide : %s");
        t.put("modpack.error.missingFileUrl", "Le fichier du modpack n'a pas d'URL de téléchargement : %s");
        t.put("modpack.error.missingMinecraft", "Le modpack ne déclare pas de version de Minecraft");
        t.put("modpack.error.unsupportedLoader", "Chargeur de modpack non pris en charge : %s");
        t.put("modpacks.detail.install", "Installer le modpack");
        t.put("modpacks.detail.installed", "Modpack installé : %s");
        t.put("modpacks.detail.installing", "Installation du modpack %s...");
        t.put("modpacks.detail.loading", "Chargement des détails du modpack...");
        t.put("modpacks.install.instanceName", "Nom de l'instance");
        t.put("modpacks.install.instanceNameEmpty", "Le nom de l'instance ne peut pas être vide");
        t.put("modpacks.list.meta", "Version : %s · Téléchargements %s · Mise à jour %s");
        t.put("modpacks.nav", "Modpacks");
        t.put("modpacks.search.empty", "Aucun modpack correspondant");
        t.put("modpacks.search.failed", "Échec du chargement des modpacks : %s");
        t.put("modpacks.search.loading", "Recherche de modpacks...");
        t.put("modpacks.search.nameHint", "Saisir un nom de modpack");
        t.put("modpacks.search.title", "Rechercher des modpacks");

        // Noms des opérations
        t.put("operation.addOfflineAccount", "Ajouter un compte hors ligne");
        t.put("operation.downloadJava", "Télécharger et installer Java %s");
        t.put("operation.fetchAuthServer", "Récupérer le serveur d'authentification");
        t.put("operation.fetchJavaDownloadList", "Récupérer la liste des Java à télécharger : %s");
        t.put("operation.fetchModpackProject", "Récupérer le projet de modpack %s");
        t.put("operation.installModpack", "Installer le modpack %s");
        t.put("operation.loadAccounts", "Charger les comptes");
        t.put("operation.loadInstanceAccount", "Charger le compte de l'instance");
        t.put("operation.loginAccount", "Se connecter au compte");
        t.put("operation.loginMicrosoftAccount", "Se connecter avec Microsoft");
        t.put("operation.openJavaFolder", "Ouvrir le dossier Java");
        t.put("operation.removeAccount", "Supprimer le compte");
        t.put("operation.saveArchiveExtractionLimits", "Enregistrer les limites d'extraction");
        t.put("operation.saveDefaultAccount", "Enregistrer le compte par défaut");
        t.put("operation.saveInjectorPath", "Enregistrer le chemin d'authlib-injector");
        t.put("operation.saveInstanceAccount", "Enregistrer le compte de l'instance");
        t.put("operation.searchModpacks", "Rechercher des modpacks %s");
        t.put("operation.switchProfile", "Changer de profil de compte");

        // Paramètres : à propos
        t.put("settings.about.application.title", "Lanceur");
        t.put("settings.about.architecture", "Architecture");
        t.put("settings.about.backend", "Backend");
        t.put("settings.about.copyright", "Copyright");
        t.put("settings.about.dataDirectory", "Répertoire de données");
        t.put("settings.about.dependencies.title", "Dépendances du projet");
        t.put("settings.about.dependency.author", "Auteur : %s");
        t.put("settings.about.dependency.license", "Licence : %s");
        t.put("settings.about.device.title", "Informations sur l'appareil");
        t.put("settings.about.directories.title", "Répertoires");
        t.put("settings.about.gameDirectory", "Répertoire du jeu");
        t.put("settings.about.javaVendor", "Fournisseur de Java");
        t.put("settings.about.javaVersion", "Version de Java");
        t.put("settings.about.kernelVersion", "Version du noyau");
        t.put("settings.about.license", "Licence");
        t.put("settings.about.memory", "Mémoire système");
        t.put("settings.about.notDetected", "Non détecté");
        t.put("settings.about.os", "Système d'exploitation");
        t.put("settings.about.osVersion", "Version du système");
        t.put("settings.about.project.title", "Projet");
        t.put("settings.about.projectName", "Nom");
        t.put("settings.about.runtime.title", "Environnement du launcher");
        t.put("settings.about.system.title", "Système");
        t.put("settings.about.unknown", "Inconnu");
        t.put("settings.about.version", "Version");

        // Paramètres : comptes
        t.put("settings.account.description",
                "Comptes enregistrés par le launcher. Les jetons d'accès sont chiffrés avant d'être écrits ; "
                        + "les mots de passe ne sont jamais stockés.");
        t.put("settings.account.injector.description",
                "Le launcher télécharge authlib-injector une seule fois puis le réutilise ; le fichier est "
                        + "vérifié par SHA-256 à chaque démarrage.");
        t.put("settings.account.login.description",
                "Connectez-vous avec un compte d'un serveur d'authentification Yggdrasil (par exemple "
                        + "LittleSkin).");
        t.put("settings.account.login.title", "Connexion tierce");
        t.put("settings.account.microsoft.description",
                "Connectez-vous à Microsoft avec un code d'appareil. Le mot de passe n'est jamais saisi dans "
                        + "le launcher.");
        t.put("settings.account.microsoft.title", "Compte Microsoft");
        t.put("settings.account.title", "Comptes");

        // Paramètres : gestion de Java
        t.put("settings.javaManagement.count", "%d environnements Java");
        t.put("settings.javaManagement.distribution", "Distribution");
        t.put("settings.javaManagement.download", "Télécharger Java");
        t.put("settings.javaManagement.downloadAndInstall", "Télécharger et installer");
        t.put("settings.javaManagement.downloading", "Téléchargement de Java...");
        t.put("settings.javaManagement.downloadingProgress", "Téléchargement de Java... %d%%");
        t.put("settings.javaManagement.empty", "Aucun environnement Java trouvé");
        t.put("settings.javaManagement.emptyVersions", "Aucune version de Java téléchargeable");
        t.put("settings.javaManagement.installed", "Java installé : %s");
        t.put("settings.javaManagement.installFailed", "Échec de l'installation de Java : %s");
        t.put("settings.javaManagement.linuxHint",
                "Sous Linux, installez le JDK ou le JRE avec le gestionnaire de paquets de votre distribution "
                        + "(apt, dnf ou pacman par exemple). Java géré par CAML n'est pas recommandé.");
        t.put("settings.javaManagement.loadingVersions", "Chargement des versions de Java...");
        t.put("settings.javaManagement.loadVersionsFailed",
                "Échec du chargement des versions de Java : %s");
        t.put("settings.javaManagement.manual", "Ajouté manuellement");
        t.put("settings.javaManagement.meta", "%s · %d bits%s");
        t.put("settings.javaManagement.openFolder", "Ouvrir le dossier contenant");
        t.put("settings.javaManagement.openFolder.failed", "Impossible d'ouvrir le dossier Java : %s");
        t.put("settings.javaManagement.openFolder.opening", "Ouverture du dossier...");
        t.put("settings.javaManagement.packageType", "Type de paquet");
        t.put("settings.javaManagement.unavailable", "Indisponible");
        t.put("settings.javaManagement.version", "Version");
        t.put("settings.javaManagement.versionCount", "%d versions");

        // Paramètres : sections
        t.put("settings.section.about", "À propos");
        t.put("settings.section.account", "Comptes");
        t.put("settings.section.java", "Gestion de Java");

        // Tâches
        t.put("tasks.back", "Retour aux tâches");
        t.put("tasks.empty", "Aucune tâche pour l'instant");
        t.put("tasks.open", "Ouvrir la tâche %s · %s");
        t.put("tasks.status.cancelled", "Annulée");
        t.put("tasks.status.completed", "Terminée");
        t.put("tasks.status.failed", "Échouée");
        t.put("tasks.status.running", "En cours");
        t.put("tasks.subtitle", "Téléchargements et installations");
        t.put("tasks.title", "Tâches");

        // Navigation et listes

        // Paramètres supplémentaires
    }
}
