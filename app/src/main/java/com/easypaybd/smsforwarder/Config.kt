package com.easypaybd.smsforwarder

object Config {
    // CHANGE THIS to your deployed website URL
    const val BASE_URL = "https://paysbd.lovable.app"

    const val LOGO_URL = "https://i.postimg.cc/7hbf6hfM/Chat-GPT-Image-Aug-14-2026-12-11-48-PM.png"

    const val EP_CONFIG   = "/api/public/device-config"
    const val EP_INGEST   = "/api/public/sms-ingest"
    const val EP_CMD      = "/api/public/device-commands"
    const val EP_LOG      = "/api/public/device-log"
    const val EP_HEART    = "/api/public/device-heartbeat"
    const val EP_RELEASE  = "/api/public/app-release"

    const val DEFAULT_TEMPLATE = """{"from":"{{sender}}","body":"{{body}}","sim":{{sim}},"ts":{{timestamp}},"device":"{{device_id}}"}"""
    const val DEFAULT_HEADERS  = """{"Content-Type":"application/json","x-device-id":"<আপনার device id>","x-device-secret":"<আপনার secret>"}"""
    const val DEFAULT_RETRIES  = 10
}
