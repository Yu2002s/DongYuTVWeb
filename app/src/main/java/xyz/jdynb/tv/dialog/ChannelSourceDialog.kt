package xyz.jdynb.tv.dialog

import android.content.Context
import android.os.Bundle
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

class ChannelSourceDialog(
  context: Context,
  private val channelTypeModels: List<LiveChannelTypeModel>,
  private val currentChannelModel: LiveChannelModel
) : EngineDialog<DialogChannelSourceBinding>(context, R.style.ChannelDialogStyle) {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.dialog_channel_source)
  }

  var onChannelChange: ((LiveChannelModel) -> Unit)? = null

  override fun initData() {
  }

  override fun initView() {
    setMaxWidth()
    binding.rvType.dividerSpace(30).setup {
      singleMode = true
      addType<LiveChannelTypeModel>(R.layout.item_list_source)

      onChecked { position, checked, allChecked ->
        val model = getModel<LiveChannelTypeModel>(position)
        model.isSelected = checked
      }

      R.id.tv_group.onClick {
        val model = getModel<LiveChannelTypeModel>()
        val channel = model.channelList.find { it.channelName == currentChannelModel.channelName }
        if (channel == null) {
          Toast.makeText(context, "没有找到当前频道", Toast.LENGTH_SHORT).show()
          return@onClick
        }
        onChannelChange?.invoke(channel)
        dismiss()
      }
    }.models = channelTypeModels

    val position = channelTypeModels
      .indexOfFirst {
        it.source == currentChannelModel.source
      }

    if (position != -1) {
      binding.rvType.bindingAdapter.setChecked(position, true)
      binding.rvType.post {
        binding.rvType.getChildAt(position).requestFocus()
      }
    }
  }
}