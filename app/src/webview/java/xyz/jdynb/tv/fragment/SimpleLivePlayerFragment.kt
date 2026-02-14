package xyz.jdynb.tv.fragment

import android.util.Log
import android.webkit.MimeTypeMap
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import androidx.core.net.toUri
import xyz.jdynb.music.utils.SpUtils.remove
import xyz.jdynb.tv.bridge.JSBridge
import xyz.jdynb.tv.model.LiveChannelModel
import xyz.jdynb.tv.utils.NetworkUtils.inputStream

class SimpleLivePlayerFragment : BaseLivePlayerFragment() {

  companion object {

    private const val TAG = "SimpleLivePlayer"

    // private const val PLAYER_URL = "https://gitee.com/jdy2002/DongYuTvWeb/raw/master/app/src/main/assets/html/simple_player.html"
    private const val PLAYER_URL = "file:///android_asset/html/simple_player.html"
  }

  override fun shouldInterceptRequest(
    url: String,
    request: WebResourceRequest
  ): WebResourceResponse? {
    Log.i(TAG, "${request.method} url: $url")

    val shouldIntercept = super.shouldInterceptRequest(url, request)
    if (url.endsWith("dy-crypto-js.min.js")) {
      // 注入 CRYPTO.JS
      return createJsResponse("js/lib/dy-crypto-js.min.js")
    } else if (url.endsWith("dy-http-util.js")) {
      // 注入网络请求 JS
      return createJsResponse("js/lib/dy-http-util.js")
    } else if (url.endsWith("dy-hls.min.js")) {
      // 注入 HLS.JS
      return createJsResponse("js/lib/dy-hls.min.js")
    }

    val referer = request.requestHeaders["X-Referer"]
    Log.i(TAG, "Referer: $referer")

    val isSteamFile = url.contains(".m3u8") || url.contains(".ts")

    /*if (isSteamFile) {
      Log.i(TAG, "streamFile: $url")
      return createResponse(url, "application/x-mpegURL", request.requestHeaders)
    }*/

    if (referer.isNullOrEmpty() && !isSteamFile) {
      // 没有自定义 Referer 加上 非 M3U8、TS 才进行默认请求，否则下面进行重写
      return shouldIntercept
    }

    // 自定义网络请求 Referer

    request.requestHeaders["X-Referer"]?.remove()
    val body = request.requestHeaders["X-Body"]
    request.requestHeaders["X-Body"]?.remove()

    val urlObj = url.toUri()
    val extension = urlObj.path?.substringAfterLast(".") ?: return shouldIntercept
    val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)

    request.requestHeaders.put("Referer", (referer ?: (urlObj.scheme + "://" + urlObj.host + "/")))

    Log.i(TAG, "headers: ${request.requestHeaders}")

    return WebResourceResponse(
      mimeType, "UTF-8", url
        .inputStream(
          method = request.method,
          request.requestHeaders,
          body = body
        )
    )
  }

  override fun onLoadUrl(url: String?, channelModel: LiveChannelModel) {
    webView.loadUrl(PLAYER_URL)
    webView.addJavascriptInterface(JSBridge(requireContext()), "JSBridge")
  }

  override fun onPageFinished(url: String, channelModel: LiveChannelModel) {
    super.onPageFinished(url, channelModel)

    // 调试代码
    /*requireContext().assets.open("js/jiangxi/init.js").use {
      it.readBytes().toString(Charsets.UTF_8)
    }.let {
      val js = it.replace("{{m3u8Name}}", "tv_jxtv1.m3u8")
      webView.evaluateJavascript(js, null)
    }*/
  }

  override fun play(channel: LiveChannelModel) {
    super.play(channel)
  }

  override fun resumeOrPause() {
    webView.evaluateJavascript("resumeOrPause()", null)
  }
}