# HHA-Addons entwickeln

HHA bringt ein Addon-System mit: Ein Addon ist eine normale Fabric-Mod, die
sich über einen Entrypoint bei HHA einhängt und dessen Registries, Events und
Infrastruktur (Config, Commands, Cooldown-HUD, Keybinds, Fraktions- und
Trust-Logik) mitbenutzt — statt alles selbst zu bauen.

Diese Datei ist der Schnellstart; die Referenz liegt in [docs/](docs/README.md)
(Sets, Abilities, Events, Config & HUD).

## Schnellstart

**1. Projekt aufsetzen** — ein gewöhnliches Fabric-Mod-Projekt (Loom, Kotlin
oder Java). HHA als Abhängigkeit einbinden, z. B. über das gebaute Jar:

```kotlin
dependencies {
    modImplementation(files("libs/hha-<version>.jar"))
}
```

**2. Entrypoint deklarieren** — in der `fabric.mod.json` des Addons:

```json
{
  "entrypoints": {
    "hha": ["com.example.frost.FrostAddon"],
    "hha_client": ["com.example.frost.FrostAddonClient"]
  },
  "depends": { "hha": "*" }
}
```

`hha` ist der Server-/Common-Entrypoint, `hha_client` optional für
Client-Zeug (HUD-Slots). HHA ruft beide zum richtigen Zeitpunkt auf —
eigene `main`/`client`-Entrypoints braucht das Addon nur für Dinge
außerhalb der HHA-Welt.

**3. Addon implementieren:**

```kotlin
class FrostAddon : HhaAddon {

    override fun onInitialize(context: HhaAddonContext) {
        // Items registrieren (Assets liefert das Addon im eigenen Namespace mit)
        val helmet = HhaItemHelpers.registerArmor(context.id("frost_helmet"), FROST_MATERIAL, EquipmentType.HELMET)
        val chest  = HhaItemHelpers.registerArmor(context.id("frost_chestplate"), FROST_MATERIAL, EquipmentType.CHESTPLATE)
        val legs   = HhaItemHelpers.registerArmor(context.id("frost_leggings"), FROST_MATERIAL, EquipmentType.LEGGINGS)
        val boots  = HhaItemHelpers.registerArmor(context.id("frost_boots"), FROST_MATERIAL, EquipmentType.BOOTS)

        // Config-Schlüssel — tauchen als "frost.<key>" in /hha list|toggle|set auf
        context.registerToggle("frost_nova", default = true)
        context.registerNumber("nova_damage", default = 8.0)

        // Set registrieren: Voll-Set-Erkennung, Tick-Hook, HUD-/Stack-Abfragen
        val frostSet = context.armorSet("frost") {
            helmet(helmet); chestplate(chest); leggings(legs); boots(boots)
            onTick { world, player, state ->
                if (state.full && world.time % 20L == 0L) {
                    player.addStatusEffect(StatusEffectInstance(StatusEffects.RESISTANCE, 60, 0, true, false, true))
                }
            }
        }

        // Aktive Fähigkeit auf einem freien Ability-Slot (Keybind "Ability 4").
        // Muster: Ability-Taste -> Ausrüstungs-Check -> Funktion.
        context.registerAbility(
            SimpleAbility(context.id("frost_nova"), AbilitySlot.ABILITY_4, frostSet::hasFullSet, { player ->
                if (!context.toggle("frost_nova")) return@SimpleAbility
                val chestStack = player.getEquippedStack(EquipmentSlot.CHEST)
                if (player.itemCooldownManager.isCoolingDown(chestStack)) return@SimpleAbility
                // ... Nova wirken, context.number("nova_damage") als Schaden ...
            })
        )

        // Dynamische Lore mit echten Config-Werten — wie bei den HHA-Items.
        val damageKey = context.registerNumber("nova_damage", 8.0) // = "frost.nova_damage"
        context.registerLore(helmet) {
            line("item.frost.frost_helmet.lore1")
            line("item.frost.frost_helmet.lore2", num(damageKey), seconds("frost.nova_cooldown"))
        }
    }
}
```

## Bausteine im Überblick

### Sets — `dev.henny.hha.api.set`

`ArmorSet` (per `context.armorSet { ... }` gebaut) liefert `hasPiece`,
`hasFullSet`, `state(player)` und `isSetItem(stack)`. Der optionale
`onTick`-Hook läuft jeden Server-Tick, solange der Spieler mindestens ein
Teil trägt. `HhaSets` ist die globale Registry — dort stecken auch die
eingebauten Sets `hha:heaven` und `hha:hell`.

### Abilities — `dev.henny.hha.api.ability`

Sechs generische Ability-Tasten plus Bewegungs-Trigger. Das Modell ist immer
**Ability-Taste → Ausrüstungs-Check → Funktion**:

| `AbilitySlot` | Keybind (Default) | Eingebaut                     |
| ------------- | ----------------- | ----------------------------- |
| `ABILITY_1`   | „Ability 1" (G)   | Licht-/Lavastrahl (volles Set)|
| `ABILITY_2`   | „Ability 2" (H)   | Fire Camp (Hell-Leggings)     |
| `ABILITY_3`   | „Ability 3" (U)   | Ultra-Modus                   |
| `ABILITY_4–6` | unbelegt          | **frei für Addons**           |
| `MOVEMENT`    | Sprung in der Luft| Doppelsprung (Heaven)         |

Pro Slot gewinnt die **erste registrierte** Ability, deren
`isAvailable(player)` zutrifft (die eingebauten Heaven/Hell-Abilities sind
zuerst dran und matchen nur bei getragenem Heaven/Hell-Set) — Slots 4–6
gehören komplett den Addons. Über `factionGateSlot` (optionaler Parameter von
`SimpleAbility` bzw. Property im Interface) prüft der Dispatcher vor dem Cast
die Fraktionssperre auf dem getragenen Item dieses Slots. `cast` ist selbst
für Cooldowns und Config-Toggles zuständig — `Cooldowns.set(player, stack,
"frost.nova_cooldown")` liest die Dauer aus der Config und respektiert den
Ultra-Modus.

### Events — `dev.henny.hha.api.event.HhaEvents`

- `ABILITY_CAST` — vor jedem Cast, abbrechbar (`false` = blockieren)
- `PLAYER_TICK` — jeden Server-Tick pro lebendem Spieler
- `FULL_SET_CHANGED` — Voll-Set an-/abgelegt (alle registrierten Sets)
- `SHOULD_HARM` / `IS_FRIENDLY` — Freund/Feind-Erkennung überstimmen
  (`TriState.DEFAULT` = HHA-Regeln aus Trust & Brute-Verbündeten gelten)

### Config

`context.registerToggle("key", default)` / `registerNumber` legen Schlüssel
als `<addonid>.key` in `config/hha/hha.json` an. Sie sind sofort über
`/hha list` (dort gruppiert unter „Addon: <id>"), `/hha toggle`, `/hha set`,
`/hha get` administrierbar, persistieren automatisch und werden für die
Lore-Anzeige an alle Clients gesynct. Lesen per `context.toggle("key")` /
`context.number("key")` (oder `HhaConfig.enabled("addonid.key")`).

### Lore — `dev.henny.hha.api.lore.HhaLore`

`context.registerLore(item) { ... }` definiert dynamische Tooltip-Zeilen, die
live die echten Config-Werte zeigen (auch nach `/hha set`):

```kotlin
context.registerLore(FROST_BLADE) {
    line("item.frost.frost_blade.lore1")                              // statisch
    line("item.frost.frost_blade.lore2", num("frost.nova_damage"),    // %s-Platzhalter
         seconds("frost.nova_cooldown"))
}
```

`num` zeigt den Wert unverändert, `seconds` rechnet Ticks in Sekunden um,
`hearts` Half-Hearts in Herzen. Config-Schlüssel sind volle Keys
(`addonid.key` oder eingebaute wie `beam_damage`).

### Client-HUD — Entrypoint `hha_client`

```kotlin
class FrostAddonClient : HhaClientAddon {
    override fun onInitializeClient(context: HhaClientAddonContext) {
        context.registerHudCooldown(Identifier.of("frost", "hud/nova"), 0x6FD9FF) { player ->
            player.getEquippedStack(EquipmentSlot.CHEST).takeIf { frostSet.isSetItem(it) }
        }
    }
}
```

Der Slot erscheint in der HHA-Cooldown-Leiste über der Hotbar und zeigt den
Item-Cooldown des gelieferten Stacks mit Puls-Animation und Fortschrittsbalken.

### Nützliche Helfer (semi-stabil)

Diese internen Bausteine sind bewusst nutzbar, können sich aber zwischen
Versionen ändern:

- `logic.Fx` — Partikel-Ringe, Spiralen, orientierte Ringe im HHA-Look
- `logic.Cooldowns` — Config-gesteuerte Item-Cooldowns (Ultra-aware)
- `logic.Targeting` — `shouldHarm` / `canPull` / `isFriendly`
- `logic.Trust`, `logic.BruteAllies`, `logic.FactionLock`

## Grenzen (Stand jetzt)

- **FactionLock** kennt nur Heaven↔Hell. Addon-Sets sind fraktionsneutral —
  jeder darf sie tragen, sie lösen keine Bindung aus.
- **Kits** (`/hha kit`) sind auf Heaven/Hell beschränkt.
- **Keybinds** sind auf die vier Trigger begrenzt; eigene Tasten brauchen
  einen eigenen Client-Keybind + C2S-Payload im Addon.
- Assets (Texturen, Modelle, `equipment/*.json`, Lang-Dateien) liefert das
  Addon ganz normal in seinem eigenen Namespace mit — HHA übernimmt davon
  nichts.

## Ladereihenfolge

```
HHA: Items/Entities/Partikel → eingebaute Sets & Abilities
  → Addon-Entrypoints ("hha")          ← hier registriert dein Addon alles
  → Config laden (inkl. Addon-Defaults)
  → Commands, Networking, FactionLock, Combat, Tick-Loop
Client: HHA-HUD & Keybinds → Addon-Client-Entrypoints ("hha_client")
```

Wichtig: Im Entrypoint noch keine Config-Werte *lesen* und cachen — die
Datei wird erst danach geladen. In `cast`/`onTick`/Events ist Lesen immer
korrekt.
