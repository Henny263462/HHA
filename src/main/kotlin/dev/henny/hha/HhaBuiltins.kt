package dev.henny.hha

import dev.henny.hha.api.ability.AbilityTrigger
import dev.henny.hha.api.ability.HhaAbilities
import dev.henny.hha.api.ability.SimpleAbility
import dev.henny.hha.api.set.HhaSets
import dev.henny.hha.api.set.armorSetBuilder
import dev.henny.hha.config.HhaConfig
import dev.henny.hha.logic.Abilities
import dev.henny.hha.logic.AirJumps
import dev.henny.hha.logic.HeavenSet
import dev.henny.hha.logic.HellSet
import dev.henny.hha.logic.UltraMode

/**
 * Registriert die eingebauten Heaven/Hell-Sets und -Fähigkeiten über dieselben
 * Registries, die auch Addons nutzen. Die Passiveffekte der beiden Sets bleiben
 * in [dev.henny.hha.logic.PassiveEffects] — hier hängt nur die Registrierung.
 */
object HhaBuiltins {

    fun init() {
        HhaSets.register(
            armorSetBuilder(HhaSets.HELL_ID)
                .helmet(HhaItems.HELL_HELMET)
                .chestplate(HhaItems.HELL_CHESTPLATE)
                .leggings(HhaItems.HELL_LEGGINGS)
                .boots(HhaItems.HELL_BOOTS)
                .weapon(HhaItems.HELLS_SWORD)
                .weapon(HhaItems.HELLS_MACE)
                .build()
        )
        HhaSets.register(
            armorSetBuilder(HhaSets.HEAVEN_ID)
                .helmet(HhaItems.HEAVEN_HELMET)
                .chestplate(HhaItems.HEAVEN_CHESTPLATE)
                .leggings(HhaItems.HEAVEN_LEGGINGS)
                .boots(HhaItems.HEAVEN_BOOTS)
                .weapon(HhaItems.HEAVENS_SWORD)
                .weapon(HhaItems.HEAVENS_MACE)
                .build()
        )

        // Reihenfolge = Prüfreihenfolge beim Dispatch (Heaven vor Hell wie bisher).
        HhaAbilities.register(
            SimpleAbility(Hha.id("light_beam"), AbilityTrigger.PRIMARY, HeavenSet::hasFullSet) { player ->
                if (HhaConfig.enabled("light_beam")) Abilities.lightBeam(player)
            }
        )
        HhaAbilities.register(
            SimpleAbility(Hha.id("lava_beam"), AbilityTrigger.PRIMARY, HellSet::hasFullSet) { player ->
                if (HhaConfig.enabled("lava_beam")) Abilities.lavaBeam(player)
            }
        )
        HhaAbilities.register(
            SimpleAbility(Hha.id("fire_camp"), AbilityTrigger.UTILITY, HellSet::hasLeggings) { player ->
                if (HhaConfig.enabled("fire_camp")) Abilities.fireCamp(player)
            }
        )
        HhaAbilities.register(
            SimpleAbility(Hha.id("air_jump"), AbilityTrigger.MOVEMENT, { true }, AirJumps::tryJump)
        )
        HhaAbilities.register(
            SimpleAbility(Hha.id("ultra"), AbilityTrigger.ULTRA, { true }, UltraMode::tryActivate)
        )
    }
}
