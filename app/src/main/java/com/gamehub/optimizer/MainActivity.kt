package com.gamehub.optimizer

import android.app.Activity
import android.app.ActivityManager
import android.app.AlertDialog
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private var selectedPackageName: String? = null
    private var selectedAppName: String = "Жоқ"
    private lateinit var tvSelectedApp: TextView
    private var isCheckingPermissions = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(50, 50, 50, 50)
            setBackgroundColor(0xFF121212.toInt())
        }

        tvSelectedApp = TextView(this).apply {
            text = "Таңдалған ойын: $selectedAppName"
            textSize = 20f
            setTextColor(0xFFFFFFFF.toInt())
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 60)
        }
        layout.addView(tvSelectedApp)

        val btnSelectApp = Button(this).apply {
            text = "Ойын таңдау (+)"
            textSize = 16f
            setPadding(40, 30, 40, 30)
            setOnClickListener {
                showAppPickerDialog()
            }
        }
        layout.addView(btnSelectApp)

        val space = TextView(this).apply { setPadding(0, 20, 0, 20) }
        layout.addView(space)

        val btnStart = Button(this).apply {
            text = "ОҢТАЙЛАНДЫРУ ЖӘНЕ БАСТАУ"
            textSize = 18f
            setPadding(50, 40, 50, 40)
            setOnClickListener {
                boostAndPlay()
            }
        }
        layout.addView(btnStart)

        setContentView(layout)
    }

    override fun onResume() {
        super.onResume()
        // Пайдаланушы баптаулардан қайтып келген сайын келесі рұқсатты кезекпен сұрайды
        checkPermissionsSequence()
    }

    private fun checkPermissionsSequence() {
        if (isCheckingPermissions) return

        // 1-ші Қадам: Қалқымалы терезе рұқсаты
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            isCheckingPermissions = true
            Toast.makeText(this, "1-ші қадам: Қалқымалы терезе рұқсатын беріңіз", Toast.LENGTH_LONG).show()
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            startActivity(intent)
            return
        }

        // 2-ші Қадам: Мазаламау режимі рұқсаты
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !notificationManager.isNotificationPolicyAccessGranted) {
            isCheckingPermissions = true
            Toast.makeText(this, "2-ші қадам: Мазаламау режиміне рұқсат беріңіз", Toast.LENGTH_LONG).show()
            val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
            startActivity(intent)
            return
        }

        // Барлық рұқсат алынса, блоктан шығарамыз
        isCheckingPermissions = false
    }

    private fun showAppPickerDialog() {
        val pm = packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val appsList = pm.queryIntentActivities(intent, 0)

        val appNames = ArrayList<String>()
        val packageNames = ArrayList<String>()

        for (app in appsList) {
            val label = app.loadLabel(pm).toString()
            val pkgName = app.activityInfo.packageName
            
            if (pkgName != packageName) {
                appNames.add(label)
                packageNames.add(pkgName)
            }
        }

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Тізімнен ойынды таңдаңыз:")
        builder.setItems(appNames.toTypedArray()) { _, which ->
            selectedPackageName = packageNames[which]
            selectedAppName = appNames[which]
            tvSelectedApp.text = "Таңдалған ойын: $selectedAppName"
            Toast.makeText(this, "$selectedAppName таңдалды", Toast.LENGTH_SHORT).show()
        }
        builder.show()
    }

    private fun boostAndPlay() {
        val pkg = selectedPackageName
        if (pkg == null) {
            Toast.makeText(this, "Алдымен ойын таңдау (+) батырмасын басыңыз!", Toast.LENGTH_LONG).show()
            return
        }

        // Мазаламау режимін белсенді ету
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && notificationManager.isNotificationPolicyAccessGranted) {
            notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE)
        }

        // Ойын пакетінің атын Сервиске жіберіп, іске қосамыз
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this)) {
            val serviceIntent = Intent(this, FloatingWindowService::class.java).apply {
                putExtra("TARGET_PACKAGE", pkg)
            }
            startService(serviceIntent)
        }

        // Фондық қолданбаларды тазалау
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        activityManager.runningAppProcesses?.forEach { processInfo ->
            if (processInfo.processName != pkg && processInfo.processName != packageName) {
                try {
                    activityManager.killBackgroundProcesses(processInfo.processName)
                } catch (e: Exception) {}
            }
        }

        // Ойынды қосу
        val launchIntent = packageManager.getLaunchIntentForPackage(pkg)
        if (launchIntent != null) {
            startActivity(launchIntent)
        } else {
            Toast.makeText(this, "Ойынды ашу мүмкін болмады", Toast.LENGTH_LONG).show()
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        isCheckingPermissions = false
    }
}
