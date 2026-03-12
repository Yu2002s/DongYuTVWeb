package xyz.jdynb.tv.dialog

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Looper
import android.text.Editable
import android.widget.Button
import android.widget.GridLayout
import android.widget.Toast
import androidx.core.view.forEach
import androidx.core.view.setMargins
import com.bumptech.glide.Glide
import com.drake.engine.base.EngineDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.FormBody
import okhttp3.Request
import xyz.jdynb.music.utils.SpUtils.put
import xyz.jdynb.tv.R
import xyz.jdynb.tv.config.Api
import xyz.jdynb.tv.constants.SPKeyConstants
import xyz.jdynb.tv.databinding.DialogUserAuthBinding
import xyz.jdynb.tv.model.ResultModel
import xyz.jdynb.tv.model.UserAuthModel
import xyz.jdynb.tv.utils.NetworkUtils
import kotlin.concurrent.thread

class UserAuthDialog(context: Context) :
  EngineDialog<DialogUserAuthBinding>(context, R.style.Theme_SimpleDialog) {

  private val coroutineScope = CoroutineScope(Dispatchers.Default)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.dialog_user_auth)
  }

  override fun initView() {
    setCancelable(false)

    val inputStream = context.assets.open("images/qrcode_mp.jpg")
    val readBytes = inputStream.readBytes()
    Glide.with(context)
      .load(readBytes)
      .into(binding.ivQrcode)

    binding.grid.forEach {
      (it.layoutParams as GridLayout.LayoutParams).setMargins(10)
      it.setOnClickListener { v ->
        if (v.id == R.id.btn_verify) {
          // 验证
          thread {
            val formBody = FormBody.Builder()
              .add("code", binding.editCode.text.toString())
              .build()
            NetworkUtils.requestSyncResult<UserAuthModel>(Api.VERIFY_CODE, formBody, false)
              .onSuccess { result ->
                val userAuthModelString = NetworkUtils.json.encodeToString(result)
                SPKeyConstants.USER_AUTH.put(userAuthModelString)
                v.post {
                  dismiss()
                  Toast.makeText(context, "验证成功。", Toast.LENGTH_LONG).show()
                  context.sendBroadcast(Intent("xyz.jdynb.tv.AUTHORIZED").also { intent ->
                    intent.`package` = context.packageName
                  })
                }
              }
              .onFailure { error ->
                Looper.prepare()
                Toast.makeText(context, error.message, Toast.LENGTH_LONG).show()
                Looper.loop()
              }
          }
        } else if (v.id == R.id.btn_del) {
          if (binding.editCode.text.isNotEmpty()) {
            binding.editCode.text = binding.editCode.text.dropLast(1) as Editable?
          }
        } else {
          binding.editCode.append((v as Button).text)
        }
      }
    }
  }

  override fun initData() {
  }
}