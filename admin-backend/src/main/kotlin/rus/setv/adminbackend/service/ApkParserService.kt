package rus.setv.adminbackend.service

import net.dongliu.apk.parser.ApkFile
import net.dongliu.apk.parser.bean.IconFace
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

        val apkFileName = "${UUID.randomUUID()}.apk"
        val apkTargetPath = apkUploadDir.resolve(apkFileName)

        file.inputStream.use { input ->
            Files.copy(input, apkTargetPath)
        }

        ApkFile(apkTargetPath.toFile()).use { apk ->
            val meta = apk.apkMeta

            val iconUrl = extractBestIcon(apk)

            return ApkParseResultDto(
                name = meta.label ?: "Unknown",
                packageName = meta.packageName,
                versionName = meta.versionName,
                apkUrl = "$baseUrl/files/apk/$apkFileName",
                iconUrl = iconUrl
            )
        }
    }

    private fun extractBestIcon(apk: ApkFile): String? {
        val icons: List<IconFace> = apk.allIcons ?: return null
        if (icons.isEmpty()) return null

        var bestIcon: IconFace? = null
        var maxSize = 0

        for (icon in icons) {
            val bytes = apk.getFileData(icon.path)
            if (bytes.size > maxSize) {
                maxSize = bytes.size
                bestIcon = icon
            }
        }

        if (bestIcon == null) return null

        val iconFileName = "${UUID.randomUUID()}.png"
        val iconTargetPath = iconUploadDir.resolve(iconFileName)

        val iconBytes = apk.getFileData(bestIcon.path)
        Files.write(iconTargetPath, iconBytes)

        return "$baseUrl/files/icons/$iconFileName"
    }
}
