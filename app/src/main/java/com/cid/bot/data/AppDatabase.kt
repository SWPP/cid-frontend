package com.cid.bot.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Muser::class, Message::class], version = 2)
abstract class AppDatabase : RoomDatabase() {
    abstract fun muserDao(): MuserDao
    abstract fun messageDao(): MessageDao
}
