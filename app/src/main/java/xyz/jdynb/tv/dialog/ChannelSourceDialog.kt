package xyz.jdynb.tv.dialog

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.drake.brv.utils.bindingAdapter
import com.drake.brv.utils.dividerSpace
import com.drake.brv.utils.setup
import com.drake.engine.base.EngineDialog
import com.drake.engine.dialog.setMaxWidth
import xyz.jdynb.tv.R
import xyz.jdynb.tv.databinding.DialogChannelSourceBinding
import xyz.jdynb.tv.model.LiveChannelModel
import xyz.jdynb.tv.model.LiveChannelTypeModel
import xyz.jdynb.tv.utils.SpUtils.get
import xyz.jdynb.tv.utils.SpUtils.put

class ChannelSourceDialog(
  context: Context,
  private val currentChannelModel: LiveChannelModel
) : EngineDialog<DialogChannelSourceBinding>(context, R.style.ChannelDialogStyle) {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.dialog_channel_source)
  }

  var onChannelChange: ((LiveChannelModel) -> Unit)? = null

  override fun initData() {
    Log.i("ChannelSourceDialog", "initData: $currentChannelModel")
  }

  override fun initView() {
    setMaxWidth()

    currentChannelModel.children.forEach {
      it.isSelected = false
    }

    binding.tvTitle.text = currentChannelModel.channelType + " " + currentChannelModel.channelName

    binding.rvType.dividerSpace(30).setup {
      singleMode = true
      addType<LiveChannelModel>(R.layout.item_list_channel)

      onChecked { position, checked, allChecked ->
        val model = getModel<LiveChannelModel>(position)
        model.isSelected = checked
      }

      R.id.tv_channel.onClick {
        val model = getModel<LiveChannelModel>()
        setChecked(modelPosition, true)
        modelPosition.put("channel_config_${currentChannelModel.channelType}", currentChannelModel.channelName)
        onChannelChange?.invoke(model)
        dismiss()
      }
    }.models = currentChannelModel.children.toMutableList()

    val index = "channel_config_${currentChannelModel.channelType}".get<Int>(
      currentChannelModel.channelName,
      0
    ) ?: 0

    binding.rvType.bindingAdapter.setChecked(index, true)
    binding.rvType.post {
      binding.rvType.getChildAt(index).requestFocus()
    }
  }
}