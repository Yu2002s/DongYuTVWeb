package xyz.jdynb.tv.dialog

import android.content.Context
import android.os.Bundle
import android.widget.SeekBar
import com.drake.engine.base.EngineDialog
import xyz.jdynb.music.utils.SpUtils.getRequired
import xyz.jdynb.music.utils.SpUtils.put
import xyz.jdynb.tv.R
import xyz.jdynb.tv.constants.SPKeyConstants
import xyz.jdynb.tv.databinding.DialogVideoSettingBinding

class VideoSettingDialog(context: Context): EngineDialog<DialogVideoSettingBinding>(context, R.style.Theme_BaseDialog) {

  var onDanmakuVisibilityListener: ((Boolean) -> Unit)? = null

  var onDanmakuAlphaListener: ((Float) -> Unit)? = null

  var onDanmakuSizeListener: ((Float) -> Unit)? = null

  var onDanmakuAreaListener: ((Float) -> Unit)? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.dialog_video_setting)
  }

  override fun initData() {

  }

  override fun initView() {
    binding.swDanmaku.isChecked = SPKeyConstants.ENABLE_DANMAKU.getRequired(true)

    binding.swDanmaku.setOnCheckedChangeListener { buttonView, isChecked ->
      SPKeyConstants.ENABLE_DANMAKU.put(isChecked)
      onDanmakuVisibilityListener?.invoke(isChecked)
    }

    binding.danmakuAlpha.progress = (SPKeyConstants.DANMAKU_ALPHA.getRequired(1f) * 10).toInt()
    binding.danmakuAlpha.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
      override fun onProgressChanged(
        seekBar: SeekBar?,
        progress: Int,
        fromUser: Boolean
      ) {
      }

      override fun onStartTrackingTouch(seekBar: SeekBar?) {
      }

      override fun onStopTrackingTouch(seekBar: SeekBar) {
        if (seekBar.progress < 1) {
          seekBar.progress = 1
        }
        SPKeyConstants.DANMAKU_ALPHA.put(seekBar.progress / 10f)
        onDanmakuAlphaListener?.invoke(seekBar.progress / 10f)
      }
    })

    binding.danmakuSize.progress = (SPKeyConstants.DANMAKU_SIZE.getRequired(1f) * 10).toInt()
    binding.danmakuSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
      override fun onProgressChanged(
        seekBar: SeekBar?,
        progress: Int,
        fromUser: Boolean
      ) {

      }

      override fun onStartTrackingTouch(seekBar: SeekBar?) {

      }

      override fun onStopTrackingTouch(seekBar: SeekBar) {
        if (seekBar.progress < 1) {
          seekBar.progress = 1
        }
        SPKeyConstants.DANMAKU_SIZE.put(seekBar.progress / 10f)
        onDanmakuSizeListener?.invoke(seekBar.progress / 10f)
      }
    })

    binding.danmakuArea.progress = (SPKeyConstants.DANMAKU_AREA.getRequired(0.5f) * 10).toInt()
    binding.danmakuArea.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
      override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
      }
      override fun onStartTrackingTouch(seekBar: SeekBar?) {
      }
      override fun onStopTrackingTouch(seekBar: SeekBar) {
        if (seekBar.progress < 1) {
          seekBar.progress = 1
        }
        SPKeyConstants.DANMAKU_AREA.put(seekBar.progress / 10f)
        onDanmakuAreaListener?.invoke(seekBar.progress / 10f)
      }
    })

    binding.btnClose.setOnClickListener {
      dismiss()
    }
    binding.btnClose.requestFocus()
  }
}