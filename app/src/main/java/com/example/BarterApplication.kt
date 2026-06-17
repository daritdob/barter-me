package com.example

import android.app.Application
import com.google.firebase.FirebaseApp

class BarterApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (FirebaseApp.getApps(this).isEmpty()) {
            try {
                FirebaseApp.initializeApp(this)
            } catch (_: Exception) {
                // Firebase not configured — local secure auth is used instead.
            }
        }
    }
}
