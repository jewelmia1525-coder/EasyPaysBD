package com.easypaybd.smsforwarder

import android.Manifest
import android.animation.ObjectAnimator
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import coil.load
import kotlinx.coroutines.*
import org.json.JSONObject

class MainActivity : AppCompatActivity() {
    private lateinit var prefs: Prefs
    private val ui = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        setContentView(R.layout.activity_main)
        prefs = Prefs(this)
        requestPerms()

        findViewById<ImageView>(R.id.headerLogo).load(Config.LOGO_URL)
        findViewById<TextView>(R.id.deviceIdView).text = "Device ID: ${prefs.deviceId}"
        findViewById<TextView>(R.id.deviceSecretView).text = "Secret: ${prefs.deviceSecret}"

        val name = findViewById<EditText>(R.id.name).apply { setText(prefs.name) }
        val desc = findViewById<EditText>(R.id.description).apply { setText(prefs.description) }
        val sender = findViewById<EditText>(R.id.sender).apply { setText(prefs.senderFilter) }
        val webhook = findViewById<EditText>(R.id.webhook).apply { setText(prefs.webhookUrl) }
        val simSpin = findViewById<Spinner>(R.id.simSlot)
        simSpin.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item,
            listOf("any","0","1","2"))
        val tpl = findViewById<EditText>(R.id.template).apply { setText(prefs.jsonTemplate) }
        val headers = findViewById<EditText>(R.id.headers).apply { setText(prefs.headers) }
        val retries = findViewById<EditText>(R.id.retries).apply { setText(prefs.retries.toString()) }
        val ignoreSsl = findViewById<CheckBox>(R.id.ignoreSsl).apply { isChecked = prefs.ignoreSsl }
        val chunked = findViewById<CheckBox>(R.id.chunked).apply { isChecked = prefs.chunked }
        val status = findViewById<TextView>(R.id.status)
        val runBtn = findViewById<ImageView>(R.id.runBtn)

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
            Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show()
            ForwarderService.start(this)
        }
        findViewById<Button>(R.id.cancel).setOnClickListener { recreate() }

        findViewById<ImageButton>(R.id.heartBtn).setOnClickListener {
            ui.launch { val code = withContext(Dispatchers.IO) { runCatching { Api.heartbeat(prefs).use { it.code } }.getOrElse { -1 } }
                status.text = if (code in 200..299) "Heartbeat OK" else "Heartbeat error $code" }
        }

        runBtn.setOnClickListener {
            val anim = ObjectAnimator.ofFloat(runBtn, "rotation", 0f, 360f).apply {
                duration = 1200; interpolator = LinearInterpolator(); repeatCount = ObjectAnimator.INFINITE; start()
            }
            status.text = "Connecting..."
            ui.launch {
                val code = withContext(Dispatchers.IO) {
                    runCatching {
                        val extra = JSONObject().put("app_version", "1.0.0").put("device_model", Build.MODEL)
                        Api.heartbeat(prefs, extra).use { it.code }
                    }.getOrElse { -1 }
                }
                anim.cancel(); runBtn.rotation = 0f
                status.text = when (code) {
                    in 200..299 -> "✔ Connected ($code)"
                    401, 403 -> "🔒 Auth failed ($code) — check device id/secret"
                    402 -> "💳 Payment required (402)"
                    -1 -> "✖ Network error"
                    else -> "⚠ Error $code"
                }
            }
        }
    }

    private fun requestPerms() {
        val need = mutableListOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS, Manifest.permission.READ_PHONE_STATE)
        if (Build.VERSION.SDK_INT >= 33) need += Manifest.permission.POST_NOTIFICATIONS
        val missing = need.filter { ActivityCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }
        if (missing.isNotEmpty()) ActivityCompat.requestPermissions(this, missing.toTypedArray(), 1)
    }
}
