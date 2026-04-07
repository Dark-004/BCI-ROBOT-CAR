package com.neurodrive.smartcar

import android.app.Application
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.neurodrive.smartcar.data.local.SecureStorage
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class BioSignalApp : Application() {

    @Inject
    lateinit var secureStorage: SecureStorage

    override fun onCreate() {
        super.onCreate()
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                if (event == Lifecycle.Event.ON_STOP) {
                    // App went to background
                    secureStorage.clearSession()
                }
            }
        })
    }
}
