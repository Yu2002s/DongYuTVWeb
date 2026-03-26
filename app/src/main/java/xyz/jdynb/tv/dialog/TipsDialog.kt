package xyz.jdynb.tv.dialog

import android.content.Context
import android.os.Bundle
import android.view.Gravity
import com.drake.engine.base.EngineDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import xyz.jdynb.tv.R
import xyz.jdynb.tv.databinding.DialogTipsBinding
import xyz.jdynb.tv.model.TipsModel

class TipsDialog(
  context: Context,
  private val tipsModel: TipsModel,
  val duration: Long = 10 * 1000L,
  val gravity: Int = Gravity.CENTER,
) : EngineDialog<DialogTipsBinding>(context, R.style.Theme_TipsDialog) {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.dialog_tips)
    window?.attributes?.let {
      it.gravity = gravity
      window?.attributes = it
    }
  }

  override fun initData() {
    CoroutineScope(Dispatchers.Main).launch {
      while (tipsModel.countdown > 0) {
        delay(1000)
        tipsModel.countdown -= 1000
      }
      dismiss()
    }
  }

  override fun initView() {
    tipsModel.countdown = duration
    binding.m = tipsModel
  }
}
