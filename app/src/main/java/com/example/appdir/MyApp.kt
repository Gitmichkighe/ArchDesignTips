// MyApp.kt
package com.example.appdir

import android.app.Application
import androidx.room.Room
import com.example.appdir.data.AppDatabase
import com.google.android.gms.ads.MobileAds

class MyApp : Application() {

    companion object {
        lateinit var db: AppDatabase
            private set
    }

    override fun onCreate() {
        super.onCreate()
      MobileAds.initialize(this)
           db = Room.databaseBuilder(
               applicationContext,
               AppDatabase::class.java,
               "app_db"
           ).fallbackToDestructiveMigration() // <-- This is where it goes
               .build()
    }
}



