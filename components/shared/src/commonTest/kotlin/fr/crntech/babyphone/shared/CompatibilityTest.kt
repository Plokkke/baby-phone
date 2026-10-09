package fr.crntech.babyphone.shared

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CompatibilityTest {
    private fun peer(protocol: Int) = Peer("id", "name", Role.RECEIVER, protocol)

    @Test
    fun `same protocol understands each other`() = assertNull(peer(PROTOCOL_VERSION).outdated)

    @Test
    fun `older or unversioned peers must be updated`() {
        assertEquals(Outdated.PEER, peer(PROTOCOL_VERSION - 1).outdated)
        assertEquals(Outdated.PEER, Peer("id", "name", Role.EMITTER).outdated)
    }

    @Test
    fun `a newer peer means this device must be updated`() = assertEquals(Outdated.SELF, peer(PROTOCOL_VERSION + 1).outdated)
}
