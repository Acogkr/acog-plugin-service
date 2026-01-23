package dev.acog.plugin

import io.github.cdimascio.dotenv.dotenv
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
@ConfigurationPropertiesScan
class Application

fun main(args: Array<String>) {
    val dotenv = dotenv {
        ignoreIfMissing = true
    }
    dotenv.entries().forEach { entry ->
        System.setProperty(entry.key, entry.value)
    }
    
    runApplication<Application>(*args)
}
