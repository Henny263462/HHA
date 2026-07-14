# H&H Armors

Adds two rival armor sets: **Heaven** and **Hell**. Both are full endgame sets with their own abilities, a sword and a mace each. I originally built this for a two-faction SMP, but it works fine in normal survival too.

The Heaven set is white netherite with golden trim. The Hell set is dark netherite burning with blue soul fire (the item textures are animated). You craft both by upgrading diamond gear in a smithing table, similar to netherite upgrades.

## Abilities

Each armor piece has its own passive, wearing the full set unlocks more. A cooldown bar above the hotbar shows what's ready.

**Heaven:**
- Halo: you and nearby players slowly regenerate
- Double jump
- Long falls release a shockwave instead of hurting you
- Below 5 hearts you leave a light trail that slows enemies walking through it
- A beam attack that damages enemies but heals and cleanses teammates
- Sneak in the air to glide
- Land your hit combo and every following hit is an auto crit, until another player hits you

**Hell:**
- Walk on lava, and get Strength while burning
- Below 3 hearts: extra speed plus a fire burst
- Heavy landings crack the ground into temporary lava
- Fire Camp: summons three piglin brute bodyguards. A soul campfire spawns with soul soil, torches and lanterns around it, and the whole camp disappears again once the brutes are dead
- Sword ability: 20 seconds of 50% faster attacks where every hit crits, and crits give you golden apple absorption

**Maces** (both factions get one):
- Heaven's Mace can be thrown. It flies up to 50 blocks and strikes everything in its path with lightning, then comes back and hits everything again on the return
- Hell's Mace pulls enemies to you on a chain from up to 16 blocks. Right-click a block instead and it works as a grappling hook. You can even latch onto your own wind charges or ender pearls mid-flight

Full sets also give +10 hearts and fall damage immunity, and there's an Ultra Mode with its own cooldown.

## Config

Basically everything can be turned off or tuned: `/hha toggle` and `/hha set` change abilities, damage values and cooldowns live, no restart needed.

Recipes are editable too. On first launch the mod puts template files into `config/hha/datapack/`. Rename a `.json.example` to `.json`, edit it, run `/hha recipes reload` and your version replaces the built-in recipe. New files add completely new recipes.

Other stuff:
- `/trust <player>` makes sure your abilities, trails and brutes never hurt your friends
- `/hha kit hell|heaven` gives out a fully enchanted PvP kit (needs `kit_mode` enabled)
- Banner patterns for both factions

## Requirements

Fabric API and Fabric Language Kotlin. Client and server.

## Notes

- Piglin brutes get a soul-blue recolor of their tunic. This applies to all brutes, including bastions, since entity textures can't be swapped per mob
- Temporary blocks (lava, soul camp) clean themselves up. If the server hard-crashes while they exist they can stay behind
- Bug reports and ideas: [GitHub](https://github.com/Henny263462/HHA/issues)
