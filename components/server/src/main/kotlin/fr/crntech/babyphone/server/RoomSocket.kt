package fr.crntech.babyphone.server

import fr.crntech.babyphone.shared.Endpoints
import fr.crntech.babyphone.shared.Peer
import fr.crntech.babyphone.shared.Role
import io.ktor.http.Parameters
import io.ktor.server.routing.Route
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readBytes
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("RoomSocket")
private val ROOM_ID = Regex("^[0-9a-f]{64}$")
private const val MAX_NAME_LENGTH = 64

fun Route.roomSocket(registry: RoomRegistry) = webSocket("${Endpoints.SOCKET_PATH}/{roomId}") {
    val roomId = call.parameters["roomId"]?.takeIf(ROOM_ID::matches)
    val peer = call.request.queryParameters.toPeer()
    if (roomId == null || peer == null) {
        return@webSocket close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Invalid room or peer"))
    }
    val member = Member(peer)
    val room = registry.join(roomId, member)
        ?: return@webSocket close(CloseReason(CloseReason.Codes.TRY_AGAIN_LATER, "Room is full"))

    val tag = "room=${roomId.take(8)} device=${peer.deviceId.take(8)} role=${peer.role}"
    log.info("joined {}", tag)
    val outgoing = launch { member.drainTo(this@webSocket) }
    try {
        for (frame in incoming) {
            if (frame is Frame.Binary) room.relay(member, frame.readBytes())
        }
    } finally {
        outgoing.cancel()
        registry.leave(roomId, member)
        log.info("left {}", tag)
    }
}

private fun Parameters.toPeer(): Peer? {
    val deviceId = get(Endpoints.PARAM_DEVICE)?.takeIf { it.isNotBlank() } ?: return null
    val role = get(Endpoints.PARAM_ROLE)?.let { runCatching { Role.valueOf(it) }.getOrNull() } ?: return null
    val name = get(Endpoints.PARAM_NAME).orEmpty().take(MAX_NAME_LENGTH)
    val protocol = get(Endpoints.PARAM_PROTOCOL)?.toIntOrNull() ?: 0
    val version = get(Endpoints.PARAM_VERSION)?.take(MAX_NAME_LENGTH)
    return Peer(deviceId.take(MAX_NAME_LENGTH), name, role, protocol, version)
}
