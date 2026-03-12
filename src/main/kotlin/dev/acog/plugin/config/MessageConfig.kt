package dev.acog.plugin.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "messages")
data class MessageConfig(
    var setupTitle: String = "플러그인 주문 제작 문의",
    var setupContent: String = "문의하기 버튼을 눌러주세요.",
    var setupButtonLabel: String = "문의하기",
    var setupComplete: String = "패널이 성공적으로 설치되었습니다.",

    var modalTitle: String = "플러그인 주문 제작 신청",
    var modalLabelName: String = "플러그인 이름",
    var modalLabelVersion: String = "버전",
    var modalLabelDescription: String = "기획 내용 (간략히)",
    var modalPlaceholderName: String = "예: 텔레포트 플러그인",
    var modalPlaceholderVersion: String = "예: 1.20.4",
    var modalPlaceholderDescription: String = "어떤 플러그인을 만들고 싶으신가요?",

    var ticketCreateSuccess: String = "티켓이 생성되었습니다! %s",
    var ticketCreateFail: String = "티켓 생성에 실패했습니다. (서버 설정 또는 권한 문제)",
    var ticketWelcome: String = "%s님 환영합니다!\n\n원활한 기획 진행을 위해 **기획서 파일(PDF, DOCX, TXT 등)** 또는 **상세한 기획 내용**을 먼저 올려주세요.\n파일 업로드가 어려우시면 채팅으로 상세히 작성해주셔도 됩니다.\n\n기획 내용을 확인한 뒤, 보완이 필요한 부분을 한 번에 정리해서 질문드리겠습니다.",

    var closeStart: String = "티켓 종료 절차를 시작합니다.",
    var surveyTitle: String = "서비스 만족도 조사",
    var surveyDescription: String = "\"%s\" 플러그인 제작 서비스는 만족하셨나요?\n\n아래 버튼을 눌러 평가해주세요.",
    var surveyButton5: String = "매우 만족",
    var surveyButton3: String = "보통",
    var surveyButton1: String = "불만족",
    var surveyThanks: String = "소중한 의견 감사합니다!",
    var surveyThankYouTitle: String = "소중한 의견 감사합니다!",
    var surveyThankYouDescription: String = "선택하신 만족도: %s\n\n저희 서비스를 이용해주셔서 감사합니다.",
    var surveyError: String = "만족도 조사 처리 중 오류가 발생했습니다.",
    var surveySaveError: String = "만족도 조사 저장 중 오류가 발생했습니다.",
    var surveyUnknownPlugin: String = "Unknown",

    var errorRateLimit: String = "AI 요청 한도를 초과했습니다. 잠시 후 다시 시도해주세요. (남은 요청: %d회)",
    var errorAiDelay: String = "현재 AI 사용량이 많아 응답이 지연되고 있습니다. 1분 뒤에 다시 시도해주세요.",
    var errorGeneral: String = "오류가 발생했습니다. 잠시 후 다시 시도해주세요.",
    var errorNotTicketChannel: String = "이 채널은 티켓 채널이 아닙니다.",
    var errorCloseFailed: String = "티켓 종료 중 오류가 발생했습니다. 관리자에게 문의해주세요.",

    var aiActivated: String = "AI 면접관이 활성화되었습니다.",
    var aiDeactivated: String = "AI 면접관이 비활성화되었습니다.",

    var incomeRecorded: String = "수익 기록 완료\n`%s`님 (`%s`): %s원",
    var errorNegativeAmount: String = "수익 금액은 0원 이상이어야 합니다.",
    var errorInvalidAmount: String = "올바른 금액을 입력해주세요.",

    var commandUnknown: String = "알 수 없는 명령어입니다.",
    var commandAdminOnly: String = "관리자만 사용할 수 있는 명령어입니다.",
    var commandError: String = "명령어 실행 중 오류가 발생했습니다: %s",

    var monitor: MonitorConfig = MonitorConfig(),
    var channelDelete: ChannelDeleteConfig = ChannelDeleteConfig(),
    var reopen: ReopenConfig = ReopenConfig(),
    var ticketChat: TicketChatConfig = TicketChatConfig(),
    var aiDefaults: AiDefaultsConfig = AiDefaultsConfig(),
    var files: FileConfig = FileConfig(),
    var logLabels: LogLabels = LogLabels(),

    var logs: LogConfig = LogConfig(),
    var errors: ErrorConfig = ErrorConfig(),
    var sales: SalesConfig = SalesConfig(),
    var summary: SummaryConfig = SummaryConfig(),
    var income: IncomeConfig = IncomeConfig(),
    var reload: ReloadConfig = ReloadConfig()
)

data class LogConfig(
    var ticketOpen: LogDetail = LogDetail("티켓 생성됨", "GREEN"),
    var ticketClose: LogDetail = LogDetail("티켓 종료됨", "RED"),
    var ticketReopen: LogDetail = LogDetail("티켓 재오픈됨", "BLUE"),
    var ticketDelete: LogDetail = LogDetail("티켓 삭제됨", "ORANGE"),
    var ticketIncome: LogDetail = LogDetail("수익 기록됨", "YELLOW"),
    var ticketSurvey: LogDetail = LogDetail("만족도 조사 제출됨", "PURPLE"),
    var ticketInactive: LogDetail = LogDetail("티켓 비활성화 알림", "GRAY")
)

data class ErrorConfig(
    var guildNotFound: String = "서버를 찾을 수 없습니다. 봇 설정을 확인해주세요.",
    var categoryNotFound: String = "티켓 카테고리를 찾을 수 없습니다. 관리자에게 문의해주세요.",
    var memberNotFound: String = "서버 멤버 정보를 가져올 수 없습니다. 서버에 참여했는지 확인해주세요.",
    var channelCreateFail: String = "채널 생성에 실패했습니다: %s"
)

data class SalesConfig(
    var description: String = "월별 매출을 조회합니다.",
    var optionDate: String = "조회할 년-월 (예: 2024-02)",
    var dateError: String = "날짜 형식이 올바르지 않습니다. (예: 2024-02)",
    var title: String = "%s 매출 현황",
    var totalAmount: String = "총 수익",
    var count: String = "총 건수",
    var footer: String = "최근 10건만 표시됩니다.",
    var listFormat: String = "- [#%s] %s | %s: %s원",
    var dateFormat: String = "dd일"
)

data class SummaryConfig(
    var description: String = "현재 대화 내용을 바탕으로 기획서를 작성합니다.",
    var content: String = "### 중간 점검: 기술 명세서 (파일 첨부됨)"
)

data class IncomeConfig(
    var description: String = "현재 티켓의 수익을 기록합니다.",
    var optionAmount: String = "수익 금액",
    var deleteDescription: String = "수익 기록을 삭제합니다.",
    var optionId: String = "삭제할 기록의 ID",
    var deleteSuccess: String = "수익 기록 #%d (이)가 삭제되었습니다.",
    var deleteFail: String = "해당 ID의 수익 기록을 찾을 수 없습니다."
)

data class ReloadConfig(
    var description: String = "설정을 다시 불러옵니다.",
    var message: String = "설정이 리로드되었습니다."
)

data class LogDetail(
    var title: String = "로그",
    var color: String = "GRAY"
)

data class MonitorConfig(
    var inactiveNotification: String = "%s\n\n이 티켓은 7일 이상 활동이 없습니다.\n고객: %s\n플러그인: %s (%s)\n마지막 활동: %s",
    var defaultAdminMention: String = "@관리자"
)

data class ChannelDeleteConfig(
    var title: String = "티켓 채널 수동 삭제됨",
    var description: String = "관리자에 의해 채널이 수동으로 삭제되었습니다.\n데이터는 'DELETED' 상태로 보존됩니다.",
    var fieldPlugin: String = "플러그인",
    var fieldCustomer: String = "고객",
    var fieldTime: String = "삭제 시각",
    var footer: String = "Ticket ID: %d"
)

data class ReopenConfig(
    var description: String = "닫힌 티켓을 다시 엽니다.",
    var successTitle: String = "티켓 재오픈",
    var successDescription: String = "티켓이 다시 열렸습니다.",
    var fail: String = "티켓 재오픈에 실패했습니다.",
    var notTicketChannel: String = "이 채널은 티켓 채널이 아닙니다.",
    var fieldPlugin: String = "플러그인",
    var fieldCustomer: String = "고객"
)

data class TicketChatConfig(
    var contextHeader: String = "[티켓 기본 정보]\n",
    var contextPlugin: String = "- 대상 플러그인: %s\n",
    var contextVersion: String = "- 마인크래프트 버전: %s\n",
    var contextCustomer: String = "- 의뢰인: %s\n\n",
    var contextHistory: String = "[대화 내역]\n",
    var contextAttachment: String = "[첨부 파일 내용]\n%s\n"
)

data class AiDefaultsConfig(
    var specError: String = "AI 응답을 생성할 수 없습니다.",
    var specPromptFallback: String = "대화 내용을 바탕으로 마인크래프트 플러그인 기술 명세서를 작성해주세요.\n\n\$chatHistory",
    var chatPromptFallback: String = "플러그인 기획에 대해 질문해주세요.\n\n\$chatHistory"
)

data class FileConfig(
    var tooLarge: String = "[파일이 너무 큽니다: %s (최대 10MB)]",
    var attachment: String = "[파일: %s]\n%s",
    var imageAttachment: String = "[이미지 첨부: %s]",
    var downloadFail: String = "[파일 다운로드 실패: %s]",
    var maxSizeBytes: Long = 10485760
)

data class LogLabels(
    var labelPlugin: String = "플러그인",
    var labelCustomer: String = "고객",
    var labelCreatedAt: String = "생성일",
    var labelClosedAt: String = "종료일",
    var labelReopenedAt: String = "재오픈일",
    var labelDeletedAt: String = "삭제일",
    var labelRecordedAt: String = "기록일",
    var labelSubmittedAt: String = "제출일",
    var labelAmount: String = "수익금",
    var labelRating: String = "평점"
)
