package com.easypaybd.smsforwarder

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.view.animation.OvershootInterpolator
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {
    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        setContentView(R.layout.activity_splash)
        val logo = findViewById<android.widget.ImageView>(R.id.logo)
        logo.setImageResource(R.drawable.paysbd_logo)
        logo.scaleX = 0.2f; logo.scaleY = 0.2f; logo.alpha = 0f
        ObjectAnimator.ofFloat(logo, "scaleX", 0.2f, 1.2f, 1f).apply { duration = 1400; interpolator = OvershootInterpolator(); start() }
        ObjectAnimator.ofFloat(logo, "scaleY", 0.2f, 1.2f, 1f).apply { duration = 1400; interpolator = OvershootInterpolator(); start() }
        ObjectAnimator.ofFloat(logo, "alpha", 0f, 1f).apply { duration = 900; start() }
        logo.postDelayed({
            startActivity(Intent(this, MainActivity::class.java)); finish()
        }, 1800)
    }
}
