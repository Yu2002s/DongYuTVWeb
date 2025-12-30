package xyz.jdynb.tv.fragment

class SimpleLivePlayerFragment : LivePlayerFragment() {

  override fun onLoadUrl(url: String?) {
    webView.loadUrl("file:///android_asset/html/simple_player.html")
  }
}