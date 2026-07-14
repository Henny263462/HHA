package dev.henny.hha

import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes
import net.minecraft.particle.SimpleParticleType
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry

/**
 * Custom-Partikel des Mods. Die Client-Factories werden in
 * HhaParticleFactories registriert, die Texturen liegen unter
 * assets/hha/textures/particle/ mit Definitionen in assets/hha/particles/.
 */
object HhaParticles {

    /** Goldener Funke, der sanft aufsteigt und als Stern aufblitzt (Heaven). */
    lateinit var HOLY_SPARK: SimpleParticleType; private set

    /** Weiße Feder, die sacht herabschwebt und dabei pendelt (Heaven). */
    lateinit var FEATHER: SimpleParticleType; private set

    /** Großer Lichtblitz-Stern für Einschläge und Trigger (Heaven). */
    lateinit var DIVINE_FLASH: SimpleParticleType; private set

    /** Glühender Glutbrocken mit Flacker-Animation (Hell). */
    lateinit var EMBER_SPARK: SimpleParticleType; private set

    /** Kleine züngelnde Flamme (Hell). */
    lateinit var HELLFIRE: SimpleParticleType; private set

    /** Großer Feuerblitz-Stern für Einschläge und Trigger (Hell). */
    lateinit var INFERNAL_BURST: SimpleParticleType; private set

    /** Weiches Leuchtkorn — der Allzweck-Glitzer beider Sets. */
    lateinit var LIGHT_MOTE: SimpleParticleType; private set

    /** Dunkler Aschewisch mit Glutkern (Hell). */
    lateinit var SOOT: SimpleParticleType; private set

    /** Kettenglied — visualisiert die Ketten des Hell's Mace. */
    lateinit var CHAIN_LINK: SimpleParticleType; private set

    /** Seelenfeuer-Leuchtkorn — der Soul-Glanz des Hell-Sets. */
    lateinit var SOUL_MOTE: SimpleParticleType; private set

    /** Züngelnde Seelenfeuer-Flamme — lodert aus Hell-Rüstung und -Waffen. */
    lateinit var SOUL_FLAME: SimpleParticleType; private set

    /** Züngelnde Lichtflamme — lodert aus Heaven-Rüstung und -Waffen. */
    lateinit var HOLY_FLAME: SimpleParticleType; private set

    fun init() {
        HOLY_SPARK = register("holy_spark")
        FEATHER = register("feather")
        DIVINE_FLASH = register("divine_flash")
        EMBER_SPARK = register("ember_spark")
        HELLFIRE = register("hellfire")
        INFERNAL_BURST = register("infernal_burst")
        LIGHT_MOTE = register("light_mote")
        SOOT = register("soot")
        CHAIN_LINK = register("chain_link")
        SOUL_MOTE = register("soul_mote")
        SOUL_FLAME = register("soul_flame")
        HOLY_FLAME = register("holy_flame")
    }

    private fun register(name: String): SimpleParticleType =
        Registry.register(Registries.PARTICLE_TYPE, Hha.id(name), FabricParticleTypes.simple())
}
