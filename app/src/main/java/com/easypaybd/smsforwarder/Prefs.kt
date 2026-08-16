package com.easypaybd.smsforwarder

import android.content.Context
import java.util.UUID

class Prefs(ctx: Context) {
    private val p = ctx.getSharedPreferences("easypay", Context.MODE_PRIVATE)

    var deviceId: String
        get() = p.getString("device_id", null) ?: UUID.randomUUID().toString().also { deviceId = it }
        set(v) { p.edit().putString("device_id", v).apply() }

    var deviceSecret: String
        get() = p.getString("device_secret", null) ?: UUID.randomUUID().toString().replace("-", "").also { deviceSecret = it }
        set(v) { p.edit().putString("device_secret", v).apply() }

    var baseUrl: String get() = p.getString("base_url", Config.BASE_URL) ?: Config.BASE_URL; set(v) { p.edit().putString("base_url", v.trimEnd('/')).apply() }
    var featuresJson: String get() = p.getString("features", "{}") ?: "{}"; set(v) { p.edit().putString("features", v).apply() }
    fun feature(key: String, def: Boolean = false): Boolean = runCatching { org.json.JSONObject(featuresJson).optBoolean(key, def) }.getOrDefault(def)

    var name: String get() = p.getString("name", "") ?: ""; set(v) { p.edit().putString("name", v).apply() }
    var description: String get() = p.getString("description", "") ?: ""; set(v) { p.edit().putString("description", v).apply() }
    var senderFilter: String get() = p.getString("sender", "*") ?: "*"; set(v) { p.edit().putString("sender", v).apply() }
    var webhookUrl: String get() = p.getString("webhook", "") ?: ""; set(v) { p.edit().putString("webhook", v).apply() }
    var simSlot: String get() = p.getString("sim_slot", "any") ?: "any"; set(v) { p.edit().putString("sim_slot", v).apply() }
    var jsonTemplate: String get() = p.getString("tpl", Config.DEFAULT_TEMPLATE) ?: Config.DEFAULT_TEMPLATE; set(v) { p.edit().putString("tpl", v).apply() }
    var headers: String get() = p.getString("headers", Config.DEFAULT_HEADERS) ?: Config.DEFAULT_HEADERS; set(v) { p.edit().putString("headers", v).apply() }
    var retries: Int get() = p.getInt("retries", Config.DEFAULT_RETRIES); set(v) { p.edit().putInt("retries", v).apply() }
    var ignoreSsl: Boolean get() = p.getBoolean("ignore_ssl", false); set(v) { p.edit().putBoolean("ignore_ssl", v).apply() }
    var chunked: Boolean get() = p.getBoolean("chunked", true); set(v) { p.edit().putBoolean("chunked", v).apply() }
    var forwardingEnabled: Boolean get() = p.getBoolean("fwd", true); set(v) { p.edit().putBoolean("fwd", v).apply() }
}
