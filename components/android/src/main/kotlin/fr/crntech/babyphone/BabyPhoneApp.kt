package fr.crntech.babyphone

import android.app.Application
import android.content.Context
import fr.crntech.babyphone.client.AppContainer
import fr.crntech.babyphone.monitor.Notifications
import fr.crntech.babyphone.platform.androidPlatform

class BabyPhoneApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(androidPlatform(this))
        Notifications.createChannels(this)
    }
}

val Context.container get() = (applicationContext as BabyPhoneApp).container
