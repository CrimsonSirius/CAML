package org.chs.camlauncher.language.generator.lang;

import org.chs.camlauncher.language.generator.LanguageTranslationGenerator;
import org.chs.camlauncher.language.generator.TranslationTable;

/** 한국어 번역. */
public final class KOKRGenerator extends LanguageTranslationGenerator {
    @Override
    protected String locale() {
        return "ko_kr";
    }

    @Override
    protected void populate(TranslationTable t) {
        // 공통
        t.put("common.back", "뒤로");
        t.put("common.cancel", "취소");
        t.put("common.browse", "찾아보기");
        t.put("common.save", "저장");

        // 내비게이션과 하단 표시줄
        t.put("nav.home", "홈");
        t.put("nav.downloads", "다운로드");
        t.put("nav.settings", "설정");
        t.put("app.subtitle", "Minecraft 런처");
        t.put("footer.status", "CAML %s · 백엔드 %s · 데이터 디렉터리: %s");
        t.put("footer.backend.ready", "준비됨");
        t.put("footer.backend.notReady", "준비 안 됨");

        // 실행 양식
        t.put("home.username", "오프라인 사용자 이름");
        t.put("home.launch", "게임 실행");
        t.put("home.launching", "실행 중...");
        t.put("home.browseInstances", "인스턴스 찾아보기");
        t.put("home.manageInstances", "인스턴스 관리");
        t.put("home.instance.current", "현재 인스턴스: %s");
        t.put("home.instance.current.none", "현재 인스턴스: 선택되지 않음");
        t.put("home.gameDirectory.none", "게임 디렉터리: 설정되지 않음");
        t.put("home.gameDirectory.missing", "게임 디렉터리: 없음 · %s");
        t.put("home.gameDirectory.value", "게임 디렉터리: %s");
        t.put("home.saveLocation.none", "저장 위치: 설정되지 않음");
        t.put("home.saveLocation.fixDirectory", "저장 위치: 먼저 게임 디렉터리를 수정하세요");
        t.put("home.saveLocation.selectInstance", "저장 위치: 먼저 게임 인스턴스를 선택하세요");
        t.put("home.saveLocation.value", "저장 위치: %s");
        t.put("home.saveLocation.isolated", " (완전 버전 격리)");
        t.put("home.error.inspectDirectory", "게임 디렉터리 검사 실패: %s");
        t.put("home.error.noGameDirectory", "먼저 설정에서 게임 디렉터리를 저장하세요");
        t.put("home.error.missingGameDirectory", "게임 디렉터리가 없습니다. 설정에서 유효한 디렉터리를 저장하세요");
        t.put("home.error.noInstance", "먼저 게임 인스턴스를 찾아 선택하세요");
        t.put("home.error.noUsername", "오프라인 사용자 이름을 입력하세요");

        // 로그
        t.put("log.title", "실행 로그");
        t.put("log.subtitle", "다운로드 진행 상황과 게임 프로세스 출력");
        t.put("log.accessible", "실행 로그");
        t.put("log.download.installed.vanilla", "[다운로드] 인스턴스 %s 설치됨(바닐라 %s)");
        t.put("log.download.installed.fabric", "[다운로드] Fabric 인스턴스 %s 설치됨(Loader %s)");
        t.put("log.download.installed.loader", "[다운로드] %s 인스턴스 %s 설치됨");
        t.put("log.download.progress", "[다운로드] %s (%d/%d)");
        t.put("log.download.progressNoTotal", "[다운로드] %s (%d)");
        t.put("log.gameProcess.exit", "게임 프로세스가 종료되었습니다. 코드: %d");
        t.put("log.gameProcess.exitError", "게임 프로세스가 비정상 종료되었습니다. 코드: %d");
        t.put("log.error.operationFailed", "[%s] 실패: %s");

        // 실행 상태
        t.put("status.starting", "%s 실행 중...");
        t.put("status.started", "실행 명령이 전송되었습니다");
        t.put("status.launchFailed", "실행 실패!");
        t.put("status.launchFailedDetail", "실행 실패: %s");
        t.put("error.unknown", "알 수 없는 오류가 발생했습니다. 로그를 확인하세요.");
        t.put("download.error.failed", "%s 다운로드에 실패했습니다(%s회 시도, 마지막 주소: %s)");
        t.put("network.requestFailed", "%s 요청 실패(마지막 주소: %s)");
        t.put("network.timeout", "요청 시간 초과(%s ms): %s");
        t.put("network.httpError", "HTTP %s: %s");
        t.put("network.responseInvalid", "응답을 해석하지 못했습니다: %s");
        t.put("network.redirectLocationMissing", "리디렉션에 Location이 없습니다: %s");
        t.put("network.tooManyRedirects", "리디렉션이 너무 많습니다: %s");
        t.put("network.noSource", "사용 가능한 다운로드 소스가 없습니다: %s");
        t.put("metadata.manifestEmpty", "버전 매니페스트가 비어 있습니다");
        t.put("metadata.versionJsonEmpty", "버전 JSON이 비어 있습니다");
        t.put("metadata.versionJsonHashMismatch", "버전 JSON SHA-1 검증 실패: %s");
        t.put("launch.permissionDenied", "Java 실행 또는 게임 디렉터리 접근 권한이 없습니다. 파일 권한을 확인하세요.");
        t.put("launch.executableNotFound", "Java 실행 파일을 찾을 수 없습니다. Java를 다시 선택하세요.");
        t.put("launch.commandTooLong", "실행 명령이 너무 깁니다. 게임 경로나 클래스 경로를 줄이세요.");
        t.put("launch.commandTooLongAfterFallback", "argfile 및 manifest 대체 후에도 실행 명령이 너무 깁니다. 게임 경로나 클래스 경로를 줄이세요.");
        t.put("launch.processStartFailed", "게임 프로세스를 시작하지 못했습니다. 자세한 내용은 로그를 확인하세요.");
        t.put("launch.manualJavaNotConfigured", "수동 Java 선택이 활성화되어 있지만 Java 경로가 선택되지 않았습니다.");
        t.put("launch.manualJavaUnavailable", "선택한 Java를 사용할 수 없습니다: %s");
        t.put("launch.javaRequirementNotFound", "버전 요구 사항 %s을(를) 충족하는 Java를 찾을 수 없습니다. Java를 설치하거나 선택하세요.");

        // 버전 분류와 소스
        t.put("download.category.release", "정식 버전");
        t.put("download.category.snapshot", "스냅샷");
        t.put("download.category.old", "구버전");
        t.put("download.category.unknown", "알 수 없는 버전");
        t.put("download.source.mirror", "미러");
        t.put("download.source.official", "공식");
        t.put("download.source.custom", "사용자 지정");

        // 버전 목록
        t.put("download.versions.loading", "%1$s에서 %2$s 목록을 가져오는 중...");
        t.put("download.versions.empty", "다운로드 소스가 다운로드 가능한 %s을(를) 반환하지 않았습니다");
        t.put("download.versions.count", "총 %d개의 %s, 출시 시간 기준 최신순");
        t.put("download.versions.error", "%s 목록을 가져오지 못했습니다: %s");
        t.put("download.versions.accessible", "%s, %s");

        // 설명
        t.put("download.description.optifine", "Forge %s + OptiFine %s");
        t.put("download.description.forge", "Forge %s");
        t.put("download.description.fabric", "Fabric Loader %s");
        t.put("download.description.fabricWithApi", "Fabric Loader %s + Fabric API %s");
        t.put("download.description.neoforge", "NeoForge %s");
        t.put("download.description.vanilla", "Minecraft %s");

        // 다운로드 설정 페이지
        t.put("download.setup.status.idle", "로더를 선택하지 않으면 바닐라 게임을 다운로드합니다.");
        t.put("download.setup.instanceName", "인스턴스 이름");
        t.put("download.setup.download", "인스턴스 다운로드");
        t.put("download.setup.option.versionInfo", "버전 정보");
        t.put("download.setup.option.forge", "Forge 버전 선택");
        t.put("download.setup.option.optifine", "OptiFine 버전 선택");
        t.put("download.setup.option.fabric", "Fabric 버전 선택");
        t.put("download.setup.option.fabricApi", "Fabric API 버전 선택");
        t.put("download.setup.option.neoforge", "NeoForge 버전 선택");
        t.put("download.setup.versionInfo", "Minecraft %s · %s");
        t.put("download.setup.versionInfoWithDate", "Minecraft %s · %s · %s");
        t.put("download.setup.optifineSwitched", "Forge와 OptiFine은 호환되지 않습니다. Forge %s(으)로 전환했습니다");
        t.put("download.setup.optifineIncompatibleForge", "현재 Forge는 OptiFine과 호환되지 않습니다");
        t.put("download.setup.noCompatibleOptiFine", "현재 Forge와 호환되는 OptiFine 버전이 없습니다");
        t.put("download.setup.optifineForgeConflict", "Forge %s은(는) OptiFine %s과(와) 호환되지 않습니다. Forge %s 또는 이전 버전을 선택하세요.");
        t.put("download.setup.nameEmpty", "인스턴스 이름은 비워 둘 수 없습니다");
        t.put("download.setup.nameDotSegments", "인스턴스 이름은 . 또는 ..일 수 없습니다");
        t.put("download.setup.nameTrailing", "인스턴스 이름은 점이나 공백으로 끝날 수 없습니다");
        t.put("download.setup.nameIllegalCharacter", "인스턴스 이름에 허용되지 않는 문자가 있습니다: %s");
        t.put("download.setup.nameExists", "인스턴스 이름이 이미 존재합니다: %s. 다른 이름을 선택하세요.");
        t.put("download.setup.noGameDirectory", "게임 디렉터리가 설정되지 않았습니다");
        t.put("download.setup.checkingName", "인스턴스 이름 확인 중...");
        t.put("download.setup.nameSameAsLoader", "로더를 설치할 때 인스턴스 이름은 게임 버전과 같을 수 없습니다");
        t.put("download.setup.nameAvailable", "인스턴스 이름을 사용할 수 있습니다");
        t.put("download.setup.checkNameFailed", "인스턴스 이름 확인 실패: %s");
        t.put("download.setup.nameNotChecked", "인스턴스 이름이 아직 검사를 통과하지 못했습니다. 잠시 후 다시 시도하세요.");
        t.put("download.setup.noGameDirectoryLong", "게임 디렉터리가 설정되지 않았습니다. 먼저 설정에서 저장하세요.");
        t.put("download.setup.optifineRequiresForge", "OptiFine은 Forge와 함께만 설치할 수 있습니다.");

        // 로더 안내
        t.put("download.prompt.loading.fabric", "Fabric 버전을 가져오는 중...");
        t.put("download.prompt.loading.forge", "Forge 버전을 가져오는 중...");
        t.put("download.prompt.loading.neoforge", "NeoForge 버전을 가져오는 중...");
        t.put("download.prompt.fabricFirst", "먼저 Fabric 버전을 선택하세요");
        t.put("download.prompt.forgeFirst", "먼저 Forge 버전을 선택하세요");
        t.put("download.prompt.fabricApiFirst", "먼저 Fabric 버전을 선택하세요");
        t.put("download.prompt.select.fabric", "Fabric 버전을 선택하세요");
        t.put("download.prompt.select.forge", "Forge 버전을 선택하세요");
        t.put("download.prompt.select.neoforge", "NeoForge 버전을 선택하세요");
        t.put("download.prompt.select.optifine", "OptiFine 버전을 선택하세요");
        t.put("download.prompt.select.fabricApi", "Fabric API 버전을 선택하세요");
        t.put("download.prompt.empty.fabric", "사용 가능한 Fabric 버전이 없습니다");
        t.put("download.prompt.empty.fabricApi", "사용 가능한 Fabric API 버전이 없습니다");
        t.put("download.prompt.empty.forge", "사용 가능한 Forge 버전이 없습니다");
        t.put("download.prompt.empty.neoforge", "사용 가능한 NeoForge 버전이 없습니다");
        t.put("download.prompt.empty.optifine", "사용 가능한 OptiFine 버전이 없습니다");
        t.put("download.prompt.error.fabric", "Fabric 버전을 가져오지 못했습니다");
        t.put("download.prompt.error.fabricApi", "Fabric API 버전을 가져오지 못했습니다");
        t.put("download.prompt.error.forge", "Forge 버전을 가져오지 못했습니다");
        t.put("download.prompt.error.neoforge", "NeoForge 버전을 가져오지 못했습니다");
        t.put("download.prompt.error.optifine", "OptiFine 버전을 가져오지 못했습니다");

        // 진행 페이지
        t.put("download.progress.title", "%s 다운로드 중");
        t.put("download.progress.subtitle", "%s · Minecraft %s");
        t.put("download.progress.back", "다운로드 목록으로 돌아가기");
        t.put("download.progress.speed", "네트워크 속도");
        t.put("download.progress.speedValue", "0 B/s");
        t.put("download.progress.progress", "다운로드 진행률");
        t.put("download.progress.preparing", "다운로드 준비 중");
        t.put("download.progress.waiting", "다운로드 작업을 기다리는 중...");
        t.put("download.progress.completed", "다운로드 완료");
        t.put("download.progress.completedTotal", "다운로드가 완료되었습니다");
        t.put("download.progress.total", "합계 %s");
        t.put("download.progress.totalWithMax", "합계 %s / %s");
        t.put("download.progress.failed", "다운로드 실패: %s");
        t.put("download.progress.currentFile", "현재 파일: %s");
        t.put("download.progress.currentFileWithBytes", "현재 파일: %s · %s / %s");
        t.put("download.progress.finishedHint", "인스턴스 설치가 완료되었습니다. 목록으로 돌아갈 수 있습니다.");
        t.put("download.progress.failedHint", "다운로드가 중지되었습니다. 목록으로 돌아가 다시 시도하세요.");
        t.put("download.progress.bytesDetail", "%.1f%% · %s / %s · 파일 %d / %d");
        t.put("download.progress.filesDetail", "파일 %d / %d");
        t.put("download.progress.planning", "다운로드 계획 준비 중...");

        // 다운로드 단계
        t.put("download.stage.preparing", "다운로드 준비 중");
        t.put("download.stage.completed", "완료");
        t.put("download.stage.failed", "다운로드 실패");
        t.put("download.stage.unknownError", "알 수 없는 오류");
        t.put("download.stage.version.fetch", "버전 메타데이터 가져오는 중");
        t.put("download.stage.version.checkLocal", "로컬 버전 확인 중");
        t.put("download.stage.version.downloadFiles", "게임 파일 및 라이브러리 다운로드 중");
        t.put("download.stage.version.downloadAssets", "리소스 다운로드 중");
        t.put("download.stage.version.writeJson", "버전 메타데이터 쓰는 중");
        t.put("download.stage.loader.installer.prepare", "%s 설치 프로그램 준비 중");
        t.put("download.stage.loader.installer.download", "%s 설치 프로그램 다운로드 중");
        t.put("download.stage.loader.installer.run", "%s 설치 프로그램 실행 중");
        t.put("download.stage.loader.libraries.download", "%s 라이브러리 다운로드 중");
        t.put("download.stage.loader.files.merge", "%s 파일 병합 중");
        t.put("download.stage.loader.verify", "%s 설치 확인 중");
        t.put("download.stage.fabric.config.fetch", "Fabric 구성 가져오는 중");
        t.put("download.stage.fabric.loader.install", "Fabric Loader 설치 중");
        t.put("download.stage.fabric.api.download", "Fabric API 다운로드 중");
        t.put("download.stage.fabric.verify", "Fabric 설치 확인 중");
        t.put("download.stage.optifine.installer.download", "OptiFine 설치 프로그램 다운로드 중");
        t.put("download.stage.optifine.installer.run", "OptiFine 설치 프로그램 실행 중");
        t.put("download.stage.optifine.files.merge", "OptiFine 파일 병합 중");

        // 다운로드 설정
        t.put("download.settings.source", "다운로드 소스");
        t.put("download.settings.source.description", "버전, 라이브러리, 리소스에 사용할 우선 다운로드 주소를 선택하세요.");
        t.put("download.settings.source.mirror", "미러 우선");
        t.put("download.settings.source.official", "공식 우선");
        t.put("download.settings.source.custom", "사용자 지정 API 가져오기");
        t.put("download.settings.customApi.placeholder", "예: https://bmclapi2.bangbang93.com");
        t.put("download.settings.concurrency", "다운로드 동시 수");
        t.put("download.settings.concurrency.description", "동시에 다운로드할 파일 수(1–256).");
        t.put("download.settings.concurrency.label", "다운로드 동시 수");
        t.put("download.settings.concurrency.value", "현재 동시 수: %d");
        t.put("download.settings.concurrency.accessible", "다운로드 동시 수");

        // 설정 내비게이션
        t.put("settings.section.game", "게임 설정");
        t.put("settings.section.download", "다운로드 설정");
        t.put("settings.section.appearance", "모양");

        // 게임 디렉터리
        t.put("settings.game.location.title", "게임 디렉터리");
        t.put("settings.game.location.description", "Minecraft 게임 데이터 디렉터리를 설정합니다. 직접 입력한 경우 저장을 눌러야 적용됩니다.");
        t.put("settings.game.location.browse", "찾아보기");
        t.put("settings.game.location.save", "저장");
        t.put("settings.game.location.chooserTitle", "게임 디렉터리 선택");
        t.put("settings.game.location.notSaved", "저장되지 않음");
        t.put("settings.game.location.empty", "게임 디렉터리는 비워 둘 수 없습니다");
        t.put("settings.game.location.invalid", "게임 디렉터리 형식이 잘못되었습니다");
        t.put("settings.game.location.checking", "디렉터리 확인 중...");
        t.put("settings.game.location.missing", "디렉터리가 없습니다: %s");
        t.put("settings.game.location.saved", "게임 디렉터리가 저장되었습니다");
        t.put("settings.game.location.checkFailed", "디렉터리 확인 실패: %s");

        // 메모리
        t.put("settings.game.memory.title", "메모리");
        t.put("settings.game.memory.accessible", "최대 메모리");
        t.put("settings.game.memory.description", "게임 실행 시 사용할 메모리를 조정합니다.");
        t.put("settings.game.memory.label", "메모리 할당");
        t.put("settings.game.memory.hint", "핸들을 드래그하여 게임 할당량을 조정하세요. 회색 막대는 사용 중 + 게임 메모리, 오른쪽 연한 회색은 미사용 메모리입니다.");
        t.put("settings.game.memory.summary", "사용 중 %d MB / 게임 %d MB / 사용 가능 %d MB / 전체 %d MB");

        // 버전 격리
        t.put("settings.game.isolation.title", "버전 격리");
        t.put("settings.game.isolation.full", "완전 버전 격리");
        t.put("settings.game.isolation.none", "버전 격리 안 함");

        // 게임 언어
        t.put("settings.game.language.title", "게임 언어");
        t.put("settings.game.language.description", "새 인스턴스를 처음 실행할 때 게임 언어를 런처의 현재 언어로 자동 설정합니다.");
        t.put("settings.game.language.enabled", "자동 전환");
        t.put("settings.game.language.disabled", "끔");
        t.put("settings.game.language.hint", "인스턴스에 아직 언어가 설정되지 않은 경우에만 적용됩니다. options.txt에 이미 설정된 언어는 덮어쓰지 않습니다. ‘地道中国话’는 중국어 간체로 매핑됩니다.");

        // Java
        t.put("settings.game.java.title", "Java");
        t.put("settings.game.java.description", "게임 실행에 사용할 Java 런타임을 선택합니다. 자동 선택은 게임 버전 요구 사항을 충족하는 Java를 고릅니다.");
        t.put("settings.game.java.auto", "자동 선택");
        t.put("settings.game.java.manual", "수동 선택");
        t.put("settings.game.java.rescan", "다시 검색");
        t.put("settings.game.java.add", "Java 추가");
        t.put("settings.game.java.scanning", "Java 검색 중...");
        t.put("settings.game.java.scanningAll", "시스템 전체 Java 검색 중...");
        t.put("settings.game.java.scanFailedPrompt", "Java 검색 실패");
        t.put("settings.game.java.scanFailed", "Java 검색 실패: %s");
        t.put("settings.game.java.noneFound", "Java를 찾을 수 없습니다");
        t.put("settings.game.java.select", "Java를 선택하세요");
        t.put("settings.game.java.autoSelected", "자동 선택");
        t.put("settings.game.java.chooserTitle", "Java 실행 파일 선택");
        t.put("settings.game.java.checking", "Java 확인 중...");
        t.put("settings.game.java.addFailed", "Java 추가 실패: %s");
        t.put("settings.game.java.autoHint", "자동 선택은 검색 결과에서 현재 게임 버전 요구 사항을 충족하는 Java를 고릅니다.");
        t.put("settings.game.java.selectOrAdd", "Java를 선택하거나 ‘Java 추가’를 클릭하세요.");
        t.put("settings.game.java.unavailable", "이 Java는 현재 사용할 수 없습니다: %s");
        t.put("settings.game.java.detail", "버전 %s · %s · %s · %d비트%s");
        t.put("settings.game.java.jdk", " · JDK");
        t.put("settings.game.java.jre", " · JRE");

        // 모양
        t.put("settings.appearance.title", "모양");
        t.put("settings.appearance.description", "런처 자체의 모양을 조정합니다.");
        t.put("settings.appearance.themeHint", "테마는 ~/.caml/themes에서 가져오며 전환 즉시 적용됩니다");
        t.put("settings.appearance.theme", "테마");
        t.put("settings.appearance.theme.accessible", "테마");
        t.put("settings.appearance.language.title", "언어");
        t.put("settings.appearance.language.description", "런처 인터페이스 언어를 선택하는 방법을 설정합니다.");
        t.put("settings.appearance.language.auto", "자동");
        t.put("settings.appearance.language.manual", "수동");
        t.put("settings.appearance.language.label", "인터페이스 언어");
        t.put("settings.appearance.language.hint", "자동 모드는 시스템 언어를 따르고, 수동 모드는 아래에서 선택한 언어를 사용합니다.");
        t.put("settings.appearance.language.accessible", "인터페이스 언어");
        t.put("settings.appearance.languageFallback.title", "언어 대체");
        t.put("settings.appearance.languageFallback.description", "번역이 없을 때 보충할 언어를 설정합니다.");
        t.put("settings.appearance.languageFallback.system", "시스템 언어");
        t.put("settings.appearance.languageFallback.default", "기본 언어");
        t.put("settings.appearance.languageFallback.key", "원시 키 표시");
        t.put("settings.appearance.languageFallback.custom", "사용자 지정 언어");
        t.put("settings.appearance.languageFallback.hint", "시스템 언어는 감지된 언어만 사용합니다. 기본 언어는 항상 영어를 사용합니다. 원시 키는 대체하지 않습니다. 사용자 지정 언어는 아래에서 선택한 언어만 사용합니다. 기본 언어를 제외하면 누락된 내용은 원시 키로 표시됩니다.");
        t.put("settings.appearance.languageFallback.accessible", "언어 대체");

        // 인스턴스 선택
        t.put("instances.picker.scanning", "게임 인스턴스 검색 중...");
        t.put("instances.picker.title", "게임 인스턴스 선택");
        t.put("instances.picker.noGameDirectory", "게임 디렉터리가 설정되지 않았습니다");
        t.put("instances.picker.gameDirectory", "게임 디렉터리: %s");
        t.put("instances.picker.noGameDirectoryHint", "게임 디렉터리가 설정되지 않았습니다. 먼저 설정에서 저장하세요");
        t.put("instances.picker.missing", "게임 디렉터리가 없습니다: %s");
        t.put("instances.picker.empty", "현재 게임 디렉터리에 설치된 인스턴스가 없습니다");
        t.put("instances.picker.count", "총 %d개의 게임 인스턴스");
        t.put("instances.picker.scanFailed", "게임 인스턴스 검색 실패: %s");
        t.put("instances.manage.title", "인스턴스 관리");
        t.put("instances.manage.instance", "인스턴스: %s");
        t.put("instances.manage.folder", "인스턴스 폴더: %s");
        t.put("instances.manage.openFolder", "인스턴스 폴더 열기");
        t.put("instances.manage.openFolder.opening", "인스턴스 폴더를 여는 중...");
        t.put("instances.manage.openFolder.failed", "인스턴스 폴더를 열지 못했습니다: %s");
        t.put("instances.manage.noGameDirectory", "게임 디렉터리가 설정되지 않았습니다");
        t.put("instances.manage.noInstance", "인스턴스가 선택되지 않았습니다");
        t.put("instances.manage.mods.title", "모드 목록");
        t.put("instances.manage.mods.scanning", "모드 목록을 읽는 중...");
        t.put("instances.manage.mods.count", "모드 %d개");
        t.put("instances.manage.mods.empty", "이 인스턴스에는 모드가 없습니다");
        t.put("instances.manage.mods.scanFailed", "모드 목록을 읽지 못했습니다: %s");
        t.put("instances.manage.mods.enabled", "사용");
        t.put("instances.manage.mods.disabled", "사용 안 함");

        // 모드 다운로드
        t.put("mods.nav", "모드");
        t.put("mods.search.title", "모드 검색");
        t.put("mods.search.name", "이름");
        t.put("mods.search.nameHint", "모드 이름 입력");
        t.put("mods.search.source", "소스");
        t.put("mods.search.gameVersion", "게임 버전");
        t.put("mods.search.versionHint", "예: 1.20.1, 비워 두면 전체");
        t.put("mods.search.loader", "로더");
        t.put("mods.search.allSources", "모든 소스");
        t.put("mods.search.allLoaders", "모든 로더");
        t.put("mods.search.button", "검색");
        t.put("mods.search.reset", "조건 초기화");
        t.put("mods.search.loading", "모드 검색 중...");
        t.put("mods.search.empty", "일치하는 모드가 없습니다");
        t.put("mods.search.count", "총 %d개 결과");
        t.put("mods.search.failed", "모드를 불러오지 못했습니다: %s");
        t.put("mods.page.previous", "이전");
        t.put("mods.page.next", "다음");
        t.put("mods.page.info", "%d페이지");
        t.put("mods.source.modrinth", "Modrinth");
        t.put("mods.source.curseforge", "CurseForge");
        t.put("mods.loader.fabric", "Fabric");
        t.put("mods.loader.forge", "Forge");
        t.put("mods.loader.neoforge", "NeoForge");
        t.put("mods.loader.quilt", "Quilt");
        t.put("mods.value.unknown", "알 수 없음");
        t.put("mods.list.meta", "로더: %s · 버전: %s · 다운로드 %s · 업데이트 %s");
        t.put("mods.detail.loading", "모드 세부 정보 불러오는 중...");
        t.put("mods.detail.count", "총 %d개의 파일 버전");
        t.put("mods.detail.meta", "다운로드 %s · 업데이트 %s");
        t.put("mods.detail.versionFilter", "게임 버전");
        t.put("mods.detail.loaderFilter", "로더");
        t.put("mods.detail.allVersions", "모든 버전");
        t.put("mods.detail.allLoaders", "모든 로더");
        t.put("mods.detail.empty", "현재 조건에 맞는 버전이 없습니다");
        t.put("mods.detail.versionMeta", "게시 %s · 다운로드 %s · 파일 %s");
        t.put("mods.detail.download", "다운로드");
        t.put("mods.detail.downloading", "%s 다운로드 중...");
        t.put("mods.detail.downloaded", "다운로드 위치: %s");
        t.put("mods.detail.noGameDirectory", "먼저 게임 디렉터리를 설정하세요");
        t.put("mods.detail.noInstance", "먼저 게임 인스턴스를 선택하세요");
        t.put("mods.detail.failed", "작업 실패: %s");
        t.put("mods.detail.dependencies", "의존성:");
        t.put("mods.detail.noDependencies", "의존성 없음");
        t.put("mods.dependency.required", "필수");
        t.put("mods.dependency.optional", "선택");
        t.put("mods.dependency.incompatible", "비호환");
        t.put("mods.dependency.embedded", "내장");
        t.put("mods.dependency.unknown", "알 수 없음");
        t.put("mod.error.curseforgeKeyMissing", "CurseForge API 키가 설정되지 않았습니다. CURSEFORGE_API_KEY 또는 -Dcaml.curseforge.apiKey를 설정하세요.");
        t.put("mod.error.downloadUrlMissing", "이 모드 버전에는 공식 다운로드 URL이 없습니다: %s");
        t.put("mod.error.projectNotFound", "모드 프로젝트를 찾을 수 없습니다: %s");
        t.put("mod.error.iconUrlInvalid", "모드 아이콘 URL이 잘못되었습니다: %s");
        t.put("mod.error.iconEmpty", "모드 아이콘 파일이 비어 있습니다: %s");

        // 백그라운드 작업 이름
        t.put("operation.inspectGameDirectory", "게임 디렉터리 검사");
        t.put("operation.inspectInstance", "인스턴스 정보 읽기");
        t.put("operation.openInstanceFolder", "인스턴스 폴더 열기");
        t.put("operation.loadInstanceModIcon", "모드 아이콘 불러오기");
        t.put("operation.checkInstanceName", "인스턴스 이름 확인");
        t.put("operation.fetchVersionList", "공식 %s 목록 가져오기");
        t.put("operation.installVanilla", "바닐라 인스턴스 %s 설치");
        t.put("operation.fetchFabricLoader", "Fabric Loader 버전 가져오기");
        t.put("operation.fetchFabricApi", "Fabric API 버전 가져오기");
        t.put("operation.installFabric", "Fabric 인스턴스 %s 설치");
        t.put("operation.fetchForge", "Forge 버전 가져오기");
        t.put("operation.fetchNeoForge", "NeoForge 버전 가져오기");
        t.put("operation.fetchOptiFine", "OptiFine 버전 가져오기");
        t.put("operation.installLoader", "%s 인스턴스 %s 설치");
        t.put("operation.scanJava", "Java 검색");
        t.put("operation.addManualJava", "수동 Java 추가");
        t.put("operation.saveJavaSelection", "Java 선택 저장");
        t.put("operation.saveMaxMemory", "최대 메모리 설정 저장");
        t.put("operation.saveGameDirectory", "게임 디렉터리 저장");
        t.put("operation.saveVersionIsolation", "버전 격리 설정 저장");
        t.put("operation.saveAutoGameLanguage", "게임 언어 자동 설정 저장");
        t.put("operation.saveDownloadConcurrency", "다운로드 동시 수 저장");
        t.put("operation.saveDownloadSource", "다운로드 소스 설정 저장");
        t.put("operation.saveTheme", "테마 선택 저장");
        t.put("operation.saveLanguage", "언어 설정 저장");
        t.put("operation.saveLanguageFallback", "언어 대체 설정 저장");
        t.put("operation.searchMods", "모드 검색: %s");
        t.put("operation.fetchModProject", "모드 프로젝트 가져오기: %s");
        t.put("operation.downloadMod", "모드 다운로드: %s");
        t.put("operation.loadModIcon", "모드 아이콘 불러오기");
        t.put("operation.saveLastInstance", "마지막 실행 인스턴스 저장");
        t.put("operation.launchGame", "게임 %s 실행");

        // Quilt 로더
        t.put("download.description.quilt", "Quilt Loader %s");
        t.put("download.description.quiltWithApi", "Quilt Loader %s + Quilted Fabric API %s");
        t.put("download.setup.option.quilt", "Quilt 버전 선택");
        t.put("download.setup.option.quiltApi", "Quilted Fabric API 버전 선택");
        t.put("download.prompt.loading.quilt", "Quilt 버전을 불러오는 중...");
        t.put("download.prompt.quiltFirst", "먼저 Quilt 버전을 선택하세요");
        t.put("download.prompt.select.quilt", "Quilt 버전을 선택하세요");
        t.put("download.prompt.select.quiltApi", "Quilted Fabric API 버전을 선택하세요");
        t.put("download.prompt.empty.quilt", "사용 가능한 Quilt 버전이 없습니다");
        t.put("download.prompt.empty.quiltApi", "사용 가능한 Quilted Fabric API 버전이 없습니다");
        t.put("download.prompt.error.quilt", "Quilt 버전을 불러오지 못했습니다");
        t.put("download.prompt.error.quiltApi", "Quilted Fabric API 버전을 불러오지 못했습니다");
        t.put("download.stage.quilt.config.fetch", "Quilt 설정을 가져오는 중");
        t.put("download.stage.quilt.loader.install", "Quilt Loader 설치 중");
        t.put("download.stage.quilt.api.download", "Quilted Fabric API 다운로드 중");
        t.put("download.stage.quilt.verify", "Quilt 설치 확인 중");
        t.put("log.download.installed.quilt", "[다운로드] Quilt 인스턴스 %s 설치 완료 (Loader %s)");
        t.put("operation.fetchQuiltLoader", "Quilt Loader 버전 가져오기");
        t.put("operation.fetchQuiltApi", "Quilted Fabric API 버전 가져오기");
        t.put("operation.installQuilt", "Quilt 인스턴스 %s 설치");
    }
}

