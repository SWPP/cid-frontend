package com.cid.bot

import android.os.Handler

class Repository {
    fun refreshData(onDataReadyCallback: OnDataReadyCallback) {
        Handler().postDelayed({ onDataReadyCallback.onDataReady("new data") }, 2000)
    }

}

interface OnDataReadyCallback {
    fun onDataReady(data: String)
}
