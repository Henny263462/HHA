# Changelog

## 0.4.1 — Config files consolidated under config/hha/ (2026-07-15)

### Changed
- All HHA config files now live under `config/hha/`: `config/hha/hha.json` (settings), `config/hha/factions.json` and `config/hha/trust.json` (joining the custom recipes already in `config/hha/datapack/`). Legacy files (`config/hha.json`, `config/hha_factions.json`, `config/hha_trust.json`) are migrated automatically on first launch — no settings are lost.

## 0.4.0 — Chain charges, new beam icons & tankier Brutes (2026-07-15)

### Added
- Hell's Mace chain-pull is now a charge system: `pull_charges` quick chains (default 2, `pull_cooldown` 2 s between them), then a long `pull_recharge` cooldown (default 20 s) before the charges refill
- Dedicated HUD cooldown icons for each beam — a radiant light pillar for Heaven, a molten lance for Hell — plus a brand-new Heaven's Mace icon (golden mace struck by lightning)

### Changed
- Allied Piglin Brutes: 250 HP (up from 100), Sharpness V axe (up from IV), permanent Strength II, and invisible armor (armor + toughness attributes ≈ a Protection II set) that never covers the piglin look
- Light Wave now follows your full look direction: the particle line and hit arc travel up or down where you aim instead of always going flat, and the knockback carries that vertical aim

### Fixed
- Light Wave particles no longer always shoot straight ahead when looking up or down

## 0.3.3 — Sharper HUD icons & stronger Brutes (2026-07-15)

### Changed
- HUD cooldown icons sharpened: deeper shadows, brighter highlights and a theme-tinted dark outline around every glyph — the chain icon especially is much more readable now
- Allied Piglin Brutes now have 100 HP (up from 50) and wield a diamond axe with Sharpness IV in the Hell's Sword soul-fire look: no enchantment glint, soul flames licking from the axe hand

## 0.3.2 — Split particle settings & /hha settings (2026-07-15)

### Changed
- The particle setting is now split in two: `particle_player` scales only the idle particles on the player (orbit glow, armor/hand flames, halo ring, lava sparks, grace feathers, Undying Rage aura), `particle_effects` scales ability/impact effects. Functional visuals like the grapple/fire chain are never thinned. Replaces `particle_multiplier` from 0.3.0.
- New `/hha settings` menu: `/hha settings particles player|effects <0..1>` shows or sets the multipliers (also available via `/hha set`)

## 0.3.1 — Magma Stomp lava actually disappears (2026-07-15)

### Fixed
- Magma Stomp lava pools really do remove themselves now. The cleanup used `removeBlock()`, which in vanilla re-places the fluid at that position ("break the block, keep the liquid") — for a lava block that is a no-op, so the pool stayed forever. The cleanup now sets air explicitly.

### Added
- `stomp_lava_lifetime` config value (ticks, default 40 = 2 s): how long the Magma Stomp lava pool lasts before it removes itself

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
