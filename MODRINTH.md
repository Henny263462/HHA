# H&H Armors — Heaven & Hell Sets

**Two rival endgame armor sets — one blessed by the light, one forged in soul fire — each with its own weapons, abilities and playstyle.**

Choose your side: the radiant **Heaven Set** with golden trims and holy light, or the **Hell Set** — blackened netherite burning with dark-blue soul fire. Both sets come with a full ability kit, a unique sword and a unique mace, animated textures, particle effects and satisfying combat feedback.

---

## ⚔️ The Sets

### 😇 Heaven Set
- **Halo** — regenerate yourself and nearby allies
- **Ascension** — double jump through the air
- **Shockwave** — slam the ground after a long fall, damaging everything around you
- **Heaven's Step** — at low health, leave a glowing trail of light that slows your enemies
- **Light Beam & Purify** — a targeted beam that damages enemies and heals & cleanses allies
- **Grace** — sneak mid-air to glide gently down
- **Blessing** — land your combo and *every* following hit becomes an auto-crit (with its own visuals and sound) until another player hits you

### 😈 Hell Set
- **Undying Rage** — at low health, gain extra speed and burst into hellfire
- **Warlord's Barrier** — absorption and resistance while your brutes fight beside you
- **Lava Walking & Hellforged** — stroll over lava; gain strength while burning
- **Magma Stomp & Volcanic** — heavy landings crack the ground into temporary lava
- **Fire Camp** — summon three loyal Piglin Brute vassals in soul-blue garb. A real **soul campfire** appears, and the ground around it transforms into a soul soil camp with soul torches and lanterns — everything reverts once your brutes have fallen
- **Blade Rush** — the Hell's Sword's active: 50% faster attacks and guaranteed crits for 20 seconds; crits bless you with golden-apple absorption

### 🗡️ Weapons
- **Heaven's Sword** — combo-based auto-crits with holy feedback
- **Hell's Sword** — soul-fire blade with an animated glowing edge and wandering runes
- **Heaven's Mace** — throw it: it flies up to 50 blocks, calls **lightning** on everything in its path — on the way out *and* on the way back
- **Hell's Mace** — a chain weapon: **pull enemies** to you from up to 16 blocks (they *will* arrive), **grapple** to blocks, or chain-ride your own wind charges and ender pearls

### ✨ Shared
- Full-set bonus: **+10 hearts**, fall damage immunity and more
- **Ultra Mode** — a temporary power-up state with its own HUD banner
- Animated item textures, soul/holy flames on worn equipment, custom particles
- Cooldown HUD above the hotbar with custom ability icons

---

## 🔨 Crafting

Craft **Hell / Heaven Ingots** from netherite, soul materials, amethyst and glowstone, then upgrade diamond gear in the **smithing table** using **Jötunheim's Upgrade Template** — just like netherite upgrades. Banner patterns included.

---

## ⚙️ Fully Configurable

- Every ability can be toggled and tuned live via `/hha` (OP): damage values, cooldowns, thresholds, trigger conditions
- **Custom recipes**: the folder `config/hha/datapack/` is mirrored into your world as an auto-enabled datapack — edit the provided templates to change any recipe or add your own, then `/hha recipes reload`
- Recipe toggles: `hell_ingot_recipe`, `heaven_ingot_recipe`, `template_recipe`
- **Kit mode** for PvP servers: `/hha kit hell|heaven` hands out a full, fully-enchanted PvP kit (strength & weaving splash potions included)
- **Trust system**: `/trust <player>` — trusted players are never harmed by your abilities, trails or brutes

---

## 📋 Requirements

- Fabric Loader + **Fabric API**
- **Fabric Language Kotlin**
- Minecraft 1.21.11

## ⚠️ Good to know

- The mod retextures the **Piglin Brute**'s dark tunic into soul-blue (applies to all brutes, including bastion ones — it fits the theme)
- Temporary blocks (soul camp, magma stomp lava) revert automatically; a hard server crash while they're active can leave them behind
- Works in singleplayer and on dedicated servers
