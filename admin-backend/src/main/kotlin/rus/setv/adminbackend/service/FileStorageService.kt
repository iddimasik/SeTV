package rus.setv.adminbackend.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

@Service
class FileStorageService(

    @Value("\${app.apk.storage-path}")
    private val storagePath: String
) {

    private val apkStoragePath: Path =
        Paths.get(storagePath).toAbsolutePath().normalize()

    init {
        Files.createDirectories(apkStoragePath)
    }

    fun deleteApkFile(apkUrl: String?) {
        if (apkUrl.isNullOrBlank()) return

        val fileName = apkUrl.substringAfterLast("/")
        val filePath = apkStoragePath.resolve(fileName)

        try {
            Files.deleteIfExists(filePath)
        } catch (e: Exception) {
            println("Failed to delete APK file: $filePath")
            e.printStackTrace()
        }
    }
}