package rus.setv.apk

import android.content.Context
import android.util.Log
import rus.setv.model.AppItem
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

object ApkDownloader {

    private const val TAG = "ApkDownloader"
    private const val MAX_REDIRECTS = 5

    fun download(
        context: Context,
        app: AppItem,
        onProgress: (Int) -> Unit,
        onDone: (File) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        Thread {
            try {
                var url = URL(app.apkUrl)
                var redirects = 0
                var connection: HttpURLConnection

                // ===== REDIRECT LOOP =====
                while (true) {
                    connection = url.openConnection() as HttpURLConnection
                    connection.instanceFollowRedirects = false
                    connection.connectTimeout = 20000
                    connection.readTimeout = 20000
                    connection.requestMethod = "GET"
                    connection.connect()

                    val code = connection.responseCode
                    Log.d(TAG, "HTTP $code : $url")

                    if (code in 300..399) {
                        val location = connection.getHeaderField("Location")
                            ?: throw IOException("Redirect without Location")

                        if (++redirects > MAX_REDIRECTS) {
                            throw IOException("Too many redirects")
                        }

                        url = URL(location)
                        continue
                    }

                    if (code != HttpURLConnection.HTTP_OK) {
                        throw IOException("HTTP $code")
                    }

                    break
                }

                val reportedSize = connection.contentLengthLong
                Log.d(TAG, "Reported size: $reportedSize")

                val file = File(context.cacheDir, "${app.name}.apk")

                var downloaded = 0L
                var lastProgress = -1

                connection.inputStream.use { input ->
                    FileOutputStream(file).use { output ->
                        val buffer = ByteArray(16 * 1024)
                        var count: Int

                        while (true) {
                            count = input.read(buffer)
                            if (count == -1) break

                            output.write(buffer, 0, count)
                            downloaded += count

                            if (reportedSize > 0) {
                                val progress =
                                    ((downloaded * 100) / reportedSize).toInt()
                                if (progress != lastProgress) {
                                    lastProgress = progress
                                    onProgress(progress.coerceAtMost(100))
                                }
                            }
                        }
                    }
                }

                Log.d(TAG, "Downloaded bytes: $downloaded")

                if (file.length() < 500 * 1024) {
                    throw IOException("APK too small: ${file.length()}")
                }

                onDone(file)

            } catch (e: Throwable) {
                Log.e(TAG, "Download failed", e)
                onError(e)
            }
        }.start()
    }
}