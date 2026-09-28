package fr.crntech.babyphone.server

import fr.crntech.babyphone.shared.Peer
import fr.crntech.babyphone.shared.ServerEvent
import fr.crntech.babyphone.shared.Wire
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.close
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import java.util.concurrent.ConcurrentHashMap

/**
 * A connected device. Outgoing frames go through a small bounded outbox that drops
 * the oldest frames, so one slow receiver never stalls the others with stale audio.
 */
class Member(val peer: Peer) {
    private val outbox = Channel<Frame>(OUTBOX_CAPACITY, BufferOverflow.DROP_OLDEST)

    fun offer(frame: Frame) = outbox.trySend(frame).isSuccess

    fun kick() = outbox.close()

    suspend fun drainTo(session: WebSocketSession) {
        for (frame in outbox) session.send(frame)
        session.close()
    }

    private companion object {
        const val OUTBOX_CAPACITY = 64
    }
}

/** Relays opaque frames between members; nothing is ever stored. */
class Room {
    private val members = ConcurrentHashMap<String, Member>()

    val isEmpty get() = members.isEmpty()
    val size get() = members.size

    fun add(member: Member) {
        members.put(member.peer.deviceId, member)?.kick()
        publishPresence()
    }

    fun remove(member: Member) {
        if (members.remove(member.peer.deviceId, member)) publishPresence()
    }

    fun relay(from: Member, payload: ByteArray) = members.values
        .filter { it.peer.role in from.peer.role.audience }
        .forEach { it.offer(Frame.Binary(true, payload)) }

    private fun publishPresence() {
        val text = Wire.encode(ServerEvent.Presence(members.values.map(Member::peer)))
        members.values.forEach { it.offer(Frame.Text(text)) }
    }
}

class RoomRegistry(private val maxMembersPerRoom: Int) {
    private val rooms = HashMap<String, Room>()

    /** Returns null when the room is full. A device reconnecting replaces its previous session. */
    @Synchronized
    fun join(roomId: String, member: Member): Room? {
        val room = rooms.getOrPut(roomId, ::Room)
        if (room.size >= maxMembersPerRoom) return null
        return room.also { it.add(member) }
    }

    @Synchronized
    fun leave(roomId: String, member: Member) {
        val room = rooms[roomId] ?: return
        room.remove(member)
        if (room.isEmpty) rooms.remove(roomId)
    }

    @get:Synchronized
    val roomCount get() = rooms.size
}
