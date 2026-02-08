package rus.setv.adminbackend.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.*

@Service
class FileStorageService(

    @Value("\${app.apk.storage-path}")
    private val apkStoragePathStr: String,

    @Value("\${app.icon.storage-path}")
    private val iconStoragePathStr: String,

    @Value("\${app.images.storage-path}")
    private val imageStoragePathStr: String
) {

    private val apkStoragePath: Path =
        Paths.get(apkStoragePathStr).toAbsolutePath().normalize()

    private val iconStoragePath: Path =
        Paths.get(iconStoragePathStr).toAbsolutePath().normalize()

    private val imageStoragePath: Path =
        Paths.get(imageStoragePathStr).toAbsolutePath().normalize()

    private val allowedImageTypes = setOf(
        "image/png",
        "image/jpeg",
        "image/webp"
    )

    init {
        Files.createDirectories(apkStoragePath)
        Files.createDirectories(iconStoragePath)
        Files.createDirectories(imageStoragePath)
    }

    fun storeImage(file: MultipartFile): String {
        if (file.isEmpty) {
            throw IllegalArgumentException("Empty image file")
        }

        if (!allowedImageTypes.contains(file.contentType)) {
            throw IllegalArgumentException("Unsupported image type: ${file.contentType}")
        }

        val extension = when (file.contentType) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            else -> "jpg"
        }

        val fileName = "${UUID.randomUUID()}.$extension"
        val targetPath = imageStoragePath.resolve(fileName)

        Files.copy(
            file.inputStream,
            targetPath,
            StandardCopyOption.REPLACE_EXISTING
        )

        return "/files/images/$fileName"
    }

    fun deleteImage(imageUrl: String?) {
        if (imageUrl.isNullOrBlank()) return

        val fileName = imageUrl.substringAfterLast("/")
        val filePath = imageStoragePath.resolve(fileName)

        try {
            Files.deleteIfExists(filePath)
        } catch (e: Exception) {
            println("Failed to delete image file: $filePath")
            e.printStackTrace()
        }
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
