package xyz.jdynb.tv.receiver

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import xyz.jdynb.tv.MainActivity
import xyz.jdynb.tv.service.BootService

/**
 * 开机自启动广播接收器
 * 监听系统开机完成广播,自动启动应用主界面
 */
class BootReceiver : BroadcastReceiver() {

  companion object {
    private const val TAG = "BootReceiver"
  }

  override fun onReceive(context: Context, intent: Intent) {
    val action = intent.action
    Log.d(TAG, "Received broadcast: $action")

    // 判断是否为开机广播
    if (isBootIntent(action)) {
      try {
        // 延迟启动,避免系统刚启动时资源紧张
        // 增加延迟时间到3秒,确保系统完全启动
        android.os.Handler(context.mainLooper).postDelayed({
          startMainActivity(context)
          // 多次尝试将应用置于前台
          for (i in 1..3) {
            android.os.Handler(context.mainLooper).postDelayed({
              bringAppToFront(context)
            }, (i * 1000).toLong())
          }
        }, 3000) // 延迟3秒启动
      } catch (e: Exception) {
        Log.e(TAG, "Error starting app on boot", e)
      }
    }
  }

  /**
   * 判断是否为开机相关的 Intent
   */
  private fun isBootIntent(action: String?): Boolean {
    return when (action) {
      Intent.ACTION_BOOT_COMPLETED,
      "android.intent.action.QUICKBOOT_POWERON",
      Intent.ACTION_LOCKED_BOOT_COMPLETED,
      "com.htc.intent.action.QUICKBOOT_POWERON" -> true

      else -> false
    }
  }

  /**
   * 将应用带到前台
   */
  private fun bringAppToFront(context: Context) {
    try {
      val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
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

  /**
   * 启动主 Activity
   */
  private fun startMainActivity(context: Context) {
    try {
      val intent = Intent(context, MainActivity::class.java).apply {
        // 必须添加 FLAG_ACTIVITY_NEW_TASK,因为从非 Activity 的 Context 启动
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        // 将应用带到前台,非常重要!
        addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        // 清除之前的任务栈
        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        // 避免重复创建
        addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        // 排除从最近任务中启动
        addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
        // 重置任务
        addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
      }
      ContextCompat.startForegroundService(context, Intent(context, BootService::class.java))
      context.startActivity(intent)
      Log.i(TAG, "Successfully started MainActivity on boot")
    } catch (e: Exception) {
      Log.e(TAG, "Failed to start MainActivity", e)
    }
  }
}
