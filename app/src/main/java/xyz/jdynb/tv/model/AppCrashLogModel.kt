package xyz.jdynb.tv.model

import android.os.Build
import com.drake.engine.utils.AppUtils
import com.drake.engine.utils.NetworkUtils
import kotlinx.serialization.Serializable

@Serializable
data class AppCrashLogModel(
  var content: String = "",
  var deviceModel: String = Build.MODEL,
  var appVersionCode: Int = AppUtils.getAppVersionCode(),
  var systemVersion: String = Build.VERSION.INCREMENTAL + "," + Build.VERSION.RELEASE,
  var systemSdk: Int = Build.VERSION.SDK_INT,
  var cpuAbis: String = Build.SUPPORTED_ABIS.joinToString(","),
  var networkType: String = NetworkUtils.getNetworkType().name
)
