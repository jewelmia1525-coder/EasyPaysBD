package com.easypaybd.smsforwarder

import android.content.Context
import android.net.Uri
import org.json.JSONObject

object SmsScanner {
    fun scanAll(ctx: Context, prefs: Prefs, q: SmsQueue) {
        val cur = ctx.contentResolver.query(Uri.parse("content://sms/inbox"),
            arrayOf("address","body","date","sub_id"), null, null, "date DESC") ?: return
        cur.use {
            while (it.moveToNext()) {
                val obj = JSONObject()
                    .put("sender", it.getString(0) ?: "")
                    .put("body", it.getString(1) ?: "")
                    .put("timestamp", it.getLong(2))
                    .put("sim_slot", it.getInt(3))
                    .put("device_id", prefs.deviceId)
                q.add(obj)
            }
        }
    }
}
