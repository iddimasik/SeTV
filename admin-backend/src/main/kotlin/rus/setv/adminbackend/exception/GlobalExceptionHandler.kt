package rus.setv.adminbackend.exception

import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.multipart.MaxUploadSizeExceededException

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(MaxUploadSizeExceededException::class)
    fun handleMaxSize(
        ex: MaxUploadSizeExceededException
    ): ResponseEntity<Map<String, String>> {
        return ResponseEntity
            .status(HttpStatus.PAYLOAD_TOO_LARGE)
            .body(
                mapOf(
                    "error" to "FILE_TOO_LARGE",
                    "message" to "Размер APK превышает допустимый лимит"
                )
            )
    }

    @ExceptionHandler(DataIntegrityViolationException::class)
    fun handleDuplicatePackage(
        ex: DataIntegrityViolationException
    ): ResponseEntity<Map<String, String>> {

        val message =
            if (ex.rootCause?.message?.contains("package_name") == true) {
                "Приложение с таким packageName уже существует"
            } else {
                "Ошибка сохранения данных"
            }

        return ResponseEntity
            .status(HttpStatus.CONFLICT) // 409
            .body(
                mapOf(
                    "error" to "DUPLICATE_PACKAGE",
                    "message" to message
                )
            )
    }

    @ExceptionHandler(Exception::class)
    fun handleGeneric(
        ex: Exception
    ): ResponseEntity<Map<String, String>> {
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(
                mapOf(
                    "error" to "INTERNAL_ERROR",
                    "message" to "Внутренняя ошибка сервера"
                )
            )
    }
}
