package rus.setv.adminbackend.controller

import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import rus.setv.adminbackend.dto.*
import rus.setv.adminbackend.model.AppEntity
import rus.setv.adminbackend.model.AppStatus
import rus.setv.adminbackend.repository.AppRepository
import rus.setv.adminbackend.service.ApkParserService
import rus.setv.adminbackend.service.FileStorageService
import java.time.LocalDateTime
import java.util.*

@RestController
@RequestMapping("/api")
class AppController(
    private val appRepository: AppRepository,
    private val apkParserService: ApkParserService,
    private val fileStorageService: FileStorageService
) {

    @GetMapping("/public/apps")
    fun getPublicApps(): List<AppDto> =
        appRepository.findAll()
            .filter { it.status == AppStatus.ACTIVE }
            .map { it.toDto() }

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

        return ResponseEntity.ok(
            apkParserService.parseAndStore(file)
        )
    }

    @PostMapping("/apps")
    fun createApp(@RequestBody dto: AppRequest): AppDto {
        val app = AppEntity(
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

        return appRepository.save(app).toDto()
    }

    @GetMapping("/apps")
    fun getAllApps(): List<AppDto> =
        appRepository.findAll().map { it.toDto() }

    @GetMapping("/apps/{id}")
    fun getAppById(@PathVariable id: UUID): ResponseEntity<AppDto> {
        val app = appRepository.findById(id).orElse(null)
            ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok(app.toDto())
    }

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

        app.name = dto.name
        app.packageName = dto.packageName
        app.version = dto.version
        app.description = dto.description
        app.iconUrl = dto.iconUrl
        app.apkUrl = dto.apkUrl
        app.category = dto.category
        app.status = dto.status
        app.featured = dto.featured
        app.updatedAt = LocalDateTime.now()

        return ResponseEntity.ok(
            appRepository.save(app).toDto()
        )
    }

    @DeleteMapping("/apps/{id}")
    fun deleteApp(@PathVariable id: UUID): ResponseEntity<Void> {
        val app = appRepository.findById(id).orElse(null)
            ?: return ResponseEntity.notFound().build()

        fileStorageService.deleteApkFile(app.apkUrl)
        fileStorageService.deleteIconFile(app.iconUrl)

        appRepository.delete(app)

        return ResponseEntity.noContent().build()
    }
}
