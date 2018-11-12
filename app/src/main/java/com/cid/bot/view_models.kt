package com.cid.bot

import android.arch.lifecycle.ViewModel
import android.databinding.ObservableField


class ProfileViewModel : ViewModel() {
    val repoModel = Repository()
    val text = ObservableField<String>()
    val isLoading = ObservableField<Boolean>()

    fun refresh() {
        isLoading.set(true)
        repoModel.refreshData(object : OnDataReadyCallback {
            override fun onDataReady(data: String) {
                isLoading.set(false)
                text.set(data)
            }
        })
    }
}
