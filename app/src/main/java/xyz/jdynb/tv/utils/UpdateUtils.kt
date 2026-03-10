package xyz.jdynb.tv.utils

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.drake.engine.utils.AppUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import xyz.jdynb.tv.BuildConfig
import xyz.jdynb.tv.dialog.UpdateDialog
import xyz.jdynb.tv.model.UpdateModel
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

object UpdateUtils {

  /**
   * 检查更新地址
   */
  private const val CHECK_UPDATE_URL =
    "https://gitee.com/jdy2002/DongYuTvWeb/raw/master/update.json"
  /**
   * 标签
   */
  private const val TAG = "UpdateUtils"

  /**
   * 检查更新，TV 端目前不知道能不能用
   *
   * @param context 上下文
   */
  suspend fun checkUpdate(context: Context) {
    try {
      val updateModel = withContext(Dispatchers.IO) {
        if (BuildConfig.DEBUG) {
          return@withContext UpdateModel(
            versionCode = 9999,
            url = "https://lz.qaiu.top/parser?url=https://jdy2002.lanzoue.com/iU10g3fp8mpe"
          )
        }
        val connection: HttpURLConnection =
          URL(CHECK_UPDATE_URL).openConnection() as HttpURLConnection
        connection.inputStream.use { inputStream ->
          val content = inputStream.readBytes().toString(StandardCharsets.UTF_8)
          Log.i(TAG, "content: $content")
          val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
          }
          json.decodeFromString<UpdateModel>(content)
        }
      }
      Log.i(TAG, "updateModel: $updateModel")
      if (AppUtils.getAppVersionCode() < updateModel.versionCode) {
        // 发现新版本
        UpdateDialog(context, updateModel).run {
          setCancelable(false)
          setCanceledOnTouchOutside(false)
          show()
        }
      } else {
        Toast.makeText(context, "当前已是最新版本", Toast.LENGTH_SHORT).show()
      }
    } catch (e: Exception) {
      Log.e(TAG, "检查更新异常: $e")
    }
  }

}