# Bedienung, Test im Spiel und Grenzen (Version 0.1.0)

| Stock Keeper mit Auswahl | Profilverwaltung |
|---|---|
| ![Auswahlliste am Stock Keeper](img/stock-keeper-dropdown.png) | ![Fenster „Crafter-Profile“](img/profile-manager.png) |

*Screenshots aus dem automatischen Client-Test (kleine Auflösung 854×480).*

## Was die Mod kann

1. **Anlage registrieren:** Strg halten und einen Mechanical Crafter rechtsklicken. Die ganze verbundene Crafter-Gruppe wird als Bereich gespeichert.
   - Ohne aktives Profil entsteht ein neues Profil („Anlage 1“), sonst kommt der Bereich zum zuletzt bearbeiteten Profil. So landen z. B. die 21er-Anlage und danach ein 3×3-Feld im selben Profil.
   - Strg + Shift + Rechtsklick auf einen registrierten Bereich entfernt ihn. Auf einer neuen Gruppe startet es ein neues Profil.
   - Die Taste ist unter *Steuerung → Big Create Auto Crafter* umbelegbar. Strg ist auch die Sprint-Taste.
2. **Auswahl im Stock Keeper:** Links neben dem Fenster sitzt ein Button. Ein Klick öffnet eine Liste mit allen Möglichkeiten in voller Länge:
   - **Standard (Create 3×3):** Creates Verhalten, unverändert.
   - **Anlage · Auto:** Der kleinste Bereich, in den das Rezept passt.
   - **↳ Bereich:** Genau dieser Bereich.
   - **Profile verwalten…:** Öffnet das Fenster „Crafter-Profile“.
3. **Fenster „Crafter-Profile“:** Es öffnet sich aus der Liste oder über die Taste *Crafter-Profile öffnen*. Die ist standardmäßig nicht belegt und unter Steuerung einstellbar.
   - **Umbenennen:** Namen ins Feld schreiben, dann Enter drücken oder ins nächste Feld klicken.
   - **Paketadresse pro Bereich:** Das Feld neben dem Bereich. Wird der Bereich benutzt, trägt die Mod die Adresse automatisch in den Stock Keeper ein.
   - **Löschen / Entfernen:** Zweimal klicken („Sicher?“). Wird der letzte Bereich eines Profils entfernt, verschwindet das Profil.
   - **Fertig** führt zurück zum Stock Keeper.
   - Dieselben Aktionen gibt es auch als Befehl: `/bcac list | select | rename | address | delete`.
4. **JEI „+“:** Mit gewählter Anlage funktioniert das „+“ auch für Rezepte mit mehr als 9 Zutaten (z. B. Crushing Wheel, 21 Zutaten). Kleine Rezepte gehen in kleine Bereiche oder an die erste passende Stelle eines großen Bereichs.
5. **Factory Gauge (automatisches Crafting):** Eine Gauge, deren Adresse die Paketadresse eines deiner Bereiche ist, craftet für diesen Bereich.
   - Im Gauge-Fenster wird dann auch ein Mechanical-Crafting-Rezept wie das Crushing Wheel angeboten, wenn es in einen deiner Bereiche passt.
   - Beim Bestätigen bekommt die Gauge das Muster und die Mengen pro Eingang für genau diesen Bereich, beim Crushing Wheel also 16 Andesit, 4 Bretter und 1 Stein.
   - Gauges mit anderen Adressen bleiben bei Creates 3×3.
6. **Pakete:** Für große Muster packt der Repackager nach Itemtyp, bis zu 64 gleiche Items pro Paket-Slot, damit ein Craft in **ein** Paket passt. Creates Crafter-Entpacker verteilt die Items dann auf die richtigen Crafter.

## Wann ein Repackager nötig ist (wie bei Create selbst)

Create legt die Crafting-Informationen nur an die Pakete **eines** Packagers. Sein Crafter-Entpacker nimmt nur Pakete an, deren Items genau in die freien Felder passen. Das gilt für 3×3 genauso wie für große Bereiche:

| Situation | ohne Repackager | mit Repackager |
|---|---|---|
| 1 Craft, alle Zutaten aus **einem** Packager | ✅ | ✅ |
| 1 Craft, Zutaten aus **mehreren** Packagern (z. B. ein Vault pro Item) | ❌ Pakete ohne Info werden der Reihe nach eingefüllt | ✅ |
| **mehrere** Crafts in einem Auftrag | ❌ das Paket enthält Items für mehrere Crafts und wird abgelehnt | ✅ ein Paket pro Craft |

Eine Factory Gauge fordert immer **einen** Craft pro Auftrag an. Ohne Repackager genügt es dort also, wenn alle Zutaten aus einem Packager kommen.

## So testest du im Spiel

1. Lege `bigcreateautocrafter-0.1.0.jar` auf Client **und** Server in den `mods`-Ordner und entferne die alte JAR. Beide brauchen dieselbe Version (Netzwerkprotokoll 4).
2. Halte Strg und rechtsklicke die 21er-Anlage. Im Chat erscheint die Bestätigung.
3. **Stock Keeper:**
   1. Öffne den Stock Keeper, klicke den Button links und wähle **Profile verwalten…**.
   2. Benenne das Profil um, trage die Paketadresse des Bereichs ein und klicke **Fertig**.
   3. Wähle in der Liste die Anlage.
   4. Bestelle das Crushing Wheel per JEI „+“ und sende den Auftrag.
4. **Factory Gauge:**
   1. Baue die Gauge wie bei Create: eine Gauge mit Crushing Wheel als Ziel, verbunden mit Eingangs-Gauges für Andesit, Bretter und Stein.
   2. Trage im Gauge-Fenster die Paketadresse des 21er-Bereichs ein.
   3. Schalte das Rezept ein und bestätige.
   4. **Erwartet:** Die Gauge fordert 16 / 4 / 1 an, und die Anlage craftet das Crushing Wheel.
5. **Verwalten:** Lösche ein Test-Profil im Fenster mit zwei Klicks auf „Löschen“.

## Änderungen

- **0.1.0:** Neue Bedienung und Factory Gauge.
  - Aufklappliste statt des abgeschnittenen Wechsel-Buttons.
  - Fenster „Crafter-Profile“ zum Umbenennen und Löschen, für Adressen und zum Entfernen von Bereichen. Dazu eine Taste zum Öffnen.
  - Factory Gauges auf Crafter-Bereichen inklusive Mechanical-Crafting-Rezepten.
- **0.0.3:** Code-Qualität, keine neuen Funktionen.
  - Die Platzierung hängt am Bestelleintrag.
  - Der Repackager-Mixin greift nur beim `packages.add` ein.
  - Ein Netzwerkpaket weniger bei Strg + Rechtsklick.
  - Die tote Einstellung `autoSelectSmallestRegion` ist entfernt.
- **0.0.2:** Fehler aus dem ersten Spieltest behoben. Der Muster-Bau zählte die Bestellliste herunter, und Create schickte den Auftrag deshalb ohne Crafting-Informationen. Die Crafter wurden dann der Reihe nach gefüllt.
- **0.0.1:** erster Prototyp.

## Grenzen

- **Echtes Craften mit Factory Gauges ist ungetestet.**
  - Geprüft sind die Server-Logik (20 GameTests, darunter Rezeptsuche und Muster mit dem echten Crushing-Wheel-Rezept) und der Client (Smoke-Test: beide Client-Mixins, Liste, Profilfenster, Umbenennen über den Server).
  - Nicht getestet ist der volle Ablauf einer Gauge mit Paketwegen. Den Stock-Keeper-Weg hast du mit 0.0.2 im Spiel bestätigt.
- **Gauge und Bereich müssen demselben Spieler gehören.** Der Client kennt nur die eigenen Profile.
- **Höchstens 9 verschiedene Itemtypen pro Auftrag** (Create-Limit, unverändert).
- **Unstapelbare Zutaten** (z. B. Eimer) können einen Craft auf mehrere Pakete verteilen. Kommen die Pakete über mehrere Crafts hinweg durcheinander an, kann der Packager blockieren (R14).
- **JEI-Version des Packs** ist weiter unbestätigt. Die Mod nutzt die Transfer-API, die in allen JEI-19.x-Versionen existiert.
