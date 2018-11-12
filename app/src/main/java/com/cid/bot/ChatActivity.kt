package com.cid.bot

import android.app.Activity
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.*
import android.databinding.DataBindingUtil
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.*
import android.widget.Toast
import com.cid.bot.data.Message
import com.cid.bot.databinding.ActivityChatBinding
import com.cid.bot.databinding.ItemMessageBinding
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.activity_chat.*

class ChatActivity : AppCompatActivity() {
    companion object {
        const val REQUEST_SIGN_IN = 101
        const val REQUEST_PROFILE = 102
    }

    private lateinit var binding: ActivityChatBinding
    private val messageAdapter = MessageAdapter(mutableListOf())

    private val messagingReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            val messageId = intent.getIntExtra("message_id", -1)
            tryLoadMessage(messageId)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_chat)
        val viewModel = ViewModelProviders.of(this).get(ChatViewModel::class.java)
        binding.viewModel = viewModel
        with (binding) {
            executePendingBindings()

            rVmessages.layoutManager = LinearLayoutManager(applicationContext)
            rVmessages.adapter = messageAdapter
        }
        viewModel.messages.observe(this, Observer { messages ->
            messages?.let{ messageAdapter.messages = it.sortedBy(Message::created).toMutableList() }
        })

        bTsend.setOnClickListener { trySendMessage() }
        eTtext.setOnKeyListener { v, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN) {
                trySendMessage()
                true
            } else false
        }

        requestSignIn()
    }

    private fun refresh(messages: List<Message>) {
        (rVmessages.adapter as MessageAdapter).messages = messages.sortedBy(Message::created).toMutableList()
        rVmessages.scrollToPosition(messages.size - 1)
    }

    private fun addMessage(message: Message) {
        val adapter = rVmessages.adapter as MessageAdapter
        adapter.addMessage(message)
        rVmessages.scrollToPosition(adapter.itemCount - 1)
    }

    private var loadAllMessagesTask: Disposable? = null
    private fun tryLoadAllMessages() {
        if (loadAllMessagesTask != null) return

        loadAllMessagesTask = NetworkManager.call(API.loadAllMessages(), {
            refresh(it)
        }, {
            Toast.makeText(this, "Could not load message list, please try later.", Toast.LENGTH_LONG).show()
        }, {
            loadAllMessagesTask = null
        })
    }

    private var loadMessageTaskMap = mutableMapOf<Int, Disposable>()
    private fun tryLoadMessage(id: Int) {
        if (loadMessageTaskMap[id] != null) return

        loadMessageTaskMap[id] = NetworkManager.call(API.loadMessage(id), {
            addMessage(it)
        }, {
            Toast.makeText(this, "Could not load a message, please check your network status.", Toast.LENGTH_LONG).show()
        }, {
            loadMessageTaskMap.remove(id)
        })
    }

    private var sendMessageTask: Disposable? = null
    private fun trySendMessage() {
        if (sendMessageTask != null) return

        val text = eTtext.text.toString()
        if (text.isEmpty()) return
        val selectionStart = eTtext.selectionStart
        val selectionEnd = eTtext.selectionEnd
        eTtext.setText("")

        sendMessageTask = NetworkManager.call(API.sendMessage(text), {
            addMessage(it)
        }, {
            Toast.makeText(this, "Try later", Toast.LENGTH_SHORT).show()
            eTtext.setText(text)
            eTtext.setSelection(selectionStart, selectionEnd)
        }, {
            sendMessageTask = null
        })
    }

    private fun requestSignIn() {
        fun openSignActivity() {
            startActivityForResult(Intent(this@ChatActivity, SignActivity::class.java), REQUEST_SIGN_IN)
        }

        with (getSharedPreferences(getString(R.string.pref_name_sign), 0)) {
            if (getBoolean(getString(R.string.pref_key_auto_sign_in), false)) {
                NetworkManager.authToken = getString(getString(R.string.pref_key_token), null)
                NetworkManager.call(API.loadMyInfo(), {
                    Toast.makeText(this@ChatActivity, "Signed in as ${it.username}", Toast.LENGTH_SHORT).show()
                    tryLoadAllMessages()
                }, {
                    openSignActivity()
                })
            } else {
                openSignActivity()
            }
        }
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

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_chat, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.mIprofile -> {
                startActivityForResult(Intent(this, ProfileActivity::class.java), REQUEST_PROFILE)
            }
            R.id.mIsignOut -> {
                NetworkManager.call(API.signOut(), {}, {}, { requestSignIn() })
            }
            R.id.mIexit -> {
                finish()
            }
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    class MessageAdapter(messages: MutableList<Message>) : RecyclerView.Adapter<MessageAdapter.ViewHolder>() {
        var messages = messages
            set(value) {
                field = value
                notifyDataSetChanged()
            }

        fun addMessage(message: Message) {
            val id = message.id
            if (id != null && messages.find { it.id == id } != null) return

            messages.add(message)
            messages.sortBy(Message::created)
            notifyItemInserted(messages.indexOf(message))
        }

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

    override fun onDestroy() {
        super.onDestroy()
        loadAllMessagesTask?.dispose()
        loadMessageTaskMap.values.forEach { it.dispose() }
        sendMessageTask?.dispose()
    }
}
