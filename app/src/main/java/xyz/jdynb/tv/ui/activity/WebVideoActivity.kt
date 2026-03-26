package xyz.jdynb.tv.ui.activity

import androidx.activity.viewModels
import com.drake.engine.base.EngineActivity
import xyz.jdynb.tv.R
import xyz.jdynb.tv.databinding.ActivityWebVideoBinding
import xyz.jdynb.tv.utils.WebViewUtils.setupWebSettings

class WebVideoActivity: EngineActivity<ActivityWebVideoBinding>(R.layout.activity_web_video) {

  private val webVideoViewModel by viewModels<WebVideoViewModel>()

  override fun initData() {
    binding.webview.loadUrl("file:///android_asset/html/video_player.html")
  }

  override fun initView() {
    binding.webview.setupWebSettings()
  }
}