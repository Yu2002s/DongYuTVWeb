package xyz.jdynb.tv.fragment

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.http.SslError
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.PermissionRequest
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import xyz.jdynb.tv.MainViewModel
import xyz.jdynb.tv.R
import xyz.jdynb.tv.databinding.FragmentLivePlayerBinding
import xyz.jdynb.tv.enums.JsType
import xyz.jdynb.tv.enums.LivePlayer
import xyz.jdynb.tv.event.Playable
import xyz.jdynb.tv.model.LiveChannelModel
import xyz.jdynb.tv.model.LiveModel
import xyz.jdynb.tv.model.LivePlayerModel
import xyz.jdynb.tv.utils.JsManager.execJs
import xyz.jdynb.tv.utils.NetworkUtils.inputStream
import xyz.jdynb.tv.utils.toArray
import java.io.ByteArrayInputStream

abstract class LivePlayerFragment : Fragment(), Playable {

  companion object {

    private const val TAG = "LivePlayerFragment"

    const val USER_AGENT =
      "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36"

    private const val JS_INTERFACE_NAME = "AndroidVideo"

  }

  private var _binding: FragmentLivePlayerBinding? = null

  protected val binding get() = _binding!!

  protected val webView get() = binding.webview

  private var videoJsInterface = VideoJavaScriptInterface()

  private val livePlayerModel = LivePlayerModel()

  protected val mainViewModel by activityViewModels<MainViewModel>()

  /**
   * 播放器名称
   */
  lateinit var playerName: String

  protected var loadUrl = ""

  /**
   * 播放器配置
   */
  lateinit var playerConfig: LiveModel.Player

  protected var isPageFinished = false

  inner class VideoJavaScriptInterface {
    /**
     * 视频播放事件
     */
    @JavascriptInterface
    fun onPlay() {
    }

    @JavascriptInterface
    fun onPause() {
    }

    @JavascriptInterface
    fun onKeyDown(key: String, keyCode: Int) {
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    playerName = LivePlayer.getLivePlayerForClass(this.javaClass).player
    val player = mainViewModel.currentChannelModel.value?.player ?: return
    playerConfig = mainViewModel.liveModel.player.find { it.id == player } ?: LiveModel.Player()
    Log.i(TAG, "playerConfig: $playerConfig")
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    _binding = DataBindingUtil.inflate(inflater, R.layout.fragment_live_player, container, false)
    return _binding?.root
  }

  @SuppressLint("ClickableViewAccessibility")
  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    binding.m = livePlayerModel

    binding.webview.setOnTouchListener { v, event ->
      // 拦截触摸事件
      true
    }

    initWebView(webView)

    val url = mainViewModel.liveModel.player.find { it.name == playerName }?.url
    loadUrl = url ?: ""
    onLoadUrl(url)

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
        webView.execJs(playerConfig, it.first, *(it.second ?: arrayOf()))
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
      // 添加自定义的接口
      addJavascriptInterface(videoJsInterface, JS_INTERFACE_NAME)
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

      allowFileAccessFromFileURLs = true
      allowUniversalAccessFromFileURLs = true
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
  protected open fun shouldInterceptRequest(
    url: String,
    request: WebResourceRequest
  ): WebResourceResponse? {
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

      @SuppressLint("WebViewClientOnReceivedSslError")
      override fun onReceivedSslError(p0: WebView?, p1: SslErrorHandler?, p2: SslError?) {
        p1?.proceed()
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

  protected fun createResponse(
    url: String,
    mimeType: String = "text/plain",
    headers: Map<String, String> = mapOf(
      "User-Agent" to USER_AGENT,
    )
  ): WebResourceResponse {
    val allHeaders = mutableMapOf(
      "Access-Control-Allow-Origin" to "*",
    )
    allHeaders.putAll(headers)
    val index = url.indexOf('/', 12)
    if (index != -1) {
      allHeaders.put("Referer", url.substring(0, index))
    } else {
      allHeaders.put("Referer", url)
    }
    Log.i(TAG, "requestHeaders: $allHeaders")
    return WebResourceResponse(
      mimeType,
      "UTF-8",
      200, "ok", allHeaders,
      url.inputStream(
        headers = allHeaders
      )
    )
  }

  /**
   * 创建 JS 响应
   */
  protected fun createJsResponse(fileName: String): WebResourceResponse {
    val js = requireContext().assets.open(fileName)
    // 创建一个空的响应
    return WebResourceResponse(
      "application/javascript",
      "UTF-8",
      js
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
    _binding = null
  }
}