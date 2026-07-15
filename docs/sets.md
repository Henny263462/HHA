# Sets

Paket: `dev.henny.hha.api.set`

Ein `ArmorSet` bündelt vier Rüstungsteile plus optionale Set-Waffen und ist
die Grundlage für Voll-Set-Erkennung, Passiveffekte und HUD-Abfragen.

## Registrieren

Items zuerst regulär registrieren (z. B. über
`dev.henny.hha.api.item.HhaItemHelpers`), dann das Set über den Kontext:

```kotlin
val frostSet = context.armorSet("frost") {
    helmet(FROST_HELMET)
    chestplate(FROST_CHESTPLATE)
    leggings(FROST_LEGGINGS)
    boots(FROST_BOOTS)
    weapon(FROST_BLADE)          // optional, zählt für isSetItem()
    onTick { world, player, state ->
        if (state.full && world.time % 20L == 0L) {
            // Voll-Set-Bonus, im Sekundenraster
        }
    }
}
```

`context.armorSet("frost")` registriert unter `<addonid>:frost`. Alternativ:
`armorSetBuilder(id).…​.build()` + `context.registerSet(set)`.

## Abfragen

| Methode | Bedeutung |
| --- | --- |
| `hasPiece(player, slot)` | Trägt der Spieler das Setteil in diesem Slot? |
| `hasFullSet(player)` | Alle definierten Teile getragen? |
| `state(player)` | `SetState` mit `helmet/chestplate/leggings/boots/full/anyPiece` |
| `isSetItem(stack)` | Gehört der Stack zum Set (Rüstung oder Waffe)? |
| `piece(slot)` | Item des Sets im Slot oder `null` |

## Tick-Hook

Der `onTick`-Ticker läuft **jeden Server-Tick**, sobald der Spieler
mindestens ein Teil trägt (Spectator/tote Spieler ausgenommen). Für
Sekunden-Effekte selbst rastern (`world.time % 20L == 0L`). Der `SetState`
ist bereits berechnet — keine eigenen Equipment-Abfragen nötig.

## Globale Registry

`HhaSets` hält alle Sets, auch die eingebauten (`HhaSets.HEAVEN_ID`,
`HhaSets.HELL_ID`):

```kotlin
HhaSets.get(Identifier.of("hha", "hell"))   // ArmorSet?
HhaSets.setOf(stack)                        // zu welchem Set gehört der Stack?
HhaSets.all()
```

Voll-Set-Wechsel aller registrierten Sets melden sich über das Event
`HhaEvents.FULL_SET_CHANGED` — siehe [Events](events.md).
