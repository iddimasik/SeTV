package rus.setv.adminbackend.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

@Service
class FileStorageService(

    @Value("\${app.apk.storage-path}")
    private val apkStoragePathStr: String,

    @Value("\${app.icon.storage-path}")
    private val iconStoragePathStr: String
) {

    private val apkStoragePath: Path =
        Paths.get(apkStoragePathStr).toAbsolutePath().normalize()

    private val iconStoragePath: Path =
        Paths.get(iconStoragePathStr).toAbsolutePath().normalize()

    init {
        Files.createDirectories(apkStoragePath)
        Files.createDirectories(iconStoragePath)
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

    fun deleteIconFile(iconUrl: String?) {
        if (iconUrl.isNullOrBlank()) return

        val fileName = iconUrl.substringAfterLast("/")
        val filePath = iconStoragePath.resolve(fileName)

        try {
            Files.deleteIfExists(filePath)
        } catch (e: Exception) {
            println("Failed to delete icon file: $filePath")
            e.printStackTrace()
        }
    }
}
