package xyz.jdynb.tv.fragment

import android.os.Bundle
import android.util.Log
import android.view.View
import xyz.jdynb.tv.enums.JsType
import xyz.jdynb.tv.model.LiveChannelModel

@Deprecated("使用 BaseLivePlayerFragment 替代", replaceWith = ReplaceWith("BaseLivePlayerFragment"))
class YspLivePlayerFragment : BaseLivePlayerFragment() {

  companion object {
    private const val TAG = "YspLivePlayerFragment"

  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
  }

  override fun onLoadUrl(url: String?, channelModel: LiveChannelModel) {
    // Log.i(TAG, "url: $url?pid=${mainViewModel.currentChannelModel.value!!.pid}")
    // webView.loadUrl("${url}?pid=${mainViewModel.currentChannelModel.value!!.pid}")
  }

  /**
   * 播放指定直播
   *
   * @param  channel 直播频道
   */
  override fun play(channel: LiveChannelModel) {
    Log.i(TAG, "play: $channel")
    // execJs(JsType.PLAY, "pid" to channel.pid, "streamId" to channel.streamId)
  }
}