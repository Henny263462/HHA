# Config & HUD

## Config-Schlüssel

Addons legen ihre Einstellungen über den Kontext an — im `"hha"`-Entrypoint,
**bevor** HHA die Config lädt:

```kotlin
context.registerToggle("frost_nova", default = true)    // → "myaddon.frost_nova"
context.registerNumber("nova_damage", default = 8.0)    // → "myaddon.nova_damage"
context.registerNumber("nova_cooldown", default = 200.0)
```

Die Schlüssel landen als `<addonid>.<key>` in `config/hha/hha.json` und verhalten
sich wie eingebaute:

- `/hha list` zeigt sie, `/hha toggle|set|get` ändern sie live (OP Level 2)
- Werte persistieren und überleben `/hha reset` als Default

Lesen (überall außer im Entrypoint selbst — dort ist die Datei noch nicht
geladen):

```kotlin
context.toggle("frost_nova")        // kurzer Key
context.number("nova_damage")
HhaConfig.enabled("myaddon.frost_nova")  // voller Key, z. B. aus statischem Kontext
```

Konvention: Cooldown-Schlüssel enthalten die Dauer in **Ticks** und werden
über `Cooldowns.set(player, stack, "myaddon.nova_cooldown")` angewendet.

## Cooldown-HUD

Über den `"hha_client"`-Entrypoint (`HhaClientAddon`) hängen Addons Slots in
die HHA-Leiste über der Hotbar:

```kotlin
class MyAddonClient : HhaClientAddon {
    override fun onInitializeClient(context: HhaClientAddonContext) {
        context.registerHudCooldown(
            Identifier.of("myaddon", "hud/nova"),   // GUI-Sprite
            0x6FD9FF,                               // Akzentfarbe 0xRRGGBB
        ) { player ->
            player.getEquippedStack(EquipmentSlot.CHEST)
                .takeIf { stack -> frostSet.isSetItem(stack) }
        }
    }
}
```

- Der Provider läuft pro Frame; `null`/leerer Stack blendet den Slot aus
  (z. B. wenn das Setteil nicht getragen wird).
- Angezeigt wird der **Item-Cooldown** des gelieferten Stacks
  (`itemCooldownManager`) — genau das, was `Cooldowns.set` setzt.
- Das Icon ist ein GUI-Sprite: `assets/<ns>/textures/gui/sprites/<pfad>.png`
  (im Beispiel `assets/myaddon/textures/gui/sprites/hud/nova.png`).
- Bereite Fähigkeiten pulsieren in der Akzentfarbe, laufende Cooldowns
  dunkeln ab und zeigen einen Fortschrittsbalken — wie die eingebauten Slots.
