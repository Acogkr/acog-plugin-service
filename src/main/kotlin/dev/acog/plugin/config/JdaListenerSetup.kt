package dev.acog.plugin.config

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component

@Component
class JdaListenerSetup(
    private val jda: JDA,
    private val listeners: List<ListenerAdapter>
) : CommandLineRunner {

    override fun run(vararg args: String?) {
        listeners.forEach { listener ->
            jda.addEventListener(listener)
        }
    }
}
