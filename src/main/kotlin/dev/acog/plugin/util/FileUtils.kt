package dev.acog.plugin.util

import dev.acog.plugin.config.MessageConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext
import net.dv8tion.jda.api.entities.Message
import org.apache.pdfbox.Loader
import org.apache.pdfbox.text.PDFTextStripper
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.io.ByteArrayInputStream

@Component
class FileUtils(
    private val messageConfig: MessageConfig
) {
    private val logger = LoggerFactory.getLogger(FileUtils::class.java)

    private val supportedTextExtensions = setOf("txt", "md", "json", "yml", "yaml")

    suspend fun downloadAttachment(attachment: Message.Attachment): String? = withContext(Dispatchers.IO) {
        try {
            if (attachment.size > messageConfig.files.maxSizeBytes) {
                logger.warn("File too large: ${attachment.fileName} (${attachment.size} bytes)")
                return@withContext messageConfig.files.tooLarge.format(attachment.fileName)
            }

            val extension = attachment.fileExtension?.lowercase()

            when {
                extension in supportedTextExtensions -> {
                    val content = attachment.proxy.download().await()
                        .bufferedReader()
                        .use { reader -> reader.readText().take(10000) }
                    messageConfig.files.attachment.format(attachment.fileName, content)
                }
                extension == "pdf" -> {
                    val bytes = downloadToBytes(attachment)
                    val content = extractPdfText(bytes).take(10000)
                    messageConfig.files.attachment.format(attachment.fileName, content)
                }
                extension == "docx" -> {
                    val bytes = downloadToBytes(attachment)
                    val content = extractDocxText(bytes).take(10000)
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

    private suspend fun downloadToBytes(attachment: Message.Attachment): ByteArray {
        val inputStream = attachment.proxy.download().await()
        return inputStream.use { stream -> stream.readBytes() }
    }

    private fun extractPdfText(bytes: ByteArray): String {
        return Loader.loadPDF(bytes).use { document ->
            PDFTextStripper().getText(document)
        }
    }

    private fun extractDocxText(bytes: ByteArray): String {
        return ByteArrayInputStream(bytes).use { inputStream ->
            XWPFDocument(inputStream).use { document ->
                document.paragraphs.joinToString("\n") { paragraph -> paragraph.text }
            }
        }
    }
}
