package xyz.jdynb.tv.fragment

import xyz.jdynb.tv.enums.JsType
import xyz.jdynb.tv.model.LiveChannelModel
import xyz.jdynb.tv.utils.toArray

/**
 * 通用的播放器
 */
open class BaseLivePlayerFragment: LivePlayerFragment() {

  companion object {

    private const val TAG = "BaseLivePlayerFragment"

  }

  override fun onLoadUrl(url: String?, channelModel: LiveChannelModel) {
    if (url.isNullOrEmpty()) {
      return
    }
    var finialUrl: String = url
    val array = channelModel.args
    array.forEach {
      finialUrl = finialUrl.replace("{{${it.key}}}", it.value)
    }
    webView.loadUrl(finialUrl)
  }

  override fun play(channel: LiveChannelModel) {
    // 默认的播放
    execJs(JsType.PLAY, *channel.toArray())
  }

  override fun resumeOrPause() {
    execJs(JsType.RESUME_PAUSE)
  }

  override fun refresh() {
    super.refresh()
  }

  override fun shouldOverride(url: String): Boolean {
    return false
  }

  override fun onPageFinished(url: String, channelModel: LiveChannelModel) {
    execJs(JsType.INIT, *channelModel.toArray())
  }

}