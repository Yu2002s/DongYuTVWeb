package xyz.jdynb.tv.utils

import android.content.Context
import android.text.TextUtils
import android.util.Log
import com.norman.webviewup.lib.WebViewUpgrade
import com.tencent.smtt.sdk.QbSdk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import xyz.jdynb.tv.DongYuTVApplication
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.Timer
import java.util.TimerTask

object X5Utils {

  private const val TAG = "X5Utils"

  private const val CORE_VERSION = 44286

  private const val INSTALL_RETRY_NUM = 20

  private const val TBS_FOLDER = "tbs"

  private const val ARM_64_CORE_NAME = "tbs_core_046281_20240119145442_nolog_fs_obfs_arm64-v8a_release.tbs"

  private const val ARM_32_CORE_NAME = "tbs_core_046270_20230713164840_nolog_fs_obfs_armeabi_release.tbs"

  /**
   * 校验架构
   */
  fun is32BitAbi(context: Context): Boolean {
    var relativePath = ""
    val nativeLibraryDir: String = context.applicationInfo.nativeLibraryDir
    if (!TextUtils.isEmpty(nativeLibraryDir)) {
      relativePath = when {
        nativeLibraryDir.endsWith("/lib/arm64") -> "lib/arm64-v8a"
        nativeLibraryDir.endsWith("/lib/arm") -> "lib/armeabi-v7a"
        else -> ""
      }
    }
    Log.i("abi_type", relativePath)
    return "lib/armeabi-v7a" == relativePath
  }

  /**
   * 安装本地内核
   */
  suspend fun startInstallX5LocationCore(
    context: Context,
    onProgress: suspend (Int) -> Unit,
    onSucceed: () -> Unit,
    onFailed: (String) -> Unit
  ) {
    val index = intArrayOf(0)
    val coreName = if (is32BitAbi(context)) ARM_32_CORE_NAME else ARM_64_CORE_NAME //内核文件名称
    val appDataPath = context.filesDir.parent //存储路径
    try {
      Log.d(
        TAG, ",内核版本号 CORE_VERSION = " + CORE_VERSION
            + ",内核文件名称 CORE_NAME = " + coreName
            + ",存储路径 path = " + appDataPath
      )
      //复制X5tbs下的内核文件到终端
      val isExitCore: Boolean = withContext(Dispatchers.IO) {
        copyAssetsToDir(context, TBS_FOLDER, appDataPath, onProgress)
      }
      if (isExitCore) {
        val installPath = appDataPath + File.separator + TBS_FOLDER + File.separator + coreName
        Log.d(TAG, "安装内核地址：$installPath")
        //开始安装
        QbSdk.installLocalTbsCore(context, CORE_VERSION, installPath)
        val timer = Timer()
        timer.schedule(object : TimerTask() {
          override fun run() {
            QbSdk.initX5Environment(context, object : QbSdk.PreInitCallback {
              override fun onCoreInitFinished() {
                Log.d(TAG, "onCoreInitFinished")
              }

              override fun onViewInitFinished(p0: Boolean) {
                Log.d(TAG, "onViewInitFinished_p0=$p0")
              }
            })
            val version = QbSdk.getTbsVersion(context)
            if (version > 0) {
              Log.d(TAG, "x5内核安装完成，版本号$version")
              timer.cancel()
              onSucceed()
            } else {
              Log.d(TAG, "循环检验内核版本 " + version + ",计数：" + index[0])
              index[0]++
              //老式的系统版本，例如7.0 8.0这些配置较低的写入比较慢，估需要等待
              if (index[0] > INSTALL_RETRY_NUM) {
                Log.d(TAG, "超过" + INSTALL_RETRY_NUM + "s")
                timer.cancel()
                onFailed("超过" + INSTALL_RETRY_NUM + "s,取消安装")
              }
            }
          }
        }, 0, 1000)
      }
    } catch (e: java.lang.Exception) {
      e.printStackTrace()
      Log.d(TAG, "本地离线内核安装异常,异常信息>" + e.message)
      onFailed("本地离线内核安装异常,异常信息：" + e.message)
    }
  }

  /**
   * 从assets目录中复制整个文件夹内容,如考贝到 /sdcard/包名/files/目录中
   * @param  activity  activity 使用CopyFiles类的Activity
   * @param  assetsFileFolder  String  文件路径,如：/assets/aa
   * @param  sdcardFolder  String  文件路径,如：/sdcard/
   */
  private suspend fun copyAssetsToDir(
    activity: Context,
    assetsFileFolder: String,
    sdcardFolder: String, onProgress: suspend (Int) -> Unit
  ): Boolean {
    var fileFolder = assetsFileFolder
    try {
      val fileList = activity.assets.list(fileFolder)
      if (!fileList.isNullOrEmpty()) { //如果是目录
        val file = File(sdcardFolder + File.separator + fileFolder)
        if (file.isFile && !file.parentFile.exists()) {
          file.parentFile?.mkdirs() //如果文件夹不存在，则递归
        } else {
          file.parentFile?.mkdirs()
          file.mkdirs()
        }
        for (fileName in fileList) {
          fileFolder = fileFolder + File.separator + fileName
          copyAssetsToDir(activity, fileFolder, sdcardFolder, onProgress)
          fileFolder =
            fileFolder.substring(0, fileFolder.lastIndexOf(File.separator))
        }
      } else { //如果是文件
        val inputStream = activity.assets.open(fileFolder)
        val file = File(sdcardFolder + File.separator + fileFolder)
        Log.d(
          TAG,
          "file.exists() ${file.exists()}" + " file.length() ${file.length()}  fileFolder $fileFolder"
        );
        if (!file.exists() || file.length() == 0L) {
          if (!file.parentFile.exists()) {
            file.parentFile?.mkdirs()
          }
          val fileSize = inputStream.available()
          val fos = FileOutputStream(file)
          var len = -1
          val buffer = ByteArray(1024)
          var totalBytesRead = 0
          while (inputStream.read(buffer).also { len = it } != -1) {
            totalBytesRead += len
            fos.write(buffer, 0, len)
            onProgress((totalBytesRead * 100 / fileSize))
          }
          fos.flush()
          inputStream.close()
          fos.close()
          Log.d(TAG, file.path + "文件复制完毕");
        } else {
          Log.d(TAG, file.path + "文件已存在，无需复制");
        }
      }
      return true
    } catch (e: IOException) {
      e.printStackTrace()
    }
    return false
  }

  @JvmStatic
  fun getWebViewVersionNumber(): Int {
    var systemWebViewPackageVersion = WebViewUpgrade.getSystemWebViewPackageVersion()
    if (systemWebViewPackageVersion.isNullOrEmpty()) {
      try {
        val packageInfo =
          DongYuTVApplication.context.packageManager.getPackageInfo("com.google.android.webview", 0)
        systemWebViewPackageVersion = packageInfo.versionName
      } catch (_: Exception) {
        return Int.MAX_VALUE
      }
    }
    val index = systemWebViewPackageVersion.indexOf(".")
    if (index > 0) {
      val version = systemWebViewPackageVersion.substring(0, index).toInt()
      return version
    }
    return Int.MAX_VALUE
  }

}
