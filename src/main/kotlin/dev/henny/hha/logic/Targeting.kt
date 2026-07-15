package dev.henny.hha.logic

import dev.henny.hha.api.event.HhaEvents
import net.fabricmc.fabric.api.util.TriState
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.server.network.ServerPlayerEntity

/** Zentrale Freund/Feind-Erkennung für alle Fähigkeiten. */
object Targeting {

    /** Darf die Fähigkeit dieses Ziel verletzen? (nie: sich selbst, eigene Brutes, getrustete Spieler) */
    @JvmStatic
    fun shouldHarm(attacker: ServerPlayerEntity, target: LivingEntity): Boolean {
        if (target === attacker || !target.isAlive) return false
        val override = HhaEvents.SHOULD_HARM.invoker().check(attacker, target)
        if (override != TriState.DEFAULT) return override.get()
        if (BruteAllies.isAlly(target.uuid)) return false
        if (target is PlayerEntity && Trust.isTrusted(attacker.uuid, target.uuid)) return false
        return true
    }

    /**
     * Darf der Mace dieses Ziel heranziehen? Spieler sind immer pullbar
     * (auch getrustete — der Pull ist auch Rettungswerkzeug), eigene Brutes nie.
     * Schaden bei der Ankunft entscheidet weiterhin [shouldHarm].
     */
    @JvmStatic
    fun canPull(attacker: ServerPlayerEntity, target: LivingEntity): Boolean {
        if (target === attacker || !target.isAlive) return false
        return !BruteAllies.isAlly(target.uuid)
    }

    /** Ist das Ziel ein Verbündeter (für Heilung/Buffs wie Purify)? */
    @JvmStatic
    fun isFriendly(owner: ServerPlayerEntity, target: LivingEntity): Boolean {
        if (target === owner || !target.isAlive) return false
        val override = HhaEvents.IS_FRIENDLY.invoker().check(owner, target)
        if (override != TriState.DEFAULT) return override.get()
        if (BruteAllies.isAlly(target.uuid)) return true
        return target is PlayerEntity && Trust.isTrusted(owner.uuid, target.uuid)
    }
}
