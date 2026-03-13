package xyz.jdynb.tv

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.net.ConnectivityManager
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
import com.drake.engine.utils.NetworkUtils
import kotlinx.coroutines.delay
import xyz.jdynb.music.utils.SpUtils.getRequired
import xyz.jdynb.tv.constants.SPKeyConstants
import xyz.jdynb.tv.ui.activity.SearchActivity

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

  private var isNetworkConnected = false

  /**
   *网络状态广播接收器
   */
  private val networkReceiver = NetworkBoardReceiver()

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
    insetsController.systemBarsBehavior =
      WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    insetsController.hide(WindowInsetsCompat.Type.systemBars())

    // 判断cpu型号决定需不需要升级
    isUpgrade = !Build.SUPPORTED_ABIS.any {
      it.contains("arm64")
    }

    Log.i(TAG, "abi: ${Build.SUPPORTED_ABIS.joinToString(",")}")

    audioManager = getSystemService(AUDIO_SERVICE) as AudioManager

    isNetworkConnected = NetworkUtils.isConnected()

    // 注册网络状态广播接收器
    registerNetworkReceiver()
  }

  /**
   * 注册网络状态广播接收器
   */
  private fun registerNetworkReceiver() {
    val filter = IntentFilter().apply {
      addAction(ConnectivityManager.CONNECTIVITY_ACTION)
    }
    registerReceiver(networkReceiver, filter)
  }

  /**
   * 注销网络状态广播接收器
   */
  private fun unregisterNetworkReceiver() {
    try {
      unregisterReceiver(networkReceiver)
    } catch (e: IllegalArgumentException) {
      //接器未注册时会抛出异常，忽略
    }
  }

  override fun initView() {
    binding.m = mainViewModel
    binding.lifecycleOwner = this

    channelListDialog = ChannelListDialog(this, mainViewModel)
    channelListDialog.onRefreshListener = {
      handleChannelTypeChange()
    }
  }

  override fun initData() {
    lifecycleScope.launch {
      mainViewModel.currentChannelType.collect {
        handleChannelTypeChange(it)
      }
    }

    // 轮询判断网络是否连接，这里不使用，而是使用动态注册的广播接收器
    /*lifecycleScope.launch {
      while (!NetworkUtils.isConnected()) {
        Toast.makeText(this@MainActivity, "检测到未连接到网络，正在尝试刷新...", Toast.LENGTH_LONG)
          .show()
        delay(3000L)
        handleChannelTypeChange()
      }
    }*/
  }

  /**
   * 处理频道类型变化
   *
   * @param type 频道类型
   */
  private fun handleChannelTypeChange(type: String = mainViewModel.currentChannelType.value) {
    // 如果当前频道类型为空，则不处理
    if (type.isEmpty()) return
    Log.i(TAG, "currentChannelType: $type")
    // 根据当前频道类型获取对应的 Fragment 类
    val fragmentClazz = mainViewModel
      .getFragmentClassForChannel(mainViewModel.currentChannelModel.value!!)
      ?: return
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

  /**
   * 显示指定的 Fragment
   *
   * @param fragmentClazz Fragment 类
   */
  private fun showFragment(fragmentClazz: Class<LivePlayerFragment>) {
    val transaction = supportFragmentManager.beginTransaction()
    val tag = fragmentClazz.name
    val target = fragmentClazz.getDeclaredConstructor().newInstance()
    livePlayerFragment = target
    transaction.replace(R.id.fragment, target, tag)
    transaction.commitNow()
  }

  /**
   * 处理点击事件
   */
  override fun onClick(v: View) {
    super.onClick(v)
    when (v.id) {
      // 菜单
      R.id.btn_menu -> {
        if (mainViewModel.channelModelList.value.isEmpty()) {
          return
        }
        channelListDialog.show()
      }

      // 左
      R.id.btn_left -> {
        if (SPKeyConstants.REVERSE_DIRECTION.getRequired(false)) {
          mainViewModel.down()
        } else {
          mainViewModel.up()
        }
      }

      // 右
      R.id.btn_right -> {
        if (SPKeyConstants.REVERSE_DIRECTION.getRequired(false)) {
          mainViewModel.up()
        } else {
          mainViewModel.down()
        }
      }

      // 刷新
      R.id.btn_refresh -> {
        handleChannelTypeChange()
      }

      // 搜索
      R.id.btn_search -> {
        startActivity(Intent(this, SearchActivity::class.java))
      }
    }
  }

  override fun dispatchTouchEvent(event: MotionEvent): Boolean {
    mainViewModel.showActions()
    return super.dispatchTouchEvent(event)
  }

  override fun onBackPressed() {
    if (handleBackPress()) {
      super.onBackPressed()
    }
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
        if (SPKeyConstants.REVERSE_DIRECTION.getRequired(false)) {
          mainViewModel.down()
        } else {
          mainViewModel.up()
        }
      }

      /**
       * 下
       */
      KeyEvent.KEYCODE_DPAD_DOWN -> {
        if (SPKeyConstants.REVERSE_DIRECTION.getRequired(false)) {
          mainViewModel.up()
        } else {
          mainViewModel.down()
        }
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

      //  volume down、left
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

      // volume up、right
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
      /*KeyEvent.KEYCODE_BACK,*/ KeyEvent.KEYCODE_ESCAPE -> {
      handleBackPress()
    }

      // #
      // 重新加载
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

  /**
   * 处理 App 默认的返回
   *
   * @return true 表示已处理返回键 false 表示未处理返回键
   */
  private fun handleBackPress(): Boolean {
    if (mainViewModel.showCurrentChannel.value) {
      // 如果显示了当前频道
      mainViewModel.showCurrentChannel(false)
      mainViewModel.rollbackIndex() // 回滚之前的频道
      return false
    } else {
      if (System.currentTimeMillis() - lastBackTime > 2000) {
        lastBackTime = System.currentTimeMillis()
        Toast.makeText(this, "再按一次返回键退出", Toast.LENGTH_SHORT).show()
        return false
      }
    }
    return true
  }

  override fun onDestroy() {
    super.onDestroy()
    // 注销网络状态广播接收器
    unregisterNetworkReceiver()
  }

  private inner class NetworkBoardReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
      Log.i(TAG, "network change")
      val currentNetworkConnected = NetworkUtils.isConnected()
      if (currentNetworkConnected && !isNetworkConnected) {
        //连接时调用 handleChannelTypeChange 方法
        handleChannelTypeChange()
        Toast.makeText(context, "已连接到网络", Toast.LENGTH_SHORT).show()
      } else if (!currentNetworkConnected) {
        Toast.makeText(context, "已断开网络，当网络连接后自动刷新页面", Toast.LENGTH_LONG).show()
      }
      isNetworkConnected = currentNetworkConnected
    }

  }
}