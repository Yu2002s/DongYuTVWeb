package xyz.jdynb.tv.utils

import android.app.ProgressDialog
import android.content.Context
import android.util.Log
import android.widget.Toast
import com.norman.webviewup.lib.UpgradeCallback
import com.norman.webviewup.lib.WebViewUpgrade
import com.norman.webviewup.lib.reflect.RuntimeAccess
import com.norman.webviewup.lib.service.interfaces.IServiceManager
import com.norman.webviewup.lib.service.interfaces.IWebViewUpdateService
import com.norman.webviewup.lib.source.UpgradeAssetSource
import com.norman.webviewup.lib.source.download.UpgradeDownloadSource
import xyz.jdynb.tv.BuildConfig
import xyz.jdynb.tv.DongYuTVApplication
import java.io.File

object WebViewUpgrade {

  /**
   * 最低支持的 WebView 版本
   */
  private const val WEBVIEW_MIN_VERSION = 75

  /**
   * WebView 内核下载地址
   *
   * 这个文件只支持 arm64v8a,arm64v7a android 6 以上系统
   */
  private const val WEBVIEW_DOWNLOAD_URL =
    "https://lz.qaiu.top/parser?url=https://jdy2002.lanzoue.com/iWgPm3ep91be"

  /**
   * WebView 内核文件名
   */
  private const val WEBVIEW_FILE_NAME = "com.google.android.webview_106.0.5249.65-524906503.apk"

  private const val TAG = "WebViewUpgrade"

  fun getCurrentLocalWebViewVersion(): String? {
    try {
      val serviceManager = RuntimeAccess.staticAccess(IServiceManager::class.java)
      val binder = serviceManager.getService(IWebViewUpdateService.SERVICE)
      var service =
        RuntimeAccess.staticAccess(IWebViewUpdateService::class.java)
      val iInterface = service.asInterface(binder)
      service = RuntimeAccess.objectAccess(
        IWebViewUpdateService::class.java,
        iInterface
      )
      if (service != null) {
        return service.currentWebViewPackage?.versionName
      }
    } catch (e: Throwable) {
      Log.e(TAG, "error: $e")
    }
    return null
  }

  fun getWebViewVersionNumber(): Int {
    val systemWebViewPackageVersion = WebViewUpgrade.getSystemWebViewPackageVersion()
    Log.i(TAG, "version: $systemWebViewPackageVersion, system: ${getCurrentLocalWebViewVersion()}")
    if (systemWebViewPackageVersion.isNullOrEmpty()) {
      return Int.MAX_VALUE
    }
    val index = systemWebViewPackageVersion.indexOf(".")
    if (index > 0) {
      val version = systemWebViewPackageVersion.substring(0, index).toInt()
      return version
    }
    return Int.MAX_VALUE
  }

  fun initWebView(context: Context, callback: () -> Unit) {
    try {
      val version = getWebViewVersionNumber()
      if (version < WEBVIEW_MIN_VERSION) {

        val file = File(context.filesDir, WEBVIEW_FILE_NAME)
        val upgradeSource = if (false/*BuildConfig.DEBUG*/) {
          UpgradeAssetSource(context, "com.google.android.webview_106.0.5249.65-524906503(arm-v8a+arm64-v7a).apk", file)
        } else {
          UpgradeDownloadSource(
            DongYuTVApplication.Companion.context,
            WEBVIEW_DOWNLOAD_URL,
            file
          )
        }

        Log.i(TAG, "start upgrade: $version")
        WebViewUpgrade.upgrade(upgradeSource)

        var progressDialog: ProgressDialog? = null

        WebViewUpgrade.addUpgradeCallback(object : UpgradeCallback {
          override fun onUpgradeComplete() {
            Log.i(TAG, "onUpgradeComplete")
            progressDialog?.dismiss()
            Toast.makeText(context, "内核更新完成！", Toast.LENGTH_LONG).show()
            callback()
          }

          override fun onUpgradeError(throwable: Throwable?) {
            Log.i(TAG, "throwable: $throwable")
            Toast.makeText(
              context,
              "内核更新错误: ${throwable?.message}",
              Toast.LENGTH_LONG
            ).show()
            progressDialog?.dismiss()
            callback()
          }

          override fun onUpgradeProcess(percent: Float) {
            Log.i(TAG, "onUpgradeProcess: $percent")
            if (progressDialog == null) {
              progressDialog = ProgressDialog(context)
                .apply {
                  setTitle("正在下载内核...")
                  setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
                  setCancelable(false)
                  setCanceledOnTouchOutside(false)
                  show()
                }
            }

            progressDialog.progress = (percent * 100).toInt()
          }
        })

        if (WebViewUpgrade.isCompleted()) {
          Log.i(TAG, "WebView Upgrade completed")
          callback()
        }
      } else {
        Log.i(TAG, "WebView version: $version >= $WEBVIEW_MIN_VERSION")
        callback()
      }
    } catch (e: Exception) {
      Log.e(TAG, "更新发生异常: $e")
      Toast.makeText(context, "内核更新错误: ${e.message}", Toast.LENGTH_LONG).show()
      callback()
    }
  }
}