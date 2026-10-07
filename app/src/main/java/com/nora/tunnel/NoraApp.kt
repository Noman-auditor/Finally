package com.nora.tunnel

import android.app.Application
import androidx.room.Room
import com.nora.tunnel.data.database.NoraDatabase
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class NoraApp: Application() {
    lateinit var db: NoraDatabase
    override fun onCreate() {
        super.onCreate()
        db = Room.databaseBuilder(this, NoraDatabase::class.java, "nora.db")
            .addMigrations()
            .fallbackToDestructiveMigrationOnDowngrade()
            .build()
        startKoin { androidContext(this@NoraApp); modules(appModule) }
    }
}
