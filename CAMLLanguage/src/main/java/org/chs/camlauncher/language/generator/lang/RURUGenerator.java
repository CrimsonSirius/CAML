package org.chs.camlauncher.language.generator.lang;

import org.chs.camlauncher.language.generator.LanguageTranslationGenerator;
import org.chs.camlauncher.language.generator.TranslationTable;

/** Русские переводы. */
public final class RURUGenerator extends LanguageTranslationGenerator {
    @Override
    protected String locale() {
        return "ru_ru";
    }

    @Override
    protected void populate(TranslationTable t) {
        // Общее
        t.put("common.back", "Назад");
        t.put("common.cancel", "Отмена");
        t.put("common.browse", "Обзор");
        t.put("common.save", "Сохранить");

        // Навигация и подвал
        t.put("nav.home", "Главная");
        t.put("nav.downloads", "Загрузки");
        t.put("nav.settings", "Настройки");
        t.put("app.subtitle", "Лаунчер Minecraft");
        t.put("footer.status", "CAML %s · Бэкенд %s · Каталог данных: %s");
        t.put("footer.backend.ready", "Готов");
        t.put("footer.backend.notReady", "Не готов");

        // Форма запуска
        t.put("home.username", "Имя пользователя в офлайн-режиме");
        t.put("home.launch", "Запустить игру");
        t.put("home.launching", "Запуск...");
        t.put("home.browseInstances", "Обзор сборок");
        t.put("home.manageInstances", "Управление сборками");
        t.put("home.instance.current", "Текущая сборка: %s");
        t.put("home.instance.current.none", "Текущая сборка: не выбрана");
        t.put("home.gameDirectory.none", "Каталог игры: не задан");
        t.put("home.gameDirectory.missing", "Каталог игры: отсутствует · %s");
        t.put("home.gameDirectory.value", "Каталог игры: %s");
        t.put("home.saveLocation.none", "Каталог сохранений: не задан");
        t.put("home.saveLocation.fixDirectory", "Каталог сохранений: сначала исправьте каталог игры");
        t.put("home.saveLocation.selectInstance", "Каталог сохранений: сначала выберите сборку");
        t.put("home.saveLocation.value", "Каталог сохранений: %s");
        t.put("home.saveLocation.isolated", " (полная изоляция версий)");
        t.put("home.error.inspectDirectory", "Не удалось проверить каталог игры: %s");
        t.put("home.error.noGameDirectory", "Сначала сохраните каталог игры в настройках");
        t.put("home.error.missingGameDirectory", "Каталог игры не существует; сохраните корректный каталог в настройках");
        t.put("home.error.noInstance", "Сначала выберите сборку в списке");
        t.put("home.error.noUsername", "Введите имя пользователя для офлайн-режима");

        // Журнал
        t.put("log.title", "Журнал выполнения");
        t.put("log.subtitle", "Ход загрузки и вывод процесса игры");
        t.put("log.accessible", "Журнал выполнения");
        t.put("log.download.installed.vanilla", "[Загрузка] Сборка %s установлена (vanilla %s)");
        t.put("log.download.installed.fabric", "[Загрузка] Сборка Fabric %s установлена (Loader %s)");
        t.put("log.download.installed.loader", "[Загрузка] Сборка %s %s установлена");
        t.put("log.download.progress", "[Загрузка] %s (%d/%d)");
        t.put("log.download.progressNoTotal", "[Загрузка] %s (%d)");
        t.put("log.gameProcess.exit", "Процесс игры завершился с кодом: %d");
        t.put("log.gameProcess.exitError", "Процесс игры аварийно завершился с кодом: %d");
        t.put("log.error.operationFailed", "[%s] ошибка: %s");

        // Состояние запуска
        t.put("status.starting", "Запуск %s...");
        t.put("status.started", "Команда запуска отправлена");
        t.put("status.launchFailed", "Не удалось запустить игру!");
        t.put("status.launchFailedDetail", "Не удалось запустить игру: %s");
        t.put("error.unknown", "Произошла неизвестная ошибка. Проверьте журнал.");
        t.put("download.error.failed", "Не удалось скачать %s после %s попыток (последний адрес: %s)");
        t.put("network.requestFailed", "Не удалось выполнить запрос %s (последний адрес: %s)");
        t.put("network.timeout", "Превышено время ожидания запроса (%s мс): %s");
        t.put("network.httpError", "HTTP %s: %s");
        t.put("network.responseInvalid", "Не удалось разобрать ответ: %s");
        t.put("network.redirectLocationMissing", "В перенаправлении отсутствует заголовок Location: %s");
        t.put("network.tooManyRedirects", "Слишком много перенаправлений: %s");
        t.put("network.noSource", "Нет доступного источника загрузки: %s");
        t.put("metadata.manifestEmpty", "Манифест версий пуст");
        t.put("metadata.versionJsonEmpty", "JSON версии пуст");
        t.put("metadata.versionJsonHashMismatch", "Не удалось проверить SHA-1 JSON версии: %s");
        t.put("launch.permissionDenied", "Нет разрешения на запуск Java или доступ к каталогу игры. Проверьте права доступа к файлам.");
        t.put("launch.executableNotFound", "Исполняемый файл Java не найден. Выберите установку Java заново.");
        t.put("launch.commandTooLong", "Команда запуска слишком длинная. Сократите путь к игре или classpath.");
        t.put("launch.commandTooLongAfterFallback", "Команда запуска всё ещё слишком длинная даже после перехода на argfile и манифест. Сократите путь к игре или classpath.");
        t.put("launch.processStartFailed", "Не удалось запустить процесс игры. Подробности смотрите в журнале.");
        t.put("launch.manualJavaNotConfigured", "Включён ручной выбор Java, но путь к Java не выбран.");
        t.put("launch.manualJavaUnavailable", "Выбранная Java недоступна: %s");
        t.put("launch.javaRequirementNotFound", "Не найдена Java, удовлетворяющая требованию версии %s. Установите или выберите Java.");

        // Категории и источники
        t.put("download.category.release", "Релиз");
        t.put("download.category.snapshot", "Снимок");
        t.put("download.category.old", "Старая");
        t.put("download.category.unknown", "Неизвестная версия");
        t.put("download.source.mirror", "Зеркало");
        t.put("download.source.official", "Официальный источник");
        t.put("download.source.custom", "Пользовательский");

        // Список версий
        t.put("download.versions.loading", "Загрузка списка %2$s из %1$s...");
        t.put("download.versions.empty", "Источник загрузки не вернул ни одной доступной версии %s");
        t.put("download.versions.count", "Всего %d версий %s, сначала новые по времени выпуска");
        t.put("download.versions.error", "Не удалось загрузить список %s: %s");
        t.put("download.versions.accessible", "%s, %s");

        // Описания
        t.put("download.description.optifine", "Forge %s + OptiFine %s");
        t.put("download.description.forge", "Forge %s");
        t.put("download.description.fabric", "Fabric Loader %s");
        t.put("download.description.fabricWithApi", "Fabric Loader %s + Fabric API %s");
        t.put("download.description.neoforge", "NeoForge %s");
        t.put("download.description.vanilla", "Minecraft %s");

        // Страница настройки загрузки
        t.put("download.setup.status.idle", "Скачивает ванильную игру, если загрузчик не выбран.");
        t.put("download.setup.instanceName", "Название сборки");
        t.put("download.setup.download", "Скачать сборку");
        t.put("download.setup.option.versionInfo", "Сведения о версии");
        t.put("download.setup.option.forge", "Выбрать версию Forge");
        t.put("download.setup.option.optifine", "Выбрать версию OptiFine");
        t.put("download.setup.option.fabric", "Выбрать версию Fabric");
        t.put("download.setup.option.fabricApi", "Выбрать версию Fabric API");
        t.put("download.setup.option.neoforge", "Выбрать версию NeoForge");
        t.put("download.setup.versionInfo", "Minecraft %s · %s");
        t.put("download.setup.versionInfoWithDate", "Minecraft %s · %s · %s");
        t.put("download.setup.optifineSwitched", "Forge и OptiFine несовместимы; выполнено переключение на Forge %s");
        t.put("download.setup.optifineIncompatibleForge", "Текущая версия Forge несовместима с OptiFine");
        t.put("download.setup.noCompatibleOptiFine", "Нет версии OptiFine, совместимой с текущей версией Forge");
        t.put("download.setup.optifineForgeConflict", "Forge %s несовместим с OptiFine %s. Выберите Forge %s или более раннюю версию.");
        t.put("download.setup.nameEmpty", "Название сборки не может быть пустым");
        t.put("download.setup.nameDotSegments", "Название сборки не может быть . или ..");
        t.put("download.setup.nameTrailing", "Название сборки не может заканчиваться точкой или пробелом");
        t.put("download.setup.nameIllegalCharacter", "Название сборки содержит недопустимый символ: %s");
        t.put("download.setup.nameExists", "Сборка с таким названием уже существует: %s. Выберите другое название.");
        t.put("download.setup.noGameDirectory", "Каталог игры не задан");
        t.put("download.setup.checkingName", "Проверка названия сборки...");
        t.put("download.setup.nameSameAsLoader", "При установке загрузчика название сборки не может совпадать с версией игры");
        t.put("download.setup.nameAvailable", "Название сборки доступно");
        t.put("download.setup.checkNameFailed", "Не удалось проверить название сборки: %s");
        t.put("download.setup.nameNotChecked", "Название сборки ещё не проверено; повторите попытку позже.");
        t.put("download.setup.noGameDirectoryLong", "Каталог игры не задан; сначала сохраните его в настройках.");
        t.put("download.setup.optifineRequiresForge", "OptiFine можно установить только вместе с Forge.");

        // Подсказки загрузчиков
        t.put("download.prompt.loading.fabric", "Загрузка версий Fabric...");
        t.put("download.prompt.loading.forge", "Загрузка версий Forge...");
        t.put("download.prompt.loading.neoforge", "Загрузка версий NeoForge...");
        t.put("download.prompt.fabricFirst", "Сначала выберите версию Fabric");
        t.put("download.prompt.forgeFirst", "Сначала выберите версию Forge");
        t.put("download.prompt.fabricApiFirst", "Сначала выберите версию Fabric");
        t.put("download.prompt.select.fabric", "Выберите версию Fabric");
        t.put("download.prompt.select.forge", "Выберите версию Forge");
        t.put("download.prompt.select.neoforge", "Выберите версию NeoForge");
        t.put("download.prompt.select.optifine", "Выберите версию OptiFine");
        t.put("download.prompt.select.fabricApi", "Выберите версию Fabric API");
        t.put("download.prompt.empty.fabric", "Нет доступных версий Fabric");
        t.put("download.prompt.empty.fabricApi", "Нет доступных версий Fabric API");
        t.put("download.prompt.empty.forge", "Нет доступных версий Forge");
        t.put("download.prompt.empty.neoforge", "Нет доступных версий NeoForge");
        t.put("download.prompt.empty.optifine", "Нет доступных версий OptiFine");
        t.put("download.prompt.error.fabric", "Не удалось загрузить версии Fabric");
        t.put("download.prompt.error.fabricApi", "Не удалось загрузить версии Fabric API");
        t.put("download.prompt.error.forge", "Не удалось загрузить версии Forge");
        t.put("download.prompt.error.neoforge", "Не удалось загрузить версии NeoForge");
        t.put("download.prompt.error.optifine", "Не удалось загрузить версии OptiFine");

        // Страница прогресса
        t.put("download.progress.title", "Загрузка %s");
        t.put("download.progress.subtitle", "%s · Minecraft %s");
        t.put("download.progress.back", "Вернуться к загрузкам");
        t.put("download.progress.speed", "Скорость сети");
        t.put("download.progress.speedValue", "0 B/s");
        t.put("download.progress.progress", "Ход загрузки");
        t.put("download.progress.preparing", "Подготовка загрузки");
        t.put("download.progress.waiting", "Ожидание задачи загрузки...");
        t.put("download.progress.completed", "Загрузка завершена");
        t.put("download.progress.completedTotal", "Загрузка завершена");
        t.put("download.progress.total", "Всего %s");
        t.put("download.progress.totalWithMax", "Всего %s / %s");
        t.put("download.progress.failed", "Ошибка загрузки: %s");
        t.put("download.progress.currentFile", "Текущий файл: %s");
        t.put("download.progress.currentFileWithBytes", "Текущий файл: %s · %s / %s");
        t.put("download.progress.finishedHint", "Сборка установлена; можно вернуться к списку.");
        t.put("download.progress.failedHint", "Загрузка остановлена; вернитесь к списку и повторите попытку.");
        t.put("download.progress.bytesDetail", "%.1f%% · %s / %s · файлы %d / %d");
        t.put("download.progress.filesDetail", "Файлы %d / %d");
        t.put("download.progress.planning", "Подготовка плана загрузки...");

        // Этапы загрузки
        t.put("download.stage.preparing", "Подготовка загрузки");
        t.put("download.stage.completed", "Готово");
        t.put("download.stage.failed", "Ошибка загрузки");
        t.put("download.stage.unknownError", "Неизвестная ошибка");
        t.put("download.stage.version.fetch", "Получение метаданных версии");
        t.put("download.stage.version.checkLocal", "Проверка локальной версии");
        t.put("download.stage.version.downloadFiles", "Загрузка файлов игры и библиотек");
        t.put("download.stage.version.downloadAssets", "Загрузка ресурсов");
        t.put("download.stage.version.writeJson", "Запись метаданных версии");
        t.put("download.stage.loader.installer.prepare", "Подготовка установщика %s");
        t.put("download.stage.loader.installer.download", "Загрузка установщика %s");
        t.put("download.stage.loader.installer.run", "Запуск установщика %s");
        t.put("download.stage.loader.libraries.download", "Загрузка библиотек %s");
        t.put("download.stage.loader.files.merge", "Объединение файлов %s");
        t.put("download.stage.loader.verify", "Проверка установки %s");
        t.put("download.stage.fabric.config.fetch", "Получение конфигурации Fabric");
        t.put("download.stage.fabric.loader.install", "Установка Fabric Loader");
        t.put("download.stage.fabric.api.download", "Загрузка Fabric API");
        t.put("download.stage.fabric.verify", "Проверка установки Fabric");
        t.put("download.stage.optifine.installer.download", "Загрузка установщика OptiFine");
        t.put("download.stage.optifine.installer.run", "Запуск установщика OptiFine");
        t.put("download.stage.optifine.files.merge", "Объединение файлов OptiFine");

        // Настройки загрузки
        t.put("download.settings.source", "Источник загрузки");
        t.put("download.settings.source.description", "Выберите предпочтительный адрес загрузки версий, библиотек и ресурсов.");
        t.put("download.settings.source.mirror", "Предпочитать зеркало");
        t.put("download.settings.source.official", "Предпочитать официальный источник");
        t.put("download.settings.source.custom", "Импортировать пользовательский API");
        t.put("download.settings.customApi.placeholder", "например https://bmclapi2.bangbang93.com");
        t.put("download.settings.concurrency", "Параллельных загрузок");
        t.put("download.settings.concurrency.description", "Количество одновременно загружаемых файлов, от 1 до 256.");
        t.put("download.settings.concurrency.label", "Параллельных загрузок");
        t.put("download.settings.concurrency.value", "Текущее значение: %d");
        t.put("download.settings.concurrency.accessible", "Параллельные загрузки");

        // Навигация настроек
        t.put("settings.section.game", "Настройки игры");
        t.put("settings.section.download", "Настройки загрузки");
        t.put("settings.section.appearance", "Оформление");

        // Каталог игры
        t.put("settings.game.location.title", "Каталог игры");
        t.put("settings.game.location.description", "Задайте каталог данных Minecraft. Изменения применяются только после сохранения.");
        t.put("settings.game.location.browse", "Обзор");
        t.put("settings.game.location.save", "Сохранить");
        t.put("settings.game.location.chooserTitle", "Выберите каталог игры");
        t.put("settings.game.location.notSaved", "Не сохранено");
        t.put("settings.game.location.empty", "Каталог игры не может быть пустым");
        t.put("settings.game.location.invalid", "Недопустимый формат каталога игры");
        t.put("settings.game.location.checking", "Проверка каталога...");
        t.put("settings.game.location.missing", "Каталог не существует: %s");
        t.put("settings.game.location.saved", "Каталог игры сохранён");
        t.put("settings.game.location.checkFailed", "Не удалось проверить каталог: %s");

        // Память
        t.put("settings.game.memory.title", "Память");
        t.put("settings.game.memory.accessible", "Максимум памяти");
        t.put("settings.game.memory.description", "Настройте объём памяти, используемый при запуске игры.");
        t.put("settings.game.memory.label", "Выделение памяти");
        t.put("settings.game.memory.hint", "Перетащите ползунок, чтобы настроить выделение памяти для игры; серая полоса показывает занятую память вместе с игрой, светло-серая справа — свободную память.");
        t.put("settings.game.memory.summary", "Занято %d МБ / Игра %d МБ / Доступно %d МБ / Всего %d МБ");

        // Изоляция версий
        t.put("settings.game.isolation.title", "Изоляция версий");
        t.put("settings.game.isolation.full", "Полная изоляция версий");
        t.put("settings.game.isolation.none", "Без изоляции версий");

        // Язык игры
        t.put("settings.game.language.title", "Язык игры");
        t.put("settings.game.language.description", "При первом запуске сборки автоматически устанавливает язык игры, совпадающий с языком лаунчера.");
        t.put("settings.game.language.enabled", "Автопереключение");
        t.put("settings.game.language.disabled", "Выкл.");
        t.put("settings.game.language.hint", "Действует, только если язык сборки ещё не задан; уже указанный в options.txt язык сохраняется. «Народный китайский» сопоставляется с упрощённым китайским.");

        // Java
        t.put("settings.game.java.title", "Java");
        t.put("settings.game.java.description", "Выберите среду Java для запуска игры. В автоматическом режиме выбирается Java, соответствующая требованиям версии игры.");
        t.put("settings.game.java.auto", "Автоматически");
        t.put("settings.game.java.manual", "Вручную");
        t.put("settings.game.java.rescan", "Пересканировать");
        t.put("settings.game.java.add", "Добавить Java");
        t.put("settings.game.java.scanning", "Поиск Java...");
        t.put("settings.game.java.scanningAll", "Сканирование системы на наличие Java...");
        t.put("settings.game.java.scanFailedPrompt", "Не удалось выполнить поиск Java");
        t.put("settings.game.java.scanFailed", "Не удалось выполнить поиск Java: %s");
        t.put("settings.game.java.noneFound", "Java не найдена");
        t.put("settings.game.java.select", "Выберите Java");
        t.put("settings.game.java.autoSelected", "Автоматически");
        t.put("settings.game.java.chooserTitle", "Выберите исполняемый файл Java");
        t.put("settings.game.java.checking", "Проверка Java...");
        t.put("settings.game.java.addFailed", "Не удалось добавить Java: %s");
        t.put("settings.game.java.autoHint", "В автоматическом режиме из результатов сканирования выбирается Java, соответствующая требованиям текущей версии игры.");
        t.put("settings.game.java.selectOrAdd", "Выберите Java или нажмите «Добавить Java».");
        t.put("settings.game.java.unavailable", "Эта Java сейчас недоступна: %s");
        t.put("settings.game.java.detail", "Версия %s · %s · %s · %d бит%s");
        t.put("settings.game.java.jdk", " · JDK");
        t.put("settings.game.java.jre", " · JRE");

        // Оформление
        t.put("settings.appearance.title", "Оформление");
        t.put("settings.appearance.description", "Настройте внешний вид лаунчера.");
        t.put("settings.appearance.themeHint", "Темы находятся в ~/.caml/themes; изменения применяются сразу");
        t.put("settings.appearance.theme", "Тема");
        t.put("settings.appearance.theme.accessible", "Тема");
        t.put("settings.appearance.language.title", "Язык");
        t.put("settings.appearance.language.description", "Выберите способ определения языка интерфейса лаунчера.");
        t.put("settings.appearance.language.auto", "Автоматически");
        t.put("settings.appearance.language.manual", "Вручную");
        t.put("settings.appearance.language.label", "Язык интерфейса");
        t.put("settings.appearance.language.hint", "Автоматический режим следует языку системы; ручной использует язык, выбранный ниже.");
        t.put("settings.appearance.language.accessible", "Язык интерфейса");
        t.put("settings.appearance.languageFallback.title", "Резервный язык");
        t.put("settings.appearance.languageFallback.description", "Выберите язык, который восполняет отсутствующие переводы.");
        t.put("settings.appearance.languageFallback.system", "Язык системы");
        t.put("settings.appearance.languageFallback.default", "Язык по умолчанию");
        t.put("settings.appearance.languageFallback.key", "Показывать ключи");
        t.put("settings.appearance.languageFallback.custom", "Пользовательский язык");
        t.put("settings.appearance.languageFallback.hint", "Язык системы использует только определённый язык; язык по умолчанию всегда использует английский; показ ключей отключает резерв; пользовательский язык использует только выбранный язык. Кроме языка по умолчанию, отсутствующий текст показывается как ключи.");
        t.put("settings.appearance.languageFallback.accessible", "Резервный язык");

        // Выбор сборки
        t.put("instances.picker.scanning", "Сканирование сборок...");
        t.put("instances.picker.title", "Выберите сборку");
        t.put("instances.picker.noGameDirectory", "Каталог игры не задан");
        t.put("instances.picker.gameDirectory", "Каталог игры: %s");
        t.put("instances.picker.noGameDirectoryHint", "Каталог игры не задан; сначала сохраните его в настройках");
        t.put("instances.picker.missing", "Каталог игры не существует: %s");
        t.put("instances.picker.empty", "В текущем каталоге игры нет установленных сборок");
        t.put("instances.picker.count", "Всего сборок: %d");
        t.put("instances.picker.scanFailed", "Не удалось просканировать сборки: %s");
        t.put("instances.manage.title", "Управление сборками");
        t.put("instances.manage.instance", "Сборка: %s");
        t.put("instances.manage.folder", "Папка сборки: %s");
        t.put("instances.manage.openFolder", "Открыть папку сборки");
        t.put("instances.manage.openFolder.opening", "Открытие папки сборки...");
        t.put("instances.manage.openFolder.failed", "Не удалось открыть папку сборки: %s");
        t.put("instances.manage.noGameDirectory", "Каталог игры не задан");
        t.put("instances.manage.noInstance", "Сборка не выбрана");
        t.put("instances.manage.mods.title", "Список модов");
        t.put("instances.manage.mods.scanning", "Чтение списка модов...");
        t.put("instances.manage.mods.count", "Модов: %d");
        t.put("instances.manage.mods.empty", "В этой сборке нет модов");
        t.put("instances.manage.mods.scanFailed", "Не удалось прочитать список модов: %s");
        t.put("instances.manage.mods.toggleFailed", "Не удалось изменить состояние мода: %s");
        t.put("instances.manage.mods.enabled", "Включён");
        t.put("instances.manage.mods.disabled", "Отключён");

        // Загрузка модов
        t.put("mods.nav", "Моды");
        t.put("mods.search.title", "Поиск модов");
        t.put("mods.search.name", "Название");
        t.put("mods.search.nameHint", "Введите название мода");
        t.put("mods.search.source", "Источник");
        t.put("mods.search.gameVersion", "Версия игры");
        t.put("mods.search.versionHint", "например 1.20.1; оставьте пустым для всех");
        t.put("mods.search.loader", "Загрузчик");
        t.put("mods.search.allSources", "Все источники");
        t.put("mods.search.allLoaders", "Все загрузчики");
        t.put("mods.search.button", "Найти");
        t.put("mods.search.reset", "Сбросить фильтры");
        t.put("mods.search.loading", "Поиск модов...");
        t.put("mods.search.empty", "Подходящие моды не найдены");
        t.put("mods.search.count", "Результатов: %d");
        t.put("mods.search.failed", "Не удалось загрузить моды: %s");
        t.put("mods.page.previous", "Назад");
        t.put("mods.page.next", "Далее");
        t.put("mods.page.info", "Страница %d");
        t.put("mods.source.modrinth", "Modrinth");
        t.put("mods.source.curseforge", "CurseForge");
        t.put("mods.loader.fabric", "Fabric");
        t.put("mods.loader.forge", "Forge");
        t.put("mods.loader.neoforge", "NeoForge");
        t.put("mods.loader.quilt", "Quilt");
        t.put("mods.value.unknown", "Неизвестно");
        t.put("mods.list.meta", "Загрузчики: %s · Версии: %s · Загрузок %s · Обновлён %s");
        t.put("mods.detail.loading", "Загрузка сведений о моде...");
        t.put("mods.detail.count", "Файловых версий: %d");
        t.put("mods.detail.meta", "Загрузок %s · Обновлён %s");
        t.put("mods.detail.versionFilter", "Версия игры");
        t.put("mods.detail.loaderFilter", "Загрузчик");
        t.put("mods.detail.allVersions", "Все версии");
        t.put("mods.detail.allLoaders", "Все загрузчики");
        t.put("mods.detail.empty", "Нет версий, соответствующих текущим фильтрам");
        t.put("mods.detail.versionMeta", "Опубликован %s · Загрузок %s · Файл %s");
        t.put("mods.detail.download", "Скачать");
        t.put("mods.detail.downloading", "Загрузка %s...");
        t.put("mods.detail.downloaded", "Скачано в: %s");
        t.put("mods.detail.noGameDirectory", "Сначала задайте каталог игры");
        t.put("mods.detail.noInstance", "Сначала выберите сборку");
        t.put("mods.detail.failed", "Операция не удалась: %s");
        t.put("mods.detail.dependencies", "Зависимости:");
        t.put("mods.detail.noDependencies", "Нет зависимостей");
        t.put("mods.dependency.required", "Обязательная");
        t.put("mods.dependency.optional", "Необязательная");
        t.put("mods.dependency.incompatible", "Несовместимая");
        t.put("mods.dependency.embedded", "Встроенная");
        t.put("mods.dependency.unknown", "Неизвестная");
        t.put("mod.error.curseforgeKeyMissing", "Ключ API CurseForge не задан. Укажите CURSEFORGE_API_KEY или -Dcaml.curseforge.apiKey.");
        t.put("mod.error.downloadUrlMissing", "Для этой версии мода нет официального адреса загрузки: %s");
        t.put("mod.error.projectNotFound", "Проект мода не найден: %s");
        t.put("mod.error.iconUrlInvalid", "Недопустимый адрес значка мода: %s");
        t.put("mod.error.iconEmpty", "Файл значка мода пуст: %s");

        // Фоновые операции
        t.put("operation.inspectGameDirectory", "Проверка каталога игры");
        t.put("operation.inspectInstance", "Чтение информации о сборке");
        t.put("operation.openInstanceFolder", "Открыть папку сборки");
        t.put("operation.loadInstanceModIcon", "Загрузить значок мода");
        t.put("operation.setModDisabled", "Изменить состояние мода");
        t.put("operation.checkInstanceName", "Проверка названия сборки");
        t.put("operation.fetchVersionList", "Получение официального списка %s");
        t.put("operation.installVanilla", "Установка ванильной сборки %s");
        t.put("operation.fetchFabricLoader", "Получение версий Fabric Loader");
        t.put("operation.fetchFabricApi", "Получение версий Fabric API");
        t.put("operation.installFabric", "Установка сборки Fabric %s");
        t.put("operation.fetchForge", "Получение версий Forge");
        t.put("operation.fetchNeoForge", "Получение версий NeoForge");
        t.put("operation.fetchOptiFine", "Получение версий OptiFine");
        t.put("operation.installLoader", "Установка сборки %s %s");
        t.put("operation.scanJava", "Сканирование Java");
        t.put("operation.addManualJava", "Добавление Java вручную");
        t.put("operation.saveJavaSelection", "Сохранение выбора Java");
        t.put("operation.saveMaxMemory", "Сохранение максимальной памяти");
        t.put("operation.saveGameDirectory", "Сохранение каталога игры");
        t.put("operation.saveVersionIsolation", "Сохранение настройки изоляции версий");
        t.put("operation.saveAutoGameLanguage", "Сохранение настройки языка игры");
        t.put("operation.saveDownloadConcurrency", "Сохранение параллельных загрузок");
        t.put("operation.saveDownloadSource", "Сохранение источника загрузки");
        t.put("operation.saveTheme", "Сохранение выбора темы");
        t.put("operation.saveLanguage", "Сохранение настройки языка");
        t.put("operation.saveLanguageFallback", "Сохранение резервного языка");
        t.put("operation.searchMods", "Поиск модов: %s");
        t.put("operation.fetchModProject", "Получение проекта мода: %s");
        t.put("operation.downloadMod", "Загрузка мода: %s");
        t.put("operation.loadModIcon", "Загрузка значка мода");
        t.put("operation.saveLastInstance", "Сохранение последней запущенной сборки");
        t.put("operation.launchGame", "Запуск игры %s");

        // Загрузчик Quilt
        t.put("download.description.quilt", "Quilt Loader %s");
        t.put("download.description.quiltWithApi", "Quilt Loader %s + Quilted Fabric API %s");
        t.put("download.setup.option.quilt", "Выбрать версию Quilt");
        t.put("download.setup.option.quiltApi", "Выбрать версию Quilted Fabric API");
        t.put("download.prompt.loading.quilt", "Загрузка версий Quilt...");
        t.put("download.prompt.quiltFirst", "Сначала выберите версию Quilt");
        t.put("download.prompt.select.quilt", "Выберите версию Quilt");
        t.put("download.prompt.select.quiltApi", "Выберите версию Quilted Fabric API");
        t.put("download.prompt.empty.quilt", "Нет доступных версий Quilt");
        t.put("download.prompt.empty.quiltApi", "Нет доступных версий Quilted Fabric API");
        t.put("download.prompt.error.quilt", "Не удалось загрузить версии Quilt");
        t.put("download.prompt.error.quiltApi", "Не удалось загрузить версии Quilted Fabric API");
        t.put("download.stage.quilt.config.fetch", "Получение конфигурации Quilt");
        t.put("download.stage.quilt.loader.install", "Установка Quilt Loader");
        t.put("download.stage.quilt.api.download", "Загрузка Quilted Fabric API");
        t.put("download.stage.quilt.verify", "Проверка установки Quilt");
        t.put("log.download.installed.quilt", "[Загрузка] Сборка Quilt %s установлена (Loader %s)");
        t.put("operation.fetchQuiltLoader", "Получить версии Quilt Loader");
        t.put("operation.fetchQuiltApi", "Получить версии Quilted Fabric API");
        t.put("operation.installQuilt", "Установить сборку Quilt %s");
    }
}

