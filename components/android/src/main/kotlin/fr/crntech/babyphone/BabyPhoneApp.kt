package fr.crntech.babyphone

import android.app.Application
import fr.crntech.babyphone.monitor.Notifications
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class BabyPhoneApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        Notifications.createChannels(this)
        MainScope().launch { container.settings.initialize() }
    }
}
