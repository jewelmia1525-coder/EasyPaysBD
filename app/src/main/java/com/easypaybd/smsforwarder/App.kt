package com.easypaybd.smsforwarder

import android.app.Application

class App : Application() {
    override fun onCreate() { super.onCreate(); ForwarderService.start(this) }
}
