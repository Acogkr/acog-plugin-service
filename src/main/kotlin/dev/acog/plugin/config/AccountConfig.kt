package dev.acog.plugin.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "account")
data class AccountConfig(
    var bank: String = "",
    var number: String = "",
    var owner: String = "",
    var title: String = "입금 계좌 안내",
    var description: String = "*입금 후 반드시 입금자명을 말씀해 주세요!*"
)
