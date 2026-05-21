package xyz.jdynb.tv.ui.dialog

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.widget.SwitchCompat
import com.drake.engine.base.EngineDialog
import xyz.jdynb.tv.R
import xyz.jdynb.tv.constants.SPKeyConstants
import xyz.jdynb.tv.databinding.DialogSettingFullBinding
import xyz.jdynb.tv.utils.SpUtils.getRequired
import xyz.jdynb.tv.utils.SpUtils.put

class FullSettingDialog(context: Context): EngineDialog<DialogSettingFullBinding>(context, R.style.Theme_BaseDialog) {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.dialog_setting_full)
  }

  override fun initData() {

  }

  override fun initView() {
    binding.btnClose.requestFocus()
    binding.btnClose.setOnClickListener {
      dismiss()
    }
    binding.swBoot.initSwitch(SPKeyConstants.BOOT_AUTO_START, false)
    binding.swReverseDirection.initSwitch(SPKeyConstants.REVERSE_DIRECTION, false)
    binding.swUpdate.initSwitch(SPKeyConstants.CHECK_UPDATE, true)
    binding.swOkChannel.initSwitch(SPKeyConstants.OK_CHANNEL, true)
    binding.swChannelSingle.initSwitch(SPKeyConstants.CHANNEL_SINGLE, false) {
      Toast.makeText(context, "需要重启软件才能生效", Toast.LENGTH_LONG).show()
    }
    binding.swShowExit.initSwitch(SPKeyConstants.SHOW_EXIT_BTN, false) {
      Toast.makeText(context, "下次启动时生效", Toast.LENGTH_SHORT).show()
    }
    binding.swHome.initSwitch(SPKeyConstants.ALLOW_SET_HOME, false) {
      Toast.makeText(context, "下次启动时生效", Toast.LENGTH_SHORT).show()
    }
    binding.swVolumeDirection.initSwitch(SPKeyConstants.VOLUME_CONTROL_DIRECTION, false)

    binding.btnCustomChannel.setOnClickListener {
      CustomChannelDialog(context).show()
      dismiss()
    }
  }

  private fun SwitchCompat.initSwitch(key: String, default: Boolean, listener: ((Boolean) -> Unit)? = null) {
    isChecked = key.getRequired<Boolean>(default)
    setOnCheckedChangeListener { _, isChecked ->
      key.put(isChecked)
      listener?.invoke(isChecked)
    }
  }
}