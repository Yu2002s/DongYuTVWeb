package xyz.jdynb.tv.fragment

import android.os.Bundle
import android.util.Log
import android.view.View
import xyz.jdynb.tv.enums.JsType
import xyz.jdynb.tv.model.LiveChannelModel

/**
 * 央视频直播播放器实现
 */
class YspLivePlayerFragment : LivePlayerFragment() {

  companion object {

    private const val YSP_HOME = "https://www.yangshipin.cn/tv/home"

    private const val TAG = "YspLivePlayerFragment"

  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
  }

  override fun onLoadUrl(url: String?) {
    Log.i(TAG, "url: $url?pid=${mainViewModel.currentChannelModel.value!!.pid}")
    webView.loadUrl("${url}?pid=${mainViewModel.currentChannelModel.value!!.pid}")
  }

  /**
   * 播放指定直播
   *
   * @param  channel 直播频道
   */
  override fun play(channel: LiveChannelModel) {
    Log.i(TAG, "play: $channel")
    execJs(JsType.PLAY, "pid" to channel.pid, "streamId" to channel.streamId)
  }

  /**
   * 播放或暂停
   */
  override fun resumeOrPause() {
    super.resumeOrPause()
  }

  /**
   * 页面加载完成时的回调
   *
   * @param url 加载的 url
   */
  override fun onPageFinished(url: String) {
    val currentChannelModel = mainViewModel.currentChannelModel.value ?: return
    super.onPageFinished(url)

    /*requireContext().assets.open("js/ysp/init.js").use {
      it.readBytes().toString(Charsets.UTF_8)
    }.let {
      val js = it.replace("{{pid}}", currentChannelModel.pid.toString())
        .replace("{{streamId}}", currentChannelModel.streamId.toString())
      webView.evaluateJavascript(js, null)
    }*/
  }
}