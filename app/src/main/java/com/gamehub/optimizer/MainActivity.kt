package com.gamehub.optimizer

import android.app.Activity
import android.app.ActivityManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.Toast

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val btnStartFreeFire = Button(this).apply {
            text = "BOOST & START FREE FIRE"
            textSize = 18f
            setOnClickListener {
                boostAndPlay()
            }
        }

        setContentView(btnStartFreeFire)
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

    private fun boostAndPlay() {
        enableDoNotDisturb()

        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val runningProcesses = activityManager.runningAppProcesses
        runningProcesses?.forEach { processInfo ->
            if (processInfo.processName != "com.dts.freefireth" && processInfo.processName != packageName) {
                try {
                    activityManager.killBackgroundProcesses(processInfo.processName)
                } catch (e: Exception) {}
            }
        }

        val launchIntent = packageManager.getLaunchIntentForPackage("com.dts.freefireth")
        if (launchIntent != null) {
            startActivity(launchIntent)
        } else {
            Toast.makeText(this, "Free Fire телефоныңызда орнатылмаған!", Toast.LENGTH_LONG).show()
        }
    }
}
