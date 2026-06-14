package co.stellarskys.miningstuff.features

import co.stellarskys.stella.annotations.Module
import co.stellarskys.stella.api.handlers.Chronos
import co.stellarskys.stella.api.zenith.player
import co.stellarskys.stella.events.core.ChatEvent
import co.stellarskys.stella.features.Feature
import co.stellarskys.stella.utils.Utils
import co.stellarskys.stella.utils.config
import net.minecraft.sounds.SoundEvents
import tech.thatgravyboat.skyblockapi.utils.extentions.getLore
import tech.thatgravyboat.skyblockapi.utils.text.TextProperties.stripped
import kotlin.time.Duration.Companion.seconds

@Module
object AbilityAlert: Feature("abilityAlert") {
    private val chatRegex = "^You used your .* Pickaxe Ability!$".toRegex()
    private val toolRegex = "DRILL|PICKAXE|GAUNTLET".toRegex()
    private val abilityRegex = """Ability:\s*(.+?)\s*RIGHT CLICK""".toRegex()
    private val cooldownRegex = """Cooldown:\s*(\d+)s""".toRegex()

    init {
        config.subcategory("Ability Alert", "Mining", "abilityAlert")
    }

    override fun initialize() {
        on<ChatEvent.Receive> { event ->
            event matches chatRegex run {
                val heldItem = player?.mainHandItem ?: return@run
                val cleanLore = heldItem.getLore().map { it.stripped }
                if (cleanLore.lastOrNull()?.contains(toolRegex) != true) return@run
                val text = cleanLore.joinToString("\n")
                val ability = abilityRegex.find(text)?.groupValues?.get(1)?.uppercase() ?: return@run
                val cooldown = cooldownRegex.find(text)?.groupValues?.get(1)?.toIntOrNull() ?: 0

                Chronos.Async after cooldown.seconds run {
                    Utils.alert("§6$ability", SoundEvents.EXPERIENCE_ORB_PICKUP)
                }
            }
        }
    }
}