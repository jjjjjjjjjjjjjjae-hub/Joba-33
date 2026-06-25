package com.gamehub.optimizer

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.*
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Button

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var floatingView: LinearLayout

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()

        floatingView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xAA000000.toInt())
            setPadding(20, 20, 20, 20)
        }

        val tvStatus = TextView(this).apply {
            text = "TikTok: Қосылуда..."
            setTextColor(0xFF00FF00.toInt())
        }

        val btnToggle = Button(this).apply {
            text = "Жасыру / Көрсету"
            setOnClickListener {
                tvStatus.visibility = if (tvStatus.visibility == View.VISIBLE) View.GONE else View.VISIBLE
            }
        }

        floatingView.addView(tvStatus)
        floatingView.addView(btnToggle)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        params.x = 100
        params.y = 100

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        windowManager.addView(floatingView, params)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::floatingView.isInitialized) windowManager.removeView(floatingView)
    }
}
