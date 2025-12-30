package xyz.jdynb.tv.dialog

import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.drake.brv.annotaion.DividerOrientation
import com.drake.brv.utils.bindingAdapter
import com.drake.brv.utils.divider
import com.drake.brv.utils.dividerSpace
import com.drake.brv.utils.models
import com.drake.brv.utils.setup
import com.drake.engine.base.EngineDialog
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import xyz.jdynb.tv.MainViewModel
import xyz.jdynb.tv.R
import xyz.jdynb.tv.databinding.DialogChannelListBinding
import xyz.jdynb.tv.databinding.ItemListGroupBinding
import xyz.jdynb.tv.model.LiveChannelTypeModel
import xyz.jdynb.tv.model.LiveChannelModel
import xyz.jdynb.tv.utils.isTv

class ChannelListDialog(
  private val activity: AppCompatActivity,
  private val mainViewModel: MainViewModel
) :
  EngineDialog<DialogChannelListBinding>(activity, R.style.ChannelDialogStyle) {

  companion object {

    private const val TAG = "ChannelListDialog"

    private const val AUTO_CLOSE_TIME = 60000L

  }

  private var closeTime = AUTO_CLOSE_TIME

  private var job: Job? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.dialog_channel_list)

    window?.setLayout(
      WindowManager.LayoutParams.MATCH_PARENT,
      (context.resources.displayMetrics.heightPixels * 0.95).toInt()
    )
  }

  override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
    closeTime = AUTO_CLOSE_TIME
    when (keyCode) {
      KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_ESCAPE -> {
        dismiss()
        return true
      }
    }
    return super.onKeyDown(keyCode, event)
  }

  override fun onStart() {
    super.onStart()
    Log.i(TAG, "onStart")
    if (!isTv(context)) {
      binding.tvTips.isVisible = false
      return
    }
    binding.tvTips.isVisible = true
    closeTime = AUTO_CLOSE_TIME
    job = activity.lifecycleScope.launch {
      while (true) {
        if (closeTime <= 0) {
          dismiss()
          break
        }
        binding.tvTips.text = "${closeTime / 1000L}秒无操作后自动返回"
        delay(1000)
        closeTime -= 1000
      }
    }
  }

  override fun onStop() {
    super.onStop()
    Log.i(TAG, "onStop")
    job?.cancel()
  }

  override fun initView() {
    binding.rvChannel.divider {
      setDivider(10)
      orientation = DividerOrientation.GRID
    }.setup {
      singleMode = true

      addType<LiveChannelModel>(R.layout.item_list_channel)

      onChecked { position, checked, allChecked ->
        val model = getModel<LiveChannelModel>(position)
        model.isSelected = checked
      }

      R.id.tv_channel.onClick {
        val model = getModel<LiveChannelModel>()
        setChecked(modelPosition, true)
        mainViewModel.changeCurrentIndex(model)
        dismiss()
      }
    }

    binding.rvGroup.dividerSpace(10).setup {
      singleMode = true

      addType<LiveChannelTypeModel>(R.layout.item_list_group)

      onCreate {
        getBinding<ItemListGroupBinding>().root.onFocusChangeListener =
          View.OnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
              setChecked(modelPosition, true)
            }
          }
      }

      onChecked { position, checked, allChecked ->
        val model = getModel<LiveChannelTypeModel>(position)
        model.isSelected = checked

        if (binding.rvChannel.isComputingLayout) {
          return@onChecked
        }
        binding.rvChannel.models = model.channelList.onEach { it.isSelected = false }
        if (mainViewModel.currentChannelType.value == model.channelType) {
          val checkedPosition = model.channelList.indexOfFirst {
            it.number == mainViewModel.currentChannelModel.value.number
          }
          if (checkedPosition == -1) {
            return@onChecked
          }
          binding.rvChannel.bindingAdapter.setChecked(checkedPosition, true)

          binding.rvChannel.post {
            binding.rvChannel.scrollToPosition(checkedPosition)
            if (window?.currentFocus == null) {
              binding.rvChannel.getChildAt(checkedPosition)?.requestFocus()
            }
          }
        }
      }

      R.id.tv_group.onClick {
        setChecked(modelPosition, true)
      }
    }

    binding.btnBack.isVisible = !isTv(context)
    binding.btnBack.setOnClickListener {
      dismiss()
    }
  }
  override fun initData() {
    activity.lifecycleScope.launch {
      mainViewModel.channelTypeModelList.collect {
        binding.rvGroup.models = it
        val checkedPosition = it.indexOfFirst { model ->
          model.channelType == mainViewModel.currentChannelType.value
        }
        if (checkedPosition == -1) {
          return@collect
        }
        binding.rvGroup.bindingAdapter.setChecked(checkedPosition, true)
      }
    }
  }

}