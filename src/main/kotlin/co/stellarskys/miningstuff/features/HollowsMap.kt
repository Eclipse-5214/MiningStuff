package co.stellarskys.miningstuff.features

import co.stellarskys.miningstuff.MiningStuff
import co.stellarskys.stella.annotations.Module
import co.stellarskys.stella.api.zenith.player
import co.stellarskys.stella.events.core.ChatEvent
import co.stellarskys.stella.events.core.GuiEvent
import co.stellarskys.stella.events.core.LocationEvent
import co.stellarskys.stella.features.Feature
import co.stellarskys.stella.hud.HUDManager
import co.stellarskys.stella.utils.Utils
import co.stellarskys.stella.utils.config
import co.stellarskys.stella.utils.render.Render2D
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.resources.Identifier
import net.minecraft.util.Mth
import org.joml.Vector2i
import org.joml.Vector2ic
import tech.thatgravyboat.skyblockapi.api.location.SkyBlockIsland
import tech.thatgravyboat.skyblockapi.platform.pushPop

@Module
object HollowsMap: Feature("hollowsMap", island = SkyBlockIsland.CRYSTAL_HOLLOWS) {
    private const val HUD_NAME = "Crystal Hollows Map"
    private val MAP_TEXTURE = Identifier.fromNamespaceAndPath(MiningStuff.NAMESPACE, "textures/gui/crystals_map.png")
    private val MAP_ICON = Identifier.withDefaultNamespace("textures/map/decorations/player.png")
    private val SMALL_LOCATIONS = setOf("Fairy Grotto", "King Yolkar", "Corleone", "Odawa", "Key Guardian", "Xalx", "Unknown")

    val map = config.subcategory("Crystal Hollows Map", "Mining", "hollowsMap")
    val grabChat by map.toggle("hollowsMap.grabChat", "Grab Chat", "Grabs locations from chat")
    val showLocations by map.toggle("hollowsMap.showLocations", "Show Locations", "Displays discovered waypoint markers on the map")
    val locationSize by map.stepslider("hollowsMap.locationSize", "Location Size", "The baseline dimension size of waypoint markers", 0, 16, 1, 8)

    override fun initialize() {
        HUDManager.registerCustom(HUD_NAME, 72, 72, this::hudEditorRender, "hollowsMap")
        on<GuiEvent.RenderHUD> { event -> renderNormal(event.context) }
        on<ChatEvent.Receive> { event -> LocationManager.addFromChat(event.stripped) }
        on<LocationEvent.AreaChange> { event -> LocationManager.checkLocaiton(event.new.name) }
        on<LocationEvent.ServerChange> { LocationManager.activeWaypoints.clear() }
    }

    fun hudEditorRender(context: GuiGraphicsExtractor) { renderMap(context, 512.0, 512.0, 0f) }
    fun renderNormal(context: GuiGraphicsExtractor) = HUDManager.renderHud(HUD_NAME, context) { player?.let { renderMap(context, it.x, it.z, it.yRot) } }

    fun renderMap(context: GuiGraphicsExtractor, px: Double, pz: Double, rot: Float) = context.pushPop {
        val matrix = context.pose()
        matrix.translate(5f, 5f)
        Render2D.drawImage(context, MAP_TEXTURE, 0, 0, 62, 62)

        if (showLocations) {
            for (waypoint in LocationManager.activeWaypoints.values) {
                val renderPos = transformLocation(waypoint.x, waypoint.z)
                var size = locationSize
                if (waypoint.name in SMALL_LOCATIONS) { size /= 2 }
                size = size.coerceAtLeast(1)

                val minX = renderPos.x() - size / 2
                val minY = renderPos.y() - size / 2
                val maxX = renderPos.x() + size / 2
                val maxY = renderPos.y() + size / 2

                context.fill(minX, minY, maxX, maxY, (0xFF shl 24) or waypoint.color)
            }
        }

        val renderPos = transformLocation(px, pz)
        val renderX = renderPos.x() - 2
        val renderY = renderPos.y() - 3

        context.pushPop {
            matrix.translate(renderX.toFloat(), renderY.toFloat())
            matrix.scale(0.75f, 0.75f)
            matrix.rotateAbout(Mth.DEG_TO_RAD * yawToCardinal(rot), 2.5f, 3.5f)
            context.blit(RenderPipelines.GUI_TEXTURED, MAP_ICON, 0, 0, 2f, 0f, 5, 7, 5, 7, 8, 8)
        }
    }

    private fun transformLocation(x: Double, z: Double): Vector2ic {
        val transformedX = Utils.mapRange(x, 202.0, 823.0, 0.0, 62.0).toInt().coerceIn(0, 62)
        val transformedY = Utils.mapRange(z, 202.0, 823.0, 0.0, 62.0).toInt().coerceIn(0, 62)
        return Vector2i(transformedX, transformedY)
    }

    private fun yawToCardinal(yaw: Float): Float {
        var adjustedYaw = yaw + 180f
        adjustedYaw += if (adjustedYaw < 0.0f) -8.0f else 8.0f
        val clipped = (adjustedYaw * 16.0f / 360.0f).toInt().toByte()
        return (clipped * 360f) / 16f
    }
}