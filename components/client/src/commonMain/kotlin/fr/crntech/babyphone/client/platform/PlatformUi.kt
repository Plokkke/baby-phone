package fr.crntech.babyphone.client.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import fr.crntech.babyphone.shared.Role

/** Platform-specific UI hooks. */
interface PlatformUi {
    /** Returns a scan launcher, or null when this device cannot scan. */
    @Composable
    fun rememberQrScanner(onScanned: (String) -> Unit): (() -> Unit)?

    /** Returns a clipboard writer for [text] (the pairing link is a secret: kept out of clipboard previews). */
    @Composable
    fun rememberCopier(): (text: String) -> Unit

    /** Returns a system share sheet launcher, or null when this device has none. */
    @Composable
    fun rememberSharer(): ((text: String) -> Unit)?

    /** Wraps [start] with whatever permission flow the platform needs. */
    @Composable
    fun rememberRoleStarter(start: (Role) -> Unit, onMicrophoneDenied: () -> Unit): (Role) -> Unit

    /** Keeps the screen as dark as possible while shown (it sits in the baby's room). */
    @Composable
    fun NightScreen()
}

val LocalPlatformUi = staticCompositionLocalOf<PlatformUi> { error("PlatformUi not provided") }
