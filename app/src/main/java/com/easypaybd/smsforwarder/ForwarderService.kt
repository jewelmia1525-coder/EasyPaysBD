package com.easypaybd.smsforwarder

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject

class ForwarderService : Service() {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var prefs: Prefs
    private lateinit var queue: SmsQueue

    companion object {
        const val CH_ID = "easypay_fwd"
        fun start(ctx: Context) {
            val i = Intent(ctx, ForwarderService::class.java)
            if (Build.VERSION.SDK_INT >= 26) ctx.startForegroundService(i) else ctx.startService(i)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        prefs = Prefs(this); queue = SmsQueue(this)
        createChannel()
        startForeground(1, notif("EasyPayBD SMS Forwarder", "Running"))
        scope.launch { loop() }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(NotificationChannel(CH_ID, "Forwarder", NotificationManager.IMPORTANCE_LOW))
        }
    }

    private fun notif(t: String, b: String): Notification =
        NotificationCompat.Builder(this, CH_ID)
            .setContentTitle(t).setContentText(b)
            .setSmallIcon(android.R.drawable.stat_sys_upload).setOngoing(true).build()

    private suspend fun loop() {
        var heartTick = 0L; var cmdTick = 0L
        while (scope.isActive) {
            try { flushQueue() } catch (_: Throwable) {}
            val now = System.currentTimeMillis()
            if (now - heartTick > 60_000) { heartTick = now; runCatching { Api.heartbeat(prefs) } }
            if (now - cmdTick > 30_000) { cmdTick = now; runCatching { pollCommands() } }
            delay(5_000)
        }
    }

    private fun flushQueue() {
        val batch = queue.drain(); if (batch.length() == 0) return
        try {
            val res = Api.ingest(prefs, batch)
            if (!res.isSuccessful) { queue.restore(batch); Api.log(prefs, "error", "ingest failed", res.code) }
            res.close()
        } catch (e: Exception) { queue.restore(batch) }
        // also forward to user's webhook if set
        val webhook = prefs.webhookUrl
        if (webhook.isNotEmpty()) forwardWebhook(batch, webhook)
    }

    private fun forwardWebhook(batch: JSONArray, webhook: String) {
        val headers = runCatching {
            val h = JSONObject(prefs.headers); val map = HashMap<String, String>()
            h.keys().forEach { map[it] = h.getString(it) }; map
        }.getOrDefault(mapOf("Content-Type" to "application/json"))
        val tpl = prefs.jsonTemplate
        for (i in 0 until batch.length()) {
            val m = batch.getJSONObject(i)
            val body = tpl
                .replace("{{sender}}", m.optString("sender"))
                .replace("{{body}}", m.optString("body").replace("\"", "\\\""))
                .replace("{{sim}}", m.optInt("sim_slot", -1).toString())
                .replace("{{timestamp}}", m.optLong("timestamp").toString())
                .replace("{{device_id}}", prefs.deviceId)
            var attempt = 0
            while (attempt < prefs.retries) {
                try {
                    val res = Api.postRaw(webhook, body, headers, prefs.ignoreSsl)
                    val code = res.code; res.close()
                    if (code in 200..299) break
                    Api.log(prefs, "warn", "webhook $code", code)
                } catch (e: Exception) { Api.log(prefs, "error", e.message ?: "webhook err") }
                attempt++; Thread.sleep(1500L * attempt)
            }
        }
    }

    private fun pollCommands() {
        val res = Api.pullCommands(prefs); val bodyStr = res.body?.string() ?: "{}"; res.close()
        val arr = JSONObject(bodyStr).optJSONArray("commands") ?: return
        for (i in 0 until arr.length()) {
            val c = arr.getJSONObject(i)
            val id = c.optString("id"); val type = c.optString("type")
            try {
                when (type) {
                    "config_sync"      -> { Api.pullConfig(prefs).close() }
                    "full_scan"        -> SmsScanner.scanAll(this, prefs, queue)
                    "offline_sync"     -> flushQueue()
                    "start_forward"    -> { prefs.forwardingEnabled = true }
                    "stop_forward"     -> { prefs.forwardingEnabled = false }
                    "webhook_test"     -> forwardWebhook(JSONArray().put(JSONObject().put("sender","test").put("body","ping").put("sim_slot",-1).put("timestamp",System.currentTimeMillis())), prefs.webhookUrl)
                    "push_notify"      -> notif("EasyPay", c.optString("message","Notification"))
                    "clear_queue"      -> queue.drain()
                    "restart_service"  -> { stopSelf(); start(this) }
                    "app_update"       -> Api.release(prefs).close()
                }
                Api.ackCommand(prefs, id, "ok").close()
            } catch (e: Exception) {
                Api.ackCommand(prefs, id, "error", e.message).close()
            }
        }
    }

    override fun onDestroy() { scope.cancel(); super.onDestroy() }
}
