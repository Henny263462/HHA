# Changelog

## 0.3.0 — Particle settings, live item lore & debug commands (2026-07-15)

### Added
- `particle_multiplier` config value (default 1): scales all HHA particle effects, e.g. `/hha set particle_multiplier 0.3` for 30% density, 0 turns them off; small emitters are thinned probabilistically
- Debug commands: `/hha save` (write config to disk), `/hha load` (re-read config/hha.json), `/hha reload` (config + custom recipes + client sync)

### Changed
- Item lore is now built live on the client and shows the actual config values — damage, cooldowns (in seconds) and thresholds (in hearts); the server syncs its numbers on join and after `/hha set|reset|load`, so tooltips are always accurate
- Chestplates gained a lore line describing the full-set beam (damage, healing, cooldown)

## 0.2.1 — Light trail fix & rebalanced defaults (2026-07-15)

### Fixed
- Heaven's Step (light trail) now triggers from actual player movement. It previously checked server-side velocity, which stays at zero for normal walking and only gets set by external pushes — so the trail only appeared when another player was shoving you.

### Changed
- Rebalanced default config: kit mode on by default; harder-hitting abilities (stomp 60, beam 60, shockwave base 12 / max 5000, light wave 20 dmg / 5 knockback, pull arrival 40, bounce 16, purify heal 16) with longer cooldowns (beam 1000, divine shield 1600, mace throw 420, pull 120, grapple 80) and Magma Stomp from 10+ blocks; sword buff cooldown down to 1200. Existing `config/hha.json` files keep their saved values — use `/hha reset` to adopt the new defaults.

## 0.2.0 — Ability keybinds, configurable throw damage & fixes (2026-07-15)

### Changed
- Keybinds are now listed as "Ability 1" (set beam), "Ability 2" (Fire Camp) and "Ability 3" (Ultra) in the controls menu; lore and `/info` texts reference them accordingly
- Heaven's Mace throw damage is now configurable via `mace_throw_damage` (default 5, i.e. the old lightning damage); the lightning strikes are purely visual now, targets still catch fire
- Halo (Heaven's Helmet) now grants Regeneration only to trusted players in range instead of everyone

### Fixed
- Magma Stomp lava pools now clean themselves up reliably: on expiry the placed sources and any lava that flowed away from them are removed (natural lava sources are left untouched)

## 0.1.0 — Addon system (2026-07-15)

### Addon system
- Addons are regular Fabric mods that hook into HHA via the new `"hha"` entrypoint (optional `"hha_client"` for HUD) — see `ADDONS.md` and `docs/`
- Public API under `dev.henny.hha.api`: armor-set registry with builder DSL and tick hooks, ability registry bound to the existing keybinds, item registration helpers
- Events: `ABILITY_CAST` (cancellable), `PLAYER_TICK`, `FULL_SET_CHANGED`, `SHOULD_HARM`/`IS_FRIENDLY` overrides
- Addon config keys (`<addonid>.<key>`) live in `config/hha.json` and work with `/hha list|toggle|set|get`
- Addons can add slots to the cooldown HUD with their own icon and accent color
- Internally, the built-in Heaven/Hell sets and abilities now register through the same registries

## 0.0.1 — Initial release (2026-07-14)

First public release for Minecraft 1.21.11 (Fabric).

### Sets & abilities
- Heaven Set: Halo, Ascension (double jump), Shockwave, Heaven's Step light trail, Light Beam + Purify, Grace glide, Divine Shield
- Hell Set: Undying Rage, Warlord's Barrier, lava walking, Magma Stomp + Volcanic, Ember Trail, Hellforged, Fire Camp with Piglin Brute vassals and reverting soul camp (soul campfire, soul soil/sand, torches, lanterns)
- Full-set bonus (+10 hearts), fall damage immunity, Ultra Mode, faction lock, trust system

### Weapons
- Heaven's Sword: combo auto-crits; after the combo lands, every hit crits until another player hits you
- Hell's Sword: Blade Rush active (Haste V ≙ +50% attack speed, guaranteed crits, absorption blessing on crit), animated soul-fire blade
- Heaven's Mace: throwable, calls lightning along its path — outbound and on return
- Hell's Mace: chain pull (enemies are reeled all the way in), block grapple, wind charge / ender pearl chain-ride

### Content & UX
- Animated item textures, soul/holy equipment flames, custom particles, cooldown HUD with custom icons
- Hell/Heaven ingots, smithing upgrades via Jötunheim's Upgrade Template, banner patterns
- PvP kit mode (`/hha kit`), fully enchanted kits with strength & weaving splash potions
- Live config via `/hha`, per-recipe toggles, customizable recipes through `config/hha/datapack/`
- Soul-blue retexture of the Piglin Brute tunic
