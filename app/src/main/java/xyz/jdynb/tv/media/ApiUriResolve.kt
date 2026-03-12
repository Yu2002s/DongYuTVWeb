package xyz.jdynb.tv.media

import android.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.ResolvingDataSource
import xyz.jdynb.tv.utils.ParseUtils

@UnstableApi
class ApiUriResolve : ResolvingDataSource.Resolver {

  companion object {

    private const val TAG = "ApiUriResolve"

  }

  @Throws(Exception::class)
  override fun resolveDataSpec(dataSpec: DataSpec): DataSpec {
    // 只处理 HTTP/HTTPS 请求
    if (dataSpec.uri.scheme != "http" && dataSpec.uri.scheme != "https") {
      return dataSpec
    }

    // 如果是已经解析过的真实地址（包含特定域名或路径），直接返回
    val uriString = dataSpec.uri.toString()
    // Log.i(TAG, "原始请求 URI: $uriString")

    // 检查是否是 m3u8 或 ts 文件请求
    val isM3u8 = uriString.contains(".m3u8", ignoreCase = true)
    val isTS = uriString.contains(".ts", ignoreCase = true)

    if (isTS || isM3u8 || uriString.contains("cdn.hls.one")) {
      return dataSpec
    }

    // 只对初始的 m3u8 请求进行 API 解析，后续的 ts 分片不解析
    /*if (!isM3u8 || uriString.startsWith("http://")) { // 假设真实地址是 http 开头
      Log.d(TAG, "非 m3u8 请求或已是真实地址，跳过解析")
      return dataSpec
    }*/

    var url: String
    try {
      url = ParseUtils.parseVideo(uriString) ?: throw NullPointerException("解析失败")
      Log.i(TAG, "解析后的真实 URL: $url")
    } catch (e: Exception) {
      Log.e(TAG, "API 解析失败", e)
      url = ParseUtils.parseVideoLocal(uriString) ?: throw e
    }

    // 使用真实 URL 重建 DataSpec
    return dataSpec.buildUpon()
      .setUri(url)
      .build()
  }
}