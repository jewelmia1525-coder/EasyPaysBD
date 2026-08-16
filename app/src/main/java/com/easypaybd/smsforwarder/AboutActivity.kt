package com.easypaybd.smsforwarder

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import org.json.JSONObject

/**
 * About screen — content is controlled remotely from the PaysBD website
 * (Admin > SMS Forwarder > App Content). Any change there shows up here instantly.
 */
class AboutActivity : AppCompatActivity() {
    private val ui = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        setContentView(R.layout.activity_about)
        val prefs = Prefs(this)

        val logo = findViewById<ImageView>(R.id.aboutLogo)
        logo.setImageResource(R.drawable.paysbd_logo)
        logo.alpha = 0f
        logo.scaleX = 0.8f; logo.scaleY = 0.8f
        logo.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(700).start()

        val title = findViewById<TextView>(R.id.aboutTitle)
        val body = findViewById<TextView>(R.id.aboutBody)
        val ann = findViewById<TextView>(R.id.aboutAnnouncement)
        val feats = findViewById<TextView>(R.id.aboutFeatures)
        val ver = findViewById<TextView>(R.id.aboutVersion)

        ui.launch {
            val text = withContext(Dispatchers.IO) {
                runCatching {
                    Api.get(prefs.baseUrl + Config.EP_ABOUT, Api.authHeaders(prefs), prefs.ignoreSsl)
                        .use { it.body?.string() ?: "" }
                }.getOrElse { "" }
            }
            if (text.isBlank()) { body.text = "About লোড করা যায়নি — ইন্টারনেট চেক করুন।"; return@launch }
            val o = runCatching { JSONObject(text) }.getOrNull() ?: return@launch
            val about = o.optJSONObject("about") ?: JSONObject()
            title.text = about.optString("title", "PaysBD SMS Forward")
            body.text = about.optString("body", "")

            val a = o.optJSONObject("announcement") ?: JSONObject()
            if (a.optBoolean("enabled")) {
                ann.visibility = View.VISIBLE
                ann.text = "📢 ${a.optString("title")}\n${a.optString("message")}"
            } else ann.visibility = View.GONE

            val f = o.optJSONObject("features") ?: JSONObject()
            prefs.featuresJson = f.toString()
            feats.text = f.keys().asSequence()
                .joinToString("\n") { k -> (if (f.optBoolean(k)) "✅ " else "⬜ ") + k.replace('_', ' ') }

            val rel = o.optJSONObject("latest_release")
            ver.text = if (rel != null)
                "Latest: v${rel.optString("version")} (build ${rel.optInt("build_number")})\n${rel.optString("changelog")}"
            else "Version info নেই"
        }
    }

    override fun onDestroy() { ui.cancel(); super.onDestroy() }
}
