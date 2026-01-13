package rus.setv.adminbackend.dto

data class ApkParseResultDto(
    val name: String,
    val packageName: String,
    val versionName: String?,
    val apkUrl: String
)
