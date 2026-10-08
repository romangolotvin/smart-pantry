package com.smartpantry.app.update

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

class UpdateService(
    private val context: Context,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()
) {
    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    var latest: UpdateManifest? = null
        private set

    fun currentVersion(): String = UpdateConfig.APP_VERSION

    suspend fun checkForUpdates(silent: Boolean = false) {
        if (_state.value is UpdateState.Checking || _state.value is UpdateState.Downloading) return
        if (!silent) _state.value = UpdateState.Checking
        try {
            val manifest = fetchManifest()
            latest = manifest
            if (isNewer(manifest.version, currentVersion())) {
                _state.value = UpdateState.Available(manifest)
            } else {
                _state.value = UpdateState.UpToDate
            }
        } catch (e: Exception) {
            _state.value = UpdateState.Error(e.message ?: "Не удалось проверить обновления.")
        }
    }

    suspend fun downloadAndInstall(activity: Activity) {
        val manifest = latest
        if (manifest == null || manifest.androidUrl.isBlank()) {
            _state.value = UpdateState.Error("Нет ссылки на APK.")
            return
        }
        if (!ensureInstallPermission(activity)) {
            _state.value = UpdateState.Error(
                "Разреши установку из «Умный холодильник» в настройках Android и нажми «Обновить» снова."
            )
            return
        }

        _state.value = UpdateState.Downloading(0, 0)
        try {
            val apk = withContext(Dispatchers.IO) { downloadApk(manifest.androidUrl) }
            _state.value = UpdateState.ReadyToInstall
            installApk(activity, apk)
        } catch (e: Exception) {
            _state.value = UpdateState.Error(e.message ?: "Ошибка загрузки обновления.")
        }
    }

    fun dismissStatus() {
        when (_state.value) {
            is UpdateState.UpToDate, is UpdateState.Error, is UpdateState.ReadyToInstall -> {
                _state.value = UpdateState.Idle
            }
            else -> Unit
        }
    }

    private suspend fun fetchManifest(): UpdateManifest = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(UpdateConfig.MANIFEST_URL)
            .header("User-Agent", "SmartPantry/${UpdateConfig.APP_VERSION}")
            .get()
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException(manifestError(response.code))
            }
            val body = response.body?.string().orEmpty()
            val json = JSONObject(body)
            val version = json.optString("version").trim()
            if (version.isEmpty()) {
                throw IllegalStateException("Неверный файл обновлений.")
            }
            UpdateManifest(
                version = version,
                notes = json.optString("notes").trim(),
                androidUrl = json.optString("android").trim()
            )
        }
    }

    private fun downloadApk(url: String): File {
        val dir = File(context.filesDir, "updates").apply { mkdirs() }
        val target = File(dir, "SmartPantry.apk")
        if (target.exists()) target.delete()

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "SmartPantry/${UpdateConfig.APP_VERSION}")
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("Ошибка загрузки обновления (${response.code}).")
            }
            val body = response.body ?: throw IllegalStateException("Пустой ответ сервера.")
            val total = body.contentLength().coerceAtLeast(0L)
            body.byteStream().use { input ->
                target.outputStream().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var loaded = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        loaded += read
                        _state.value = UpdateState.Downloading(loaded, total)
                    }
                }
            }
        }
        if (!target.exists() || target.length() < 1024) {
            throw IllegalStateException("Файл обновления повреждён.")
        }
        return target
    }

    private fun ensureInstallPermission(activity: Activity): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return true
        val pm = activity.packageManager
        if (pm.canRequestPackageInstalls()) return true
        val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
            data = Uri.parse("package:${activity.packageName}")
        }
        activity.startActivity(intent)
        return false
    }

    private fun installApk(activity: Activity, apk: File) {
        val uri = FileProvider.getUriForFile(
            activity,
            "${activity.packageName}.fileprovider",
            apk
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        activity.startActivity(intent)
    }

    fun isNewer(remote: String, local: String): Boolean {
        val a = parseVersion(remote)
        val b = parseVersion(local)
        for (i in 0 until 3) {
            if (a[i] > b[i]) return true
            if (a[i] < b[i]) return false
        }
        return false
    }

    private fun parseVersion(text: String): IntArray {
        val parts = text.trim().split('.')
        return IntArray(3) { i -> parts.getOrNull(i)?.toIntOrNull() ?: 0 }
    }

    private fun manifestError(code: Int): String = when (code) {
        404 -> "Файл обновлений ещё не выложен в интернет (404)."
        403 -> "Доступ к серверу обновлений запрещён (403)."
        else -> "Сервер обновлений ответил ошибкой ($code)."
    }
}
