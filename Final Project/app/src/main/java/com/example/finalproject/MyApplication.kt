package com.example.finalproject

import android.app.Application
import androidx.room.Room
import com.example.finalproject.data.HuntHealthDB

class HuntHealth: Application() {
    lateinit var appDB: HuntHealthDB
    override fun onCreate() {
        super.onCreate()
        appDB = Room.databaseBuilder(applicationContext,
            HuntHealthDB::class.java, name = "my-huntdb")
            .build()
    }
}