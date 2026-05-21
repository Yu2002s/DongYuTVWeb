package xyz.jdynb.tv.ui.activity

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.util.Log
import com.drake.engine.base.EngineActivity
import com.tencent.smtt.export.external.interfaces.ConsoleMessage
import com.tencent.smtt.export.external.interfaces.PermissionRequest
import com.tencent.smtt.export.external.interfaces.WebResourceError
import com.tencent.smtt.export.external.interfaces.WebResourceRequest
import com.tencent.smtt.export.external.interfaces.WebResourceResponse
import com.tencent.smtt.sdk.WebChromeClient
import com.tencent.smtt.sdk.WebSettings
import com.tencent.smtt.sdk.WebView
import com.tencent.smtt.sdk.WebViewClient
import xyz.jdynb.tv.R
import xyz.jdynb.tv.databinding.ActivityWebUpdateBinding

class X5WebViewDebugActivity: EngineActivity<ActivityWebUpdateBinding>(R.layout.activity_web_update) {
  override fun initData() {

  }

  override fun initView() {
    binding.webview.loadUrl("http://debugtbs.qq.com")

    initWebView(binding.webview)
  }

  /**
   * 创建并配置 WebView
   */
  @SuppressLint("SetJavaScriptEnabled")
  fun initWebView(webView: WebView) {
    webView.apply {
      // 基本配置
      setupWebSettings()
      setupWebChromeClient()
      setupWebViewClient()
    }
  }

  /**
   * WebSettings 配置
   */
  @SuppressLint("SetJavaScriptEnabled")
  private fun WebView.setupWebSettings() {
    settings.apply {

      // tbs x5 播放视频优化
      // setPageCacheCapacity(IX5WebSettings.DEFAULT_CACHE_CAPACITY)
      setPluginState(WebSettings.PluginState.ON_DEMAND)

      isFocusable = false

      // 获取当前的 UA，可以获取当前的浏览器内核版本
      Log.i("jdy", "userAgent: $userAgentString")
      // userAgentString = USER_AGENT

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
      loadsImagesAutomatically = true // 禁止加载图片
      // blockNetworkImage = true
      mediaPlaybackRequiresUserGesture = false

      setAllowUniversalAccessFromFileURLs(true)
      setAllowFileAccessFromFileURLs(true)
    }
  }

  /**
   * WebChromeClient 配置
   */
  private fun WebView.setupWebChromeClient() {
    webChromeClient = object : WebChromeClient() {

      override fun onPermissionRequest(request: PermissionRequest?) {
        // 处理权限请求（麦克风、摄像头等）
        request?.grant(request.resources)
      }

      override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
        consoleMessage?.let {
          Log.d("Console", "${it.message()} - ${it.lineNumber()}")
        }
        return true
      }

      override fun onProgressChanged(view: WebView?, newProgress: Int) {
        super.onProgressChanged(view, newProgress)
      }
    }
  }

  /**
   * WebViewClient 配置
   */
  private fun WebView.setupWebViewClient() {
    webViewClient = object : WebViewClient() {

      override fun shouldInterceptRequest(
        view: WebView?,
        request: WebResourceRequest?
      ): WebResourceResponse? {


        return super.shouldInterceptRequest(view, request)
      }

      override fun shouldOverrideUrlLoading(
        view: WebView?,
        request: WebResourceRequest?
      ): Boolean {
        // 处理链接跳转
        request?.url?.let { url ->
          val urlString = url.toString()

        }
        return super.shouldOverrideUrlLoading(view, request)
      }

      override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
        // 页面开始加载
        super.onPageStarted(view, url, favicon)
      }

      override fun onPageFinished(view: WebView, url: String) {
        // 页面加载完成
        super.onPageFinished(view, url)
      }

      override fun onReceivedError(view: WebView?, p1: WebResourceRequest?, p2: WebResourceError?) {
        super.onReceivedError(view, p1, p2)
      }
    }
  }

}