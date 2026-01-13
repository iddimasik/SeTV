package rus.setv.adminbackend.service

import org.springframework.stereotype.Service
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

@Service
class FileStorageService {

    private val apkStoragePath: Path =
        Paths.get("uploads/apks").toAbsolutePath().normalize()

    fun deleteApkFile(apkUrl: String?) {
        if (apkUrl.isNullOrBlank()) return

        // Если apkUrl = "/files/apks/app.apk"
        val fileName = apkUrl.substringAfterLast("/")
        val filePath = apkStoragePath.resolve(fileName)

        Files.deleteIfExists(filePath)
    }
}
