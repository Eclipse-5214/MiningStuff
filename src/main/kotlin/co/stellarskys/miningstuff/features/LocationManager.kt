package co.stellarskys.miningstuff.features

import co.stellarskys.stella.Stella
import co.stellarskys.stella.api.handlers.Signal
import co.stellarskys.stella.api.handlers.Signal.onHover
import co.stellarskys.stella.api.zenith.player
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import java.util.Locale
import tech.thatgravyboat.skyblockapi.api.location.SkyBlockAreas
import tech.thatgravyboat.skyblockapi.utils.text.TextStyle.onClick

object LocationManager {
    private val coordPattern = """\Dx?(\d{3})(?=[, ]),? ?y?(\d{2,3})(?=[, ]),? ?z?(\d{3})\D?(?!\d)""".toRegex()
    private const val REMOVE_UNKNOWN_DISTANCE = 50.0

    val activeWaypoints = mutableMapOf<String, Waypoint>()
    data class Waypoint(val name: String, val x: Double, val z: Double, val color: Int)

    val areaColors = mapOf(
        SkyBlockAreas.GOBLIN_QUEENS_DEN.name to 0x00AA00,
        SkyBlockAreas.JUNGLE_TEMPLE.name to 0x00AA00,
        SkyBlockAreas.LOST_PRECURSOR_CITY.name to 0x008080,
        SkyBlockAreas.DRAGONS_LAIR.name to 0x5555FF,
        SkyBlockAreas.MINES_OF_DIVAN.name to 0xFFFF55,
        SkyBlockAreas.KHAZAD_DUM.name to 0xFF5555,
        SkyBlockAreas.FAIRY_GROTTO.name to 0xFF55FF,
        "King Yolkar" to 0xFFAA00,
        "Corleone" to 0x008080,
        "Odawa" to 0x5555FF,
        "Key Guardian" to 0xFFFF55,
        "Xalx" to 0xAAAAAA,
    )

    fun addFromChat(message: String) {
        if (!HollowsMap.showLocations || !HollowsMap.grabChat) return

        if (message.contains(":") && !message.startsWith("[Stella]")) {
            val userContent = message.substringAfter(":")
            val match = coordPattern.find(userContent) ?: return

            val x = match.groupValues[1].toInt()
            val y = match.groupValues[2].toInt()
            val z = match.groupValues[3].toInt()
            val pos = BlockPos(x, y, z)

            if (!checkInCrystals(pos)) return
            val lowerContent = userContent.lowercase(Locale.ENGLISH)

            for (areaName in areaColors.keys) {
                val matchingWord = areaName.lowercase(Locale.ENGLISH).split(" ").any { it in lowerContent }
                if (matchingWord) {
                    if (areaName !in activeWaypoints) addWaypoint(areaName, pos.x.toDouble(), pos.z.toDouble())
                    return
                }
            }

            val locString = "${pos.x} ${pos.y} ${pos.z}"
            Signal.fakeMessage(createSelectionMenu(locString))
        }
    }

    fun checkLocaiton(location: String) {
        if (location in activeWaypoints || location == "None" || location !in areaColors) return
        player?.let { p -> addWaypoint(location, p.x, p.z) }
    }

    fun addWaypoint(name: String, x: Double, z: Double) {
        removeUnknownNear(x, z)
        val color = areaColors[name] ?: 0xFFFFFF
        activeWaypoints[name] = Waypoint(name, x, z, color)
        Signal.modMessage("§fAdded waypoint for '§b$name§r' at §6${x.toInt()} ${z.toInt()}.")
    }

    private fun removeUnknownNear(x: Double, z: Double) {
        val unknown = activeWaypoints["Unknown"] ?: return
        val dx = unknown.x - x ; val dz = unknown.z - z
        if ((dx * dx + dz * dz) < REMOVE_UNKNOWN_DISTANCE * REMOVE_UNKNOWN_DISTANCE) activeWaypoints.remove("Unknown")
    }

    private fun checkInCrystals(pos: BlockPos): Boolean = pos.x in 202..823 && pos.z in 202..823 && pos.y in 31..188
    private fun createSelectionMenu(locationCoords: String) = Component.literal(Stella.PREFIX).append(" Choose Location: ").apply {
        val text = Component.empty()
        for (areaName in areaColors.keys) {
            if (areaName in activeWaypoints) continue
            val color = areaColors[areaName] ?: 0xFFFFFF

            text.append(
                Component.literal(" [$areaName]")
                    .withColor(color)
                    .onHover("Click to add waypoint")
                    .onClick { Signal.sendCommand("/stella_internal_add_wp $locationCoords $areaName") }
            )
        }

        append(text)
    }
}