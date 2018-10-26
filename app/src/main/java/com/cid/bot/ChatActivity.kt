package com.cid.bot

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.CardView
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.*
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_chat.*

class ChatActivity : AppCompatActivity() {
    companion object {
        const val REQUEST_SIGN_IN = 101
        const val REQUEST_PROFILE = 102
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        // TODO: replace this message list to the real fetched list
        val messages = listOf(
                Message("muBot", "user", "111"),
                Message("muBot", "user","222"),
                Message("user", "muBot", "333"),
                Message("muBot", "user","test cases all right"),
                Message("user", "muBot", "Hello?"),
                Message("muBot", "user", "111"),
                Message("muBot", "user","222"),
                Message("user", "muBot", "333"),
                Message("muBot", "user","test cases all right"),
                Message("user", "muBot", "Hello?"),
                Message("muBot", "user", "111"),
                Message("muBot", "user","222"),
                Message("user", "muBot", "333"),
                Message("muBot", "user","test cases all right"),
                Message("user", "muBot", "Hello?"),
                Message("muBot", "user", "111"),
                Message("muBot", "user","222"),
                Message("user", "muBot", "333"),
                Message("muBot", "user","test cases all right"),
                Message("user", "muBot", "Hello?"),
                Message("muBot", "user", "111"),
                Message("muBot", "user","222"),
                Message("user", "muBot", "333"),
                Message("muBot", "user","test cases all right"),
                Message("user", "muBot", "Hello?")
        )
        rVmessages.layoutManager = LinearLayoutManager(applicationContext)
        rVmessages.adapter = MessageAdapter(messages)

        requestSignIn()
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
                    RESULT_OK -> {} // TODO
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

    class MessageAdapter(val messages: List<Message>) : RecyclerView.Adapter<MessageAdapter.ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_message, parent, false)
            return ViewHolder(view)
        }

        override fun getItemCount() = messages.size

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val m = messages[position]
            with (holder) {
                tVtext.text = m.text
                if (m.receiver == "muBot") {
                    cVmessage.layoutParams = (cVmessage.layoutParams as LinearLayout.LayoutParams).apply {
                        gravity = Gravity.END
                    }
                    cVmessage.setCardBackgroundColor(Color.rgb( 255, 255, 150))
                } else {
                    cVmessage.layoutParams = (cVmessage.layoutParams as LinearLayout.LayoutParams).apply {
                        gravity = Gravity.START
                    }
                    cVmessage.setCardBackgroundColor(Color.WHITE)
                }
            }
        }

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tVtext = view.findViewById<TextView>(R.id.tVtext)
            val cVmessage = view.findViewById<CardView>(R.id.cVmessage)
        }
    }
}
