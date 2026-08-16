package com.easypaybd.smsforwarder

import okhttp3.Headers
import org.json.JSONObject

/**
 * Import / export of the PaysBD device configuration bundle.
 *
 * Two ways to import from the website:
 *  1) Export Config (JSON)  -> paste the whole JSON into the Import box
 *  2) Pairing Code          -> app calls /api/public/device-enroll and pulls everything itself
 */
object ConfigBundle {

    fun applyJson(prefs: Prefs, raw: String): String {
        val o = JSONObject(raw.trim())
        val deviceId = o.optString("device_id")
        val secret = o.optString("device_secret")
        if (deviceId.isBlank() || secret.isBlank()) throw IllegalArgumentException("device_id / device_secret missing")

        prefs.deviceId = deviceId
        prefs.deviceSecret = secret
        o.optString("base_url").takeIf { it.isNotBlank() }?.let { prefs.baseUrl = it.trimEnd('/') }
        o.optString("device_name").takeIf { it.isNotBlank() }?.let { prefs.name = it }

        val cfg = o.optJSONObject("config")
        if (cfg != null) {
            cfg.optString("webhook_url").takeIf { it.isNotBlank() }?.let { prefs.webhookUrl = it }
            cfg.optString("json_template").takeIf { it.isNotBlank() }?.let { prefs.jsonTemplate = it }
            cfg.optString("sim_filter").takeIf { it.isNotBlank() }?.let { prefs.simSlot = it }
            cfg.optString("sender_filter").takeIf { it.isNotBlank() }?.let { prefs.senderFilter = it }
            if (cfg.has("retries")) prefs.retries = cfg.optInt("retries", 10)
            if (cfg.has("ignore_ssl")) prefs.ignoreSsl = cfg.optBoolean("ignore_ssl")
            if (cfg.has("chunked_mode")) prefs.chunked = cfg.optBoolean("chunked_mode", true)
            if (cfg.has("forwarding_enabled")) prefs.forwardingEnabled = cfg.optBoolean("forwarding_enabled", true)
        }
        val hdr = o.optJSONObject("headers") ?: cfg?.optJSONObject("headers")
        prefs.headers = (hdr ?: JSONObject()
            .put("Content-Type", "application/json")
            .put("x-device-id", deviceId)
            .put("x-device-secret", secret)).toString()

        return deviceId
    }

    /** Pull the whole configuration using a one-time pairing code from the website. */
    fun enroll(prefs: Prefs, pairingCode: String, deviceInfo: JSONObject): String {
        val body = JSONObject(deviceInfo.toString()).put("pairing_code", pairingCode.trim().uppercase())
        val res = Api.postRaw(
            prefs.baseUrl + Config.EP_ENROLL,
            body.toString(),
            mapOf("Content-Type" to "application/json"),
            prefs.ignoreSsl,
        )
        val text = res.body?.string() ?: ""
        val code = res.code
        res.close()
        if (code !in 200..299) throw IllegalStateException("Enroll failed ($code): $text")
        return applyJson(prefs, text)
    }

    /** Export the current configuration (same schema the website produces). */
    fun export(prefs: Prefs): JSONObject {
        val base = prefs.baseUrl
        return JSONObject()
            .put("schema", "easypaybd.smsforwarder.config/v1")
            .put("base_url", base)
            .put("device_id", prefs.deviceId)
            .put("device_secret", prefs.deviceSecret)
            .put("device_name", prefs.name)
            .put("headers", JSONObject(prefs.headers))
            .put(
                "config",
                JSONObject()
                    .put("webhook_url", prefs.webhookUrl)
                    .put("json_template", prefs.jsonTemplate)
                    .put("retries", prefs.retries)
                    .put("ignore_ssl", prefs.ignoreSsl)
                    .put("chunked_mode", prefs.chunked)
                    .put("sim_filter", prefs.simSlot)
                    .put("sender_filter", prefs.senderFilter)
                    .put("forwarding_enabled", prefs.forwardingEnabled),
            )
            .put("http_shortcut", httpShortcut(prefs))
    }

    /** Heartbeat HTTP-Shortcut definition (importable into HTTP Shortcuts app too). */
    fun httpShortcut(prefs: Prefs): JSONObject = JSONObject()
        .put("name", "PaysBD Heartbeat")
        .put("method", "POST")
        .put("url", prefs.baseUrl + Config.EP_HEART)
        .put(
            "headers",
            JSONObject()
                .put("Content-Type", "application/json")
                .put("x-device-id", prefs.deviceId)
                .put("x-device-secret", prefs.deviceSecret),
        )
        .put("body", """{"source":"http-shortcut"}""")

    fun authHeaders(prefs: Prefs): Headers = Api.authHeaders(prefs)
}
