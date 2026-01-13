package rus.setv.adminbackend.service

import net.dongliu.apk.parser.ApkFile
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import rus.setv.adminbackend.dto.ApkParseResultDto
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*

@Service
class ApkParserService(

    @Value("\${app.base-url}")
    private val baseUrl: String,

    @Value("\${app.apk.storage-path}")
    private val storagePath: String
) {

    private val uploadDir: Path =
        Paths.get(storagePath).toAbsolutePath().normalize()

    init {
        Files.createDirectories(uploadDir)
    }

    fun parseAndStore(file: MultipartFile): ApkParseResultDto {
        if (file.isEmpty) {
            throw IllegalArgumentException("APK file is empty")
        }

        val fileName = "${UUID.randomUUID()}.apk"
        val targetPath = uploadDir.resolve(fileName)

        file.inputStream.use { input ->
            Files.copy(input, targetPath)
        }

        ApkFile(targetPath.toFile()).use { apk ->
            val meta = apk.apkMeta

            return ApkParseResultDto(
                name = meta.label ?: "Unknown",
                packageName = meta.packageName,
                versionName = meta.versionName,
                apkUrl = "$baseUrl/files/apk/$fileName"
            )
        }
    }
}
