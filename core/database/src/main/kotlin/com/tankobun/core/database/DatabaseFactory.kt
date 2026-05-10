package com.tankobun.core.database

import android.content.Context
import androidx.room.Room

object DatabaseFactory {
    fun create(context: Context): TankobunDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            TankobunDatabase::class.java,
            "tankobun.db",
        ).build()
    }
}
