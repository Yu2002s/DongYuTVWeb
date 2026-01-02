package xyz.jdynb.tv.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.drake.engine.base.EngineFragment
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.mozilla.geckoview.AllowOrDeny
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoRuntimeSettings
import org.mozilla.geckoview.GeckoSession
import xyz.jdynb.tv.BuildConfig
import xyz.jdynb.tv.MainViewModel
import xyz.jdynb.tv.R
import xyz.jdynb.tv.databinding.FragmentLivePlayerBinding
import xyz.jdynb.tv.event.Playable
import xyz.jdynb.tv.model.LiveChannelModel
import xyz.jdynb.tv.model.LiveModel
import xyz.jdynb.tv.model.LivePlayerModel


abstract class LivePlayerFragment :
  EngineFragment<FragmentLivePlayerBinding>(R.layout.fragment_live_player), Playable,
  GeckoSession.ProgressDelegate, GeckoSession.ContentDelegate, GeckoSession.PermissionDelegate, GeckoSession.NavigationDelegate {

  protected val mainViewModel by activityViewModels<MainViewModel>()

  private val livePlayerModel = LivePlayerModel()

  companion object {

    private const val TAG = "LivePlayerFragment"

    /**
     * 固定 UA
     */
    const val USER_AGENT =
      "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36"

    @JvmStatic
    var runtime: GeckoRuntime? = null
  }

  protected val session = GeckoSession()

  private var currentUrl: String = ""

  private var isPageFinished = false

  protected lateinit var playerConfig: LiveModel.Player

  @SuppressLint("SetJavaScriptEnabled")
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    // val playerName = LivePlayer.getLivePlayerForClass(this.javaClass).player
    val player = mainViewModel.currentChannelModel.value?.player ?: return
    playerConfig = mainViewModel.liveModel.player.find { it.id == player } ?: LiveModel.Player()
    Log.i(TAG, "playerConfig: $playerConfig")

    val webView = WebView(requireContext())
    val settings = webView.getSettings()
    settings.javaScriptEnabled = true
    settings.mediaPlaybackRequiresUserGesture = false // 关键设置
    settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
  }

  @SuppressLint("ClickableViewAccessibility")
  override fun initView() {
    binding.webview.setOnTouchListener { v, event ->
      // 禁止触摸事件
      true
    }

    // 初始化 GeckoView
    initGeckoView()

    onLoadUrl(playerConfig.url)
  }

  override fun initData() {
    binding.m = livePlayerModel

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
    session.loadUri(url)
  }

  override fun play(channel: LiveChannelModel) {

  }

  override fun resumeOrPause() {

  }

  override fun onProgressChange(session: GeckoSession, progress: Int) {
    super.onProgressChange(session, progress)
    livePlayerModel.progress = progress
  }

  override fun onPageStart(session: GeckoSession, url: String) {
    super.onPageStart(session, url)
    isPageFinished = false
  }

  override fun onPageStop(session: GeckoSession, success: Boolean) {
    super.onPageStop(session, success)
    isPageFinished = true
    // 页面加载完成
  }

  override fun onMediaPermissionRequest(
    session: GeckoSession,
    uri: String,
    video: Array<out GeckoSession.PermissionDelegate.MediaSource?>?,
    audio: Array<out GeckoSession.PermissionDelegate.MediaSource?>?,
    callback: GeckoSession.PermissionDelegate.MediaCallback
  ) {
    callback.grant(video?.first(), audio?.first())
  }

  override fun onLoadRequest(
    session: GeckoSession,
    request: GeckoSession.NavigationDelegate.LoadRequest
  ): GeckoResult<AllowOrDeny?>? {
    return GeckoResult.allow()
  }

  private fun initGeckoView() {
    if (runtime == null) {
      val settings = GeckoRuntimeSettings.Builder()
        .javaScriptEnabled(true)
        .consoleOutput(BuildConfig.DEBUG)
        .remoteDebuggingEnabled(BuildConfig.DEBUG)
        .allowInsecureConnections(GeckoRuntimeSettings.ALLOW_ALL)
        .webManifest(true)
        .debugLogging(BuildConfig.DEBUG)
        .aboutConfigEnabled(true)
        .build()

      System.setProperty("org.mozilla.geckoview.allow_autoplay", "true");

      runtime = GeckoRuntime.create(requireContext(), settings)
    }
    session.apply {
      progressDelegate = this@LivePlayerFragment
      contentDelegate = this@LivePlayerFragment
      permissionDelegate = this@LivePlayerFragment
      navigationDelegate = this@LivePlayerFragment
    }
    session.settings.apply {
      userAgentOverride = USER_AGENT
      allowJavascript = true
    }
    session.open(runtime!!)
    binding.webview.setSession(session)
  }

  override fun onDestroyView() {
    super.onDestroyView()
    session.close()
    binding.webview.releaseSession()
  }
}