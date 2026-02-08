package rus.setv.adminbackend.controller

import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import rus.setv.adminbackend.dto.*
import rus.setv.adminbackend.model.AppEntity
import rus.setv.adminbackend.model.AppImageEntity
import rus.setv.adminbackend.model.AppStatus
import rus.setv.adminbackend.repository.AppImageRepository
import rus.setv.adminbackend.repository.AppRepository
import rus.setv.adminbackend.service.ApkParserService
import rus.setv.adminbackend.service.FileStorageService
import java.time.LocalDateTime
import java.util.*

@RestController
@RequestMapping("/api")
class AppController(
    private val appRepository: AppRepository,
    private val appImageRepository: AppImageRepository,
    private val apkParserService: ApkParserService,
    private val fileStorageService: FileStorageService
) {

    /* ================= helpers ================= */

    private fun buildAppDto(app: AppEntity): AppDto {
        val images = appImageRepository
            .findAllByAppIdOrderBySortOrder(app.id!!)
            .map {
                AppImageDto(
                    id = it.id,
                    imageUrl = it.imageUrl,
                    sortOrder = it.sortOrder
                )
            }

        return app.toDto(images)
    }

    private fun syncImages(app: AppEntity, images: List<AppImageDto>) {
        val existing = appImageRepository.findAllByAppIdOrderBySortOrder(app.id!!)

        val incomingUrls = images.map { it.imageUrl }.toSet()

        // удалить лишние
        existing
            .filter { it.imageUrl !in incomingUrls }
            .forEach {
                fileStorageService.deleteImage(it.imageUrl)
                appImageRepository.delete(it)
            }

        // обновить порядок / добавить новые
        images.forEachIndexed { index, dto ->
            val entity = existing.find { it.imageUrl == dto.imageUrl }

            if (entity != null) {
                if (entity.sortOrder != index) {
                    entity.sortOrder = index
                    appImageRepository.save(entity)
                }
            } else {
                appImageRepository.save(
                    AppImageEntity(
                        app = app,
                        imageUrl = dto.imageUrl,
                        sortOrder = index
                    )
                )
            }
        }
    }

    /* ================= public ================= */

    @GetMapping("/public/apps")
    fun getPublicApps(): List<AppDto> =
        appRepository.findAll()
            .filter { it.status == AppStatus.ACTIVE }
            .map { buildAppDto(it) }

    /* ================= apk ================= */

    @PostMapping(
        "/apps/parse-apk",
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE]
    )
    fun parseApk(
        @RequestPart("file") file: MultipartFile
    ): ResponseEntity<ApkParseResultDto> {

        if (file.isEmpty || !file.originalFilename.orEmpty().endsWith(".apk")) {
            return ResponseEntity.badRequest().build()
        }

        return ResponseEntity.ok(apkParserService.parseAndStore(file))
    }

    /* ================= create ================= */

    @PostMapping("/apps")
    fun createApp(@RequestBody dto: AppRequest): AppDto {

        val app = appRepository.save(
            AppEntity(
                name = dto.name,
                packageName = dto.packageName,
                version = dto.version,
                description = dto.description,
                iconUrl = dto.iconUrl,
                apkUrl = dto.apkUrl,
                category = dto.category,
                status = dto.status,
                featured = dto.featured,
                updatedAt = LocalDateTime.now()
            )
        )

        syncImages(app, dto.images)

        return buildAppDto(app)
    }

    /* ================= read ================= */

    @GetMapping("/apps")
    fun getAllApps(): List<AppDto> =
        appRepository.findAll().map { buildAppDto(it) }

    @GetMapping("/apps/{id}")
    fun getAppById(@PathVariable id: UUID): ResponseEntity<AppDto> {
        val app = appRepository.findById(id).orElse(null)
            ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok(buildAppDto(app))
    }

    /* ================= update ================= */

    @PutMapping("/apps/{id}")
    fun updateApp(
        @PathVariable id: UUID,
        @RequestBody dto: AppRequest
    ): ResponseEntity<AppDto> {

        val app = appRepository.findById(id).orElse(null)
            ?: return ResponseEntity.notFound().build()

        if (app.apkUrl != dto.apkUrl) {
            fileStorageService.deleteApkFile(app.apkUrl)
        }

        if (app.iconUrl != dto.iconUrl) {
            fileStorageService.deleteIconFile(app.iconUrl)
        }

        app.apply {
            name = dto.name
            packageName = dto.packageName
            version = dto.version
            description = dto.description
            iconUrl = dto.iconUrl
            apkUrl = dto.apkUrl
            category = dto.category
            status = dto.status
            featured = dto.featured
            updatedAt = LocalDateTime.now()
        }

        val saved = appRepository.save(app)

        syncImages(saved, dto.images)

        return ResponseEntity.ok(buildAppDto(saved))
    }

    /* ================= delete ================= */

    @DeleteMapping("/apps/{id}")
    fun deleteApp(@PathVariable id: UUID): ResponseEntity<Void> {

        val app = appRepository.findById(id).orElse(null)
            ?: return ResponseEntity.notFound().build()

        appImageRepository.findAllByAppIdOrderBySortOrder(id).forEach {
            fileStorageService.deleteImage(it.imageUrl)
        }
        appImageRepository.deleteAllByAppId(id)

        fileStorageService.deleteApkFile(app.apkUrl)
        fileStorageService.deleteIconFile(app.iconUrl)

        appRepository.delete(app)

        return ResponseEntity.noContent().build()
    }

    /* ================= images ================= */

    @PostMapping("/apps/upload-image")
    fun uploadImage(
        @RequestParam("file") file: MultipartFile
    ): AppImageDto {

        if (file.isEmpty) {
            throw RuntimeException("Файл пустой")
        }

        val url = fileStorageService.storeImage(file)

        return AppImageDto(
            id = null,
            imageUrl = url,
            sortOrder = 0
        )
    }
}