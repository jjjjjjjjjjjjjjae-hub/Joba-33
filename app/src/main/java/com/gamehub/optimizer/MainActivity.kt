package com.gamehub.optimizer

import android.app.Activity
import android.app.ActivityManager
import android.app.AlertDialog
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private var selectedPackageName: String? = null
    private var selectedAppName: String = "Жоқ"
    private lateinit var tvSelectedApp: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Басты терезе дизайны
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(50, 50, 50, 50)
            setBackgroundColor(0xFF121212.toInt())
        }

        // Таңдалған ойынның атын көрсететін мәтін
        tvSelectedApp = TextView(this).apply {
            text = "Таңдалған ойын: $selectedAppName"
            textSize = 20f
            setTextColor(0xFFFFFFFF.toInt())
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 60)
        }
        layout.addView(tvSelectedApp)

        // Қолданба таңдау батырмасы (+)
        val btnSelectApp = Button(this).apply {
            text = "Ойын таңдау (+)"
            textSize = 16f
            setPadding(40, 30, 40, 30)
            setOnClickListener {
                showAppPickerDialog()
            }
        }
        layout.addView(btnSelectApp)

        // Бос орын
        val space = TextView(this).apply { setPadding(0, 20, 0, 20) }
        layout.addView(space)

        // Оңтайландыру және іске қосу батырмасы
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

    // Тізімнен телефондағы ойындарды көрсету функциясы
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

    private fun enableDoNotDisturb() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (notificationManager.isNotificationPolicyAccessGranted) {
                notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE)
            } else {
                try {
                    val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(this, "Рұқсат беті ашылмады", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // RAM тазалап, таңдалған ойынды қосу
    private fun boostAndPlay() {
        val pkg = selectedPackageName
        if (pkg == null) {
            Toast.makeText(this, "Алдымен ойын таңдау (+) батырмасын басыңыз!", Toast.LENGTH_LONG).show()
            return
        }

        enableDoNotDisturb()

        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val runningProcesses = activityManager.runningAppProcesses
        runningProcesses?.forEach { processInfo ->
            if (processInfo.processName != pkg && processInfo.processName != packageName) {
                try {
                    activityManager.killBackgroundProcesses(processInfo.processName)
                } catch (e: Exception) {}
            }
        }

        val launchIntent = packageManager.getLaunchIntentForPackage(pkg)
        if (launchIntent != null) {
            startActivity(launchIntent)
        } else {
            Toast.makeText(this, "Қолданбаны ашу мүмкін болмады", Toast.LENGTH_LONG).show()
        }
    }
}
