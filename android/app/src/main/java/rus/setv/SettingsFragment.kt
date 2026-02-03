package rus.setv

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment

class SettingsFragment : Fragment(R.layout.fragment_settings) {

    private lateinit var appNameText: TextView
    private lateinit var appVersionText: TextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        appNameText = view.findViewById(R.id.text_app_name)
        appVersionText = view.findViewById(R.id.text_app_version)

        loadAppInfo()
    }

    private fun loadAppInfo() {
        try {
            val context = requireContext()
            val packageManager = context.packageManager
            val packageName = "rus.setv"

            val packageInfo = packageManager.getPackageInfo(
                packageName,
                PackageManager.GET_ACTIVITIES
            )

            val appName = "A-Store"

            val versionName = packageInfo.versionName ?: "—"

            appNameText.text = appName
            appVersionText.text = "Версия $versionName"

        } catch (e: Exception) {
            appNameText.text = "Приложение"
            appVersionText.text = "Версия —"
            e.printStackTrace()
        }
    }
}
