package dev.acog.plugin.config

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.requests.GatewayIntent
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class JdaConfig(
    private val botConfig: BotConfig
) {

    @Bean
    fun jda(): JDA {
        return JDABuilder.createDefault(botConfig.token)
            .enableIntents(GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MEMBERS)
            .build()
    }
}
