# Abilities

Paket: `dev.henny.hha.api.ability`

Aktive Fähigkeiten hängen sich an die Tastendrücke, die der HHA-Client
bereits sendet — Addons brauchen weder Keybinds noch Netzwerkpakete.

## Trigger

| `AbilityTrigger` | Taste (Default) | Fraktions-Gate | Fallback-Meldung |
| --- | --- | --- | --- |
| `PRIMARY` | G | Brustplatte | `hha.msg.need_full_set` |
| `UTILITY` | H | Leggings | `hha.msg.need_leggings` |
| `MOVEMENT` | Sprungtaste in der Luft | Leggings | keine |
| `ULTRA` | U | Helm | keine |

Ablauf pro Tastendruck (`HhaAbilities.dispatch`):

1. Fraktionssperre gegen das Item im Gate-Slot (`FactionLock.canUse`)
2. Erste registrierte Ability mit `isAvailable(player) == true` gewinnt —
   die eingebauten Heaven/Hell-Abilities sind zuerst registriert und matchen
   nur mit getragenem Heaven/Hell-Set
3. `HhaEvents.ABILITY_CAST` (abbrechbar)
4. `cast(player)`
5. Matcht nichts, erscheint die Fallback-Meldung des Triggers (falls gesetzt)

## Implementieren

Für die meisten Fälle reicht `SimpleAbility`:

```kotlin
context.registerAbility(
    SimpleAbility(context.id("frost_nova"), AbilityTrigger.PRIMARY, frostSet::hasFullSet) { player ->
        if (!context.toggle("frost_nova")) return@SimpleAbility

        val chest = player.getEquippedStack(EquipmentSlot.CHEST)
        if (player.itemCooldownManager.isCoolingDown(chest)) return@SimpleAbility
        Cooldowns.set(player, chest, "myaddon.nova_cooldown")

        // Wirkung ...
    }
)
```

Für mehr Kontrolle das Interface `Ability` direkt implementieren
(`id`, `trigger`, `isAvailable`, `cast`).

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

`hha:light_beam`, `hha:lava_beam` (PRIMARY), `hha:fire_camp` (UTILITY),
`hha:air_jump` (MOVEMENT), `hha:ultra` (ULTRA) — abfragbar über
`HhaAbilities.get(id)` / `HhaAbilities.byTrigger(trigger)`.
