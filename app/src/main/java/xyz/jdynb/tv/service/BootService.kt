package xyz.jdynb.tv.service

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
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import android.widget.TextView
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import xyz.jdynb.tv.MainActivity

/**
 * 启动服务
 */
class BootService: Service() {

  companion object {

    private const val TAG = "BootService"

  }

  override fun onBind(intent: Intent?): IBinder? {
    return null
  }

  override fun onCreate() {
    super.onCreate()
    val channel = NotificationChannelCompat.Builder("boot_notification", NotificationManagerCompat.IMPORTANCE_MAX)
      .setName("启动服务")
      .setDescription("启动服务")
      .build()
    val notificationManager = NotificationManagerCompat.from(this)
    notificationManager.createNotificationChannel(channel)

    val notification = NotificationCompat.Builder(this, "boot_notification")
      .setPriority(NotificationCompat.PRIORITY_MAX)
      .setContentTitle("启动服务")
      .build()

    startForeground(1, notification)
    Log.i(TAG, "前台服务已启动")

    val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    try {
      windowManager.addView(TextView(this).apply {
        text = "冬雨TV"
        textSize = 13f
        setTextColor(Color.WHITE)
      }, WindowManager.LayoutParams().apply {
        type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
          WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
        }
        x = 0
        y = 0
        gravity = Gravity.START or Gravity.TOP
        format = PixelFormat.TRANSLUCENT
        flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        width = WindowManager.LayoutParams.WRAP_CONTENT
        height = WindowManager.LayoutParams.WRAP_CONTENT
      })
    } catch (e: Exception) {
      Log.e(TAG, "Failed to add overlay", e)
    }

    repeat(3) {
      Handler(Looper.getMainLooper()).postDelayed({
        bringAppToFront(this)
      }, 1000)
    }
  }

  /**
   * 将应用带到前台
   */
  private fun bringAppToFront(context: Context) {
    try {
      val activityManager = context.getSystemService(ACTIVITY_SERVICE) as ActivityManager
      val packageName = context.packageName

      // 获取正在运行的任务
      val tasks = activityManager.appTasks
      for (task in tasks) {
        val taskInfo = task.taskInfo
        if (taskInfo.baseIntent?.component?.packageName == packageName) {
          // 将任务移至前台
          task.moveToFront()
          Log.i(TAG, "App moved to front")
          return
        }
      }

      Log.w(TAG, "App task not found, trying alternative method")
      // 备用方案:重新启动 Activity
      val intent = Intent(context, MainActivity::class.java).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT)
      }
      context.startActivity(intent)
    } catch (e: Exception) {
      Log.e(TAG, "Failed to bring app to front", e)
    }
  }
}