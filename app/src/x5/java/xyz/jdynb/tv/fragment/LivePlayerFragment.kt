package xyz.jdynb.tv.fragment

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.http.SslError
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.drake.engine.base.EngineFragment
import com.tencent.smtt.export.external.interfaces.ConsoleMessage
import com.tencent.smtt.export.external.interfaces.PermissionRequest
import com.tencent.smtt.export.external.interfaces.SslErrorHandler
import com.tencent.smtt.export.external.interfaces.WebResourceRequest
import com.tencent.smtt.export.external.interfaces.WebResourceResponse
import com.tencent.smtt.sdk.WebChromeClient
import com.tencent.smtt.sdk.WebSettings
import com.tencent.smtt.sdk.WebView
import com.tencent.smtt.sdk.WebViewClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import xyz.jdynb.tv.DongYuTVApplication
import xyz.jdynb.tv.MainViewModel
import xyz.jdynb.tv.R
import xyz.jdynb.tv.databinding.FragmentLivePlayerBinding
import xyz.jdynb.tv.enums.JsType
import xyz.jdynb.tv.event.Playable
import xyz.jdynb.tv.model.LiveChannelModel
import xyz.jdynb.tv.model.LiveModel
import xyz.jdynb.tv.model.LivePlayerModel
import xyz.jdynb.tv.utils.JsManager.execJs
import xyz.jdynb.tv.utils.X5JsManager.execJs
import xyz.jdynb.tv.utils.toArray
import java.io.ByteArrayInputStream
import kotlin.getValue

abstract class LivePlayerFragment :
  EngineFragment<FragmentLivePlayerBinding>(R.layout.fragment_live_player), Playable {

  companion object {

    private const val TAG = "LivePlayerFragment"

    const val USER_AGENT =
      "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36"

    private const val JS_INTERFACE_NAME = "AndroidVideo"

  }

  protected val webView get() = binding.webview

  protected val mainViewModel by activityViewModels<MainViewModel>()

  /**
   * 播放器配置
   */
  lateinit var playerConfig: LiveModel.Player

  protected var isPageFinished = false

  private val livePlayerModel = LivePlayerModel()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    // val playerName = LivePlayer.getLivePlayerForClass(this.javaClass).player
    val player = mainViewModel.currentChannelModel.value?.player ?: return
    playerConfig = mainViewModel.liveModel.player.find { it.id == player } ?: LiveModel.Player()
    Log.i(TAG, "playerConfig: $playerConfig")
  }

  override fun initData() {
    binding.m = livePlayerModel
  }

  @SuppressLint("ClickableViewAccessibility")
  override fun initView() {
    if (!DongYuTVApplication.isX5CoreInitFinished) {
      return
    }
    binding.webview.setOnTouchListener { v, event ->
      // 拦截触摸事件
      true
    }

    initWebView(webView)

    onLoadUrl(playerConfig.url)

    viewLifecycleOwner.lifecycleScope.launch {
      mainViewModel.currentChannelModel.collectLatest {
        it ?: return@collectLatest
        // 如果当前在数字切台的话, 就延迟 4 秒后进行切换
        if (mainViewModel.isTypingNumber()) {
          delay(4000L)
          mainViewModel.clearInputNumber()
        }

        if (!isPageFinished) {
          return@collectLatest
        }

        play(it)
      }
    }
  }

  protected open fun onLoadUrl(url: String?) {
    url ?: return
    webView.loadUrl(url)
  }

  /**
   * 执行 JS 脚本
   */
  fun execJs(jsType: JsType, vararg args: Pair<String, Any?>) {
    viewLifecycleOwner.lifecycleScope.launch {
      webView.execJs(playerConfig, jsType, *args)
    }
  }

  /**
   * 批量执行 JS 脚本
   */
  fun execJs(vararg args: Pair<JsType, Array<Pair<String, Any?>>?>) {
    viewLifecycleOwner.lifecycleScope.launch {
      args.forEach {
        webView.execJs(playerConfig,it.first, *(it.second ?: arrayOf()))
      }
    }
  }

  override fun play(channel: LiveChannelModel) {
    // 默认的播放
    execJs(JsType.PLAY, *channel.toArray())
  }

  override fun resumeOrPause() {
    execJs(JsType.RESUME_PAUSE)
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

      Log.i(TAG, "userAgent: $userAgentString")
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
      loadsImagesAutomatically = true // 禁止加载图片
      // blockNetworkImage = true
      mediaPlaybackRequiresUserGesture = false

      setAllowFileAccessFromFileURLs(true)
      setAllowUniversalAccessFromFileURLs(true)
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
        livePlayerModel.progress = newProgress
      }
    }
  }

  /**
   * 是否应该加载资源
   *
   * @param url 加载地址
   *
   * @return null 则默认加载，否则指定加载资源
   */
  protected open fun shouldInterceptRequest(url: String, request: WebResourceRequest): WebResourceResponse? {
    if (playerConfig.exclude?.url?.any { it == url } == true) {
      // 通过地址拦截
      return createEmptyResponse("*/*")
    } else if (playerConfig.exclude?.suffix?.any { url.endsWith(it) } == true) {
      // 通过后缀拦截
      return createEmptyResponse("*/*")
    }

    return null
  }

  /**
   * 是否拦截跳转
   *
   * @return true 拦截 false 不拦截
   */
  protected open fun shouldOverride(url: String): Boolean {
    // 自定义跳转逻辑
    // 例如：拦截特定协议，打开外部应用等

    Log.i(TAG, "shouldOverride: $url")
    return false
  }

  /**
   * 页面加载完成时的回调
   */
  protected open fun onPageFinished(url: String) {
    // 默认处理
    execJs(JsType.INIT, *mainViewModel.currentChannelModel.value.toArray())
  }

  /**
   * WebViewClient 配置
   */
  private fun WebView.setupWebViewClient() {
    webViewClient = object : WebViewClient() {

      override fun onReceivedSslError(
        wv: WebView?,
        handler: SslErrorHandler?,
        sslError: com.tencent.smtt.export.external.interfaces.SslError?
      ) {
        handler?.proceed()
      }

      override fun shouldInterceptRequest(
        view: WebView?,
        request: WebResourceRequest?
      ): WebResourceResponse? {

        val url = request?.url?.toString() ?: return createEmptyResponse()

        return shouldInterceptRequest(url, request) ?: super.shouldInterceptRequest(view, request)
      }

      override fun shouldOverrideUrlLoading(
        view: WebView?,
        request: WebResourceRequest?
      ): Boolean {
        // 处理链接跳转
        request?.url?.let { url ->
          val urlString = url.toString()
          // 自定义跳转逻辑
          if (shouldOverride(urlString)) {
            return true
          }
        }
        return super.shouldOverrideUrlLoading(view, request)
      }

      override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
        // 页面开始加载
        super.onPageStarted(view, url, favicon)
        isPageFinished = false
      }

      override fun onPageFinished(view: WebView, url: String) {
        // 页面加载完成
        super.onPageFinished(view, url)
        isPageFinished = true
        onPageFinished(url)
      }
    }
  }

  private val emptyByteArrayStream = ByteArrayInputStream("".toByteArray())

  protected fun createEmptyResponse(mimeType: String = "text/plain"): WebResourceResponse {
    // 创建一个空的响应
    return WebResourceResponse(
      mimeType,
      "UTF-8",
      emptyByteArrayStream
    )
  }

  protected fun createCryptoJsResponse(): WebResourceResponse {
    val cryptoJs = requireContext().assets.open("js/lib/dy-crypto-js.min.js")
    // 创建一个空的响应
    return WebResourceResponse(
      "application/javascript",
      "UTF-8",
      cryptoJs
    )
  }

  override fun onResume() {
    super.onResume()
    webView.onResume()
    webView.resumeTimers()
  }

  override fun onPause() {
    super.onPause()
    webView.onPause()
    webView.pauseTimers()
  }

  override fun onDestroyView() {
    super.onDestroyView()
    webView.destroy()
  }
}