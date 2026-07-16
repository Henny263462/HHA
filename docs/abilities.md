# Abilities

Paket: `dev.henny.hha.api.ability`

Aktive Fähigkeiten hängen sich an die Tastendrücke, die der HHA-Client
bereits sendet — Addons brauchen weder Keybinds noch Netzwerkpakete. In den
Steuerungsoptionen heißen die Tasten „Ability 1–6"; das Modell ist immer
**Ability-Taste → Ausrüstungs-Check → Funktion**.

## Slots

| `AbilitySlot` | Keybind (Default) | Eingebaut | Fallback-Meldung |
| --- | --- | --- | --- |
| `ABILITY_1` | „Ability 1" (G) | Licht-/Lavastrahl (volles Set) | `hha.msg.need_full_set` |
| `ABILITY_2` | „Ability 2" (H) | Fire Camp (Hell-Leggings) | `hha.msg.need_leggings` |
| `ABILITY_3` | „Ability 3" (U) | Ultra-Modus | keine |
| `ABILITY_4` | unbelegt | — frei für Addons | keine |
| `ABILITY_5` | unbelegt | — frei für Addons | keine |
| `ABILITY_6` | unbelegt | — frei für Addons | keine |
| `MOVEMENT` | Sprungtaste in der Luft | Doppelsprung (Heaven) | keine |

Ablauf pro Tastendruck (`HhaAbilities.dispatch`):

1. Erste registrierte Ability des Slots mit `isAvailable(player) == true`
   gewinnt — die eingebauten Heaven/Hell-Abilities sind zuerst registriert
   und matchen nur mit getragenem Heaven/Hell-Set
2. Fraktionssperre gegen das getragene Item im `factionGateSlot` der Ability
   (falls gesetzt; `FactionLock.canUse`)
3. `HhaEvents.ABILITY_CAST` (abbrechbar)
4. `cast(player)`
5. Matcht nichts, erscheint die Fallback-Meldung des Slots (falls gesetzt)

## Implementieren

Für die meisten Fälle reicht `SimpleAbility`:

```kotlin
context.registerAbility(
    SimpleAbility(context.id("frost_nova"), AbilitySlot.ABILITY_4, frostSet::hasFullSet, { player ->
        if (!context.toggle("frost_nova")) return@SimpleAbility

        val chest = player.getEquippedStack(EquipmentSlot.CHEST)
        if (player.itemCooldownManager.isCoolingDown(chest)) return@SimpleAbility
        Cooldowns.set(player, chest, "myaddon.nova_cooldown")

        // Wirkung ...
    }, factionGateSlot = EquipmentSlot.CHEST)
)
```

Für mehr Kontrolle das Interface `Ability` direkt implementieren
(`id`, `slot`, `factionGateSlot`, `isAvailable`, `cast`). Slots 4–6 sind
standardmäßig unbelegt — Spieler binden sie unter Optionen → Steuerung.

## Konventionen

- **`isAvailable` = nur Ausrüstungs-Voraussetzung.** Config-Toggles und
  Cooldowns gehören in `cast` — sonst rutscht der Dispatch bei deaktiviertem
  Feature zur nächsten Ability bzw. zur Fallback-Meldung durch.
- **Cooldowns über `dev.henny.hha.logic.Cooldowns.set(player, stack, configKey)`** —
  liest die Dauer (Ticks) aus der Config und setzt im Ultra-Modus keinen
  Cooldown. Den Config-Schlüssel vorher per `context.registerNumber` anlegen.
- **Ziele über `dev.henny.hha.logic.Targeting` filtern** (`shouldHarm`,
  `isFriendly`), damit Trust und Brute-Verbündete respektiert werden.

## Eingebaute Abilities

`hha:light_beam`, `hha:lava_beam` (ABILITY_1), `hha:fire_camp` (ABILITY_2),
`hha:ultra` (ABILITY_3), `hha:air_jump` (MOVEMENT) — abfragbar über
`HhaAbilities.get(id)` / `HhaAbilities.bySlot(slot)`.
