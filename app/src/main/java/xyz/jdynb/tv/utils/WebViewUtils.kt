package xyz.jdynb.tv.utils

import android.annotation.SuppressLint
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
import com.tencent.smtt.sdk.WebSettings
import com.tencent.smtt.sdk.WebView
import timber.log.Timber
import xyz.jdynb.tv.DongYuTVApplication
import xyz.jdynb.tv.ui.fragment.LivePlayerFragment.Companion.USER_AGENT
import java.io.File

object WebViewUtils {

  /**
   * 最低支持的 WebView 版本
   */
  private const val WEBVIEW_MIN_VERSION = 75

  /**
   * WebView 内核下载地址
   *
   * 这个文件只支持 arm64v8a,arm64v7a android 6 以上系统
   */
  private const val WEBVIEW_DOWNLOAD_URL_23 =
    "https://lz.qaiu.top/parser?url=https://jdy2002.lanzoue.com/iWgPm3ep91be"

  /**
   * WebView 内核下载地址
   *
   * android 5.0 以上系统 arm64v8a,arm64v7a
   */
  private const val WEBVIEW_DOWNLOAD_URL_21 = "https://lz.qaiu.top/parser?url=https://jdy2002.lanzoue.com/ia7jj3gys12f"

  /**
   * WebView 内核文件名
   */
  private const val WEBVIEW_FILE_NAME_23 = "com.google.android.webview_106.0.5249.65-524906503.apk"

  private const val WEBVIEW_FILE_NAME_21 = "com.google.android.webview_85.0.4183.59.apk"

  private const val TAG = "WebViewUtils"

  /**
   * 获取当前系统 WebView 版本
   */
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

  /**
   * 获取当前 WebView 版本号
   */
  fun getWebViewVersionNumber(): Int {
    var systemWebViewPackageVersion = WebViewUpgrade.getSystemWebViewPackageVersion()
    Log.i(TAG, "version: $systemWebViewPackageVersion, system: ${getCurrentLocalWebViewVersion()}")
    if (systemWebViewPackageVersion.isNullOrEmpty()) {
      try {
        val packageInfo =
          DongYuTVApplication.context.packageManager.getPackageInfo("com.google.android.webview", 0)
        systemWebViewPackageVersion = packageInfo.versionName
      } catch (_: Exception) {
        return Int.MAX_VALUE
      }
    }
    val index = systemWebViewPackageVersion.indexOf(".")
    if (index > 0) {
      val version = systemWebViewPackageVersion.substring(0, index).toInt()
      return version
    }
    return Int.MAX_VALUE
  }

  /**
   * WebSettings 配置
   */
  @SuppressLint("SetJavaScriptEnabled")
  fun WebView.setupWebSettings() {
    settings.apply {

      // tbs x5 播放视频优化
      // setPageCacheCapacity(IX5WebSettings.DEFAULT_CACHE_CAPACITY)
      setPluginState(WebSettings.PluginState.ON_DEMAND)

      isFocusable = false

      // 获取当前的 UA，可以获取当前的浏览器内核版本
      Timber.i( "userAgent: $userAgentString")
      userAgentString = USER_AGENT

      // 基本设置
      javaScriptEnabled = true
      domStorageEnabled = true
      databaseEnabled = true
      allowFileAccess = true
      allowContentAccess = true

      // 缓存设置
      cacheMode = WebSettings.LOAD_DEFAULT

      // 布局渲染
      layoutAlgorithm = WebSettings.LayoutAlgorithm.NORMAL
      useWideViewPort = false
      loadWithOverviewMode = false
      builtInZoomControls = false
      displayZoomControls = false
      setSupportZoom(false)

      // 文本渲染
      textZoom = 100
      defaultFontSize = 16
      defaultFixedFontSize = 13
      minimumFontSize = 8
      minimumLogicalFontSize = 8
      // setInitialScale(getMinimumScale())

      // 其他设置
      setSupportMultipleWindows(false)
      javaScriptCanOpenWindowsAutomatically = false
      loadsImagesAutomatically = true
      // blockNetworkImage = true
      mediaPlaybackRequiresUserGesture = false

      setAllowUniversalAccessFromFileURLs(true)
      setAllowFileAccessFromFileURLs(true)
    }
  }

  @Suppress("DEPRECATION")
  @Deprecated("废弃，使用 X5 内核")
  fun initWebView(context: Context, callback: () -> Unit) {
    try {
      val version = getWebViewVersionNumber()
      if (version < WEBVIEW_MIN_VERSION) {

        val fileName = if (version >= 23) WEBVIEW_FILE_NAME_23 else WEBVIEW_FILE_NAME_21

        val file = File(context.filesDir, fileName)
        val upgradeSource = if (false/*BuildConfig.DEBUG*/) {
          UpgradeAssetSource(context, "com.google.android.webview_106.0.5249.65-524906503(arm-v8a+arm64-v7a).apk", file)
        } else {
          val downloadUrl = if (version >= 23) WEBVIEW_DOWNLOAD_URL_23 else WEBVIEW_DOWNLOAD_URL_21
          UpgradeDownloadSource(
            DongYuTVApplication.Companion.context,
            downloadUrl,
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