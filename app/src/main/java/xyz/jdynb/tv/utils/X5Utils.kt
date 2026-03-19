package xyz.jdynb.tv.utils

import android.content.Context
import android.text.TextUtils
import android.util.Log
import com.norman.webviewup.lib.WebViewUpgrade
import com.tencent.smtt.sdk.QbSdk
import xyz.jdynb.tv.DongYuTVApplication
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.Timer
import java.util.TimerTask

/**
 * 校验架构
 */
fun is32BitAbi(context: Context): Boolean {
  var relativePath = ""
  val nativeLibraryDir: String = context.applicationInfo.nativeLibraryDir
  if (!TextUtils.isEmpty(nativeLibraryDir)) {
    relativePath=when{
      nativeLibraryDir.endsWith("/lib/arm64")-> "lib/arm64-v8a"
      nativeLibraryDir.endsWith("/lib/arm")->"lib/armeabi-v7a"
      else -> ""
    }
  }
  Log.i("abi_type", relativePath)
  return "lib/armeabi-v7a" == relativePath
}
/**
 * 安装本地内核
 */
fun startInstallX5LocationCore(
  context: Context,
  onSucceed: () -> Unit,
  onFailed: (String) -> Unit
) {
  val num = 20
  val index = intArrayOf(0)
  val CORE_VERSION = 44286 //内核版本号
//    val CORE_NAME =  "tbs_core_046270_20230713164840_nolog_fs_obfs_armeabi_release.tbs"
//    val CORE_NAME =  "tbs_core_046281_20240119145442_nolog_fs_obfs_arm64-v8a_release.tbs" //内核文件名称
  val CORE_NAME = if (is32BitAbi(context)) "tbs_core_046270_20230713164840_nolog_fs_obfs_armeabi_release.tbs" else "tbs_core_046281_20240119145442_nolog_fs_obfs_arm64-v8a_release.tbs" //内核文件名称
  val path = context.filesDir.parent //存储路径
  val fileFolder = "tbs"
  Log.d(
    "X5", ",context.filesDir " + context.filesDir.parent
  )
  try {
    Log.d(
      "X5", ",内核版本号 CORE_VERSION = " + CORE_VERSION
          + ",内核文件名称 CORE_NAME = " + CORE_NAME
          + ",存储路径 path = " + path
    )
    //复制X5tbs下的内核文件到终端
    val isExitCore: Boolean = copyAssetsToDir(context, fileFolder, path, onProgress = {
    })
    if (isExitCore) {
      val installPath = path + File.separator + fileFolder + File.separator + CORE_NAME
      Log.d("X5", "安装内核地址：$installPath")
      //开始安装
      QbSdk.installLocalTbsCore(context, CORE_VERSION, installPath)
      val timer = Timer()
      timer.schedule(object : TimerTask() {
        override fun run() {
          QbSdk.initX5Environment(context, object : QbSdk.PreInitCallback {
            override fun onCoreInitFinished() {
              Log.d("X5", "onCoreInitFinished")
            }

            override fun onViewInitFinished(p0: Boolean) {
              Log.d("X5", "onViewInitFinished_p0=$p0")
            }
          })
          val version = QbSdk.getTbsVersion(context)
          if (version > 0) {
            Log.d("X5", "x5内核安装完成，版本号$version")
            timer.cancel()
            onSucceed()
          } else {
            Log.d("X5", "循环检验内核版本 " + version + ",计数：" + index[0])
            index[0]++
            //老式的系统版本，例如7.0 8.0这些配置较低的写入比较慢，估需要等待
            if (index[0] > num) {
              Log.d("X5", "超过" + num + "s")
              timer.cancel()
              onFailed("超过" + num + "s,取消安装")
            }
          }
        }
      }, 0, 1000)
    }
  } catch (e: java.lang.Exception) {
    e.printStackTrace()
    Log.d("X5", "本地离线内核安装异常,异常信息>" + e.message)
    onFailed("本地离线内核安装异常,异常信息：" + e.message)
  }
}

/**
 * 从assets目录中复制整个文件夹内容,如考贝到 /sdcard/包名/files/目录中
 * @param  activity  activity 使用CopyFiles类的Activity
 * @param  assetsFileFolder  String  文件路径,如：/assets/aa
 * @param  sdcardFolder  String  文件路径,如：/sdcard/
 */
private fun copyAssetsToDir(
  activity: Context,
  assetsFileFolder: String,
  sdcardFolder: String, onProgress: (Int) -> Unit
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
        copyAssetsToDir(activity, fileFolder, sdcardFolder,onProgress)
        fileFolder =
          fileFolder.substring(0, fileFolder.lastIndexOf(File.separator))
      }
    } else { //如果是文件
      val inputStream = activity.assets.open(fileFolder)
      val file = File(sdcardFolder + File.separator + fileFolder)
      Log.d("TAG","file.exists() ${file.exists()}"+" file.length() ${file.length()}  fileFolder $fileFolder");
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
        Log.d("TAG",file.path +"文件复制完毕");
      } else {
        Log.d("TAG",file.path +"文件已存在，无需复制");
      }
    }
    return true
  } catch (e: IOException) {
    e.printStackTrace()
  }
  return false
}

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
