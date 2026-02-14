package xyz.jdynb.tv

import android.Manifest
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.drake.engine.base.EngineActivity
import kotlinx.coroutines.launch
import xyz.jdynb.tv.databinding.ActivityMainBinding
import xyz.jdynb.tv.dialog.ChannelListDialog
import xyz.jdynb.tv.fragment.LivePlayerFragment
import xyz.jdynb.tv.utils.WebViewUpgrade
import kotlin.system.exitProcess
import androidx.core.net.toUri
import androidx.core.view.WindowInsetsControllerCompat

class MainActivity : EngineActivity<ActivityMainBinding>(R.layout.activity_main) {

  companion object {

    private const val TAG = "MainActivity"

  }

  /**
   * 当前显示的 LivePlayerFragment
   */
  private var livePlayerFragment: LivePlayerFragment? = null

  private lateinit var channelListDialog: ChannelListDialog

  private val mainViewModel by viewModels<MainViewModel>()

  private lateinit var audioManager: AudioManager

  /**
   * 最后一次按下返回键的时间
   */
  private var lastBackTime = 0L

  /**
   * 是否已经更新内核了
   */
  private var isUpgrade = false

  override fun init() {
    super.init()
    // 悬浮窗权限改为手动授权，不对用户展示，以免影响体验
    /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
      val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
      val activities = packageManager.queryIntentActivities(intent, 0)
      if (activities.isNotEmpty()) {
        intent.data = "package:$packageName".toUri()
        AlertDialog.Builder(this)
          .setTitle("需要悬浮窗权限")
          .setMessage("请授予悬浮窗权限，用于实现开机自启动\n\n按左右方向键选择【确认】或取消【取消】，不开启直接按返回键")
          .setPositiveButton("确定") { _, _ ->
            startActivity(intent)
          }
          .setNegativeButton("取消", null)
          .show()
      }
    }*/

    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    val insetsController = WindowCompat.getInsetsController(window, window.decorView)
    insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    insetsController.hide(WindowInsetsCompat.Type.systemBars())

    // 判断cpu型号决定需不需要升级
    isUpgrade = !Build.SUPPORTED_ABIS.any {
      it.contains("arm64")
    }

    Log.i(TAG, "abi: ${Build.SUPPORTED_ABIS.joinToString(",")}")

    audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
  }

  override fun initView() {
    binding.m = mainViewModel
    binding.lifecycleOwner = this

    channelListDialog = ChannelListDialog(this, mainViewModel)
  }

  override fun onResume() {
    super.onResume()
    // 暂时不使用
    /*if (isFirstResume) {
      isFirstResume = false
      return
    }
    livePlayerFragment?.refresh()*/
  }

  override fun initData() {
    lifecycleScope.launch {
      mainViewModel.currentChannelType.collect {
        if (it.isEmpty()) return@collect
        val fragmentClazz = mainViewModel
          .getFragmentClassForChannel(mainViewModel.currentChannelModel.value!!)
          ?: return@collect
        Log.i(TAG, "showFragment: $fragmentClazz")

        if (isUpgrade || BuildConfig.DEBUG) {
          showFragment(fragmentClazz)
        } else {
          isUpgrade = true
          WebViewUpgrade.initWebView(this@MainActivity) {
            showFragment(fragmentClazz)
          }
        }
      }
    }
  }

  private fun showFragment(fragmentClazz: Class<LivePlayerFragment>) {
    val transaction = supportFragmentManager.beginTransaction()
    val tag = fragmentClazz.name
    val target = fragmentClazz.getDeclaredConstructor().newInstance()
    livePlayerFragment = target
    transaction.replace(R.id.fragment, target, tag)
    transaction.commitNow()
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

      R.id.btn_refresh -> {
        livePlayerFragment?.refresh()
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
        mainViewModel.up()
      }

      /**
       * 下
       */
      KeyEvent.KEYCODE_DPAD_DOWN -> {
        mainViewModel.down()
      }

      // ENTER、OK（确认）
      KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_SPACE -> {
        Log.d(TAG, "onKeyDown: Ok")
        livePlayerFragment?.resumeOrPause()
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
        if (mainViewModel.showCurrentChannel.value) {
          // 如果显示了当前频道
          mainViewModel.showCurrentChannel(false)
          mainViewModel.rollbackIndex() // 回滚之前的频道
        } else {
          if (System.currentTimeMillis() - lastBackTime > 2000) {
            lastBackTime = System.currentTimeMillis()
            Toast.makeText(this, "再按一次返回键退出", Toast.LENGTH_SHORT).show()
          } else {
            exitProcess(0)
          }
        }
      }

      KeyEvent.KEYCODE_POUND -> {
        livePlayerFragment?.refresh()
      }

      // 主页
      KeyEvent.KEYCODE_HOME -> {
        exitProcess(0)
      }

      // 菜单
      KeyEvent.KEYCODE_MENU, KeyEvent.KEYCODE_P -> {
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