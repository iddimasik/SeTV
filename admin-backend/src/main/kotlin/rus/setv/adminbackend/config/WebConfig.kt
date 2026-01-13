package rus.setv.adminbackend.config

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.nio.file.Paths

@Configuration
class WebConfig : WebMvcConfigurer {

    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        val apkDir = Paths.get("storage/apk")
            .toAbsolutePath()
            .normalize()
            .toUri()
            .toString()

        registry.addResourceHandler("/files/apk/**")
            .addResourceLocations(apkDir)
    }
}
