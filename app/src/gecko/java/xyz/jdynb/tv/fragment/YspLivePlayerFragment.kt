package xyz.jdynb.tv.fragment

import android.util.Log

/**
 * 央视屏直播播放器
 */
class YspLivePlayerFragment: LivePlayerFragment() {

  companion object {

    private const val TAG = "YspLivePlayerFragment"

  }

  override fun initView() {
    super.initView()
  }

  override fun onLoadUrl(url: String?) {
    Log.i(TAG, "onLoadUrl: $url")
    session.loadUri("$url?pid=" + mainViewModel.currentChannelModel.value?.pid)
  }

}