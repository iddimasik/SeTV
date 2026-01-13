package rus.setv.apk

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.util.Log
import rus.setv.apk.InstallResultReceiver
import java.io.File

object ApkInstaller {

    private const val TAG = "ApkInstaller"

    fun install(context: Context, apkFile: File) {
        try {
            val packageInstaller = context.packageManager.packageInstaller

            val params = PackageInstaller.SessionParams(
                PackageInstaller.SessionParams.MODE_FULL_INSTALL
            )

            val sessionId = packageInstaller.createSession(params)
            val session = packageInstaller.openSession(sessionId)

            session.openWrite("app", 0, apkFile.length()).use { out ->
                apkFile.inputStream().use { input ->
                    input.copyTo(out)
                    session.fsync(out)
                }
            }

            val intent = Intent(context, InstallResultReceiver::class.java)

            // 🔥 ВАЖНО: MUTABLE!
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                sessionId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )

            Log.d(TAG, "Committing install session $sessionId")
            session.commit(pendingIntent.intentSender)

            session.close()

        } catch (e: Exception) {
            Log.e(TAG, "Install failed", e)
        }
    }
}