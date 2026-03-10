package xyz.jdynb.tv.utils

import android.os.Build
import androidx.annotation.OptIn
import androidx.core.content.pm.PackageInfoCompat
import xyz.jdynb.tv.ui.activity.CrashActivity
import xyz.jdynb.tv.DongYuTVApplication
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.system.exitProcess

/**
 * App崩溃处理
 */
class CrashHandler : Thread.UncaughtExceptionHandler {

  private lateinit var defaultHandler: Thread.UncaughtExceptionHandler

  companion object {

    private const val TAG = "AppCrash"

    private var instance: CrashHandler? = null

    fun getInstance(): CrashHandler {
      if (instance == null) {
        instance = CrashHandler()
      }
      return instance!!
    }

  }

  fun init() {
    defaultHandler = Thread.getDefaultUncaughtExceptionHandler()!!
    Thread.setDefaultUncaughtExceptionHandler(this)
  }

  override fun uncaughtException(t: Thread, e: Throwable) {
    if (!handlerException(e)) {
      defaultHandler.uncaughtException(t, e)
    } else {
      exitProcess(0)
    }
  }

  private fun handlerException(e: Throwable?): Boolean {
    if (e == null) {
      return false
    }
    try {
      val exception = getException(e)
      CrashActivity.actionStart(exception)
    } catch (e: Exception) {
      e.printStackTrace()
    }
    return true
  }

  private fun getException(e: Throwable): String {
    val stringBuilder = StringBuilder()
    val writer = StringWriter()
    val printWriter = PrintWriter(writer)
    e.printStackTrace(printWriter)
    var cause = e.cause
    while (cause != null) {
      cause.printStackTrace(printWriter)
      cause = cause.cause
    }
    printWriter.flush()
    printWriter.close()
    val context = DongYuTVApplication.context
    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
    stringBuilder.append("\n当前软件版本：${packageInfo.versionName} ${PackageInfoCompat.getLongVersionCode(packageInfo)}\n")
    stringBuilder.append("SDK版本: " + Build.VERSION.SDK_INT)
    stringBuilder.append("\n手机系统：" + Build.VERSION.INCREMENTAL)

    stringBuilder.append("\n\n  ************错误日志***********\n\n")
    stringBuilder.append(writer.toString())
    return stringBuilder.toString()
  }

}