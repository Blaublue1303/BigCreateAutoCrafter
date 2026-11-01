# Bedienung, Test im Spiel und Grenzen (Version 0.1.4)

| Anhänger am Stock Keeper | Auswahl „Crafter-Anlage“ | Fenster „Crafter-Profile“ |
|---|---|---|
| ![Anhänger mit Tooltip](img/stock-keeper-selector.png) | ![Auswahl am Stock Keeper](img/stock-keeper-dropdown.png) | ![Fenster „Crafter-Profile“](img/profile-manager.png) |

| 21er-Anlage mit Crushing-Wheel-Zutaten | Factory Gauge mit Crushing Wheel | Tooltip einer Zutat |
|---|---|---|
| ![21er-Anlage](img/crafter-setup.png) | ![Gauge mit 1 / 16 / 4 pro Craft](img/factory-gauge.png) | ![Tooltip mit Bereich](img/factory-gauge-tooltip.png) |

*Screenshots aus dem automatischen Client-Test (1600×900, englische Sprache). Die Seite für CurseForge liegt in `curseforge/`.*

Die Oberfläche nutzt Creates eigene Stock-Keeper-Texturen (Papierkarten und Holz aus dem Kategorie-Editor), seine Farben, Tooltips und Klänge. Die Texturen werden aus Creates JAR gelesen, nicht kopiert.

## Was die Mod kann

1. **Anlage registrieren:** Strg halten und einen Mechanical Crafter rechtsklicken. Die ganze verbundene Crafter-Gruppe wird als Bereich gespeichert.
   - Ohne aktives Profil entsteht ein neues Profil („Anlage 1“, bei englischer Spielsprache „Setup 1“), sonst kommt der Bereich zum zuletzt bearbeiteten Profil. So landen z. B. die 21er-Anlage und danach ein 3×3-Feld im selben Profil.
   - Strg + Shift + Rechtsklick auf einen registrierten Bereich entfernt ihn. Auf einer neuen Gruppe startet es ein neues Profil.
   - Die Taste ist unter *Steuerung → Big Create Auto Crafter* umbelegbar. Strg ist auch die Sprint-Taste.
2. **Auswahl im Stock Keeper:** Links am Fensterrahmen, auf Höhe der Suchleiste, hängt ein Papier-Anhänger mit der aktuellen Wahl (Werkbank = Create 3×3, Mechanical Crafter = Anlage).
   - **Maus darüber:** Tooltip im Create-Stil mit der vollen Wahl, den Bereichen und ihren Adressen.
   - **Scrollen:** wechselt zur nächsten oder vorigen Möglichkeit, wie Creates Auswahlfelder.
   - **Klick:** öffnet das Holz-Panel „Crafter-Anlage“ mit einer Karte pro Möglichkeit. Die gewählte Karte ist hellblau.
     - **Standard (Create 3×3):** Creates Verhalten, unverändert.
     - **Anlage · Auto:** Der kleinste Bereich, in den das Rezept passt.
     - **Bereichskarten** (eingerückt, mit Faden an der Anlage): Genau dieser Bereich.
     - **Profile verwalten…** (Schraubenschlüssel): Öffnet das Fenster „Crafter-Profile“.
   - Klick daneben oder Esc schließt das Panel.
3. **Fenster „Crafter-Profile“:** Aufgebaut wie Creates Kategorie-Editor des Stock Keepers. Es öffnet sich aus dem Panel oder über die Taste *Crafter-Profile öffnen*. Die ist standardmäßig nicht belegt und unter Steuerung einstellbar.
   - **Profilkarte** mit Crafter-Symbol, darunter die **Bereichskarten** am Faden.
   - **Umbenennen:** Auf den Namen klicken, tippen, Enter. Esc bricht ab, ein Klick daneben speichert.
   - **Paketadresse pro Bereich:** Auf das Feld rechts in der Bereichskarte klicken. Wird der Bereich benutzt, trägt die Mod die Adresse automatisch in den Stock Keeper ein.
   - **×:** Zweimal klicken (das × wird rot). Auf einer Profilkarte löscht es das Profil, auf einer Bereichskarte entfernt es den Bereich. Wird der letzte Bereich entfernt, verschwindet das Profil.
   - **✓** (oder Esc) führt zurück zum Stock Keeper.
   - Dieselben Aktionen gibt es auch als Befehl: `/bcac list | select | rename | address | delete`.
4. **JEI „+“:** Mit gewählter Anlage funktioniert das „+“ auch für Rezepte mit mehr als 9 Zutaten (z. B. Crushing Wheel, 21 Zutaten). Kleine Rezepte gehen in kleine Bereiche oder an die erste passende Stelle eines großen Bereichs.
5. **Factory Gauge (automatisches Crafting):** Für Rezepte, die größer als 3×3 sind (z. B. Crushing Wheel), bietet das Gauge-Fenster „Use Mechanical Crafting“ an, sobald das Rezept in einen deiner Bereiche passt.
   - **Welcher Bereich:** der Bereich, dessen Paketadresse in der Gauge steht. Sonst der kleinste deiner Bereiche, in den das Rezept passt. Die Adresse der Gauge bestimmt nur, wohin die Pakete gehen.
   - **Im Fenster:** Statt des 3×3-Rasters stehen die Zutaten mit ihrer Menge pro Craft (Crushing Wheel: 16 Andesitlegierung, 4 Bretter, 1 Stein). Der Tooltip einer Zutat und der Button nennen den Bereich.
   - **Adresse:** Ist das Adressfeld leer und hat der Bereich eine Paketadresse, trägt die Mod sie beim Einschalten ein.
   - **Beim Bestätigen** bekommt die Gauge das Muster und die Mengen für den Bereich. Über der Hotbar steht „Gauge craftet auf Bereich …“.
   - **Normale 3×3-Rezepte** bleiben bei Creates 3×3, außer die Gauge schickt an die Paketadresse eines Bereichs.
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

1. Lege `bigcreateautocrafter-0.1.4.jar` auf Client **und** Server in den `mods`-Ordner und entferne die alte JAR. Beide sollten dieselbe Version haben (Netzwerkprotokoll 4, unverändert seit 0.1.0).
2. Halte Strg und rechtsklicke die 21er-Anlage. Im Chat erscheint die Bestätigung.
3. **Stock Keeper:**
   1. Öffne den Stock Keeper, klicke den Anhänger links und wähle **Profile verwalten…**.
   2. Benenne das Profil um, trage die Paketadresse des Bereichs ein und klicke **✓**.
   3. Wähle im Panel die Anlage (oder scrolle über dem Anhänger).
   4. Bestelle das Crushing Wheel per JEI „+“ und sende den Auftrag.
4. **Factory Gauge:**
   1. Baue die Gauge wie bei Create: eine Gauge mit Crushing Wheel als Ziel, verbunden mit Eingangs-Gauges für Andesitlegierung, Bretter und Stein.
   2. Klicke „Use Mechanical Crafting“ (der Button wird grün). Im Raster stehen jetzt 16 / 4 / 1.
   3. Trage im Adressfeld die Adresse des Packagers an der 21er-Anlage ein, falls sie nicht schon drinsteht, und bestätige.
   4. **Erwartet:** Über der Hotbar steht „Gauge craftet auf Bereich 5×5 (21) …“. Die Gauge fordert 16 / 4 / 1 an, und die Anlage craftet das Crushing Wheel.
   5. Hattest du die Gauge schon mit 0.1.1 oder älter eingestellt: Fenster öffnen und einmal bestätigen, damit sie das neue Muster speichert.
5. **Verwalten:** Lösche ein Test-Profil im Fenster mit zwei Klicks auf das ×.

## Änderungen

- **0.1.4:** Lizenz MIT (`LICENSE` im Repository und in der JAR). Keine Änderung am Spiel.
- **0.1.3:** Für CurseForge vorbereitet.
  - Neue Profile heißen bei englischer Spielsprache „Setup 1“, bei deutscher weiter „Anlage 1“.
  - Logo, englische Beschreibung und Screenshots liegen in `curseforge/`.
  - Der Client-Test nimmt auch die Anlage in der Welt auf (mit Crushing-Wheel-Zutaten über Creates Entpacker) und rendert die Items fürs Logo.
- **0.1.2:** Factory Gauge mit großen Rezepten repariert (Spieltest: „Use Mechanical Crafting“ für das Crushing Wheel forderte nur 1 / 1 / 1 an).
  - Ursache: Die Gauge-Adresse musste genau die Paketadresse eines Bereichs sein. Sonst schaltete die Mod das Crafting beim Speichern ab, und Create bestellte je 1 Stück.
  - Jetzt nimmt die Gauge bei großen Rezepten den passenden Bereich selbst, die Adresse bestimmt nur das Ziel der Pakete.
  - Das Gauge-Fenster zeigt die Mengen pro Craft und den Bereich, beim Speichern kommt eine Bestätigung.
  - Neuer Client-Test: echte Gauge, Klick auf „Use Mechanical Crafting“, fremde Adresse, Prüfung auf dem Server (21 Crafter-Slots, 16 / 4 / 1).
- **0.1.1:** Oberfläche im Stil des Stock Keepers, keine neuen Funktionen.
  - Papier-Anhänger am Fensterrahmen statt des grauen Buttons, Auswahl als Holz-Panel mit Karten, Scrollen zum Wechseln.
  - Fenster „Crafter-Profile“ im Aufbau von Creates Kategorie-Editor: Karten, Faden zu den Bereichen, Bearbeiten per Klick, × zum Löschen.
  - Tooltips, Farben und Klänge wie bei Create.
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

- **Echtes Craften mit Factory Gauges ist im Spiel noch nicht bestätigt.**
  - Geprüft sind die Server-Logik (21 GameTests, darunter Bereichswahl, Rezeptsuche und Muster mit dem echten Crushing-Wheel-Rezept) und der Client (Smoke-Test: beide Client-Mixins, Anhänger, Panel, Profilfenster, echte Factory Gauge bis zum gespeicherten Muster auf dem Server).
  - Nicht getestet ist das Bestellen und Craften mit echten Paketwegen. Den Stock-Keeper-Weg hast du mit 0.0.2 im Spiel bestätigt, er nutzt dasselbe Muster-Format.
- **Gauge und Bereich müssen demselben Spieler gehören.** Der Client kennt nur die eigenen Profile.
- **Höchstens 9 verschiedene Itemtypen pro Auftrag** (Create-Limit, unverändert).
- **Unstapelbare Zutaten** (z. B. Eimer) können einen Craft auf mehrere Pakete verteilen. Kommen die Pakete über mehrere Crafts hinweg durcheinander an, kann der Packager blockieren (R14).
- **JEI-Version des Packs** ist weiter unbestätigt. Die Mod nutzt die Transfer-API, die in allen JEI-19.x-Versionen existiert.
