package com.easypaybd.smsforwarder

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Simple durable queue for offline SMS. */
class SmsQueue(ctx: Context) {
    private val p = ctx.getSharedPreferences("smsq", Context.MODE_PRIVATE)

    @Synchronized
    fun add(obj: JSONObject) {
        val arr = JSONArray(p.getString("q", "[]"))
        arr.put(obj)
        p.edit().putString("q", arr.toString()).apply()
    }

    @Synchronized
    fun drain(): JSONArray {
        val arr = JSONArray(p.getString("q", "[]"))
        p.edit().putString("q", "[]").apply()
        return arr
    }

    @Synchronized
    fun restore(arr: JSONArray) {
        val cur = JSONArray(p.getString("q", "[]"))
        for (i in 0 until arr.length()) cur.put(arr.get(i))
        p.edit().putString("q", cur.toString()).apply()
    }

    fun size(): Int = JSONArray(p.getString("q", "[]")).length()
}
