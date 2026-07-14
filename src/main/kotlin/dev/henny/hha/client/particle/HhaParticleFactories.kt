package dev.henny.hha.client.particle

import dev.henny.hha.HhaParticles
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry
import net.minecraft.client.particle.ParticleFactory
import net.minecraft.particle.SimpleParticleType

/** Registriert die Client-Factories für alle HHA-Partikel. */
object HhaParticleFactories {

    fun register() {
        ParticleFactoryRegistry.getInstance().register(HhaParticles.HOLY_SPARK) { provider ->
            ParticleFactory<SimpleParticleType> { _, world, x, y, z, vx, vy, vz, _ ->
                SparkleParticle(world, x, y, z, vx, vy + 0.02, vz, provider, 24, 1.4f, 0.001f, true)
            }
        }
        ParticleFactoryRegistry.getInstance().register(HhaParticles.EMBER_SPARK) { provider ->
            ParticleFactory<SimpleParticleType> { _, world, x, y, z, vx, vy, vz, _ ->
                SparkleParticle(world, x, y, z, vx, vy + 0.04, vz, provider, 20, 1.2f, 0.004f, true)
            }
        }
        ParticleFactoryRegistry.getInstance().register(HhaParticles.HELLFIRE) { provider ->
            ParticleFactory<SimpleParticleType> { _, world, x, y, z, vx, vy, vz, _ ->
                SparkleParticle(world, x, y, z, vx, vy + 0.03, vz, provider, 16, 1.6f, 0.002f, true)
            }
        }
        ParticleFactoryRegistry.getInstance().register(HhaParticles.FEATHER) { provider ->
            ParticleFactory<SimpleParticleType> { _, world, x, y, z, _, _, _, _ ->
                FeatherParticle(world, x, y, z, provider)
            }
        }
        ParticleFactoryRegistry.getInstance().register(HhaParticles.DIVINE_FLASH) { provider ->
            ParticleFactory<SimpleParticleType> { _, world, x, y, z, _, _, _, _ ->
                BurstParticle(world, x, y, z, provider)
            }
        }
        ParticleFactoryRegistry.getInstance().register(HhaParticles.INFERNAL_BURST) { provider ->
            ParticleFactory<SimpleParticleType> { _, world, x, y, z, _, _, _, _ ->
                BurstParticle(world, x, y, z, provider)
            }
        }
        ParticleFactoryRegistry.getInstance().register(HhaParticles.LIGHT_MOTE) { provider ->
            ParticleFactory<SimpleParticleType> { _, world, x, y, z, vx, vy, vz, _ ->
                SparkleParticle(world, x, y, z, vx, vy, vz, provider, 26, 1.0f, 0.0005f, true)
            }
        }
        ParticleFactoryRegistry.getInstance().register(HhaParticles.SOOT) { provider ->
            ParticleFactory<SimpleParticleType> { _, world, x, y, z, vx, vy, vz, _ ->
                SparkleParticle(world, x, y, z, vx, vy + 0.02, vz, provider, 30, 1.8f, 0.005f, false)
            }
        }
        ParticleFactoryRegistry.getInstance().register(HhaParticles.CHAIN_LINK) { provider ->
            ParticleFactory<SimpleParticleType> { _, world, x, y, z, _, _, _, _ ->
                ChainLinkParticle(world, x, y, z, provider)
            }
        }
        ParticleFactoryRegistry.getInstance().register(HhaParticles.SOUL_MOTE) { provider ->
            ParticleFactory<SimpleParticleType> { _, world, x, y, z, vx, vy, vz, _ ->
                SparkleParticle(world, x, y, z, vx, vy, vz, provider, 26, 1.0f, 0.0005f, true)
            }
        }
        ParticleFactoryRegistry.getInstance().register(HhaParticles.SOUL_FLAME) { provider ->
            ParticleFactory<SimpleParticleType> { _, world, x, y, z, vx, vy, vz, _ ->
                SparkleParticle(world, x, y, z, vx, vy + 0.03, vz, provider, 14, 1.3f, 0.002f, true)
            }
        }
        ParticleFactoryRegistry.getInstance().register(HhaParticles.HOLY_FLAME) { provider ->
            ParticleFactory<SimpleParticleType> { _, world, x, y, z, vx, vy, vz, _ ->
                SparkleParticle(world, x, y, z, vx, vy + 0.03, vz, provider, 14, 1.3f, 0.002f, true)
            }
        }
    }
}
