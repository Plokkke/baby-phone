package fr.crntech.babyphone.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import fr.crntech.babyphone.client.ui.App
import fr.crntech.babyphone.client.ui.AppViewModel
import fr.crntech.babyphone.container
import fr.crntech.babyphone.monitor.ServiceMonitorController
import fr.crntech.babyphone.platform.AndroidPlatformUi

class MainActivity : ComponentActivity() {
    private val viewModel: AppViewModel by viewModels {
        viewModelFactory { initializer { AppViewModel(container, ServiceMonitorController(applicationContext)) } }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) handlePairingIntent(intent)
        setContent { App(viewModel, AndroidPlatformUi) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handlePairingIntent(intent)
    }

    private fun handlePairingIntent(intent: Intent) {
        intent.data?.toString()?.let(viewModel::onPairingLink)
    }
}
