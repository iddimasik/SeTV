package rus.setv.adminbackend.service

import net.dongliu.apk.parser.ApkFile
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import rus.setv.adminbackend.dto.ApkParseResultDto
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.*

@Service
class ApkParserService {

    private val uploadDir: Path = Path.of("storage/apk")

    init {
        Files.createDirectories(uploadDir)
    }

    fun parseAndStore(file: MultipartFile): ApkParseResultDto {
        if (file.isEmpty) {
            throw IllegalArgumentException("APK file is empty")
        }

        val fileName = "${UUID.randomUUID()}.apk"
        val targetPath = uploadDir.resolve(fileName)
        file.transferTo(targetPath)

        ApkFile(targetPath.toFile()).use { apk ->
            val meta = apk.apkMeta

            return ApkParseResultDto(
                name = meta.label ?: "Unknown",
                packageName = meta.packageName,
                versionName = meta.versionName,
                apkUrl = "/files/apk/$fileName"
            )
        }
    }
}
