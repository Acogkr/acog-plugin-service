package dev.acog.plugin.util

import dev.acog.plugin.config.MessageConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext
import net.dv8tion.jda.api.entities.Message
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class FileUtils(
    private val messageConfig: MessageConfig
) {
    private val logger = LoggerFactory.getLogger(FileUtils::class.java)
    
    private val SUPPORTED_TEXT_EXTENSIONS = setOf("txt", "md", "json", "yml", "yaml")

    
    suspend fun downloadAttachment(attachment: Message.Attachment): String? = withContext(Dispatchers.IO) {
        try {
            if (attachment.size > messageConfig.files.maxSizeBytes) {
                logger.warn("File too large: ${attachment.fileName} (${attachment.size} bytes)")
                return@withContext messageConfig.files.tooLarge.format(attachment.fileName)
            }
            
            val extension = attachment.fileExtension?.lowercase()
            
            when {
                extension in SUPPORTED_TEXT_EXTENSIONS -> {
                    val content = attachment.proxy.download().await()
                        .bufferedReader()
                        .use { reader -> reader.readText().take(10000) }
                    messageConfig.files.attachment.format(attachment.fileName, content)
                }
                attachment.isImage -> {
                    messageConfig.files.imageAttachment.format(attachment.fileName)
                }
                else -> {
                    logger.debug("Unsupported file type: ${attachment.fileName}")
                    null
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to download attachment: ${attachment.fileName}", e)
            messageConfig.files.downloadFail.format(attachment.fileName)
        }
    }
}
