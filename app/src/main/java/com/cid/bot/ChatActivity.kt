package com.cid.bot

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle

class ChatActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        startActivityForResult(Intent(this, SignActivity::class.java), 0)
    }
}
