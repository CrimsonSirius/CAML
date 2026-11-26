package org.chs.camlauncher.language.generator.lang;

import org.chs.camlauncher.language.generator.TranslationTable;

/** Общие русские тексты: учётные записи, настройки и задачи. */
final class RURUSharedTexts {
    private RURUSharedTexts() {
    }

    static void populate(TranslationTable t) {
        // Учётные записи
        t.put("account.choice.default", "Использовать учётную запись по умолчанию");
        t.put("account.choice.offline", "Офлайн-имя пользователя");
        t.put("account.default.saved", "%s теперь используется по умолчанию");
        t.put("account.error.passwordRequired", "Введите пароль");
        t.put("account.error.serverRequired", "Введите адрес сервера проверки подлинности");
        t.put("account.error.usernameRequired", "Введите имя пользователя");
        t.put("account.injector.builtin", "Использовать встроенную версию (загружается по необходимости)");
        t.put("account.injector.chooserTitle", "Выберите jar authlib-injector");
        t.put("account.injector.custom", "Свой файл: %s");
        t.put("account.injector.reset", "Использовать встроенную версию");
        t.put("account.injector.saved", "Настройка authlib-injector сохранена");
        t.put("account.instance.count",
                "Экземпляров: %d; выберите учётную запись для запуска каждого из них");
        t.put("account.instance.loading", "Загрузка учётных записей...");
        t.put("account.instance.mismatch",
                "Некоторые учётные записи экземпляров были привязаны к %s и больше здесь не действуют "
                        + "(экземпляров: %d)");
        t.put("account.instance.mismatchApplied",
                "Привязки учётных записей экземпляров (%d) применены к этому расположению игры");
        t.put("account.instance.mismatchApply", "Применить к этому расположению игры");
        t.put("account.instance.saved", "Учётная запись для экземпляра %s сохранена");
        t.put("account.instance.title", "Учётные записи экземпляров");
        t.put("account.list.default", "По умолчанию");
        t.put("account.list.empty",
                "Учётных записей пока нет. Добавьте офлайн-учётную запись, войдите через Microsoft или "
                        + "войдите на сторонний сервер.");
        t.put("account.list.needsLogin", "Сеанс истёк");
        t.put("account.list.relogin", "Войти снова");
        t.put("account.list.remove", "Удалить");
        t.put("account.login.customServer", "Свой адрес сервера");
        t.put("account.login.customServerOption", "Свой адрес");
        t.put("account.login.hint", "Пароль используется только для этого входа и не сохраняется.");
        t.put("account.login.password", "Пароль");
        t.put("account.login.reloginHint",
                "Введите пароль для %s ещё раз. Лаунчер никогда не сохраняет пароли.");
        t.put("account.login.remember", "Запомнить вход");
        t.put("account.login.running", "Выполняется вход...");
        t.put("account.login.server", "Сервер проверки подлинности");
        t.put("account.login.submit", "Войти");
        t.put("account.login.success", "Выполнен вход как %s");
        t.put("account.login.username", "Эл. почта или имя пользователя");
        t.put("account.microsoft.codeReady",
                "Откройте страницу подтверждения в браузере и введите скопированный код.");
        t.put("account.microsoft.copyCode", "Копировать код");
        t.put("account.microsoft.copyUrl", "Копировать ссылку подтверждения");
        t.put("account.microsoft.login", "Войти через Microsoft");
        t.put("account.microsoft.running", "Запрос кода устройства Microsoft...");
        t.put("account.microsoft.userCode", "Код устройства: %s");
        t.put("account.microsoft.verificationUri", "Страница подтверждения: %s");
        t.put("account.offline.add", "Добавить");
        t.put("account.offline.added", "Офлайн-учётная запись %s добавлена");
        t.put("account.offline.label", "Добавить офлайн-учётную запись");
        t.put("account.offline.placeholder", "Офлайн-имя пользователя");
        t.put("account.profile.label", "Профиль");
        t.put("account.profile.switch", "Сменить");
        t.put("account.profile.switched", "Профиль изменён на %s");
        t.put("account.removed", "%s удалена");

        // Ошибки входа
        t.put("auth.error.accountMissing", "Учётная запись не найдена: %s");
        t.put("auth.error.accountRequired", "Сначала выберите учётную запись");
        t.put("auth.error.injectorChecksum", "Не удалось проверить authlib-injector: %s");
        t.put("auth.error.injectorNotFound", "authlib-injector не найден: %s");
        t.put("auth.error.invalidCredentials", "Неверное имя пользователя или пароль для %s");
        t.put("auth.error.loginExpired", "Сеанс %s (%s) истёк, войдите снова");
        t.put("auth.error.loginRequired",
                "У учётной записи %s (%s) нет сохранённых данных для входа, войдите снова");
        t.put("auth.error.metadataUnavailable",
                "Не удалось прочитать метаданные сервера проверки подлинности: %s (HTTP %s)");
        t.put("auth.error.noAccount",
                "Учётная запись не настроена. Добавьте её в настройках или используйте офлайн-имя "
                        + "пользователя.");
        t.put("auth.error.noSelectedProfile", "У этой учётной записи нет профиля Minecraft: %s");
        t.put("auth.error.passwordRequired", "Требуется пароль");
        t.put("auth.error.profileMissing", "Этот профиль больше недоступен: %s");
        t.put("auth.error.requestFailed", "Запрос проверки подлинности не удался: %s (HTTP %s)");
        t.put("auth.error.responseInvalid", "Сервер проверки подлинности вернул неожиданный ответ: %s");
        t.put("auth.error.tooManyRequests",
                "Слишком много запросов к %s, подождите немного и повторите попытку");
        t.put("auth.error.usernameRequired", "Требуется имя пользователя");

        // Ошибки Microsoft
        t.put("auth.microsoft.accountSuspended", "Похоже, эта учётная запись Microsoft заблокирована.");
        t.put("auth.microsoft.clientIdMissing",
                "Вход через Microsoft не настроен: задайте идентификатор клиента Microsoft OAuth для CAML.");
        t.put("auth.microsoft.forbidden",
                "Текущая сеть отклонена Minecraft Services. Попробуйте другую сеть.");
        t.put("auth.microsoft.loginDeclined", "Вход через Microsoft отклонён.");
        t.put("auth.microsoft.loginExpired", "Сеанс Microsoft истёк. Войдите снова.");
        t.put("auth.microsoft.loginFailed", "Не удалось войти через Microsoft: %s");
        t.put("auth.microsoft.loginTimeout", "Код устройства Microsoft истёк. Повторите попытку.");
        t.put("auth.microsoft.missingField", "В ответе Microsoft отсутствует %s.");
        t.put("auth.microsoft.noLicense",
                "Эта учётная запись не владеет Minecraft: Java Edition или срок действия Game Pass истёк.");
        t.put("auth.microsoft.noProfile",
                "У этой учётной записи нет профиля Minecraft. Создайте его и войдите снова.");
        t.put("auth.microsoft.profileChanged",
                "При обновлении учётной записи профиль Minecraft изменился. Войдите снова.");
        t.put("auth.microsoft.requestFailed",
                "Запрос проверки подлинности Microsoft к %s не удался (HTTP %s).");
        t.put("auth.microsoft.responseInvalid", "Неожиданный ответ проверки подлинности Microsoft от %s.");
        t.put("auth.microsoft.serviceUnavailable",
                "Minecraft Services временно недоступен. Повторите попытку позже.");
        t.put("auth.microsoft.tooManyRequests",
                "Слишком много попыток входа. Подождите несколько минут и повторите.");
        t.put("auth.microsoft.wrongVerifyMethod",
                "На странице входа Microsoft используйте пароль, а не другой способ входа.");
        t.put("auth.microsoft.xboxBanned", "Эта учётная запись Microsoft забанена в Xbox Live.");
        t.put("auth.microsoft.xboxFamily",
                "Этот детский аккаунт нужно сначала добавить в семейную группу Microsoft.");
        t.put("auth.microsoft.xboxNoAccount",
                "У этой учётной записи Microsoft нет аккаунта Xbox. Создайте его и повторите попытку.");
        t.put("auth.microsoft.xboxRegion",
                "Xbox Live недоступен в текущей стране или регионе.");
        t.put("auth.microsoft.xboxUnknown", "Xbox Live отклонил вход (XErr %s).");

        // Общее
        t.put("common.close", "Закрыть");
        t.put("common.reset", "Сбросить");

        // Ограничения распаковки
        t.put("download.settings.archive.description",
                "Ограничения, применяемые при распаковке сборок модов и миров.");
        t.put("download.settings.archive.entries", "Максимум записей");
        t.put("download.settings.archive.entrySize", "Максимальный размер записи (МиБ)");
        t.put("download.settings.archive.invalid",
                "Введите корректные ограничения. Общий размер должен быть не меньше ограничения на запись.");
        t.put("download.settings.archive.saved", "Ограничения распаковки сохранены.");
        t.put("download.settings.archive.title", "Ограничения распаковки");
        t.put("download.settings.archive.totalSize", "Максимальный общий размер после распаковки (МиБ)");

        // Главная
        t.put("home.account", "Учётная запись");
        t.put("home.error.loadAccounts", "Не удалось загрузить учётные записи: %s");
        t.put("home.manageAccounts", "Учётные записи экземпляров");
        t.put("home.username.managed", "Имя пользователя берётся из выбранной учётной записи: %s");

        // Предупреждения при запуске
        t.put("launch.warning.java.force", "Всё равно запустить");
        t.put("launch.warning.java.message",
                "Для этой игры требуется Java %s, но выбрана Java %s. Принудительный запуск может не удаться.");
        t.put("launch.warning.java.stop", "Остановить");
        t.put("launch.warning.java.title", "Версия Java может быть несовместима");

        // Журнал
        t.put("log.collapse", "Свернуть");
        t.put("log.empty", "Активности пока нет");
        t.put("log.expand", "Развернуть");

        // Сборки модов
        t.put("modpack.error.invalidArchive", "Недопустимый архив сборки: %s");
        t.put("modpack.error.invalidFile", "Недопустимый путь к файлу сборки: %s");
        t.put("modpack.error.missingFileUrl", "У файла сборки нет ссылки для скачивания: %s");
        t.put("modpack.error.missingMinecraft", "В сборке не указана версия Minecraft");
        t.put("modpack.error.unsupportedLoader", "Неподдерживаемый загрузчик сборки: %s");
        t.put("modpacks.detail.install", "Установить сборку");
        t.put("modpacks.detail.installed", "Сборка установлена: %s");
        t.put("modpacks.detail.installing", "Установка сборки %s...");
        t.put("modpacks.detail.loading", "Загрузка сведений о сборке...");
        t.put("modpacks.install.instanceName", "Имя экземпляра");
        t.put("modpacks.install.instanceNameEmpty", "Имя экземпляра не может быть пустым");
        t.put("modpacks.list.meta", "Версия: %s · Загрузок %s · Обновлено %s");
        t.put("modpacks.nav", "Сборки модов");
        t.put("modpacks.search.empty", "Подходящие сборки не найдены");
        t.put("modpacks.search.failed", "Не удалось загрузить сборки: %s");
        t.put("modpacks.search.loading", "Поиск сборок...");
        t.put("modpacks.search.nameHint", "Введите название сборки");
        t.put("modpacks.search.title", "Поиск сборок модов");

        // Названия операций
        t.put("operation.addOfflineAccount", "Добавить офлайн-учётную запись");
        t.put("operation.downloadJava", "Скачать и установить Java %s");
        t.put("operation.fetchAuthServer", "Получить сервер проверки подлинности");
        t.put("operation.fetchJavaDownloadList", "Получить список Java для скачивания: %s");
        t.put("operation.fetchModpackProject", "Получить проект сборки %s");
        t.put("operation.installModpack", "Установить сборку %s");
        t.put("operation.loadAccounts", "Загрузить учётные записи");
        t.put("operation.loadInstanceAccount", "Загрузить учётную запись экземпляра");
        t.put("operation.loginAccount", "Войти в учётную запись");
        t.put("operation.loginMicrosoftAccount", "Войти через Microsoft");
        t.put("operation.openJavaFolder", "Открыть папку Java");
        t.put("operation.removeAccount", "Удалить учётную запись");
        t.put("operation.saveArchiveExtractionLimits", "Сохранить ограничения распаковки");
        t.put("operation.saveDefaultAccount", "Сохранить учётную запись по умолчанию");
        t.put("operation.saveInjectorPath", "Сохранить путь authlib-injector");
        t.put("operation.saveInstanceAccount", "Сохранить учётную запись экземпляра");
        t.put("operation.searchModpacks", "Поиск сборок %s");
        t.put("operation.switchProfile", "Переключить профиль учётной записи");

        // Настройки: о программе
        t.put("settings.about.application.title", "Лаунчер");
        t.put("settings.about.architecture", "Архитектура");
        t.put("settings.about.backend", "Бэкенд");
        t.put("settings.about.copyright", "Авторские права");
        t.put("settings.about.dataDirectory", "Каталог данных");
        t.put("settings.about.dependencies.title", "Зависимости проекта");
        t.put("settings.about.dependency.author", "Автор: %s");
        t.put("settings.about.dependency.license", "Лицензия: %s");
        t.put("settings.about.device.title", "Сведения об устройстве");
        t.put("settings.about.directories.title", "Каталоги");
        t.put("settings.about.gameDirectory", "Каталог игры");
        t.put("settings.about.javaVendor", "Поставщик Java");
        t.put("settings.about.javaVersion", "Версия Java");
        t.put("settings.about.kernelVersion", "Версия ядра");
        t.put("settings.about.license", "Лицензия");
        t.put("settings.about.memory", "Системная память");
        t.put("settings.about.notDetected", "Не обнаружено");
        t.put("settings.about.os", "Операционная система");
        t.put("settings.about.osVersion", "Версия системы");
        t.put("settings.about.project.title", "Проект");
        t.put("settings.about.projectName", "Название");
        t.put("settings.about.runtime.title", "Среда лаунчера");
        t.put("settings.about.system.title", "Система");
        t.put("settings.about.unknown", "Неизвестно");
        t.put("settings.about.version", "Версия");

        // Настройки: учётные записи
        t.put("settings.account.description",
                "Учётные записи, сохранённые лаунчером. Токены доступа шифруются перед записью на диск, "
                        + "пароли никогда не сохраняются.");
        t.put("settings.account.injector.description",
                "Лаунчер скачивает authlib-injector один раз и затем переиспользует его; при каждом запуске "
                        + "файл проверяется по SHA-256.");
        t.put("settings.account.login.description",
                "Войдите с учётной записью сервера проверки подлинности Yggdrasil (например, LittleSkin).");
        t.put("settings.account.login.title", "Вход через сторонний сервис");
        t.put("settings.account.microsoft.description",
                "Войдите через Microsoft с помощью кода устройства. Пароль никогда не вводится в лаунчере.");
        t.put("settings.account.microsoft.title", "Учётная запись Microsoft");
        t.put("settings.account.title", "Учётные записи");

        // Настройки: управление Java
        t.put("settings.javaManagement.count", "Сред выполнения Java: %d");
        t.put("settings.javaManagement.distribution", "Дистрибутив");
        t.put("settings.javaManagement.download", "Скачать Java");
        t.put("settings.javaManagement.downloadAndInstall", "Скачать и установить");
        t.put("settings.javaManagement.downloading", "Скачивание Java...");
        t.put("settings.javaManagement.downloadingProgress", "Скачивание Java... %d%%");
        t.put("settings.javaManagement.empty", "Среды выполнения Java не найдены");
        t.put("settings.javaManagement.emptyVersions", "Нет версий Java для скачивания");
        t.put("settings.javaManagement.installed", "Java установлена: %s");
        t.put("settings.javaManagement.installFailed", "Не удалось установить Java: %s");
        t.put("settings.javaManagement.linuxHint",
                "В Linux устанавливайте JDK или JRE через пакетный менеджер дистрибутива (например, apt, "
                        + "dnf или pacman). Java под управлением CAML не рекомендуется.");
        t.put("settings.javaManagement.loadingVersions", "Загрузка версий Java...");
        t.put("settings.javaManagement.loadVersionsFailed",
                "Не удалось загрузить версии Java: %s");
        t.put("settings.javaManagement.manual", "Добавлено вручную");
        t.put("settings.javaManagement.meta", "%s · %d бит%s");
        t.put("settings.javaManagement.openFolder", "Открыть содержащую папку");
        t.put("settings.javaManagement.openFolder.failed", "Не удалось открыть папку Java: %s");
        t.put("settings.javaManagement.openFolder.opening", "Открытие папки...");
        t.put("settings.javaManagement.packageType", "Тип пакета");
        t.put("settings.javaManagement.unavailable", "Недоступно");
        t.put("settings.javaManagement.version", "Версия");
        t.put("settings.javaManagement.versionCount", "Версий: %d");

        // Настройки: разделы
        t.put("settings.section.about", "О программе");
        t.put("settings.section.account", "Учётные записи");
        t.put("settings.section.java", "Управление Java");

        // Задачи
        t.put("tasks.back", "Назад к задачам");
        t.put("tasks.empty", "Задач пока нет");
        t.put("tasks.open", "Открыть задачу %s · %s");
        t.put("tasks.status.cancelled", "Отменена");
        t.put("tasks.status.completed", "Завершена");
        t.put("tasks.status.failed", "Ошибка");
        t.put("tasks.status.running", "Выполняется");
        t.put("tasks.subtitle", "Загрузки и установки");
        t.put("tasks.title", "Задачи");
    }
}

