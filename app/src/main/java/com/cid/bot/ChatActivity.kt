package com.cid.bot

import android.app.Activity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import android.content.*
import androidx.databinding.DataBindingUtil
import android.os.Bundle
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.*
import com.cid.bot.data.Message
import com.cid.bot.databinding.ActivityChatBinding
import com.cid.bot.databinding.ItemMessageBinding
import kotlinx.android.synthetic.main.activity_chat.*
import javax.inject.Inject

class ChatActivity : BaseDaggerActivity() {
    companion object {
        const val REQUEST_SIGN_IN = 101
        const val REQUEST_PROFILE = 102
    }

    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    lateinit var viewModel: ChatViewModel
    private lateinit var binding: ActivityChatBinding
    private val messageAdapter = MessageAdapter(mutableListOf())

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

    class MessageAdapter(val messages: MutableList<Message>) : RecyclerView.Adapter<MessageAdapter.ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            val binding = ItemMessageBinding.inflate(inflater, parent, false)
            return ViewHolder(binding)
        }

        override fun getItemCount() = messages.size

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(messages[position])
        }

        class ViewHolder(private val binding: ItemMessageBinding) : RecyclerView.ViewHolder(binding.root) {
            fun bind(message: Message) {
                binding.message = message
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
