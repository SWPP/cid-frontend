package com.cid.bot.data

import android.os.Handler

data class Muser(val id: Int?, val username: String, val gender: Int, val birthdate: String?)

class MuserRepository {
    fun getMuser(onMuserReady: (Muser) -> Unit) {
        val muser = Muser(1, "user", 1, "1111-11-11")
        Handler().postDelayed({ onMuserReady(muser) }, 2000)
    }
}
