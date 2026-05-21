package xyz.jdynb.tv.ui.dialog

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.drake.brv.annotaion.DividerOrientation
import com.drake.brv.utils.dividerSpace
import com.drake.brv.utils.models
import com.drake.brv.utils.setup
import com.drake.engine.base.EngineDialog
import com.drake.engine.dialog.setMaxWidth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import xyz.jdynb.tv.R
import xyz.jdynb.tv.databinding.DialogVideoHistoryBinding
import xyz.jdynb.tv.model.MovieModel
import xyz.jdynb.tv.ui.activity.SearchListActivity
import xyz.jdynb.tv.ui.activity.VideoActivity
import xyz.jdynb.tv.utils.NetworkUtils
import java.io.File
import kotlin.concurrent.thread

/**
 * 播放记录
 */
class VideoHistoryDialog(context: Context) :
  EngineDialog<DialogVideoHistoryBinding>(context, R.style.Theme_BaseDialog) {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.dialog_video_history)
  }

  override fun initData() {
    CoroutineScope(Dispatchers.Main).launch {
      binding.rvHistory.models = withContext(Dispatchers.IO) {
        File(context.cacheDir, "play_history").listFiles()?.sortedByDescending { it.lastModified() }
          ?.map { it.name } ?: listOf<String>()
      }

      if (binding.rvHistory.models.isNullOrEmpty()) {
        Toast.makeText(context, "暂无播放记录", Toast.LENGTH_SHORT).show()
        dismiss()
      }
    }
  }

  override fun initView() {
    setMaxWidth(percent = 0.8f)
    binding.btnClose.setOnClickListener {
      dismiss()
    }

    binding.btnClose.requestFocus()

    val handler = Handler(Looper.getMainLooper())
    binding.rvHistory.dividerSpace(14, DividerOrientation.GRID).setup {
      addType<String>(R.layout.item_list_title)

      R.id.item_name.onClick {
        val model = getModel<String>()
        thread {
          try {
            val file = File(context.cacheDir, "play_history/$model")
            if (!file.exists()) {
              handler.post {
                Toast.makeText(context, "记录不存在，可能被清理", Toast.LENGTH_SHORT).show()
              }
              return@thread
            }
            val movieModel = NetworkUtils.json.decodeFromString<MovieModel>(file.readText())
            handler.post {
              VideoActivity.play(movieModel)
              dismiss()
            }
          } catch (e: Exception) {
            handler.post {
              Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
            }
          }
        }
      }
    }
  }
}