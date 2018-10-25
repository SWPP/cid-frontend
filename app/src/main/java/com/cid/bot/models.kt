package com.cid.bot

data class Muser(val id: Int?, val username: String, val gender: Int, val birthdate: String?)

data class Message(val sender: String?, val receiver: String?, val text: String, val created: String? = null)
