package rus.setv

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import rus.setv.apk.ApkDownloader
import rus.setv.apk.ApkInstaller
import rus.setv.model.AppItem
import rus.setv.model.AppStatus
import java.text.DecimalFormat

class AppDetailsFragment : Fragment(R.layout.lb_app_details) {

    private lateinit var app: AppItem

    private lateinit var image: ImageView
    private lateinit var title: TextView
    private lateinit var version: TextView
    private lateinit var size: TextView
    private lateinit var category: TextView
    private lateinit var desc: TextView
    private lateinit var status: TextView
    private lateinit var progress: LinearProgressIndicator
    private lateinit var installButton: MaterialButton
    private lateinit var backButton: MaterialButton

    // ───────────────────────
    // RECEIVER УСТАНОВКИ
    // ───────────────────────
    private val installReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val data = intent.data ?: return
            val installedPackage = data.schemeSpecificPart

            if (installedPackage == app.packageName) {
                app.status = AppStatus.INSTALLED
                updateUi()

                Toast.makeText(
                    requireContext(),
                    "Приложение установлено",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        app = requireArguments().getParcelable(ARG_APP)!!

        image = view.findViewById(R.id.appImage)
        title = view.findViewById(R.id.appTitle)
        version = view.findViewById(R.id.appVersion)
        size = view.findViewById(R.id.appSize)
        category = view.findViewById(R.id.appCategory)
        desc = view.findViewById(R.id.appDescription)
        status = view.findViewById(R.id.statusText)
        progress = view.findViewById(R.id.progress)
        installButton = view.findViewById(R.id.installButton)
        backButton = view.findViewById(R.id.backButton)

        bindApp()
        setupButtons()

        // Устанавливаем фокус на кнопку "Установить" для Android TV
        installButton.post {
            installButton.requestFocus()
        }
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addDataScheme("package")
        }
        requireContext().registerReceiver(installReceiver, filter)
    }

    override fun onStop() {
        super.onStop()
        requireContext().unregisterReceiver(installReceiver)
    }

    // ───────────────────────
    // BIND APP
    // ───────────────────────
    private fun bindApp() {
        title.text = app.name
        version.text = app.version ?: "N/A"
        size.text = "13 mb"//formatSize(app.size)
        category.text = app.category
        desc.text = app.description

        Glide.with(this)
            .load(app.iconUrl)
            .placeholder(R.drawable.ic_app_placeholder)
            .error(R.drawable.ic_app_placeholder)
            .into(image)

        updateUi()
    }

    private fun formatSize(sizeInBytes: Long?): String {
        if (sizeInBytes == null || sizeInBytes <= 0) return "N/A"

        val df = DecimalFormat("#.##")
        val sizeInMB = sizeInBytes / (1024.0 * 1024.0)

        return if (sizeInMB < 1024) {
            "${df.format(sizeInMB)}MB"
        } else {
            "${df.format(sizeInMB / 1024.0)}GB"
        }
    }

    private fun setupButtons() {
        installButton.setOnClickListener {
            when {
                !isAppInstalled(app.packageName) -> startDownloadAndInstall()
                isUpdateAvailable() -> startDownloadAndInstall()
                else -> openApp(app.packageName)
            }
        }

        backButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    // ───────────────────────
    // UI
    // ───────────────────────
    private fun updateUi() {
        if (!isAdded) return

        requireActivity().runOnUiThread {
            progress.visibility = View.GONE
            status.text = ""

            when (app.status) {
                AppStatus.DOWNLOADING -> {
                    progress.visibility = View.VISIBLE
                    progress.isIndeterminate = false
                    progress.progress = app.progress
                    status.text = "Загрузка… ${app.progress}%"
                    installButton.isEnabled = false
                }

                AppStatus.INSTALLING -> {
                    progress.visibility = View.VISIBLE
                    progress.isIndeterminate = true
                    status.text = "Установка…"
                    installButton.isEnabled = false
                }

                AppStatus.ERROR -> {
                    status.text = "Ошибка загрузки"
                    installButton.isEnabled = true
                }

                else -> {
                    installButton.isEnabled = true
                }
            }

            installButton.text = when {
                !isAppInstalled(app.packageName) -> "Установить"
                isUpdateAvailable() -> "Обновить"
                else -> "Открыть"
            }
        }
    }

    // ───────────────────────
    // INSTALL / UPDATE
    // ───────────────────────
    private fun startDownloadAndInstall() {
        ApkDownloader.download(
            context = requireContext(),
            app = app,
            onProgress = {
                app.status = AppStatus.DOWNLOADING
                app.progress = it
                updateUi()
            },
            onDone = { file ->
                app.status = AppStatus.INSTALLING
                updateUi()
                ApkInstaller.install(requireContext(), file)
            },
            onError = {
                app.status = AppStatus.ERROR
                updateUi()
            }
        )
    }

    // ───────────────────────
    // VERSION LOGIC (STRING)
    // ───────────────────────
    private fun getInstalledVersionName(pkg: String): String? =
        try {
            requireContext()
                .packageManager
                .getPackageInfo(pkg, 0)
                .versionName
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }

    private fun isUpdateAvailable(): Boolean {
        val installedVersion = getInstalledVersionName(app.packageName)
            ?: return false

        return isServerVersionNewer(
            server = app.version,
            installed = installedVersion
        )
    }

    private fun isServerVersionNewer(
        server: String?,
        installed: String?
    ): Boolean {
        if (server.isNullOrBlank() || installed.isNullOrBlank()) return false

        val serverParts = server.split(".").map { it.toIntOrNull() ?: 0 }
        val installedParts = installed.split(".").map { it.toIntOrNull() ?: 0 }

        val maxSize = maxOf(serverParts.size, installedParts.size)

        for (i in 0 until maxSize) {
            val s = serverParts.getOrElse(i) { 0 }
            val iV = installedParts.getOrElse(i) { 0 }

            if (s > iV) return true
            if (s < iV) return false
        }
        return false
    }

    private fun isAppInstalled(pkg: String): Boolean =
        getInstalledVersionName(pkg) != null

    private fun openApp(pkg: String) {
        requireContext()
            .packageManager
            .getLaunchIntentForPackage(pkg)
            ?.let { startActivity(it) }
            ?: Toast.makeText(
                requireContext(),
                "Не удалось открыть приложение",
                Toast.LENGTH_SHORT
            ).show()
    }

    companion object {
        private const val ARG_APP = "app"

        fun newInstance(app: AppItem) =
            AppDetailsFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_APP, app)
                }
            }
    }
}