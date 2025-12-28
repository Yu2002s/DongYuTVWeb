package xyz.jdynb.tv

import android.app.ProgressDialog
import android.media.AudioManager
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.drake.engine.base.EngineActivity
import com.norman.webviewup.lib.UpgradeCallback
import com.norman.webviewup.lib.WebViewUpgrade
import com.norman.webviewup.lib.source.download.UpgradeDownloadSource
import kotlinx.coroutines.launch
import xyz.jdynb.tv.DongYuTVApplication.Companion.context
import xyz.jdynb.tv.databinding.ActivityMainBinding
import xyz.jdynb.tv.dialog.ChannelListDialog
import xyz.jdynb.tv.fragment.LivePlayerFragment
import xyz.jdynb.tv.fragment.YspLivePlayerFragment
import java.io.File

class MainActivity : EngineActivity<ActivityMainBinding>(R.layout.activity_main) {

  companion object {

    private const val TAG = "MainActivity"

    private const val WEBVIEW_MIN_VERSION = 106

    /**
     * WebView 内核下载地址
     *
     * 这个文件只支持 arm64v8a,arm64v7a android 6 以上系统
     */
    private const val WEBVIEW_DOWNLOAD_URL =
      "https://lz.qaiu.top/parser?url=https://jdy2002.lanzoue.com/iWgPm3ep91be"

    /**
     * WebView 内核文件名
     */
    private const val WEBVIEW_FILE_NAME = "com.google.android.webview_106.0.5249.65-524906503.apk"
  }

  private val livePlayerFragment: LivePlayerFragment = YspLivePlayerFragment()

  private lateinit var channelListDialog: ChannelListDialog

  private val mainViewModel by viewModels<MainViewModel>()

  private lateinit var audioManager: AudioManager

  private var lastBackTime = 0L

  override fun init() {
    super.init()
    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    val insetsController = WindowCompat.getInsetsController(window, window.decorView)
    insetsController.hide(WindowInsetsCompat.Type.systemBars())

    audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
  }

  override fun initView() {
    binding.m = mainViewModel
    binding.lifecycleOwner = this

    channelListDialog = ChannelListDialog(this, mainViewModel)

    val systemWebViewPackageName = WebViewUpgrade.getSystemWebViewPackageName()
    val systemWebViewPackageVersion = WebViewUpgrade.getSystemWebViewPackageVersion()

    Log.i(TAG, "systemWebViewPackageName: $systemWebViewPackageName, $systemWebViewPackageVersion")

    val index = systemWebViewPackageVersion.indexOf(".")
    if (index > 0) {
      val version = systemWebViewPackageVersion.substring(0, index).toInt()
      if (version < WEBVIEW_MIN_VERSION) {
        val upgradeSource = UpgradeDownloadSource(
          context,
          WEBVIEW_DOWNLOAD_URL,
          File(filesDir, WEBVIEW_FILE_NAME)
        )
        Log.i(TAG, "start upgrade: $version")
        WebViewUpgrade.upgrade(upgradeSource)

        var progressDialog: ProgressDialog? = null

        WebViewUpgrade.addUpgradeCallback(object : UpgradeCallback {
          override fun onUpgradeComplete() {
            Log.i(TAG, "onUpgradeComplete")
            progressDialog?.dismiss()
            Toast.makeText(this@MainActivity, "内核更新完成！", Toast.LENGTH_LONG).show()

            initLivePlayerFragment()
          }

          override fun onUpgradeError(throwable: Throwable?) {
            Log.i(TAG, "throwable: $throwable")
            Toast.makeText(
              this@MainActivity,
              "内核更新错误: ${throwable?.message}",
              Toast.LENGTH_LONG
            ).show()
            progressDialog?.dismiss()
          }

          override fun onUpgradeProcess(percent: Float) {
            Log.i(TAG, "onUpgradeProcess: $percent")
            if (progressDialog == null) {
              progressDialog = ProgressDialog(this@MainActivity)
                .apply {
                  setTitle("正在下载内核...")
                  setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
                  setCancelable(false)
                  setCanceledOnTouchOutside(false)
                  show()
                }
            }

            progressDialog.progress = (percent * 100).toInt()
          }
        })

        if (WebViewUpgrade.isCompleted()) {
          initLivePlayerFragment()
        }

        return
      }
    }

    Log.i(TAG, "WebView Version > 106, no need to upgrade")

    initLivePlayerFragment()
  }

  private fun initLivePlayerFragment() {
    supportFragmentManager.beginTransaction()
      .replace(R.id.fragment, livePlayerFragment)
      .commitNow()
  }

  override fun initData() {
    lifecycleScope.launch {
      mainViewModel.currentChannelModel.collect {
        Log.i(TAG, "currentChannelModel: $it")
      }
    }
  }

  override fun onClick(v: View) {
    super.onClick(v)
    when (v.id) {
      R.id.btn_menu -> {
        if (mainViewModel.channelModelList.value.isEmpty()) {
          return
        }
        channelListDialog.show()
      }

      R.id.btn_left -> {
        mainViewModel.down()
      }

      R.id.btn_right -> {
        mainViewModel.up()
      }
    }
  }

  override fun dispatchTouchEvent(event: MotionEvent): Boolean {
    mainViewModel.showActions()
    return super.dispatchTouchEvent(event)
  }

  /**
   * 事件分发时就拦截，避免事件被 webview 拦截
   */
  override fun dispatchKeyEvent(event: KeyEvent): Boolean {
    Log.i(TAG, "dispatchKeyEvent: ${event.keyCode}")
    val keyCode = event.keyCode
    val action = event.action
    if (action != KeyEvent.ACTION_DOWN) {
      return super.dispatchKeyEvent(event)
    }
    when (keyCode) {
      /**
       * 上
       */
      KeyEvent.KEYCODE_DPAD_UP -> {
        if (channelListDialog.isShowing) {
          return true
        }
        mainViewModel.up()
      }

      /**
       * 下
       */
      KeyEvent.KEYCODE_DPAD_DOWN -> {
        if (channelListDialog.isShowing) {
          return true
        }
        mainViewModel.down()
      }

      // ENTER、OK（确认）
      KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_SPACE -> {
        if (channelListDialog.isShowing) {
          return true
        }
        Log.d(TAG, "onKeyDown: Ok")
        livePlayerFragment.playOrPause()
      }

      // 静音
      KeyEvent.KEYCODE_MUTE -> {
        try {
          audioManager.setStreamVolume(
            AudioManager.STREAM_SYSTEM,
            0,
            AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE
          )
        } catch (_: SecurityException) {
        }
      }

      KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.KEYCODE_DPAD_LEFT -> {
        if (channelListDialog.isShowing) {
          return true
        }
        try {
          audioManager.adjustStreamVolume(
            AudioManager.STREAM_MUSIC,
            AudioManager.ADJUST_LOWER,
            AudioManager.FLAG_SHOW_UI
          )
        } catch (_: SecurityException) {
        }
      }

      KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_DPAD_RIGHT -> {
        if (channelListDialog.isShowing) {
          return true
        }
        val volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        if (volume < audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)) {
          try {
            audioManager.adjustStreamVolume(
              AudioManager.STREAM_MUSIC,
              AudioManager.ADJUST_RAISE,
              AudioManager.FLAG_SHOW_UI
            )
          } catch (e: SecurityException) {
            Log.e(TAG, e.message.toString())
          }
        }
      }

      // 返回
      KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_ESCAPE -> {
        if (channelListDialog.isShowing) {
          return true
        }
        if (mainViewModel.showCurrentChannel.value) {
          // 如果显示了当前频道
          mainViewModel.showCurrentChannel(false)
          mainViewModel.rollbackIndex() // 回滚之前的频道
        } else {
          if (System.currentTimeMillis() - lastBackTime > 2000) {
            lastBackTime = System.currentTimeMillis()
            Toast.makeText(this, "再按一次返回键退出", Toast.LENGTH_SHORT).show()
          } else {
            finish()
          }
        }
      }

      // 主页
      KeyEvent.KEYCODE_HOME -> {

      }

      // 菜单
      KeyEvent.KEYCODE_MENU, KeyEvent.KEYCODE_P -> {
        if (channelListDialog.isShowing) {
          return true
        }
        binding.btnMenu.callOnClick()
      }

      // 0
      // 数字
      KeyEvent.KEYCODE_0, KeyEvent.KEYCODE_1, KeyEvent.KEYCODE_2, KeyEvent.KEYCODE_3,
      KeyEvent.KEYCODE_4, KeyEvent.KEYCODE_5, KeyEvent.KEYCODE_6, KeyEvent.KEYCODE_7,
      KeyEvent.KEYCODE_8, KeyEvent.KEYCODE_9,
      KeyEvent.KEYCODE_NUMPAD_0, KeyEvent.KEYCODE_NUMPAD_1, KeyEvent.KEYCODE_NUMPAD_2,
      KeyEvent.KEYCODE_NUMPAD_3, KeyEvent.KEYCODE_NUMPAD_4, KeyEvent.KEYCODE_NUMPAD_5,
      KeyEvent.KEYCODE_NUMPAD_6, KeyEvent.KEYCODE_NUMPAD_7, KeyEvent.KEYCODE_NUMPAD_8,
      KeyEvent.KEYCODE_NUMPAD_9 -> {
        val num = getNumForKeyCode(keyCode)

        Log.i(TAG, "input number: $num")

        mainViewModel.appendNumber(num)
      }
    }
    return super.dispatchKeyEvent(event)
  }

  private fun getNumForKeyCode(keyCode: Int): String {
    return when (keyCode) {
      KeyEvent.KEYCODE_0, KeyEvent.KEYCODE_NUMPAD_0 -> "0"
      KeyEvent.KEYCODE_1, KeyEvent.KEYCODE_NUMPAD_1 -> "1"
      KeyEvent.KEYCODE_2, KeyEvent.KEYCODE_NUMPAD_2 -> "2"
      KeyEvent.KEYCODE_3, KeyEvent.KEYCODE_NUMPAD_3 -> "3"
      KeyEvent.KEYCODE_4, KeyEvent.KEYCODE_NUMPAD_4 -> "4"
      KeyEvent.KEYCODE_5, KeyEvent.KEYCODE_NUMPAD_5 -> "5"
      KeyEvent.KEYCODE_6, KeyEvent.KEYCODE_NUMPAD_6 -> "6"
      KeyEvent.KEYCODE_7, KeyEvent.KEYCODE_NUMPAD_7 -> "7"
      KeyEvent.KEYCODE_8, KeyEvent.KEYCODE_NUMPAD_8 -> "8"
      KeyEvent.KEYCODE_9, KeyEvent.KEYCODE_NUMPAD_9 -> "9"
      else -> ""
    }
  }
}