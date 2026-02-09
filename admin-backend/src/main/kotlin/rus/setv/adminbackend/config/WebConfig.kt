package rus.setv.adminbackend.config

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.nio.file.Paths

@Configuration
class WebConfig : WebMvcConfigurer {

    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {

        // Папка с APK файлами
        val apkDir = Paths.get("storage/apk")
            .toAbsolutePath()
            .normalize()
            .toString()

        // Папка с изображениями
        val imagesDir = Paths.get("storage/images")
            .toAbsolutePath()
            .normalize()
            .toString()

        // Настройка отдачи APK файлов
        registry.addResourceHandler("/files/apk/**")
            .addResourceLocations("file:$apkDir/")   // обязательно file:

        // Настройка отдачи изображений
        registry.addResourceHandler("/files/images/**")
            .addResourceLocations("file:$imagesDir/") // обязательно file:
    }
}
