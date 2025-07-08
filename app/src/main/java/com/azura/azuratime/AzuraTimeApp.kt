package com.azura.azuratime

import android.app.Application
import com.google.firebase.FirebaseApp

class AzuraTimeApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        com.azura.azuratime.sync.SyncOnBootAndNetwork.register(this)
    }
}
