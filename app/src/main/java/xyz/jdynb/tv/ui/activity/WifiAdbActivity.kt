package xyz.jdynb.tv.ui.activity

import android.os.Build
import android.view.View
import android.widget.Toast
import androidx.annotation.WorkerThread
import androidx.appcompat.app.AlertDialog
import androidx.databinding.Observable
import com.drake.engine.base.EngineToolbarActivity
import io.github.muntashirakon.adb.AdbPairingRequiredException
import io.github.muntashirakon.adb.AdbStream
import io.github.muntashirakon.adb.android.AdbMdns
import kotlinx.coroutines.Job
import timber.log.Timber
import xyz.jdynb.tv.BR
import xyz.jdynb.tv.R
import xyz.jdynb.tv.databinding.ActivityWifiAdbBinding
import xyz.jdynb.tv.model.AdbConnectModel
import xyz.jdynb.tv.utils.AdbConnectionManager
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.InetAddress
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread


class WifiAdbActivity : EngineToolbarActivity<ActivityWifiAdbBinding>(R.layout.activity_wifi_adb) {

  private val adbConnectModel = AdbConnectModel()

  private var job: Job? = null

  private lateinit var adbShellStream: AdbStream

  override fun initData() {
    binding.m = adbConnectModel
    /*thread {
      try {
        AdbConnectionManager.getInstance().autoConnect(this, 10 * 1000L)
      } catch (e: Exception) {
        Timber.e(e, "自动连接超时")
      }
    }*/

    /*job = lifecycleScope.launch {
      repeatOnLifecycle(Lifecycle.State.RESUMED) {
        while (isActive) {
          delay(1000L)
          adbConnectModel.isConnected = AdbConnectionManager.getInstance(this@WifiAdbActivity).isConnected
        }
      }
    }*/

    autoConnect()

    getPairingPort()

    adbConnectModel.addOnPropertyChangedCallback(object : Observable.OnPropertyChangedCallback() {
      override fun onPropertyChanged(
        sender: Observable?,
        propertyId: Int
      ) {
        if (propertyId == BR.connected) {
          runOnUiThread {
            Toast.makeText(this@WifiAdbActivity, adbConnectModel.status, Toast.LENGTH_SHORT).show()
          }
        }
      }
    })
  }

  override fun initView() {
    title = "无线连接"
  }

  private fun pair() {
    thread {
      try {
        val pairingStatus: Boolean
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
          val manager = AdbConnectionManager.getInstance(this)
          pairingStatus =
            manager.pair(adbConnectModel.host, adbConnectModel.port.toInt(), adbConnectModel.code)
        } else pairingStatus = false
        Timber.i("配对结果: %s", pairingStatus)
        autoConnectInternal()
      } catch (e: Exception) {
        Timber.e(e, "配对失败")
      }
    }
  }

  private fun getPairingPort() {
    thread {
      val atomicPort = AtomicInteger(-1)
      val resolveHostAndPort = CountDownLatch(1)

      val adbMdns = AdbMdns(
        application,
        AdbMdns.SERVICE_TYPE_TLS_PAIRING
      ) { hostAddress: InetAddress?, port: Int ->
        atomicPort.set(port)
        resolveHostAndPort.countDown()
      }
      adbMdns.start()
      try {
        if (!resolveHostAndPort.await(1, TimeUnit.MINUTES)) {
          return@thread
        }
      } catch (ignore: InterruptedException) {
      } finally {
        adbMdns.stop()
      }

      adbConnectModel.port = atomicPort.get().toString()
    }
  }

  private fun connect() {
    thread {
      try {
        val manager = AdbConnectionManager.getInstance(this)
        var connectionStatus = false
        try {
          connectionStatus = manager.connect(adbConnectModel.host, adbConnectModel.port.toInt())
        } catch (e: Exception) {
          Timber.e(e, "连接失败")
        }

        adbConnectModel.isConnected = connectionStatus
        if (connectionStatus) {
          Timber.i("已连接")
        } else {
          Timber.i("未连接")
        }
      } catch (e: Exception) {
        runOnUiThread {
          Toast.makeText(this@WifiAdbActivity, "连接失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
        Timber.e(e, "连接失败")
      }
    }
  }

  private fun autoConnect() {
    thread {
      autoConnectInternal()
    }
  }

  @WorkerThread
  private fun autoConnectInternal() {
    try {
      val manager = AdbConnectionManager.getInstance(this)
      var connected = false
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        try {
          connected = manager.autoConnect(this, 5000)
        } catch (e: AdbPairingRequiredException) {
          Timber.w(e, "必须配对")
          return
        } catch (e: Exception) {
          Timber.e(e, "自动连接失败")
        }
      }
      if (!connected) {
        connected =
          manager.connect(adbConnectModel.host, adbConnectModel.port.ifEmpty { "5555" }.toInt())
      }
      if (connected) {
        Timber.i("已连接")
      }
      adbConnectModel.isConnected
    } catch (e: Exception) {
      Timber.e(e)
      /*runOnUiThread {
        Toast.makeText(this@WifiAdbActivity, "自动连接失败", Toast.LENGTH_SHORT).show()
      }*/
    }
  }

  private fun output() {
    val inputStream = adbShellStream.openInputStream()
    val inputStreamReader = InputStreamReader(inputStream)
    val bufferedReader = BufferedReader(inputStreamReader)
    val sb = StringBuilder()
    var line: String?
    while (bufferedReader.readLine().also { line = it } != null) {
      sb.append(line).appendLine()
      Timber.i("message: %s", line)
      adbConnectModel.log = "\n" + line + adbConnectModel.log
      runOnUiThread {
        binding.scrollView.fullScroll(View.FOCUS_UP)
      }
    }
  }

  override fun onClick(v: View) {
    when (v.id) {
      R.id.btn_connect -> {
        // pair()
        connect()
      }

      R.id.btn_install -> {
        thread {
          try {
            val apkPath = packageManager.getApplicationInfo(packageName, 0).sourceDir
            val apkFile = File(apkPath)
            Timber.i("准备推送并安装：%s，大小：%d", apkPath, apkFile.length())
            
            runOnUiThread {
              adbConnectModel.log = "\n正在将APK推送到远程设备...(耐心等待)" + adbConnectModel.log
              Toast.makeText(this, "正在将APK推送到远程设备...", Toast.LENGTH_SHORT).show()
            }

            val manager = AdbConnectionManager.getInstance(this)

            Timber.i("1. 开始推送APK到 /data/local/tmp/temp.apk")
            val pushStream = manager.openStream("exec:cat > /data/local/tmp/temp.apk")
            val outputStream = pushStream.openOutputStream()
            apkFile.inputStream().use { input ->
              input.copyTo(outputStream)
            }
            outputStream.flush()
            outputStream.close()
            
            // cat 指令没有控制台输出，直接读取 inputStream 会导致界面永久阻塞，直接关闭流即可结束占用
            pushStream.close()
            Timber.i("推送结束。")

            Timber.i("2. 开始执行 pm install")
            runOnUiThread {
              Toast.makeText(this, "正在安装...", Toast.LENGTH_SHORT).show()
            }
            
            val installStream = manager.openStream("exec:pm install -r /data/local/tmp/temp.apk")
            val result = installStream.openInputStream().bufferedReader().readText()
            Timber.i("安装结果: %s", result)
            installStream.close()
            
            runOnUiThread {
              adbConnectModel.log = "\n安装结果:\n$result" + adbConnectModel.log
              binding.scrollView.fullScroll(View.FOCUS_UP)
              Toast.makeText(this, "安装结束", Toast.LENGTH_SHORT).show()
            }
            Timber.i("安装流程结束")
          } catch (e: Exception) {
            Timber.e(e, "安装失败")
            runOnUiThread {
              Toast.makeText(this, "安装失败：${e.message}", Toast.LENGTH_SHORT).show()
            }
          }
        }
      }

      R.id.btn_exec -> {
        AlertDialog.Builder(this)
          .setItems(arrayOf("开启自启动", "取消自启动", "设置默认桌面", "取消设置默认桌面")) {dialog, which ->
            when (which) {
              0 -> {
                // 开启自启动：实质上是启用 BootReceiver 组件
                exec("pm enable ${packageName}/.receiver.BootReceiver")
              }
              1 -> {
                // 取消自启动：实质上是禁用 BootReceiver 组件
                exec("pm disable ${packageName}/.receiver.BootReceiver")
              }
              2 -> {
                AlertDialog.Builder(this)
                  .setTitle("提示")
                  .setMessage("谨慎开启！一旦设置了只能手动去取消，无法通过这里控制取消，当然你也可以通过下面的命令取消，前提是你得知道默认桌面的包名")
                  .setNegativeButton("我知道了") { dialog, which ->
                    // 设置默认桌面：需要完整的组件类路径，另外部分高版本系统支持 cmd package 命令
                    exec("cmd package set-home-activity ${packageName}/.ui.activity.SplashActivity")
                  }
                  .setPositiveButton("取消", null)
                  .show()
              }
              3 -> {
                // 取消默认桌面使用 clear-default 清除默认应用设置
                // exec("cmd package clear-default $packageName")
                AlertDialog.Builder(this)
                  .setTitle("提示")
                  .setMessage("打开设备的设置 -> 应用管理 -> 取消默认应用。或者你可以删除本App。")
                  .setNegativeButton("我知道了", null)
                  .show()
              }
            }
          }.show()
      }

      R.id.btn_run -> {
        exec(adbConnectModel.command)
      }
    }
  }

  private fun exec(command: String) {
    thread {
      try {
        val manager = AdbConnectionManager.getInstance(this)
        manager.openStream("exec:$command").use {
          val inputStream = it.openInputStream()
          val inputStreamReader = InputStreamReader(inputStream)
          val bufferedReader = BufferedReader(inputStreamReader)
          val sb = StringBuilder()
          var line: String?
          while (bufferedReader.readLine().also { line = it } != null) {
            sb.append(line).appendLine()
            Timber.i("message: %s", line)
            runOnUiThread {
              adbConnectModel.log = "\n" + line + adbConnectModel.log
              binding.scrollView.fullScroll(View.FOCUS_UP)
            }
          }
          runOnUiThread {
            Toast.makeText(this, "命令执行完毕", Toast.LENGTH_SHORT).show()
          }
        }
      } catch (e: Exception) {
        Timber.e(e, "执行命令失败")
        runOnUiThread {
          Toast.makeText(this, "执行命令失败：${e.message}", Toast.LENGTH_SHORT).show()
        }
      }
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    job?.cancel(null)

    if (::adbShellStream.isInitialized && !adbShellStream.isClosed) {
      try {
        adbShellStream.close()
      } catch (e: Exception) {
      }
    }

    try {
      AdbConnectionManager.getInstance(this).close()
    } catch (e: Exception) {

    }
  }
}