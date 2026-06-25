package com.gamehub.optimizer

import android.app.ActivityManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Choreographer
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class FloatingWindowService : Service() {

    private lateinit var windowManager: WindowManager
    private var floatingView: LinearLayout? = null
    private var collapsedView: TextView? = null
    private var expandedView: LinearLayout? = null
    private var crosshairView: View? = null
    
    private var choreographer: Choreographer? = null
    private var frameCount = 0
    private var lastTimeNanos: Long = 0
    private lateinit var tvFps: TextView
    
    private var targetPackage: String? = null
    private var isCrosshairEnabled = false
    private var isProtectionEnabled = true
    
    private val handler = Handler(Looper.getMainLooper())
    
    // Кездейсоқ ойыннан шығып кетуді автоматты түрде бұғаттау таймері
    private val appMonitorRunnable = object : Runnable {
        override fun run() {
            if (isProtectionEnabled && targetPackage != null) {
                val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                val runningTasks = am.getRunningTasks(1)
                if (runningTasks.isNotEmpty()) {
                    val topActivity = runningTasks[0].topActivity
                    if (topActivity != null && topActivity.packageName != targetPackage && topActivity.packageName != packageName) {
                        // Егер пайдаланушы ойыннан шығып кетсе, оны лезде ойынға қайта кіргізеді
                        Toast.makeText(applicationContext, "Кездейсоқ шығу бұғатталды! Ойын қайта жүктелуде...", Toast.LENGTH_SHORT).show()
                        val launchIntent = packageManager.getLaunchIntentForPackage(targetPackage!!)
                        launchIntent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                        startActivity(launchIntent)
                    }
                }
            }
            handler.postDelayed(this, 1500) // Әр 1.5 секунд сайын тексеріп отырады
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        targetPackage = intent?.getStringExtra("TARGET_PACKAGE")
        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        floatingView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        collapsedView = TextView(this).apply {
            text = "⚡ Game Turbo"
            textSize = 12f
            setTextColor(Color.WHITE)
            setBackgroundColor(0xCCFF3333.toInt())
            setPadding(20, 15, 20, 15)
        }

        expandedView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xE61A1A1A.toInt())
            setPadding(35, 35, 35, 35)
            visibility = View.GONE
            gravity = Gravity.CENTER
        }

        val tvTitle = TextView(this).apply {
            text = "GAMEHUB TURBO PANEL"
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

        // Функция 1: Экран ортасына прицел салу (Crosshair)
        val btnCrosshair = Button(this).apply {
            text = "Прицел (Crosshair): ӨШІРУЛІ"
            textSize = 12f
            setOnClickListener {
                toggleCrosshair()
                text = if (isCrosshairEnabled) "Прицел (Crosshair): ҚОСУЛЫ" else "Прицел (Crosshair): ӨШІРУЛІ"
            }
        }

        // Функция 2: Кездейсоқ шығудан қорғау ауыстырғышы
        val btnProtection = Button(this).apply {
            text = "Шығудан қорғау: ҚОСУЛЫ"
            textSize = 12f
            setOnClickListener {
                isProtectionEnabled = !isProtectionEnabled
                text = if (isProtectionEnabled) "Шығудан қорғау: ҚОСУЛЫ" else "Шығудан қорғау: ӨШІРУЛІ"
            }
        }

        // Функция 3: Ойын ішінен жедел жадыны тазалау
        val btnBoostInGame = Button(this).apply {
            text = "Ойын ішінен RAM тазалау"
            textSize = 12f
            setOnClickListener {
                val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                am.runningAppProcesses?.forEach { process ->
                    if (process.processName != targetPackage && process.processName != packageName) {
                        try { am.killBackgroundProcesses(process.processName) } catch (e: Exception) {}
                    }
                }
                Toast.makeText(applicationContext, "Ойын оңтайландырылды! RAM тазаланды 🔥", Toast.LENGTH_SHORT).show()
            }
        }

        val btnClose = Button(this).apply {
            text = "Панельді жабу"
            textSize = 11f
            setBackgroundColor(Color.RED)
            setTextColor(Color.WHITE)
            setOnClickListener {
                stopSelf()
            }
        }

        expandedView?.addView(tvTitle)
        expandedView?.addView(tvFps)
        expandedView?.addView(btnCrosshair)
        expandedView?.addView(btnProtection)
        expandedView?.addView(btnBoostInGame)
        expandedView?.addView(btnClose)

        floatingView?.addView(collapsedView)
        floatingView?.addView(expandedView)

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
                            if (expandedView?.visibility == View.GONE) {
                                expandedView?.visibility = View.VISIBLE
                                collapsedView?.text = "❌ Жабу"
                            } else {
                                expandedView?.visibility = View.GONE
                                collapsedView?.text = "⚡ Game Turbo"
                            }
                        }
                        return true
                    }
                }
                return false
            }
        })

        windowManager.addView(floatingView, params)

        choreographer = Choreographer.getInstance()
        lastTimeNanos = System.nanoTime()
        startFpsCounter()

        // Кездейсоқ шығуды қадағалайтын мониторды іске қосу
        handler.post(appMonitorRunnable)
    }

    private fun startFpsCounter() {
        choreographer?.postFrameCallback(object : Choreographer.FrameCallback {
            override fun doFrame(frameTimeNanos: Long) {
                frameCount++
                val diff = frameTimeNanos - lastTimeNanos
                if (diff >= 1_000_000_000L) {
                    val fps = (frameCount * 1_000_000_000L / diff).toInt()
                    tvFps.text = "FPS: $fps"
                    frameCount = 0
                    lastTimeNanos = frameTimeNanos
                }
                choreographer?.postFrameCallback(this)
            }
        })
    }

    // Экранның қақ ортасына өзгермейтін қызыл нүкте (Прицел) шығару функциясы
    private fun toggleCrosshair() {
        if (!isCrosshairEnabled) {
            val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            }

            crosshairView = View(this).apply {
                setBackgroundColor(Color.RED)
            }

            // Ортаға дәл орналасатын 12х12 пиксельді кішкентай нысана нүктесі
            val params = WindowManager.LayoutParams(
                12, 12,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.CENTER
            }

            windowManager.addView(crosshairView, params)
            isCrosshairEnabled = true
        } else {
            if (crosshairView != null) {
                windowManager.removeView(crosshairView)
                crosshairView = null
            }
            isCrosshairEnabled = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(appMonitorRunnable)
        if (floatingView != null) {
            windowManager.removeView(floatingView)
        }
        if (crosshairView != null) {
            windowManager.removeView(crosshairView)
        }
    }
}
