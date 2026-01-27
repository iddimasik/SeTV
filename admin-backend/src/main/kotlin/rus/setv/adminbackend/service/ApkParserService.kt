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
    private val apkStoragePath: String,

    @Value("\${app.icon.storage-path}")
    private val iconStoragePath: String
) {

    private val apkUploadDir: Path =
        Paths.get(apkStoragePath).toAbsolutePath().normalize()

    private val iconUploadDir: Path =
        Paths.get(iconStoragePath).toAbsolutePath().normalize()

    init {
        Files.createDirectories(apkUploadDir)
        Files.createDirectories(iconUploadDir)
    }

    fun parseAndStore(file: MultipartFile): ApkParseResultDto {
        if (file.isEmpty) {
            throw IllegalArgumentException("APK file is empty")
        }

        // ===== Save APK =====
        val apkFileName = "${UUID.randomUUID()}.apk"
        val apkTargetPath = apkUploadDir.resolve(apkFileName)

        file.inputStream.use { input ->
            Files.copy(input, apkTargetPath)
        }

        ApkFile(apkTargetPath.toFile()).use { apk ->
            val meta = apk.apkMeta

            // ===== Extract icon =====
            var iconUrl: String? = null

            val icon = apk.iconFile
            if (icon != null) {
                val iconFileName = "${UUID.randomUUID()}.png"
                val iconTargetPath = iconUploadDir.resolve(iconFileName)

                val iconBytes = apk.getFileData(icon.path)
                Files.write(iconTargetPath, iconBytes)

                iconUrl = "$baseUrl/files/icons/$iconFileName"
            }

            return ApkParseResultDto(
                name = meta.label ?: "Unknown",
                packageName = meta.packageName,
                versionName = meta.versionName,
                apkUrl = "$baseUrl/files/apk/$apkFileName",
                iconUrl = iconUrl
            )
        }
    }
}
