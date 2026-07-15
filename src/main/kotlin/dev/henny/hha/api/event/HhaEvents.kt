package dev.henny.hha.api.event

import dev.henny.hha.api.ability.Ability
import dev.henny.hha.api.set.ArmorSet
import net.fabricmc.fabric.api.event.Event
import net.fabricmc.fabric.api.event.EventFactory
import net.fabricmc.fabric.api.util.TriState
import net.minecraft.entity.LivingEntity
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld

/**
 * Hooks in die HHA-Kernlogik im Fabric-Event-Stil. Addons können Verhalten
 * beobachten oder verändern, ohne eigene Mixins zu schreiben.
 */
object HhaEvents {

    /** Vor jedem Ability-Cast; liefert ein Listener `false`, wird abgebrochen. */
    fun interface AbilityCast {
        fun allowCast(player: ServerPlayerEntity, ability: Ability): Boolean
    }

    /** Jeden Server-Tick pro lebendem Spieler (nach den HHA-Passiveffekten). */
    fun interface PlayerTick {
        fun onTick(world: ServerWorld, player: ServerPlayerEntity)
    }

    /** Voll-Set-Status eines registrierten Sets hat sich geändert. */
    fun interface FullSetChanged {
        fun onChanged(player: ServerPlayerEntity, set: ArmorSet, equipped: Boolean)
    }

    /**
     * Überstimmt die Freund/Feind-Erkennung: TRUE/FALSE erzwingt das Ergebnis,
     * DEFAULT überlässt es den HHA-Regeln (Trust, Brute-Verbündete).
     */
    fun interface TargetFilter {
        fun check(owner: ServerPlayerEntity, target: LivingEntity): TriState
    }

    @JvmField
    val ABILITY_CAST: Event<AbilityCast> =
        EventFactory.createArrayBacked(AbilityCast::class.java) { listeners ->
            AbilityCast { player, ability -> listeners.all { it.allowCast(player, ability) } }
        }

    @JvmField
    val PLAYER_TICK: Event<PlayerTick> =
        EventFactory.createArrayBacked(PlayerTick::class.java) { listeners ->
            PlayerTick { world, player -> listeners.forEach { it.onTick(world, player) } }
        }

    @JvmField
    val FULL_SET_CHANGED: Event<FullSetChanged> =
        EventFactory.createArrayBacked(FullSetChanged::class.java) { listeners ->
            FullSetChanged { player, set, equipped ->
                listeners.forEach { it.onChanged(player, set, equipped) }
            }
        }

    /** Hook in [dev.henny.hha.logic.Targeting.shouldHarm] — erster Nicht-DEFAULT gewinnt. */
    @JvmField
    val SHOULD_HARM: Event<TargetFilter> = targetFilterEvent()

    /** Hook in [dev.henny.hha.logic.Targeting.isFriendly] — erster Nicht-DEFAULT gewinnt. */
    @JvmField
    val IS_FRIENDLY: Event<TargetFilter> = targetFilterEvent()

    private fun targetFilterEvent(): Event<TargetFilter> =
        EventFactory.createArrayBacked(TargetFilter::class.java) { listeners ->
            TargetFilter { owner, target ->
                for (listener in listeners) {
                    val result = listener.check(owner, target)
                    if (result != TriState.DEFAULT) return@TargetFilter result
                }
                TriState.DEFAULT
            }
        }
}
