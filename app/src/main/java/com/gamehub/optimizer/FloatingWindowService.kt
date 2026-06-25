package com.gamehub.optimizer

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Choreographer
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class FloatingWindowService : Service() {

    private lateinit var windowManager: WindowManager
    private var floatingView: LinearLayout? = null
    private var collapsedView: TextView? = null
    private var expandedView: LinearLayout? = null
    
    private var choreographer: Choreographer? = null
    private var frameCount = 0
    private var lastTimeNanos: Long = 0
    private lateinit var tvFps: TextView

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        // Басты қалқымалы контейнер
        floatingView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        // 1. Кішірейтілген түрі (Экран шетіндегі кішкентай батырма)
        collapsedView = TextView(this).apply {
            text = "⚡ Гейм Турбо"
            textSize = 12f
            setTextColor(Color.WHITE)
            setBackgroundColor(0xCCFF3333.toInt()) // Қызыл мөлдір түс
            setPadding(20, 15, 20, 15)
        }

        // 2. Үлкейтілген толық мәзір түрі
        expandedView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xE61A1A1A.toInt()) // Қою қара-сұр
            setPadding(30, 30, 30, 30)
            visibility = View.GONE
            gravity = Gravity.CENTER
        }

        // Мәзір ішіндегі элементтер
        val tvTitle = TextView(this).apply {
            text = "GAMEHUB OPTIMIZER"
            textSize = 14f
            setTextColor(Color.CYAN)
            setPadding(0, 0, 0, 20)
        }
        
        tvFps = TextView(this).apply {
            text = "FPS: --"
            textSize = 18f
            setTextColor(Color.GREEN)
            setPadding(0, 0, 0, 10)
        }

        val tvMode = TextView(this).apply {
            text = "РЕЖИМ: МАКСИМАЛДЫ ӨНІМДІЛІК 🔥"
            textSize = 12f
            setTextColor(Color.YELLOW)
            setPadding(0, 0, 0, 10)
        }

        val tvProtection = TextView(this).apply {
            text = "🔒 Кездейсоқ шығудан қорғау: ҚОСУЛЫ"
            textSize = 11f
            setTextColor(Color.WHITE)
            setPadding(0, 0, 0, 20)
        }

        val btnClose = Button(this).apply {
            text = "Панельді жабу"
            textSize = 12f
            setBackgroundColor(Color.RED)
            setTextColor(Color.WHITE)
            setOnClickListener {
                stopSelf() // Сервисті толық өшіру
            }
        }

        expandedView?.addView(tvTitle)
        expandedView?.addView(tvFps)
        expandedView?.addView(tvMode)
        expandedView?.addView(tvProtection)
        expandedView?.addView(btnClose)

        floatingView?.addView(collapsedView)
        floatingView?.addView(expandedView)

        // Экран параметрлері
        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 200
        }

        // Жылжыту (Drag & Drop) және басу функциясы
        collapsedView?.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f
            private var isMoved = false

            override fun onTouch(v: View?, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        isMoved = false
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val diffX = (event.rawX - initialTouchX).toInt()
                        val diffY = (event.rawY - initialTouchY).toInt()
                        if (Math.abs(diffX) > 10 || Math.abs(diffY) > 10) {
                            params.x = initialX + diffX
                            params.y = initialY + diffY
                            windowManager.updateViewLayout(floatingView, params)
                            isMoved = true
                        }
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (!isMoved) {
                            // Егер жай ғана басса, мәзірді ашады не жабады
                            if (expandedView?.visibility == View.GONE) {
                                expandedView?.visibility = View.VISIBLE
                                collapsedView?.text = "❌ Жабу"
                            } else {
                                expandedView?.visibility = View.GONE
                                collapsedView?.text = "⚡ Гейм Турбо"
                            }
                        }
                        return true
                    }
                }
                return false
            }
        })

        windowManager.addView(floatingView, params)

        // FPS есептеуді іске қосу
        choreographer = Choreographer.getInstance()
        lastTimeNanos = System.nanoTime()
        startFpsCounter()
    }

    // Нақты уақыттағы FPS есептегіш (Choreographer)
    private fun startFpsCounter() {
        choreographer?.postFrameCallback(object : Choreographer.FrameCallback {
            override fun doFrame(frameTimeNanos: Long) {
                frameCount++
                val diff = frameTimeNanos - lastTimeNanos
                if (diff >= 1_000_000_000L) { // 1 секунд өткен сайын жаңарту
                    val fps = (frameCount * 1_000_000_000L / diff).toInt()
                    tvFps.text = "FPS: $fps"
                    frameCount = 0
                    lastTimeNanos = frameTimeNanos
                }
                choreographer?.postFrameCallback(this)
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        if (floatingView != null) {
            windowManager.removeView(floatingView)
        }
    }
}
