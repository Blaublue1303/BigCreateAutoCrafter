# CurseForge-Seite

Alles für die Projektseite auf CurseForge. Beschreibung und Bildtexte sind auf Englisch, weil CurseForge englische Titel und Beschreibungen verlangt.

| Datei | Inhalt |
|---|---|
| `logo.png` | **Empfohlenes Logo** (400 × 400). Komplett selbst gezeichnet, ohne Create- oder Minecraft-Texturen. |
| `logo-create-renders.png` | Alternative im Stil deiner Beispiel-Icons, mit echten Renders von Mechanical Crafter und Crushing Wheel. Siehe „Logo“ unten. |
| `description.md` | Projektbeschreibung (Markdown). |
| `screenshots/` | Echte In-Game-Screenshots (1600 × 900, unbearbeitet) aus dem automatischen Client-Test. |
| `make_logo.py`, `make_logo_original.py` | Skripte, die die Logos erzeugen. |

## Hochladen

1. **Projekt anlegen** (Minecraft → Mods):
   - **Name:** Big Create Auto Crafter
   - **Summary:** `Order and auto-craft recipes bigger than 3×3 with Create's Stock Keeper and Factory Gauges`
   - **Kategorie:** z. B. Technology / Automation
   - **Logo:** `logo.png`
   - **Lizenz:** im Mod steht bisher „All Rights Reserved“. Das musst du entscheiden.
2. **Bilder:** Lade die Dateien aus `screenshots/` im Tab *Images* hoch.
3. **Beschreibung:**
   1. Im Editor das Format *Markdown* wählen.
   2. `description.md` einfügen.
   3. Jede `screenshots/…png` durch die Adresse des hochgeladenen Bildes ersetzen. Die Adresse bekommst du per Rechtsklick auf das Bild im Tab *Images* → Bildadresse kopieren.
4. **Datei:** die JAR aus `build/libs/` hochladen.
   - Spielversion 1.21.1, Loader NeoForge.
   - Abhängigkeiten: *Create* „Required Dependency“, *JEI* „Optional Dependency“.

## KI-Regeln von CurseForge (Stand: Moderationsrichtlinien vom 22.10.2026)

- **Erlaubt:** Komplett von KI erstellte Mods sind nicht verboten.
- **Einzige KI-Regel:** Showcase-Bilder, die mit KI erzeugt, bearbeitet oder verbessert wurden und den Inhalt falsch darstellen könnten, brauchen einen sichtbaren Hinweis.
  - Unsere Screenshots sind echte, unbearbeitete Aufnahmen, das betrifft sie also nicht.
- **Hinweis in der Beschreibung:** `description.md` endet trotzdem mit einem kurzen Transparenz-Absatz. Manche Modder sehen KI-Inhalte kritisch, Offenheit vermeidet Ärger.
- **Weitere Regeln, die hier zählen:**
  - Das Avatar muss 400 × 400 Pixel groß sein, darf keine einfarbige Fläche sein, nichts NSFW und keine urheberrechtlich geschützten Bilder enthalten. Kein WebP verwenden.
  - Die Beschreibung muss erklären, was die Mod hinzufügt.
  - Inhalte anderer Autoren nur mit Erlaubnis oder passender Lizenz, mit Credits.

## Logo: warum zwei Varianten

Creates Lizenz trennt Code und Grafik (Datei `LICENSE.md` in Creates JAR):

- **Code:** MIT.
- **Grafiken** (`assets/`): „All Rights Reserved“.

`logo-create-renders.png` zeigt Creates Block-Texturen. Viele Create-Addons machen das so, auch deine Beispiel-Icons. Nach der Avatar-Regel („keine urheberrechtlich geschützten Bilder“) ist es aber ein Risiko bei der Moderation. `logo.png` hat dieses Risiko nicht.

Die Mod selbst liefert keine Create-Grafiken mit. Die Menüs lesen die Texturen zur Laufzeit aus dem installierten Create.

## Neu erzeugen

```
xvfb-run -a -s "-screen 0 1920x1080x24" ./gradlew runClientSmokeTest
python3 -I curseforge/make_logo.py run-smoketest/screenshots/bcac-item-renders.png curseforge/logo-create-renders.png
python3 -I curseforge/make_logo_original.py curseforge/logo.png
```

Die Screenshots liegen danach in `run-smoketest/screenshots/` (`bcac-world.png`, `bcac-world-chat.png`, `bcac-smoke-*.png`).
