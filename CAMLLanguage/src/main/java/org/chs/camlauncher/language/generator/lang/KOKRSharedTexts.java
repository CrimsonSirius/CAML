package org.chs.camlauncher.language.generator.lang;

import org.chs.camlauncher.language.generator.TranslationTable;

/** 계정, 설정, 작업 등 한국어 공통 문구. */
final class KOKRSharedTexts {
    private KOKRSharedTexts() {
    }

    static void populate(TranslationTable t) {
        // 계정
        t.put("account.choice.default", "기본 계정 따르기");
        t.put("account.choice.offline", "오프라인 사용자 이름");
        t.put("account.default.saved", "%s이(가) 기본 계정이 되었습니다");
        t.put("account.error.passwordRequired", "비밀번호를 입력하세요");
        t.put("account.error.serverRequired", "인증 서버 주소를 입력하세요");
        t.put("account.error.usernameRequired", "사용자 이름을 입력하세요");
        t.put("account.injector.builtin", "내장 버전 사용(필요 시 다운로드)");
        t.put("account.injector.chooserTitle", "authlib-injector jar 선택");
        t.put("account.injector.custom", "사용자 지정 파일: %s");
        t.put("account.injector.reset", "내장 버전 사용");
        t.put("account.injector.saved", "authlib-injector 설정을 저장했습니다");
        t.put("account.instance.count", "인스턴스 %d개 · 각 인스턴스에서 실행할 계정을 선택하세요");
        t.put("account.instance.loading", "계정을 불러오는 중...");
        t.put("account.instance.mismatch",
                "일부 인스턴스 계정이 %s에 연결되어 있어 이 게임 위치에서는 적용되지 않습니다(인스턴스 %d개)");
        t.put("account.instance.mismatchApplied", "인스턴스 계정 연결 %d개를 이 게임 위치에 적용했습니다");
        t.put("account.instance.mismatchApply", "이 게임 위치에 적용");
        t.put("account.instance.saved", "인스턴스 %s의 계정을 저장했습니다");
        t.put("account.instance.title", "인스턴스 계정");
        t.put("account.list.default", "기본");
        t.put("account.list.empty",
                "아직 계정이 없습니다. 오프라인 계정을 추가하거나 Microsoft로 로그인하거나 "
                        + "타사 서버에 로그인하세요.");
        t.put("account.list.needsLogin", "로그인이 만료됨");
        t.put("account.list.relogin", "다시 로그인");
        t.put("account.list.remove", "삭제");
        t.put("account.login.customServer", "사용자 지정 서버 주소");
        t.put("account.login.customServerOption", "사용자 지정 주소");
        t.put("account.login.hint", "비밀번호는 이번 로그인에만 사용되며 저장되지 않습니다.");
        t.put("account.login.password", "비밀번호");
        t.put("account.login.reloginHint",
                "%s의 비밀번호를 다시 입력하세요. 런처는 비밀번호를 저장하지 않습니다.");
        t.put("account.login.remember", "로그인 상태 유지");
        t.put("account.login.running", "로그인하는 중...");
        t.put("account.login.server", "인증 서버");
        t.put("account.login.submit", "로그인");
        t.put("account.login.success", "%s(으)로 로그인했습니다");
        t.put("account.login.username", "이메일 또는 사용자 이름");
        t.put("account.microsoft.codeReady", "브라우저에서 인증 페이지를 열고 복사한 코드를 입력하세요.");
        t.put("account.microsoft.copyCode", "코드 복사");
        t.put("account.microsoft.copyUrl", "인증 페이지 복사");
        t.put("account.microsoft.login", "Microsoft로 로그인");
        t.put("account.microsoft.running", "Microsoft 장치 코드를 요청하는 중...");
        t.put("account.microsoft.userCode", "장치 코드: %s");
        t.put("account.microsoft.verificationUri", "인증 페이지: %s");
        t.put("account.offline.add", "추가");
        t.put("account.offline.added", "오프라인 계정 %s을(를) 추가했습니다");
        t.put("account.offline.label", "오프라인 계정 추가");
        t.put("account.offline.placeholder", "오프라인 사용자 이름");
        t.put("account.profile.label", "프로필");
        t.put("account.profile.switch", "전환");
        t.put("account.profile.switched", "프로필을 %s(으)로 전환했습니다");
        t.put("account.removed", "%s을(를) 삭제했습니다");

        // 인증 오류
        t.put("auth.error.accountMissing", "계정을 찾을 수 없습니다: %s");
        t.put("auth.error.accountRequired", "먼저 계정을 선택하세요");
        t.put("auth.error.injectorChecksum", "authlib-injector 검증에 실패했습니다: %s");
        t.put("auth.error.injectorNotFound", "authlib-injector를 찾을 수 없습니다: %s");
        t.put("auth.error.invalidCredentials", "%s의 사용자 이름 또는 비밀번호가 올바르지 않습니다");
        t.put("auth.error.loginExpired", "%s(%s)의 로그인이 만료되었습니다. 다시 로그인하세요");
        t.put("auth.error.loginRequired", "계정 %s(%s)에 저장된 자격 증명이 없습니다. 다시 로그인하세요");
        t.put("auth.error.metadataUnavailable", "인증 서버 메타데이터를 읽을 수 없습니다: %s (HTTP %s)");
        t.put("auth.error.noAccount",
                "설정된 계정이 없습니다. 설정에서 추가하거나 오프라인 사용자 이름을 사용하세요.");
        t.put("auth.error.noSelectedProfile", "이 계정에는 Minecraft 프로필이 없습니다: %s");
        t.put("auth.error.passwordRequired", "비밀번호가 필요합니다");
        t.put("auth.error.profileMissing", "해당 프로필은 더 이상 사용할 수 없습니다: %s");
        t.put("auth.error.requestFailed", "인증 요청에 실패했습니다: %s (HTTP %s)");
        t.put("auth.error.responseInvalid", "인증 서버가 예상하지 못한 응답을 반환했습니다: %s");
        t.put("auth.error.tooManyRequests", "%s에 대한 요청이 너무 많습니다. 잠시 후 다시 시도하세요");
        t.put("auth.error.usernameRequired", "사용자 이름이 필요합니다");

        // Microsoft 인증 오류
        t.put("auth.microsoft.accountSuspended", "이 Microsoft 계정은 정지된 것으로 보입니다.");
        t.put("auth.microsoft.clientIdMissing",
                "Microsoft 로그인이 구성되지 않았습니다. CAML의 Microsoft OAuth 클라이언트 ID를 설정하세요.");
        t.put("auth.microsoft.forbidden",
                "현재 네트워크가 Minecraft Services에서 거부되었습니다. 다른 네트워크로 시도하세요.");
        t.put("auth.microsoft.loginDeclined", "Microsoft 로그인이 거부되었습니다.");
        t.put("auth.microsoft.loginExpired", "Microsoft 로그인이 만료되었습니다. 다시 로그인하세요.");
        t.put("auth.microsoft.loginFailed", "Microsoft 로그인에 실패했습니다: %s");
        t.put("auth.microsoft.loginTimeout", "Microsoft 장치 코드가 만료되었습니다. 다시 시도하세요.");
        t.put("auth.microsoft.missingField", "Microsoft 인증 응답에 %s이(가) 없습니다.");
        t.put("auth.microsoft.noLicense",
                "이 계정은 Minecraft: Java Edition을 보유하지 않았거나 Game Pass 혜택이 만료되었습니다.");
        t.put("auth.microsoft.noProfile",
                "이 계정에는 Minecraft 프로필이 없습니다. 프로필을 만든 후 다시 로그인하세요.");
        t.put("auth.microsoft.profileChanged",
                "계정을 새로 고치는 중에 Minecraft 프로필이 변경되었습니다. 다시 로그인하세요.");
        t.put("auth.microsoft.requestFailed", "%s에서 Microsoft 인증 요청에 실패했습니다(HTTP %s).");
        t.put("auth.microsoft.responseInvalid", "%s에서 예상하지 못한 Microsoft 인증 응답이 왔습니다.");
        t.put("auth.microsoft.serviceUnavailable",
                "Minecraft Services를 일시적으로 사용할 수 없습니다. 나중에 다시 시도하세요.");
        t.put("auth.microsoft.tooManyRequests", "로그인 시도가 너무 많습니다. 몇 분 후 다시 시도하세요.");
        t.put("auth.microsoft.wrongVerifyMethod",
                "Microsoft 로그인 페이지에서 다른 방법 대신 비밀번호를 사용하세요.");
        t.put("auth.microsoft.xboxBanned", "이 Microsoft 계정은 Xbox Live에서 차단되었습니다.");
        t.put("auth.microsoft.xboxFamily", "이 자녀 계정은 먼저 Microsoft 가족에 추가되어야 합니다.");
        t.put("auth.microsoft.xboxNoAccount",
                "이 Microsoft 계정에는 Xbox 계정이 없습니다. 만든 후 다시 시도하세요.");
        t.put("auth.microsoft.xboxRegion", "현재 국가 또는 지역에서는 Xbox Live를 사용할 수 없습니다.");
        t.put("auth.microsoft.xboxUnknown", "Xbox Live가 로그인을 거부했습니다(XErr %s).");

        // 공통
        t.put("common.close", "닫기");
        t.put("common.reset", "초기화");

        // 압축 해제 제한
        t.put("download.settings.archive.description", "모드팩과 월드를 압축 해제할 때 적용되는 제한입니다.");
        t.put("download.settings.archive.entries", "최대 항목 수");
        t.put("download.settings.archive.entrySize", "항목 최대 크기(MiB)");
        t.put("download.settings.archive.invalid",
                "올바른 제한을 입력하세요. 전체 크기는 항목 제한 이상이어야 합니다.");
        t.put("download.settings.archive.saved", "압축 해제 제한을 저장했습니다.");
        t.put("download.settings.archive.title", "압축 해제 제한");
        t.put("download.settings.archive.totalSize", "압축 해제 총 크기 제한(MiB)");

        // 홈
        t.put("home.account", "계정");
        t.put("home.error.loadAccounts", "계정을 불러오지 못했습니다: %s");
        t.put("home.manageAccounts", "인스턴스 계정");
        t.put("home.username.managed", "사용자 이름은 선택한 계정에서 가져옵니다: %s");

        // 실행 경고
        t.put("launch.warning.java.force", "강제 실행");
        t.put("launch.warning.java.message",
                "이 게임에는 Java %s이(가) 필요하지만 Java %s이(가) 선택되어 있습니다. 강제로 실행하면 실패할 수 있습니다.");
        t.put("launch.warning.java.stop", "중지");
        t.put("launch.warning.java.title", "Java 버전이 호환되지 않을 수 있습니다");

        // 로그
        t.put("log.collapse", "접기");
        t.put("log.empty", "아직 기록이 없습니다");
        t.put("log.expand", "펼치기");

        // 모드팩
        t.put("modpack.error.invalidArchive", "모드팩 압축 파일이 올바르지 않습니다: %s");
        t.put("modpack.error.invalidFile", "모드팩 파일 경로가 올바르지 않습니다: %s");
        t.put("modpack.error.missingFileUrl", "모드팩 파일에 다운로드 URL이 없습니다: %s");
        t.put("modpack.error.missingMinecraft", "모드팩에 Minecraft 버전이 지정되어 있지 않습니다");
        t.put("modpack.error.unsupportedLoader", "지원하지 않는 모드팩 로더입니다: %s");
        t.put("modpacks.detail.install", "모드팩 설치");
        t.put("modpacks.detail.installed", "모드팩을 설치했습니다: %s");
        t.put("modpacks.detail.installing", "모드팩 %s을(를) 설치하는 중...");
        t.put("modpacks.detail.loading", "모드팩 정보를 불러오는 중...");
        t.put("modpacks.install.instanceName", "인스턴스 이름");
        t.put("modpacks.install.instanceNameEmpty", "인스턴스 이름은 비워 둘 수 없습니다");
        t.put("modpacks.list.meta", "버전: %s · 다운로드 %s · 업데이트 %s");
        t.put("modpacks.nav", "모드팩");
        t.put("modpacks.search.empty", "일치하는 모드팩이 없습니다");
        t.put("modpacks.search.failed", "모드팩을 불러오지 못했습니다: %s");
        t.put("modpacks.search.loading", "모드팩을 검색하는 중...");
        t.put("modpacks.search.nameHint", "모드팩 이름 입력");
        t.put("modpacks.search.title", "모드팩 검색");

        // 작업 이름
        t.put("operation.addOfflineAccount", "오프라인 계정 추가");
        t.put("operation.downloadJava", "Java %s 다운로드 및 설치");
        t.put("operation.fetchAuthServer", "인증 서버 정보 가져오기");
        t.put("operation.fetchJavaDownloadList", "Java 다운로드 목록 가져오기: %s");
        t.put("operation.fetchModpackProject", "모드팩 프로젝트 가져오기 %s");
        t.put("operation.installModpack", "모드팩 %s 설치");
        t.put("operation.loadAccounts", "계정 불러오기");
        t.put("operation.loadInstanceAccount", "인스턴스 계정 불러오기");
        t.put("operation.loginAccount", "계정 로그인");
        t.put("operation.loginMicrosoftAccount", "Microsoft로 로그인");
        t.put("operation.openJavaFolder", "Java 폴더 열기");
        t.put("operation.removeAccount", "계정 삭제");
        t.put("operation.saveArchiveExtractionLimits", "압축 해제 제한 저장");
        t.put("operation.saveDefaultAccount", "기본 계정 저장");
        t.put("operation.saveInjectorPath", "authlib-injector 경로 저장");
        t.put("operation.saveInstanceAccount", "인스턴스 계정 저장");
        t.put("operation.searchModpacks", "모드팩 검색 %s");
        t.put("operation.switchProfile", "계정 프로필 전환");

        // 설정: 정보
        t.put("settings.about.application.title", "런처");
        t.put("settings.about.architecture", "아키텍처");
        t.put("settings.about.backend", "백엔드");
        t.put("settings.about.copyright", "저작권");
        t.put("settings.about.dataDirectory", "데이터 디렉터리");
        t.put("settings.about.dependencies.title", "프로젝트 의존성");
        t.put("settings.about.dependency.author", "작성자: %s");
        t.put("settings.about.dependency.license", "라이선스: %s");
        t.put("settings.about.device.title", "장치 정보");
        t.put("settings.about.directories.title", "디렉터리");
        t.put("settings.about.gameDirectory", "게임 디렉터리");
        t.put("settings.about.javaVendor", "Java 공급업체");
        t.put("settings.about.javaVersion", "Java 버전");
        t.put("settings.about.kernelVersion", "커널 버전");
        t.put("settings.about.license", "라이선스");
        t.put("settings.about.memory", "시스템 메모리");
        t.put("settings.about.notDetected", "감지되지 않음");
        t.put("settings.about.os", "운영 체제");
        t.put("settings.about.osVersion", "시스템 버전");
        t.put("settings.about.project.title", "프로젝트");
        t.put("settings.about.projectName", "이름");
        t.put("settings.about.runtime.title", "런처 실행 환경");
        t.put("settings.about.system.title", "시스템");
        t.put("settings.about.unknown", "알 수 없음");
        t.put("settings.about.version", "버전");

        // 설정: 계정
        t.put("settings.account.description",
                "런처에 저장된 계정입니다. 액세스 토큰은 암호화된 후에만 저장되며 비밀번호는 저장되지 않습니다.");
        t.put("settings.account.injector.description",
                "authlib-injector는 처음 한 번만 내려받아 계속 재사용하며, 실행할 때마다 SHA-256으로 검증합니다.");
        t.put("settings.account.login.description",
                "Yggdrasil 인증 서버(예: LittleSkin)의 계정으로 로그인합니다.");
        t.put("settings.account.login.title", "타사 로그인");
        t.put("settings.account.microsoft.description",
                "장치 코드로 Microsoft에 로그인합니다. 런처에 비밀번호를 입력하지 않습니다.");
        t.put("settings.account.microsoft.title", "Microsoft 계정");
        t.put("settings.account.title", "계정");

        // 설정: Java 관리
        t.put("settings.javaManagement.count", "Java 런타임 %d개");
        t.put("settings.javaManagement.distribution", "배포판");
        t.put("settings.javaManagement.download", "Java 다운로드");
        t.put("settings.javaManagement.downloadAndInstall", "다운로드 및 설치");
        t.put("settings.javaManagement.downloading", "Java를 다운로드하는 중...");
        t.put("settings.javaManagement.downloadingProgress", "Java를 다운로드하는 중... %d%%");
        t.put("settings.javaManagement.empty", "Java 런타임을 찾지 못했습니다");
        t.put("settings.javaManagement.emptyVersions", "다운로드할 수 있는 Java 버전이 없습니다");
        t.put("settings.javaManagement.installed", "Java를 설치했습니다: %s");
        t.put("settings.javaManagement.installFailed", "Java 설치에 실패했습니다: %s");
        t.put("settings.javaManagement.linuxHint",
                "Linux에서는 배포판 패키지 관리자(예: apt, dnf, pacman)로 JDK 또는 JRE를 설치하는 것을 "
                        + "권장하며, CAML이 관리하는 Java는 권장하지 않습니다.");
        t.put("settings.javaManagement.loadingVersions", "Java 버전을 불러오는 중...");
        t.put("settings.javaManagement.loadVersionsFailed", "Java 버전을 불러오지 못했습니다: %s");
        t.put("settings.javaManagement.manual", "수동으로 추가함");
        t.put("settings.javaManagement.meta", "%s · %d비트%s");
        t.put("settings.javaManagement.openFolder", "포함된 폴더 열기");
        t.put("settings.javaManagement.openFolder.failed", "Java 폴더를 열지 못했습니다: %s");
        t.put("settings.javaManagement.openFolder.opening", "폴더를 여는 중...");
        t.put("settings.javaManagement.packageType", "패키지 유형");
        t.put("settings.javaManagement.unavailable", "사용할 수 없음");
        t.put("settings.javaManagement.version", "버전");
        t.put("settings.javaManagement.versionCount", "버전 %d개");

        // 설정: 섹션
        t.put("settings.section.about", "정보");
        t.put("settings.section.account", "계정");
        t.put("settings.section.java", "Java 관리");

        // 작업
        t.put("tasks.back", "작업 목록으로 돌아가기");
        t.put("tasks.empty", "아직 작업이 없습니다");
        t.put("tasks.open", "작업 %s 열기 · %s");
        t.put("tasks.status.cancelled", "취소됨");
        t.put("tasks.status.completed", "완료됨");
        t.put("tasks.status.failed", "실패");
        t.put("tasks.status.running", "실행 중");
        t.put("tasks.subtitle", "다운로드 및 설치");
        t.put("tasks.title", "작업");
    }
}

