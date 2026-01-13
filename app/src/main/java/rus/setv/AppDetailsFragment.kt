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

class AppDetailsFragment : Fragment(R.layout.lb_app_details) {

    private lateinit var app: AppItem

    private lateinit var image: ImageView
    private lateinit var title: TextView
    private lateinit var desc: TextView
    private lateinit var status: TextView
    private lateinit var progress: LinearProgressIndicator
    private lateinit var installButton: MaterialButton
    private lateinit var backButton: MaterialButton

    // 🔥 RECEIVER УСТАНОВКИ
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
        desc = view.findViewById(R.id.appDescription)
        status = view.findViewById(R.id.statusText)
        progress = view.findViewById(R.id.progress)
        installButton = view.findViewById(R.id.installButton)
        backButton = view.findViewById(R.id.backButton)

        bindApp()
        setupButtons()
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
        desc.text = app.description

        Glide.with(this)
            .load(app.iconUrl)
            .placeholder(R.drawable.ic_app_placeholder)
            .error(R.drawable.ic_app_placeholder)
            .into(image)

        if (isAppInstalled(app.packageName)) {
            app.status = AppStatus.INSTALLED
        }

        updateUi()
    }

    private fun setupButtons() {
        installButton.setOnClickListener {
            if (isAppInstalled(app.packageName)) {
                openApp(app.packageName)
            } else {
                startDownloadAndInstall()
            }
        }

        backButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    /**
     * 🔥 ВСЕ UI ИЗМЕНЕНИЯ ТОЛЬКО В MAIN THREAD
     */
    private fun updateUi() {
        if (!isAdded) return

        requireActivity().runOnUiThread {
            progress.visibility = View.GONE
            progress.isIndeterminate = false
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

                AppStatus.INSTALLED -> {
                    progress.visibility = View.GONE
                    status.text = "Приложение установлено"
                    installButton.isEnabled = true
                }

                AppStatus.ERROR -> {
                    progress.visibility = View.GONE
                    status.text = "Ошибка загрузки"
                    installButton.isEnabled = true
                }

                else -> {
                    installButton.isEnabled = true
                }
            }

            installButton.text =
                if (isAppInstalled(app.packageName)) "Открыть" else "Установить"
        }
    }

    private fun startDownloadAndInstall() {
        Toast.makeText(
            requireContext(),
            "Загрузка ${app.name}",
            Toast.LENGTH_SHORT
        ).show()

        ApkDownloader.download(
            context = requireContext(),
            app = app,

            onProgress = { progress ->
                app.status = AppStatus.DOWNLOADING
                app.progress = progress
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

    private fun isAppInstalled(pkg: String): Boolean =
        try {
            requireContext().packageManager.getPackageInfo(pkg, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }

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
