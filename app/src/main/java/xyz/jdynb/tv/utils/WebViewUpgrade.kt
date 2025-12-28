package xyz.jdynb.tv.utils

import android.app.ProgressDialog
import android.content.Context
import android.util.Log
import android.widget.Toast
import com.norman.webviewup.lib.UpgradeCallback
import com.norman.webviewup.lib.WebViewUpgrade
import com.norman.webviewup.lib.source.download.UpgradeDownloadSource
import xyz.jdynb.tv.DongYuTVApplication
import java.io.File

object WebViewUpgrade {

  /**
   * 最低支持的 WebView 版本
   */
  private const val WEBVIEW_MIN_VERSION = 106

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

  fun getWebViewVersionNumber(): Int {
    val systemWebViewPackageVersion = WebViewUpgrade.getSystemWebViewPackageVersion()
    Log.i(TAG, "version: $systemWebViewPackageVersion")
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
        val upgradeSource = UpgradeDownloadSource(
          DongYuTVApplication.Companion.context,
          WEBVIEW_DOWNLOAD_URL,
          File(context.filesDir, WEBVIEW_FILE_NAME)
        )
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
          callback()
        }
      } else {
        callback()
      }
    } catch (e: Exception){
      Toast.makeText(context, "内核更新错误: ${e.message}", Toast.LENGTH_LONG).show()
      callback()
    }
  }
}