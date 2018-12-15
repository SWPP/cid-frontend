package com.cid.bot

import android.annotation.SuppressLint
import android.app.Activity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import android.content.*
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.databinding.DataBindingUtil
import android.os.Bundle
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.*
import android.widget.RatingBar
import androidx.appcompat.app.AlertDialog
import com.cid.bot.data.Message
import com.cid.bot.databinding.ActivityChatBinding
import com.cid.bot.databinding.ItemMessageBinding
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_chat.*
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.net.URL
import javax.inject.Inject

class ChatActivity : BaseDaggerActivity() {
    companion object {
        const val REQUEST_SIGN_IN = 101
        const val REQUEST_PROFILE = 102
    }

    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    lateinit var viewModel: ChatViewModel
    private lateinit var binding: ActivityChatBinding
    private lateinit var messageAdapter: MessageAdapter

    private val messagingReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            val messageId = intent.getIntExtra("message_id", -1)
            viewModel.loadMessage(messageId, HObserver(onError = {
                toastLong(it.zip())
            }))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /* Binding */
        binding = DataBindingUtil.setContentView(this, R.layout.activity_chat)
        viewModel = ViewModelProviders.of(this, viewModelFactory).get(ChatViewModel::class.java)
        binding.viewModel = viewModel
        binding.executePendingBindings()

        /* Message RecyclerView */
        val layoutManager = LinearLayoutManager(applicationContext)
        layoutManager.stackFromEnd = true
        rVmessages.layoutManager = layoutManager
        messageAdapter = MessageAdapter(mutableListOf(), ContextWrapper(this).getDir("imageDir", Context.MODE_PRIVATE), this) { text -> eTtext.setText(text); trySendMessage() }
        rVmessages.adapter = messageAdapter
        viewModel.messages.observe(this, Observer { messages ->
            messages ?: return@Observer
            val original = messageAdapter.messages
            if (original.size * 2 < messages.size) {
                original.clear()
                original.addAll(messages)
                original.sortBy(Message::created)
                rVmessages.scrollToPosition(messageAdapter.itemCount - 1)
                messageAdapter.notifyDataSetChanged()
            } else {
                val adding = messages.toSet() - original.toSet()
                original.addAll(adding)
                original.sortBy(Message::created)
                rVmessages.scrollToPosition(messageAdapter.itemCount - 1)
                for (message in adding.sortedBy(Message::created)) {
                    messageAdapter.notifyItemInserted(original.indexOfLast { message.id == it.id })
                }
            }
        })

        /* Listeners */
        bTsend.setOnClickListener { trySendMessage() }
        eTtext.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN) {
                trySendMessage()
                true
            } else false
        }

        viewModel.loadMuserConfig(HObserver(onFinish = {
            requestSignIn()
        }))
    }

    private fun tryLoadAllMessages() {
        viewModel.loadMessages(HObserver(onError = {
            toastLong(it.simple())
        }))
    }

    private fun trySendMessage() {
        val text = eTtext.text.toString()
        if (text.isEmpty()) return
        val selectionStart = eTtext.selectionStart
        val selectionEnd = eTtext.selectionEnd
        eTtext.setText("")

        viewModel.saveMessage(text, HObserver(onError = {
            toastLong(it.simple())
            eTtext.setText(text)
            eTtext.setSelection(selectionStart, selectionEnd)
        }))
    }

    private fun requestSignIn(force: Boolean = false) {
        fun openSignActivity() {
            messageAdapter.messages.clear()
            messageAdapter.notifyDataSetChanged()
            startActivityForResult(Intent(this, SignActivity::class.java), REQUEST_SIGN_IN)
        }

        if (force) {
            openSignActivity()
            return
        }

        viewModel.loadMuserConfig(HObserver(onError = {
            openSignActivity()
        }, onSuccess = {
            if(it.autoSignIn && it.token != null) {
                if (net.isConnectedToInternet) {
                    register(net.api.loadMyInfo(), onSuccess = {
                        toastShort("Signed in as ${it.username}")
                        tryLoadAllMessages()
                    }, onError = {
                        openSignActivity()
                    })
                } else {
                    toastShort("OFFLINE MODE as ${it.username}")
                    tryLoadAllMessages()
                }
            } else {
                openSignActivity()
            }
        }))
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQUEST_SIGN_IN -> {
                when (resultCode) {
                    RESULT_CANCELED -> finish()
                    RESULT_OK -> tryLoadAllMessages()
                }
            }
            REQUEST_PROFILE -> {
                when (resultCode) {
                    Activity.RESULT_CANCELED -> requestSignIn()
                }
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private var lastBackPressed = 0L
    override fun onBackPressed() {
        val current = System.currentTimeMillis()
        val delta = current - lastBackPressed
        if (delta > 1500) {
            toastShort("Press once more to exit.")
            lastBackPressed = current
            return
        }
        super.onBackPressed()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_chat, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.mIrefresh -> {
                tryLoadAllMessages()
            }
            R.id.mIprofile -> {
                startActivityForResult(Intent(this, ProfileActivity::class.java), REQUEST_PROFILE)
            }
            R.id.mIsignOut -> {
                register(net.api.signOut(), {}, {}, { requestSignIn(true) })
            }
            R.id.mIexit -> {
                finish()
            }
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    class MessageAdapter(val messages: MutableList<Message>, private val imageDirectory: File, private val context: Context, private val onSendMessage: (String) -> Unit) : RecyclerView.Adapter<MessageAdapter.ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            val binding = ItemMessageBinding.inflate(inflater, parent, false)
            return ViewHolder(binding)
        }

        override fun getItemCount() = messages.size

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(messages[position], imageDirectory, context, onSendMessage)
        }

        class ViewHolder(private val binding: ItemMessageBinding) : RecyclerView.ViewHolder(binding.root) {
            @SuppressLint("CheckResult")
            fun bind(message: Message, imageDirectory: File, context: Context, onSendMessage: (String) -> Unit) {
                binding.message = message
                binding.imageBitmap = null
                binding.chip1 = null
                binding.chip2 = null
                binding.chip3 = null
                binding.chip4 = null
                binding.executePendingBindings()

                /* Set Album Image */
                val music = message.music ?: return
                val albumId = music.albumId ?: return
                if (albumId == 0) return
                val imageFileName = "album_image_$albumId.jpg"
                val path = File(imageDirectory, imageFileName)

                singleObservable {
                    /* Get Image Bitmap From Local Storage */
                    try {
                        with(BitmapFactory.decodeStream(FileInputStream(path))) {
                            if (this != null) return@singleObservable this
                        }
                    } catch (e: FileNotFoundException) {}

                    /* Get Image Bitmap From Remote Storage */
                    val url = URL("$BASE_URL/chatbot/album-image/$albumId/")
                    val conn = url.openConnection()
                    conn.doInput = true
                    conn.connect()
                    val input = conn.getInputStream()
                    val bitmap = BitmapFactory.decodeStream(input)

                    /* Save Image Bitmap To Local Storage */
                    val fos = FileOutputStream(path)
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
                    fos.close()

                    /* Return Bitmap Instance */
                    return@singleObservable bitmap
                }.subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({
                            binding.imageBitmap = it
                            binding.executePendingBindings()
                        }, {

                        })

                /* Set Suggested Chip List */
                val chipList = message.chips.map {
                    when (it) {
                        1 -> Pair("rate it", View.OnClickListener {
                            val layout = LayoutInflater.from(context).inflate(R.layout.dialog_rate, null)
                            AlertDialog.Builder(context)
                                    .setTitle("Rate")
                                    .setMessage("Please rate on \"${message.music.title}\"")
                                    .setView(layout)
                                    .setPositiveButton("Rate") { _, _ ->
                                        val rating = layout.findViewById<RatingBar>(R.id.ratingBar).rating
                                        onSendMessage("I'll rate ${(rating * 2).toInt()} points on \"${message.music.title}\".")
                                    }
                                    .setNegativeButton("Cancel", null)
                                    .show()
                        })
                        2 -> Pair("another one", View.OnClickListener {
                            onSendMessage("Recommend another one, please.")
                        })
                        else -> Pair("unknown chip", null)
                    }
                }
                if (chipList.isNotEmpty()) {
                    binding.chip1 = chipList[0].first
                    binding.chip1onClick = chipList[0].second
                }
                if (chipList.size >= 2) {
                    binding.chip2 = chipList[1].first
                    binding.chip2onClick = chipList[1].second
                }
                if (chipList.size >= 3) {
                    binding.chip3 = chipList[2].first
                    binding.chip3onClick = chipList[2].second
                }
                if (chipList.size >= 4) {
                    binding.chip4 = chipList[3].first
                    binding.chip4onClick = chipList[3].second
                }
                binding.executePendingBindings()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        LocalBroadcastManager.getInstance(this).registerReceiver(messagingReceiver, IntentFilter(MessagingService.ACTION_MESSAGE_RECEIVED))
    }

    override fun onPause() {
        super.onPause()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(messagingReceiver)
    }
}
