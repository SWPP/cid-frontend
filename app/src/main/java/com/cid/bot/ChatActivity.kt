package com.cid.bot

import android.app.Activity
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast

class ChatActivity : AppCompatActivity() {
    companion object {
        const val REQUEST_SIGN_IN = 101
        const val REQUEST_PROFILE = 102
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

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
}
