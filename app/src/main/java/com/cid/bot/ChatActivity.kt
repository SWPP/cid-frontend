package com.cid.bot

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem

class ChatActivity : AppCompatActivity() {
    companion object {
        const val REQUEST_SIGN_IN = 101
        const val REQUEST_PROFILE = 102
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        startActivityForResult(Intent(this, SignActivity::class.java), REQUEST_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQUEST_SIGN_IN -> {
                when (resultCode) {
                    RESULT_CANCELED -> finish()
                    RESULT_OK -> {} // TODO
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
        return when (item.itemId) {
            R.id.mIprofile -> {
                startActivityForResult(Intent(this, ProfileActivity::class.java), REQUEST_PROFILE)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
