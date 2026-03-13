package xyz.jdynb.tv.utils

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.drake.engine.utils.AppUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import xyz.jdynb.tv.BuildConfig
import xyz.jdynb.tv.config.Api
import xyz.jdynb.tv.dialog.UpdateDialog
import xyz.jdynb.tv.exception.ResultDataNullException
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
        NetworkUtils.requestSuspend<UpdateModel>(Api.CHECK_UPDATE, mapOf("versionCode" to AppUtils.getAppVersionCode().toString()))
      }
      Log.i(TAG, "updateModel: $updateModel")
      // 发现新版本
      UpdateDialog(context, updateModel).run {
        setCancelable(false)
        setCanceledOnTouchOutside(false)
        show()
      }
    } catch (e: Exception) {
      if (e is ResultDataNullException) {
        Toast.makeText(context, "当前已是最新版本", Toast.LENGTH_SHORT).show()
      } else {
        Toast.makeText(context, "检查更新异常", Toast.LENGTH_SHORT).show()
      }
      Log.e(TAG, "检查更新异常: $e")
    }
  }

}