package dev.acog.plugin.exception

sealed class TicketCreationException(message: String) : RuntimeException(message)

class GuildNotFoundException(guildId: String) : TicketCreationException("길드를 찾을 수 없습니다: $guildId")

class CategoryNotFoundException(categoryId: String) : TicketCreationException("카테고리를 찾을 수 없습니다: $categoryId")

class MemberNotFoundException(userId: String) : TicketCreationException("멤버를 찾을 수 없습니다: $userId")

class ChannelCreationException(reason: String) : TicketCreationException("채널 생성 실패: $reason")
