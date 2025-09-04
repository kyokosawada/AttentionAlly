package com.example.attentionally

import android.app.Application
import com.google.firebase.FirebaseApp
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import timber.log.Timber
import com.example.attentionally.di.authModule

class AttentionAllyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize Firebase (ensures reliability across modules and Compose/MVVM)
        FirebaseApp.initializeApp(this)

        // Start Timber logging - DebugTree for debug builds, swap for release as needed
        Timber.plant(Timber.DebugTree())
        // TODO: Swap Timber DebugTree for custom ReleaseTree in production builds

        // Start Koin Dependency Injection
        // You may add DI modules from di/ directory here as your app grows
        startKoin {
            androidContext(this@AttentionAllyApplication)
            modules(authModule)
        }
    }
}
