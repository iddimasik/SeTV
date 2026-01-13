package rus.setv.adminbackend.controller

import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import rus.setv.adminbackend.dto.*
import rus.setv.adminbackend.model.AppEntity
import rus.setv.adminbackend.repository.AppRepository
import rus.setv.adminbackend.service.ApkParserService
import java.time.LocalDateTime
import java.util.*

@RestController
@RequestMapping("/api/apps")
@CrossOrigin(origins = ["http://localhost:5173"])
class AppController(
    private val appRepository: AppRepository,
    private val apkParserService: ApkParserService
) {

    // =========================
    // APK PARSE (UPLOAD ONLY)
    // =========================
    @PostMapping(
        "/parse-apk",
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE]
    )
    fun parseApk(
        @RequestPart("file") file: MultipartFile
    ): ResponseEntity<ApkParseResultDto> {

        if (file.isEmpty) {
            return ResponseEntity.badRequest().build()
        }

        if (!file.originalFilename.orEmpty().endsWith(".apk")) {
            return ResponseEntity.badRequest().build()
        }

        val result = apkParserService.parseAndStore(file)
        return ResponseEntity.ok(result)
    }

    // =========================
    // CREATE APP
    // =========================
    @PostMapping
    fun createApp(@RequestBody dto: AppRequest): AppDto {
        val app = AppEntity(
            name = dto.name,
            packageName = dto.packageName,
            version = dto.version,
            description = dto.description,
            iconUrl = dto.iconUrl,
            bannerUrl = dto.bannerUrl,
            apkUrl = dto.apkUrl,
            category = dto.category,
            status = dto.status,
            featured = dto.featured,
            updatedAt = LocalDateTime.now()
        )

        return appRepository.save(app).toDto()
    }

    // =========================
    // READ ALL
    // =========================
    @GetMapping
    fun getAllApps(): List<AppDto> =
        appRepository.findAll().map { it.toDto() }

    // =========================
    // READ ONE
    // =========================
    @GetMapping("/{id}")
    fun getAppById(@PathVariable id: UUID): ResponseEntity<AppDto> {
        return appRepository.findById(id)
            .map { ResponseEntity.ok(it.toDto()) }
            .orElse(ResponseEntity.notFound().build())
    }

    // =========================
    // UPDATE
    // =========================
    @PutMapping("/{id}")
    fun updateApp(
        @PathVariable id: UUID,
        @RequestBody dto: AppRequest
    ): ResponseEntity<AppDto> {

        return appRepository.findById(id).map { app ->
            app.name = dto.name
            app.packageName = dto.packageName
            app.version = dto.version
            app.description = dto.description
            app.iconUrl = dto.iconUrl
            app.bannerUrl = dto.bannerUrl
            app.apkUrl = dto.apkUrl
            app.category = dto.category
            app.status = dto.status
            app.featured = dto.featured
            app.updatedAt = LocalDateTime.now()

            ResponseEntity.ok(appRepository.save(app).toDto())
        }.orElse(ResponseEntity.notFound().build())
    }

    // =========================
    // DELETE
    // =========================
    @DeleteMapping("/{id}")
    fun deleteApp(@PathVariable id: UUID): ResponseEntity<Void> {
        return if (appRepository.existsById(id)) {
            appRepository.deleteById(id)
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }
}
