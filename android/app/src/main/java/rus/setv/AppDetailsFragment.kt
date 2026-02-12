package rus.setv

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import okhttp3.OkHttpClient
import okhttp3.Request
import rus.setv.apk.ApkDownloader
import rus.setv.apk.ApkInstaller
import rus.setv.model.AppItem
import rus.setv.model.AppStatus
import rus.setv.adapter.ScreenshotsAdapter
import java.text.DecimalFormat
import kotlin.concurrent.thread

class AppDetailsFragment : Fragment(R.layout.fragment_app_details) {

    private lateinit var app: AppItem

    private lateinit var image: ImageView
    private lateinit var title: TextView
    private lateinit var version: TextView
    private lateinit var size: TextView
    private lateinit var desc: TextView
    private lateinit var status: TextView
    private lateinit var progress: LinearProgressIndicator
    private lateinit var installButton: MaterialButton
    private lateinit var uninstallButton: MaterialButton
    private lateinit var backButton: MaterialButton

    // 🆕 screenshots
    private lateinit var screenshotsList: RecyclerView
    private lateinit var screenshotsAdapter: ScreenshotsAdapter
    private var lastOpenedScreenshotPosition: Int = -1

    private val httpClient = OkHttpClient()

    private val isSelfApp: Boolean
        get() = app.packageName == requireContext().packageName

    // ───────────────────────
    // RECEIVER
    // ───────────────────────
    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val pkg = intent.data?.schemeSpecificPart ?: return
            if (pkg != app.packageName) return

            when (intent.action) {
                Intent.ACTION_PACKAGE_ADDED -> {
                    app.status = AppStatus.INSTALLED
                    Toast.makeText(context, "Приложение установлено", Toast.LENGTH_SHORT).show()
                }
                Intent.ACTION_PACKAGE_REMOVED -> {
                    app.status = AppStatus.NOT_INSTALLED
                    Toast.makeText(context, "Приложение удалено", Toast.LENGTH_SHORT).show()
                }
            }
            updateUi()
        }
    }

    // ───────────────────────
    // LIFECYCLE
    // ───────────────────────
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        app = requireArguments().getParcelable(ARG_APP)!!

        image = view.findViewById(R.id.appImage)
        title = view.findViewById(R.id.appTitle)
        version = view.findViewById(R.id.appVersion)
        size = view.findViewById(R.id.appSize)
        desc = view.findViewById(R.id.appDescription)
        status = view.findViewById(R.id.statusText)
        progress = view.findViewById(R.id.progress)
        installButton = view.findViewById(R.id.installButton)
        uninstallButton = view.findViewById(R.id.uninstallButton)
        backButton = view.findViewById(R.id.backButton)

        // 🆕 screenshots
        screenshotsList = view.findViewById(R.id.screenshotsList)
        screenshotsAdapter = ScreenshotsAdapter { position ->
            openScreenshotViewer(position)
        }

        screenshotsList.apply {
            layoutManager = LinearLayoutManager(
                requireContext(),
                LinearLayoutManager.HORIZONTAL,
                false
            )
            adapter = screenshotsAdapter
            setHasFixedSize(true)
        }

        installButton.isFocusable = true
        installButton.isFocusableInTouchMode = true

        bindApp()
        setupButtons()
    }

    override fun onStart() {
        super.onStart()
        requireContext().registerReceiver(
            packageReceiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_PACKAGE_ADDED)
                addAction(Intent.ACTION_PACKAGE_REMOVED)
                addDataScheme("package")
            }
        )
    }

    override fun onStop() {
        super.onStop()
        requireContext().unregisterReceiver(packageReceiver)
    }

    override fun onResume() {
        super.onResume()

        // If returning from screenshot viewer, restore focus to that screenshot
        if (lastOpenedScreenshotPosition >= 0) {
            val position = lastOpenedScreenshotPosition
            lastOpenedScreenshotPosition = -1

            screenshotsList.post {
                val viewHolder = screenshotsList.findViewHolderForAdapterPosition(position)
                viewHolder?.itemView?.requestFocus() ?: run {
                    screenshotsList.scrollToPosition(position)
                    screenshotsList.postDelayed({
                        screenshotsList.findViewHolderForAdapterPosition(position)?.itemView?.requestFocus()
                    }, 100)
                }
            }
        } else {
            // Normal resume - focus back button
            backButton.post { backButton.requestFocus() }
        }
    }

    // ───────────────────────
    // BIND
    // ───────────────────────
    private fun bindApp() {
        title.text = app.name
        version.text = app.version ?: "N/A"
        desc.text = app.description

        Glide.with(this)
            .load(app.iconUrl)
            .placeholder(R.drawable.ic_app_placeholder)
            .error(R.drawable.ic_app_placeholder)
            .into(image)

        if (app.images.isNotEmpty()) {
            screenshotsAdapter.submitList(app.images)
            screenshotsList.visibility = View.VISIBLE
        } else {
            screenshotsList.visibility = View.GONE
        }

        if (app.apkSizeBytes > 0) {
            size.text = formatSize(app.apkSizeBytes)
        } else {
            size.text = "—"
            fetchApkSize()
        }

        updateUi()
    }

    // ───────────────────────
    // APK SIZE
    // ───────────────────────
    private fun fetchApkSize() {
        val url = app.apkUrl
        thread {
            val bytes = getApkSize(url)
            if (bytes > 0 && isAdded) {
                app.apkSizeBytes = bytes
                requireActivity().runOnUiThread {
                    size.text = formatSize(bytes)
                }
            }
        }
    }

    private fun getApkSize(url: String): Long {
        try {
            val head = Request.Builder().url(url).head().build()
            httpClient.newCall(head).execute().use {
                it.header("Content-Length")?.toLongOrNull()?.let { return it }
            }
        } catch (_: Exception) {}

        try {
            val range = Request.Builder()
                .url(url)
                .addHeader("Range", "bytes=0-0")
                .build()

            httpClient.newCall(range).execute().use {
                return it.header("Content-Range")
                    ?.substringAfter("/")
                    ?.toLongOrNull() ?: 0L
            }
        } catch (_: Exception) {}

        return 0L
    }

    private fun formatSize(bytes: Long): String {
        val df = DecimalFormat("#.#")
        val mb = bytes / (1024.0 * 1024.0)
        return if (mb < 1024) "${df.format(mb)} MB"
        else "${df.format(mb / 1024)} GB"
    }

    // ───────────────────────
    // UI
    // ───────────────────────
    private fun updateUi() {
        if (!isAdded) return

        progress.visibility = View.GONE
        status.text = ""

        val installed = isAppInstalled(app.packageName)
        val hasUpdate = isUpdateAvailable()

        when (app.status) {
            AppStatus.DOWNLOADING -> {
                progress.visibility = View.VISIBLE
                progress.isIndeterminate = false
                progress.progress = app.progress
                status.text = "Загрузка… ${app.progress}%"
                installButton.isEnabled = false
                uninstallButton.visibility = View.GONE
            }

            AppStatus.INSTALLING -> {
                progress.visibility = View.VISIBLE
                progress.isIndeterminate = true
                status.text = "Установка…"
                installButton.isEnabled = false
                uninstallButton.visibility = View.GONE
            }

            AppStatus.ERROR -> {
                status.text = "Ошибка загрузки"
                installButton.isEnabled = true
                uninstallButton.visibility =
                    if (installed && !isSelfApp) View.VISIBLE else View.GONE
            }

            else -> {
                installButton.isEnabled = true
                uninstallButton.visibility =
                    if (installed && !isSelfApp) View.VISIBLE else View.GONE
            }
        }

        if (isSelfApp) {
            uninstallButton.visibility = View.GONE
            installButton.visibility = if (hasUpdate) View.VISIBLE else View.GONE
            installButton.text = "Обновить"
            return
        }

        installButton.visibility = View.VISIBLE
        installButton.text = when {
            !installed -> "Установить"
            hasUpdate -> "Обновить"
            else -> "Открыть"
        }
    }

    // ───────────────────────
    // BUTTONS
    // ───────────────────────
    private fun setupButtons() {
        // Add LEFT key handler only to backButton (leftmost button)
        backButton.setOnKeyListener { _, keyCode, event ->
            if (keyCode == android.view.KeyEvent.KEYCODE_DPAD_LEFT &&
                event.action == android.view.KeyEvent.ACTION_DOWN) {
                openSidebarAndFocus()
                true
            } else {
                false
            }
        }

        installButton.setOnClickListener {
            when {
                !isAppInstalled(app.packageName) -> startDownloadAndInstall()
                isUpdateAvailable() -> startDownloadAndInstall()
                else -> openApp(app.packageName)
            }
        }

        uninstallButton.setOnClickListener {
            uninstallApp(app.packageName)
        }

        backButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun openSidebarAndFocus() {
        val mainActivity = activity as? MainActivity
        mainActivity?.openSidebar()

        view?.postDelayed({
            val sidebar = requireActivity().findViewById<View>(R.id.sidebar_container)
            val searchItem = sidebar?.findViewById<View>(R.id.menu_search)
            searchItem?.requestFocus()
        }, 50)
    }

    // ───────────────────────
    // INSTALL
    // ───────────────────────
    private fun startDownloadAndInstall() {
        ApkDownloader.download(
            context = requireContext(),
            app = app,
            onProgress = {
                runOnUi {
                    app.status = AppStatus.DOWNLOADING
                    app.progress = it
                    updateUi()
                }
            },
            onDone = { file ->
                runOnUi {
                    app.status = AppStatus.INSTALLING
                    updateUi()
                }
                ApkInstaller.install(requireContext(), file)
            },
            onError = {
                runOnUi {
                    app.status = AppStatus.ERROR
                    updateUi()
                }
            }
        )
    }

    private fun runOnUi(block: () -> Unit) {
        if (!isAdded) return
        requireActivity().runOnUiThread(block)
    }

    private fun uninstallApp(pkg: String) {
        startActivity(
            Intent(Intent.ACTION_UNINSTALL_PACKAGE).apply {
                data = Uri.parse("package:$pkg")
                putExtra(Intent.EXTRA_RETURN_RESULT, true)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }

    // ───────────────────────
    // VERSION
    // ───────────────────────
    private fun getInstalledVersionName(pkg: String): String? =
        try {
            requireContext().packageManager.getPackageInfo(pkg, 0).versionName
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }

    private fun isUpdateAvailable(): Boolean {
        val installed = getInstalledVersionName(app.packageName) ?: return false
        return isServerVersionNewer(app.version, installed)
    }

    private fun isServerVersionNewer(server: String?, installed: String?): Boolean {
        if (server.isNullOrBlank() || installed.isNullOrBlank()) return false
        val s = normalizeVersion(server)
        val i = normalizeVersion(installed)
        for (idx in 0 until maxOf(s.size, i.size)) {
            if (s.getOrElse(idx) { 0 } > i.getOrElse(idx) { 0 }) return true
            if (s.getOrElse(idx) { 0 } < i.getOrElse(idx) { 0 }) return false
        }
        return false
    }

    private fun normalizeVersion(v: String): List<Int> =
        v.replace(Regex("[^0-9.]"), "")
            .split(".")
            .filter { it.isNotBlank() }
            .map { it.toIntOrNull() ?: 0 }

    private fun isAppInstalled(pkg: String) =
        getInstalledVersionName(pkg) != null

    private fun openApp(pkg: String) {
        requireContext().packageManager
            .getLaunchIntentForPackage(pkg)
            ?.let { startActivity(it) }
            ?: Toast.makeText(requireContext(), "Не удалось открыть приложение", Toast.LENGTH_SHORT).show()
    }

    private fun openScreenshotViewer(startPosition: Int) {
        lastOpenedScreenshotPosition = startPosition

        parentFragmentManager.beginTransaction()
            .replace(
                R.id.main_container,
                ScreenshotViewerFragment.newInstance(
                    images = app.images.map { it.imageUrl },
                    startPosition = startPosition
                )
            )
            .addToBackStack(null)
            .commit()
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
