package xyz.jdynb.tv.ui.fragment

import android.webkit.MimeTypeMap
import androidx.core.net.toUri
import com.tencent.smtt.export.external.interfaces.WebResourceRequest
import com.tencent.smtt.export.external.interfaces.WebResourceResponse
import timber.log.Timber
import xyz.jdynb.tv.BuildConfig
import xyz.jdynb.tv.model.LiveChannelModel
import xyz.jdynb.tv.utils.NetworkUtils.inputStream
import xyz.jdynb.tv.utils.SpUtils.remove
import kotlin.random.Random

class SimpleLivePlayerFragment : BaseLivePlayerFragment() {

  companion object {

    private const val PLAYER_URL = "file:///android_asset/html/simple_player.html"
  }

  override fun shouldInterceptRequest(
    url: String,
    request: WebResourceRequest
  ): WebResourceResponse? {
    val shouldIntercept = super.shouldInterceptRequest(url, request)

    val referer = request.requestHeaders["X-Referer"]
    if (BuildConfig.DEBUG) {
      Timber.i("Referer: $referer")
    }

    val isSteamFile = url.contains(".m3u8") || url.contains(".ts")

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

    request.requestHeaders["Referer"] = (referer ?: (urlObj.scheme + "://" + urlObj.host + "/"))

    if (BuildConfig.DEBUG) {
      Timber.i("mimeType: $mimeType, headers: ${request.requestHeaders}, body: $body")
    }

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
  }

  override fun resumeOrPause() {
    webView.evaluateJavascript("resumeOrPause()", null)
  }

  override fun onProgressChanged(newProgress: Int): Int {
    return Random.nextInt(50, 95) // 固定值，表示正在加载中...
  }
}