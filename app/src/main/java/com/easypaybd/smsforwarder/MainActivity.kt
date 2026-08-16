package com.easypaybd.smsforwarder

import android.Manifest
import android.animation.ObjectAnimator
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import coil.load
import kotlinx.coroutines.*
import org.json.JSONObject

class MainActivity : AppCompatActivity() {
    private lateinit var prefs: Prefs
    private val ui = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var importDone: (() -> Unit)? = null
    private var pendingExport: String? = null

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        setContentView(R.layout.activity_main)
        prefs = Prefs(this)
        requestPerms()

        val logo = findViewById<ImageView>(R.id.headerLogo)
        logo.setImageResource(R.drawable.paysbd_logo)
        logo.alpha = 0f
        logo.animate().alpha(1f).setDuration(600).start()

        val deviceIdView = findViewById<TextView>(R.id.deviceIdView)
        val deviceSecretView = findViewById<TextView>(R.id.deviceSecretView)
        fun renderIds() {
            deviceIdView.text = "Device ID: ${prefs.deviceId}"
            deviceSecretView.text = "Server: ${prefs.baseUrl}"
        }
        renderIds()

        val name = findViewById<EditText>(R.id.name).apply { setText(prefs.name) }
        val desc = findViewById<EditText>(R.id.description).apply { setText(prefs.description) }
        val sender = findViewById<EditText>(R.id.sender).apply { setText(prefs.senderFilter) }
        val webhook = findViewById<EditText>(R.id.webhook).apply { setText(prefs.webhookUrl) }
        val simSpin = findViewById<Spinner>(R.id.simSlot)
        simSpin.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, listOf("any", "0", "1", "2"))
        val tpl = findViewById<EditText>(R.id.template).apply { setText(prefs.jsonTemplate) }
        val headers = findViewById<EditText>(R.id.headers).apply { setText(prefs.headers) }
        val retries = findViewById<EditText>(R.id.retries).apply { setText(prefs.retries.toString()) }
        val ignoreSsl = findViewById<CheckBox>(R.id.ignoreSsl).apply { isChecked = prefs.ignoreSsl }
        val chunked = findViewById<CheckBox>(R.id.chunked).apply { isChecked = prefs.chunked }
        val status = findViewById<TextView>(R.id.status)
        val runBtn = findViewById<ImageView>(R.id.runBtn)

        fun reloadForm() {
            name.setText(prefs.name); sender.setText(prefs.senderFilter); webhook.setText(prefs.webhookUrl)
            tpl.setText(prefs.jsonTemplate); headers.setText(prefs.headers); retries.setText(prefs.retries.toString())
            ignoreSsl.isChecked = prefs.ignoreSsl; chunked.isChecked = prefs.chunked
            renderIds()
        }

        findViewById<Button>(R.id.save).setOnClickListener {
            prefs.name = name.text.toString()
            prefs.description = desc.text.toString()
            prefs.senderFilter = sender.text.toString().ifBlank { "*" }
            prefs.webhookUrl = webhook.text.toString()
            prefs.simSlot = simSpin.selectedItem?.toString() ?: "any"
            prefs.jsonTemplate = tpl.text.toString()
            prefs.headers = headers.text.toString()
            prefs.retries = retries.text.toString().toIntOrNull() ?: 10
            prefs.ignoreSsl = ignoreSsl.isChecked
            prefs.chunked = chunked.isChecked
            toast("Saved")
            ForwarderService.start(this)
        }
        findViewById<Button>(R.id.cancel).setOnClickListener { recreate() }

        // ---- Import config (JSON bundle or pairing code from the website) ----
        findViewById<Button>(R.id.importBtn).setOnClickListener { showImportDialog(::reloadForm) }

        // ---- Export config / HTTP shortcut ----
        findViewById<Button>(R.id.exportBtn).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Export")
                .setItems(
                    arrayOf("Save config as file", "Save HTTP Shortcut as file", "Share config JSON", "Copy config JSON"),
                ) { _, which ->
                    val cfg = ConfigBundle.export(prefs).toString(2)
                    when (which) {
                        0 -> saveToFile("easypaybd-device-${prefs.deviceId}.json", cfg)
                        1 -> saveToFile("heartbeat-shortcut-${prefs.deviceId}.json", ConfigBundle.httpShortcut(prefs).toString(2))
                        2 -> share(cfg)
                        else -> copy(cfg)
                    }
                }.show()
        }

        findViewById<Button>(R.id.aboutBtn).setOnClickListener { startActivity(Intent(this, AboutActivity::class.java)) }

        // ---- Heartbeat shortcut (top-right) ----
        findViewById<ImageButton>(R.id.heartBtn).setOnClickListener {
            status.text = "Heartbeat…"
            ui.launch {
                val code = withContext(Dispatchers.IO) { runCatching { Api.heartbeat(prefs).use { it.code } }.getOrElse { -1 } }
                status.text = if (code in 200..299) "✔ Heartbeat OK" else "⚠ Heartbeat error $code"
            }
        }

        runBtn.setOnClickListener {
            val anim = ObjectAnimator.ofFloat(runBtn, "rotation", 0f, 360f).apply {
                duration = 1200; interpolator = LinearInterpolator(); repeatCount = ObjectAnimator.INFINITE; start()
            }
            status.text = "Connecting..."
            ui.launch {
                val code = withContext(Dispatchers.IO) {
                    runCatching {
                        val extra = JSONObject()
                            .put("app_version", BuildConfigCompat.versionName(this@MainActivity))
                            .put("device_model", Build.MODEL)
                            .put("android_version", Build.VERSION.RELEASE)
                        Api.heartbeat(prefs, extra).use { it.code }
                    }.getOrElse { -1 }
                }
                anim.cancel(); runBtn.rotation = 0f
                status.text = when (code) {
                    in 200..299 -> { ForwarderService.start(this@MainActivity); "✔ Connected ($code) — forwarding চালু" }
                    401 -> "🔒 Auth failed (401) — Import config দিয়ে device যুক্ত করুন"
                    403 -> "⛔ Device paused/revoked (403)"
                    402 -> "💳 Payment required (402)"
                    -1 -> "✖ Network error"
                    else -> "⚠ Error $code"
                }
            }
        }
    }

    private fun showImportDialog(onDone: () -> Unit) {
        importDone = onDone
        val pad = (16 * resources.displayMetrics.density).toInt()
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(pad, pad, pad, 0) }
        val url = EditText(this).apply { hint = "Server URL"; setText(prefs.baseUrl) }
        val input = EditText(this).apply { hint = "Pairing code (8 digit) অথবা পুরো Config JSON পেস্ট করুন"; minLines = 4 }
        val hint = TextView(this).apply {
            text = "ওয়েবসাইট থেকে ডাউনলোড করা .json ফাইল থাকলে নিচের “Select File” চাপুন — সব সেটিং অটো সেট হবে।"
            textSize = 12f
        }
        box.addView(url); box.addView(input); box.addView(hint)

        AlertDialog.Builder(this)
            .setTitle("Import from website")
            .setView(box)
            .setPositiveButton("Import") { _, _ ->
                prefs.baseUrl = url.text.toString().ifBlank { Config.BASE_URL }
                val raw = input.text.toString().trim()
                if (raw.isBlank()) { toast("Pairing code বা JSON দিন, অথবা Select File চাপুন"); return@setPositiveButton }
                ui.launch {
                    val result = withContext(Dispatchers.IO) {
                        runCatching {
                            if (raw.startsWith("{")) ConfigBundle.applyJson(prefs, raw)
                            else ConfigBundle.enroll(prefs, raw, deviceInfo())
                        }
                    }
                    result.onSuccess { toast("Imported: $it"); onDone(); ForwarderService.start(this@MainActivity) }
                    result.onFailure { toast(it.message ?: "Import failed") }
                }
            }
            .setNeutralButton("Select File") { _, _ ->
                prefs.baseUrl = url.text.toString().ifBlank { Config.BASE_URL }
                runCatching { pickConfigFile.launch(arrayOf("application/json", "text/plain", "*/*")) }
                    .onFailure { toast("File picker খোলা যায়নি") }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /** Website থেকে ডাউনলোড করা config .json সিলেক্ট করলে সব সেটিং অটো বসে যায়। */
    private val pickConfigFile = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@registerForActivityResult
        ui.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val text = contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                        ?: throw IllegalStateException("ফাইল পড়া যায়নি")
                    val body = text.trim()
                    if (body.startsWith("{")) ConfigBundle.applyJson(prefs, body)
                    else ConfigBundle.enroll(prefs, body, deviceInfo())
                }
            }
            result.onSuccess {
                toast("✔ Config import হয়েছে: $it")
                importDone?.invoke()
                ForwarderService.start(this@MainActivity)
                runCatching {
                    val code = withContext(Dispatchers.IO) { Api.heartbeat(prefs).use { r -> r.code } }
                    findViewById<TextView>(R.id.status).text =
                        if (code in 200..299) "✔ Connected ($code) — device সার্ভারে যুক্ত" else "⚠ Heartbeat $code"
                }
            }
            result.onFailure { toast(it.message ?: "Import failed") }
        }
    }

    private fun deviceInfo(): JSONObject = JSONObject()
        .put("device_model", Build.MODEL)
        .put("android_version", Build.VERSION.RELEASE)
        .put("app_version", BuildConfigCompat.versionName(this))

    /** Export করা JSON ফোনে ফাইল হিসেবে সেভ করার জন্য। */
    private val saveConfigFile = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
        val text = pendingExport ?: return@registerForActivityResult
        pendingExport = null
        if (uri == null) return@registerForActivityResult
        runCatching { contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray()) } }
            .onSuccess { toast("ফাইল সেভ হয়েছে") }
            .onFailure { toast("সেভ করা যায়নি") }
    }

    private fun saveToFile(name: String, text: String) {
        pendingExport = text
        runCatching { saveConfigFile.launch(name) }.onFailure { toast("File picker খোলা যায়নি") }
    }


    private fun copy(text: String) {
        (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("easypaybd", text))
        toast("Copied")
    }

    private fun share(text: String) {
        startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = "application/json"; putExtra(Intent.EXTRA_TEXT, text)
        }, "Export"))
    }

    private fun toast(m: String) = Toast.makeText(this, m, Toast.LENGTH_SHORT).show()

    private fun requestPerms() {
        val need = mutableListOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS, Manifest.permission.READ_PHONE_STATE)
        if (Build.VERSION.SDK_INT >= 33) need += Manifest.permission.POST_NOTIFICATIONS
        val missing = need.filter { ActivityCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }
        if (missing.isNotEmpty()) ActivityCompat.requestPermissions(this, missing.toTypedArray(), 1)
    }

    override fun onDestroy() { ui.cancel(); super.onDestroy() }
}

object BuildConfigCompat {
    fun versionName(ctx: android.content.Context): String = runCatching {
        ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: "1.0.0"
    }.getOrDefault("1.0.0")
}
