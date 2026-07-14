# H&H Armors — Heaven & Hell Sets

**Zwei rivalisierende Endgame-Rüstungssets — eines vom Licht gesegnet, eines im Seelenfeuer geschmiedet — jedes mit eigenen Waffen, Fähigkeiten und Spielstil.**

Wähle deine Seite: das strahlende **Heaven-Set** mit goldenen Verzierungen und heiligem Licht, oder das **Hell-Set** — geschwärztes Netherite, das in dunkelblauem Seelenfeuer brennt. Beide Sets kommen mit komplettem Fähigkeiten-Kit, einem einzigartigen Schwert und Streitkolben, animierten Texturen, Partikeleffekten und knackigem Kampf-Feedback.

---

## ⚔️ Die Sets

### 😇 Heaven-Set
- **Halo** — regeneriert dich und Verbündete in der Nähe
- **Ascension** — Doppelsprung durch die Luft
- **Shockwave** — nach tiefem Fall in den Boden schmettern und alles ringsum schädigen
- **Heaven's Step** — bei wenig Leben hinterlässt du eine leuchtende Lichtspur, die Gegner verlangsamt
- **Light Beam & Purify** — gezielter Strahl: Schaden für Feinde, Heilung + Debuff-Entfernung für Verbündete
- **Grace** — in der Luft schleichen und sanft herabgleiten
- **Segen** — Combo treffen, und *jeder* weitere Schlag ist ein Auto-Crit (mit eigenen Effekten und Sound), bis dich ein anderer Spieler trifft

### 😈 Hell-Set
- **Undying Rage** — bei wenig Leben: Extra-Tempo und ein Ausbruch aus Höllenfeuer
- **Warlord's Barrier** — Absorption und Resistenz, solange deine Brutes an deiner Seite kämpfen
- **Lava Walking & Hellforged** — über Lava spazieren; Stärke, solange du brennst
- **Magma Stomp & Volcanic** — harte Landungen reißen den Boden zu temporärer Lava auf
- **Fire Camp** — beschwöre drei treue Piglin-Brute-Vasallen in seelenblauem Gewand. Ein echtes **Soul Campfire** erscheint, und der Boden ringsum verwandelt sich in ein Soul-Soil-Lager mit Soul Torches und Laternen — alles verschwindet, sobald deine Brutes gefallen sind
- **Klingenrausch** — die Aktiv-Fähigkeit des Hell's Sword: 20 Sekunden lang 50 % schnellere Angriffe und garantierte Crits; Crits segnen dich mit Goldapfel-Absorption

### 🗡️ Waffen
- **Heaven's Sword** — Combo-basierte Auto-Crits mit heiligem Feedback
- **Hell's Sword** — Seelenfeuer-Klinge mit animiert glühender Schneide und wandernden Runen
- **Heaven's Mace** — wirf ihn: Er fliegt bis zu 50 Blöcke und ruft auf alles im Weg **Blitze** herab — auf dem Hinweg *und* auf dem Rückweg
- **Hell's Mace** — eine Kettenwaffe: **ziehe Gegner** aus bis zu 16 Blöcken zu dir (sie kommen *garantiert* an), **hake dich an Blöcke** oder reite deine eigenen Wind Charges und Enderperlen per Kette hinterher

### ✨ Gemeinsam
- Voll-Set-Bonus: **+10 Herzen**, Fallschaden-Immunität und mehr
- **Ultra-Modus** — temporärer Power-Up-Zustand mit eigenem HUD-Banner
- Animierte Item-Texturen, Seelen-/Lichtflammen an getragener Ausrüstung, eigene Partikel
- Cooldown-HUD über der Hotbar mit eigenen Fähigkeiten-Icons

---

## 🔨 Crafting

Stelle **Hell-/Heaven-Barren** aus Netherite, Seelen-Materialien, Amethyst und Glowstone her und rüste Diamant-Ausrüstung am **Schmiedetisch** mit **Jötunheims Upgrade-Vorlage** auf — genau wie Netherite-Upgrades. Bannermuster inklusive.

---

## ⚙️ Voll konfigurierbar

- Jede Fähigkeit lässt sich live per `/hha` schalten und tunen (OP): Schadenswerte, Cooldowns, Schwellen
- **Eigene Rezepte**: Der Ordner `config/hha/datapack/` wird als automatisch aktiviertes Datapack in deine Welt gespiegelt — bearbeite die mitgelieferten Vorlagen, um jedes Rezept zu ändern oder eigene hinzuzufügen, dann `/hha recipes reload`
- Rezept-Schalter: `hell_ingot_recipe`, `heaven_ingot_recipe`, `template_recipe`
- **Kit-Modus** für PvP-Server: `/hha kit hell|heaven` gibt ein volles, voll verzaubertes PvP-Kit (inkl. Stärke- und Weaving-Wurftränken)
- **Trust-System**: `/trust <spieler>` — getrustete Spieler werden von deinen Fähigkeiten, Spuren und Brutes niemals verletzt

---

## 📋 Voraussetzungen

- Fabric Loader + **Fabric API**
- **Fabric Language Kotlin**
- Minecraft 1.21.11

## ⚠️ Gut zu wissen

- Die Mod retexturiert das dunkle Gewand des **Piglin Brute** in Seelenblau (gilt für alle Brutes, auch in Bastionen — passt zum Theme)
- Temporäre Blöcke (Soul Camp, Magma-Stomp-Lava) bauen sich automatisch zurück; ein harter Server-Absturz währenddessen kann sie zurücklassen
- Funktioniert im Einzelspieler und auf dedizierten Servern
