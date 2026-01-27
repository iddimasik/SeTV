package rus.setv.apk

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.util.Log
import android.widget.Toast

class InstallResultReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        val status = intent.getIntExtra(
            PackageInstaller.EXTRA_STATUS,
            PackageInstaller.STATUS_FAILURE
        )

        val message = intent.getStringExtra(
            PackageInstaller.EXTRA_STATUS_MESSAGE
        )

        Log.d("InstallReceiver", "status=$status message=$message")

        when (status) {

            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val confirmIntent =
                    intent.getParcelableExtra<Intent>(
                        Intent.EXTRA_INTENT
                    )
                confirmIntent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(confirmIntent)
            }

            PackageInstaller.STATUS_SUCCESS -> {
                Toast.makeText(
                    context,
                    "Приложение установлено",
                    Toast.LENGTH_LONG
                ).show()

                Log.d("InstallReceiver", "SUCCESS")
                // TODO: обновить app.status = INSTALLED
            }

            else -> {
                Toast.makeText(
                    context,
                    "Ошибка установки",
                    Toast.LENGTH_LONG
                ).show()

                Log.e("InstallReceiver", "FAIL: $message")
            }
        }
    }
}