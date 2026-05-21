package xyz.jdynb.tv.ui.fragment

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.tencent.smtt.export.external.interfaces.ConsoleMessage
import com.tencent.smtt.export.external.interfaces.PermissionRequest
import com.tencent.smtt.export.external.interfaces.WebResourceRequest
import com.tencent.smtt.export.external.interfaces.WebResourceResponse
import com.tencent.smtt.sdk.WebChromeClient
import com.tencent.smtt.sdk.WebView
import com.tencent.smtt.sdk.WebViewClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import xyz.jdynb.tv.MainActivity
import xyz.jdynb.tv.MainViewModel
import xyz.jdynb.tv.R
import xyz.jdynb.tv.databinding.FragmentLivePlayerBinding
import xyz.jdynb.tv.enums.JsType
import xyz.jdynb.tv.enums.LivePlayer
import xyz.jdynb.tv.event.Playable
import xyz.jdynb.tv.model.LiveChannelModel
import xyz.jdynb.tv.model.LiveModel
import xyz.jdynb.tv.model.LivePlayerModel
import xyz.jdynb.tv.utils.WebViewUtils.setupWebSettings
import xyz.jdynb.tv.utils.X5JsManager.execJs
import java.io.ByteArrayInputStream
import java.util.Timer
import kotlin.random.Random

abstract class LivePlayerFragment : Fragment(), Playable {

  companion object {

    const val USER_AGENT =
      "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36"

    /**
     * 离开的最大时间
     */
    private const val MAX_LEAVE_TIME = 30 * 1000L
  }

  private var _binding: FragmentLivePlayerBinding? = null

  protected val binding get() = _binding!!

  protected val webView get() = binding.webview

  private val livePlayerModel = LivePlayerModel()

  protected val mainViewModel by activityViewModels<MainViewModel>()

  /**
   * 当前频道
   */
  protected val currentChannelModel get() = mainViewModel.currentChannelModel.value!!

  /**
   * 播放器名称
   */
  lateinit var playerName: String

  /**
   * 加载地址
   */
  protected var loadUrl = ""

  /**
   * 播放器配置
   */
  lateinit var playerConfig: LiveModel.Player

  /**
   * 页面是否已经加载完成
   */
  protected var isPageFinished = false

  /**
   * 离开播放页的时间
   */
  private var leaveTime = System.currentTimeMillis()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    playerName = LivePlayer.getLivePlayerForClass(this.javaClass).player
    val player = mainViewModel.currentChannelModel.value?.player
    if (player == null) {
      // 异常情况，目前未知，这里先尝试刷新操作
      Handler(Looper.getMainLooper()).postDelayed({
        (activity as? MainActivity)?.refreshFragment()
      }, 1500L)
      return
    }
    playerConfig = mainViewModel.liveModel.player.find { it.id == player } ?: LiveModel.Player()
    Timber.i("playerConfig: $playerConfig")
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
    if (!::playerConfig.isInitialized) {
      return
    }

    binding.m = livePlayerModel

    // 禁用触摸
    view.isFocusable = false
    webView.setOnTouchListener { _, _ ->
      true
    }

    initWebView(webView)

    val url = playerConfig.url
    loadUrl = url ?: ""
    Timber.i( "loadUrl: $url playerName: $playerName")
    onLoadUrl(url, currentChannelModel)

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

  override fun refresh() {
    mainViewModel.currentChannelModel.value?.let {
      play(it)
    }
  }

  /**
   * 执行加载 URL
   *
   * @param url 加载的 URL
   * @param channelModel 当前频道信息
   */
  abstract fun onLoadUrl(url: String?, channelModel: LiveChannelModel)

  /**
   * 是否拦截跳转
   *
   * @return true 拦截 false 不拦截
   */
  abstract fun shouldOverride(url: String): Boolean

  /**
   * 页面加载完成时的回调
   *
   * @param url 加载的 URL
   * @param channelModel 当前频道信息
   */
  abstract fun onPageFinished(url: String, channelModel: LiveChannelModel)

  /**
   * 当页面开始加载时的回调
   */
  abstract fun onPageStarted(url: String, channelModel: LiveChannelModel)

  /**
   * 进度条变化时的回调
   */
  protected open fun onProgressChanged(newProgress: Int): Int {
    return newProgress
  }

  /**
   * 设置进度条进度
   */
  fun setProgress(progress: Int) {
    livePlayerModel.progress = progress
    onProgressChanged(progress)
  }

  /**
   * 设置随机进度
   */
  fun setProgress() {
    livePlayerModel.progress = Random.nextInt(1, 95)
  }

  /**
   * 获取进度条的进度
   */
  fun getProgress(): Int {
    return livePlayerModel.progress
  }

  /**
   * 执行 JS 脚本
   */
  fun execJs(jsType: JsType, vararg args: Pair<String, Any?>, callback: (() -> Unit)? = null) {
    viewLifecycleOwner.lifecycleScope.launch {
      webView.execJs(playerConfig, jsType, *args)
      withContext(Dispatchers.Main) {
        callback?.invoke()
      }
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

  /**
   * 创建并配置 WebView
   */
  @SuppressLint("SetJavaScriptEnabled")
  protected fun initWebView(webView: WebView) {
    webView.apply {
      // 基本配置
      setupWebSettings()
      setupWebChromeClient()
      setupWebViewClient()
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
          Timber.tag("Console").d("${it.message()} - ${it.lineNumber()}")
        }
        return true
      }

      override fun onProgressChanged(view: WebView?, newProgress: Int) {
        super.onProgressChanged(view, newProgress)
        livePlayerModel.progress = onProgressChanged(newProgress)
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
   * WebViewClient 配置
   */
  private fun WebView.setupWebViewClient() {
    webViewClient = object : WebViewClient() {

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
        onPageStarted(url, currentChannelModel)
      }

      override fun onPageFinished(view: WebView, url: String) {
        // 页面加载完成
        super.onPageFinished(view, url)
        isPageFinished = true
        onPageFinished(url, currentChannelModel)
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

  override fun onResume() {
    super.onResume()
    webView.onResume()
    webView.resumeTimers()
    Timber.i( "onResume")
    val now = System.currentTimeMillis()
    if (now - leaveTime > MAX_LEAVE_TIME) {
      refresh()
      leaveTime = now
    }
  }

  override fun onPause() {
    super.onPause()
    Timber.i( "onPause")
    webView.onPause()
    webView.pauseTimers()
    leaveTime = System.currentTimeMillis()
  }

  override fun onDestroyView() {
    super.onDestroyView()
    webView.destroy()
    _binding = null
  }
}