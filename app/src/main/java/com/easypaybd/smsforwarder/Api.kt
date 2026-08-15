package com.easypaybd.smsforwarder

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.*

object Api {
    private val JSON = "application/json; charset=utf-8".toMediaType()

    fun client(ignoreSsl: Boolean = false): OkHttpClient {
        val b = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
        if (ignoreSsl) {
            val tm = arrayOf<TrustManager>(object : X509TrustManager {
                override fun checkClientTrusted(c: Array<X509Certificate>, a: String) {}
                override fun checkServerTrusted(c: Array<X509Certificate>, a: String) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            })
            val sc = SSLContext.getInstance("TLS").apply { init(null, tm, java.security.SecureRandom()) }
            b.sslSocketFactory(sc.socketFactory, tm[0] as X509TrustManager)
            b.hostnameVerifier { _, _ -> true }
        }
        return b.build()
    }

    fun authHeaders(p: Prefs): Headers = Headers.headersOf(
        "x-device-id", p.deviceId,
        "x-device-secret", p.deviceSecret,
        "Content-Type", "application/json"
    )

    fun post(url: String, body: JSONObject, headers: Headers, ignoreSsl: Boolean = false): Response {
        val req = Request.Builder().url(url).headers(headers)
            .post(body.toString().toRequestBody(JSON)).build()
        return client(ignoreSsl).newCall(req).execute()
    }

    fun get(url: String, headers: Headers, ignoreSsl: Boolean = false): Response {
        val req = Request.Builder().url(url).headers(headers).get().build()
        return client(ignoreSsl).newCall(req).execute()
    }

    fun postRaw(url: String, bodyStr: String, headersMap: Map<String, String>, ignoreSsl: Boolean): Response {
        val hb = Headers.Builder(); headersMap.forEach { (k, v) -> hb.add(k, v) }
        val req = Request.Builder().url(url).headers(hb.build())
            .post(bodyStr.toRequestBody(JSON)).build()
        return client(ignoreSsl).newCall(req).execute()
    }

    fun heartbeat(p: Prefs, extra: JSONObject = JSONObject()): Response =
        post(Config.BASE_URL + Config.EP_HEART, extra, authHeaders(p))

    fun pullConfig(p: Prefs): Response =
        get(Config.BASE_URL + Config.EP_CONFIG, authHeaders(p))

    fun pushTelemetry(p: Prefs, body: JSONObject): Response =
        post(Config.BASE_URL + Config.EP_CONFIG, body, authHeaders(p))

    fun ingest(p: Prefs, messages: JSONArray): Response {
        val body = JSONObject().put("messages", messages)
        return post(Config.BASE_URL + Config.EP_INGEST, body, authHeaders(p))
    }

    fun pullCommands(p: Prefs): Response =
        get(Config.BASE_URL + Config.EP_CMD, authHeaders(p))

    fun ackCommand(p: Prefs, id: String, status: String, result: String? = null): Response {
        val body = JSONObject().put("command_id", id).put("status", status)
        if (result != null) body.put("result", result)
        return post(Config.BASE_URL + Config.EP_CMD, body, authHeaders(p))
    }

    fun log(p: Prefs, level: String, msg: String, code: Int? = null): Response {
        val body = JSONObject().put("level", level).put("message", msg)
        if (code != null) body.put("http_code", code)
        return post(Config.BASE_URL + Config.EP_LOG, body, authHeaders(p))
    }

    fun release(p: Prefs): Response = get(Config.BASE_URL + Config.EP_RELEASE, authHeaders(p))
}
