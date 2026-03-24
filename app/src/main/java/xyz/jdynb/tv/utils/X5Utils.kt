package xyz.jdynb.tv.utils

import android.content.Context
import android.text.TextUtils
import android.util.Log
import com.norman.webviewup.lib.WebViewUpgrade
import com.tencent.smtt.sdk.QbSdk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Response
import okhttp3.internal.headersContentLength
import timber.log.Timber
import xyz.jdynb.tv.BuildConfig
import xyz.jdynb.tv.DongYuTVApplication
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.URL
import java.util.Timer
import java.util.TimerTask
import kotlin.jvm.Throws

object X5Utils {

  private const val TAG = "X5Utils"

  private const val CORE_VERSION = 44286

  private const val INSTALL_RETRY_NUM = 20

  private const val TBS_FOLDER = "tbs"

  private const val ARM_64_CORE_NAME =
    "tbs_core_046281_20240119145442_nolog_fs_obfs_arm64-v8a_release.tbs"

  private const val ARM_32_CORE_NAME =
    "tbs_core_046270_20230713164840_nolog_fs_obfs_armeabi_release.tbs"

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

  suspend fun downloadCoreFile(fileName: String, url: String, onProgress: suspend (Int) -> Unit): String {
    return withContext(Dispatchers.IO) {
      val response = NetworkUtils.requestSuspendResponseResult(url).getOrThrow()
      val responseBody = response.body ?: throw IOException("Response body is null")
      val inputStream = responseBody.byteStream()
      val contentLength = response.headersContentLength()
      val cacheFile = DongYuTVApplication.context.externalCacheDir!!
      if (!cacheFile.exists()) {
        cacheFile.mkdirs()
      }
      val tbsFilePath = File(cacheFile, fileName)
      val fileOutputStream = FileOutputStream(tbsFilePath)
      fileOutputStream.use { outputStream ->
        val buffer = ByteArray(1024)
        var len = -1
        var totalBytesRead = 0
        while (inputStream.read(buffer).also { len = it } != -1) {
          totalBytesRead += len
          outputStream.write(buffer, 0, len)
          onProgress((totalBytesRead * 100 / contentLength).toInt())
        }
      }
      inputStream.close()
      tbsFilePath.path
    }
  }

  suspend fun installFromNetwork(
    context: Context,
    url: String,
    onProgress: suspend (Int) -> Unit,
    onSucceed: () -> Unit,
    onFailed: (String) -> Unit
  ) {
    try {
      val installPath = downloadCoreFile("x5.tbs", url, onProgress)
      installTbsCore(context = context, installPath = installPath, onSucceed = onSucceed, onFailed = onFailed)
    } catch (e: Exception) {
      onFailed(e.message ?: "下载失败")
    }
  }

  /**
   * 安装本地内核
   */
  suspend fun installFromAssets(
    context: Context,
    onProgress: suspend (Int) -> Unit,
    onSucceed: () -> Unit,
    onFailed: (String) -> Unit
  ) {
    val coreName = if (is32BitAbi(context)) ARM_32_CORE_NAME else ARM_64_CORE_NAME //内核文件名称
    val appDataPath = context.filesDir.parent //存储路径
    try {
      Timber.tag(TAG).d(
        ",内核版本号 CORE_VERSION = " + CORE_VERSION
            + ",内核文件名称 CORE_NAME = " + coreName
            + ",存储路径 path = " + appDataPath
      )
      //复制X5tbs下的内核文件到终端
      val isExitCore: Boolean = withContext(Dispatchers.IO) {
        copyAssetsToDir(context, TBS_FOLDER, appDataPath, onProgress)
      }
      if (isExitCore) {
        val installPath = appDataPath + File.separator + TBS_FOLDER + File.separator + coreName
        Timber.i("installPath: $installPath")
        installTbsCore(context = context, installPath= installPath, onSucceed = onSucceed, onFailed = onFailed)
      }
    } catch (e: java.lang.Exception) {
      e.printStackTrace()
      onFailed("本地离线内核安装异常,异常信息：" + e.message)
    }
  }

  @Throws(Exception::class)
  private fun installTbsCore(
    context: Context,
    installPath: String,
    onSucceed: () -> Unit,
    onFailed: (String) -> Unit
  ) {
    Log.d(TAG, "安装内核地址：$installPath")
    val index = intArrayOf(0)
    //开始安装
    QbSdk.installLocalTbsCore(context, CORE_VERSION, installPath)
    val timer = Timer()
    timer.schedule(object : TimerTask() {
      override fun run() {
        QbSdk.initX5Environment(context, object : QbSdk.PreInitCallback {
          override fun onCoreInitFinished() {
            Timber.tag(TAG).d("onCoreInitFinished")
          }

          override fun onViewInitFinished(p0: Boolean) {
            Timber.tag(TAG).d("onViewInitFinished_p0=$p0")
          }
        })
        val version = QbSdk.getTbsVersion(context)
        if (version > 0) {
          Timber.tag(TAG).d("x5内核安装完成，版本号$version")
          timer.cancel()
          onSucceed()
        } else {
          Timber.tag(TAG).d("循环检验内核版本 " + version + ",计数：" + index[0])
          index[0]++
          //老式的系统版本，例如7.0 8.0这些配置较低的写入比较慢，估需要等待
          if (index[0] > INSTALL_RETRY_NUM) {
            Timber.tag(TAG).d("超过" + INSTALL_RETRY_NUM + "s")
            timer.cancel()
            onFailed("超过" + INSTALL_RETRY_NUM + "s,取消安装")
          }
        }
      }
    }, 0, 1000)
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
          Timber.d("开始复制文件 ${file.name}")
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
          Timber.d( file.path + "文件已存在，无需复制");
        }
      }
      return true
    } catch (e: IOException) {
      Timber.e(e)
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
