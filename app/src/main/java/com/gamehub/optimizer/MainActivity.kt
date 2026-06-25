package com.gamehub.optimizer

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 50, 50, 50)
            setBackgroundColor(0xFF121212.toInt())
        }

        val btnStart = Button(this).apply {
            text = "STREAMER MODE БАСТАУ"
            setOnClickListener { startOverlayService() }
        }
        layout.addView(btnStart)

        setContentView(layout)
    }

    private fun startOverlayService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            startActivity(intent)
            return
        }
        val intent = Intent(this, OverlayService::class.java)
        startService(intent)
        Toast.makeText(this, "Сервис іске қосылды!", Toast.LENGTH_SHORT).show()
        finish()
    }
}
