package com.easypaybd.smsforwarder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import org.json.JSONObject

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prefs = Prefs(context)
        if (!prefs.forwardingEnabled) return
        val msgs = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        val sim = intent.extras?.getInt("subscription", -1) ?: -1
        val bodyMap = LinkedHashMap<String, StringBuilder>()
        for (m in msgs) {
            val key = (m.originatingAddress ?: "unknown") + "|" + sim
            bodyMap.getOrPut(key) { StringBuilder() }.append(m.messageBody ?: "")
        }
        val q = SmsQueue(context)
        val filter = prefs.senderFilter.trim()
        for ((k, sb) in bodyMap) {
            val sender = k.substringBefore("|")
            if (filter.isNotEmpty() && filter != "*" && !filter.split(",").any { sender.contains(it.trim()) }) continue
            val slot = prefs.simSlot
            if (slot != "any" && slot != sim.toString()) continue
            val obj = JSONObject()
                .put("sender", sender)
                .put("body", sb.toString())
                .put("sim_slot", sim)
                .put("timestamp", System.currentTimeMillis())
                .put("device_id", prefs.deviceId)
            q.add(obj)
        }
        ForwarderService.start(context)
    }
}
