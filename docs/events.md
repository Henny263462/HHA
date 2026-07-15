# Events

Paket: `dev.henny.hha.api.event` — alles in `HhaEvents`, im Fabric-Event-Stil
(`EVENT.register { … }`).

## `ABILITY_CAST`

Vor jedem Ability-Cast (eingebaute und Addon-Abilities). Liefert ein Listener
`false`, wird der Cast abgebrochen.

```kotlin
HhaEvents.ABILITY_CAST.register { player, ability ->
    ability.id != Identifier.of("hha", "ultra") || !player.isInLava
}
```

## `PLAYER_TICK`

Jeden Server-Tick pro lebendem, nicht-zuschauendem Spieler — nach den
HHA-Passiveffekten. Der bequemste Ort für eigene Dauereffekte ohne eigene
Tick-Registrierung.

```kotlin
HhaEvents.PLAYER_TICK.register { world, player -> /* ... */ }
```

## `FULL_SET_CHANGED`

Feuert, wenn sich der Voll-Set-Status eines **registrierten** Sets ändert —
auch für `hha:heaven` und `hha:hell`. `equipped = true` beim Anlegen des
letzten Teils, `false` beim Ablegen des ersten.

```kotlin
HhaEvents.FULL_SET_CHANGED.register { player, set, equipped ->
    if (set.id == frostSet.id && equipped) { /* Einmal-Effekt */ }
}
```

## `SHOULD_HARM` / `IS_FRIENDLY`

Überstimmen die zentrale Freund/Feind-Erkennung (`logic.Targeting`), die alle
HHA-Fähigkeiten nutzen. Rückgabe ist `TriState`:

- `TriState.TRUE` / `FALSE` — Ergebnis erzwingen (erster Nicht-DEFAULT gewinnt)
- `TriState.DEFAULT` — HHA-Regeln gelten (nie sich selbst, eigene Brutes,
  getrustete Spieler)

```kotlin
HhaEvents.SHOULD_HARM.register { attacker, target ->
    if (target is VillagerEntity) TriState.FALSE else TriState.DEFAULT
}
```

Hinweis: Der Selbst-/Tot-Check (`target === owner`, `!target.isAlive`) läuft
vor den Listenern und ist nicht überstimmbar.
