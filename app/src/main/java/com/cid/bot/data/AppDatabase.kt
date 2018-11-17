package com.cid.bot.data

import android.arch.persistence.room.Database
import android.arch.persistence.room.RoomDatabase

@Database(entities = [Muser::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun muserDao(): MuserDao
}
