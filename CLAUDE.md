# Kontext für neue Sessions – Big Create Auto Crafter

Stand: 2026-11-04, Version **0.1.4** (`gradle.properties` → `mod_version`). Diese Datei fasst alles zusammen, was eine neue Session wissen muss. Ausführliche Doku in `docs/01…07`, CurseForge-Material in `curseforge/`.

## 1. Worum es geht

- NeoForge-Mod für **Minecraft 1.21.1** und **Create 6.0.10**. Gebaut für das Modpack **Create Exta+ 2.0** des Nutzers.
- **Ziel:** Creates Crafting-Ablauf (Stock Keeper mit JEI-„+“, Factory Gauges) funktioniert auch mit großen Mechanical-Crafter-Anlagen. Beispiel: Crushing Wheel mit 21 Zutaten auf einer 5×5-Anlage ohne Ecken.
- **Profile:** Strg + Rechtsklick auf einen Mechanical Crafter registriert die ganze verbundene Crafter-Gruppe als **Bereich** (Region) eines **Profils**. Ein Profil kann mehrere Bereiche haben, z. B. 21er-Wand plus 3×3-Feld.
- **Repository:** https://github.com/Blaublue1303/BigCreateAutoCrafter (öffentlich). `main` enthält alles; PR #1 und #2 sind gemerged.
- **Lizenz:** MIT (`LICENSE`, auch in der JAR als `LICENSE_bigcreateautocrafter`).

## 2. Der Nutzer und wie wir zusammenarbeiten

- GitHub `Blaublue1303`. Schreibt **Deutsch**, oft per Spracheingabe, deshalb Transkriptionsfehler. Bisher vorgekommen:
  - „Siedlegierungen“ = Andesite Alloy
  - „Caution Wheels“ = Crushing Wheels
  - „Stockkeeper“ = Stock Keeper
  - „Mod-Rint“ = Modrinth
  - „1.3.0 Jar“ = 0.1.3
- Antworten auf Deutsch, einfach und direkt. Erklärungen mit Spielbegriffen.
- Testet selbst im Spiel und schickt Screenshots. Bestätigt:
  - **0.0.2:** Der Stock-Keeper-Ablauf funktioniert im Spiel.
  - **0.1.0/0.1.1:** Mit der Gauge kam der Fehler „1/1/1 statt 16/4/1“, behoben in 0.1.2.
  - **Noch nicht bestätigt:** Gauge-Crafting ab 0.1.2 mit echten Paketen.
- **Wünscht Create-Optik:** Die Oberfläche soll wie Creates Stock Keeper aussehen, nicht „KI-gecodet“. Keine Vanilla-Buttons, keine lila Tooltip-Kästen als Menü.
- **Arbeitsregeln:**
  - **Versionen nie raten:** belegen (Maven, JAR-Vergleich, Manifest).
  - **Blockierte Aktionen nie umgehen.** Früher wurde ein CurseForge-Abruf mit falschem User-Agent als „Bot Check Circumvention“ abgelehnt: so etwas nie wiederholen.
  - **Keine Modellnamen** in Commits, PRs oder Code. Commit- und PR-Trailer nach den Vorgaben der jeweiligen Session.
  - Nach Änderungen bauen und testen (GameTests, bei UI-Änderungen auch den Client-Smoke-Test). Danach die Version erhöhen, die Doku (`docs/07`, `docs/06`, README) anpassen und dem Nutzer die JAR schicken (`out/` ist gitignored).
- **Dem Nutzer gegebener Lern-Prompt:** Er bekam einen Prompt, mit dem eine andere KI ihm den Code Lektion für Lektion erklärt. Der Prompt steht nur im Chat, nicht im Repo.

## 3. Veröffentlichung

- **CurseForge:**
  - Projekt-ID **1778075** (Autoren-URL `authors.curseforge.com/#/projects/1778075`).
  - Der Nutzer hat **0.1.3** hochgeladen (in der JAR steht noch „All Rights Reserved“) und auf CurseForge MIT gewählt. **0.1.4** sollte als nächste Datei hochgeladen werden: gleicher Code, nur Lizenz-Metadaten.
  - Abhängigkeiten: Create (CurseForge-ID **328085**) als Required, JEI (**238222**) als Optional. Die IDs stehen in `manifest.json` des Modpacks.
  - Source-Link: das GitHub-Repository.
  - Material in `curseforge/`: `logo-round.png` (rundes Logo, gewählt), Alternativen `logo.png` (selbst gezeichnet, ohne Create-Texturen) und `logo-create-renders.png`, `description.md` (englisch), `screenshots/` (1600×900), `description-images/` (unter 850 px, die Grenze des Beschreibungs-Editors), Skripte. Die Anleitung steht in `curseforge/README.md`.
  - **Regeln (Stand 22.10.2026):** KI-Mods erlaubt. Nur KI-veränderte Showcase-Bilder, die die Mod falsch darstellen könnten, brauchen einen Hinweis. Avatar 400×400, keine urheberrechtlich geschützten Bilder, kein WebP. Beschreibung auf Englisch.
  - **Risiko:** `logo-round.png` zeigt Creates Textur. Creates Grafiken sind „All Rights Reserved“ (Datei `LICENSE.md` in Creates JAR, der Code ist MIT). Lehnt die Moderation das Logo ab, `logo.png` nehmen.
- **Modrinth:** **nicht** veröffentlichen. Content Rules Abschnitt 6 (13.08.2026): Projekte, die „überwiegend oder ganz“ aus KI-Ausgabe bestehen, dürfen nicht öffentlich sein, und KI-Bilder auf der Projektseite sind verboten. Ob unlisted/privat erlaubt ist, ist offen (Modrinth-Support fragen). KI-Nutzung nie verschweigen.

## 4. Offene Aufgaben

1. **Spieltest durch den Nutzer** (wichtigster offener Punkt):
   - Factory Gauge mit Crushing Wheel ab 0.1.2.
   - Erwartet: Gauge-Fenster zeigt 16/4/1, Meldung über der Hotbar „Gauge craftet auf Bereich 5×5 (21) …“, die Anlage craftet.
   - Alte Gauges einmal öffnen und bestätigen.
2. **CurseForge:** 0.1.4 hochladen. Bei Ablehnung des Logos `logo.png` nehmen.
3. **Aus `docs/06-todo.md`:**
   - R6: eigene Servervalidierung der Muster
   - L3: mehr als 9 Itemtypen pro Auftrag
   - R14: robuste Reihenfolge bei geteilten Paketen, z. B. Eimer
   - Paketadresse automatisch vom Packager lesen
   - Datapack-Profile
   - Client-Config für die Anhänger-Position
   - Test im vollständigen Modpack (Mixin-Konflikte R7)
4. **Unbestätigt:** JEI-Version des Packs (CurseForge-fileID 8857687). Wir bauen gegen JEI 19.70.0.468 und nutzen nur Transfer-API, die es in allen 19.x gibt.
5. **Optional:** englische README für Besucher von CurseForge (README und Doku sind deutsch).

## 5. Technik und Versionen

| Was | Version / Quelle |
|---|---|
| Java / Gradle | JDK 21, Gradle Wrapper 9.2.1 |
| Build-Plugin | NeoForge ModDevGradle 2.0.148, Parchment 2024.11.17 |
| NeoForge | 21.1.250 |
| Create | `com.simibubi.create:create-1.21.1:6.0.10-280:slim` (maven.createmod.net). Byte-identisch mit der Pack-JAR (CurseForge-File 7963363). Erlaubter Bereich in der Mod `[6.0.10,6.1)` |
| Ponder (enthält Catnip) | `net.createmod.ponder:ponder-neoforge:1.0.82+mc1.21.1`, `transitive = false` (sonst falsches Flywheel) |
| Flywheel | 1.0.6 (API compileOnly, Rest runtimeOnly) |
| Registrate | `MC1.21-1.3.0+67` (mvn.devos.one/snapshots) |
| JEI | 19.70.0.468 (maven.blamejared.com, braucht Repo-Filter `includeGroupByRegex 'net\\.mezzdev(\\..*)?'`) |
| MixinExtras | 0.5.3 (`@WrapOperation`, `@Local`) |

- Mod-ID `bigcreateautocrafter`, Paket `com.blaublue1303.bigcreateautocrafter`.
- Netzwerkprotokoll `"4"` (`net/BcacNetwork.java`). Bei Änderungen an Payloads erhöhen.
- Server-Config (`Config.java`): maxRegionWidth/Height 9, maxRegionsPerProfile 8, maxProfilesPerPlayer 16.
- `scripts/fetch-dev-jars.sh` lädt Create- und JEI-JARs nach `libs/` (gitignored) zum Dekompilieren (z. B. mit Vineflower). Für den Build wird das nicht gebraucht.

## 6. Befehle und Tests

- `./gradlew build`: JAR in `build/libs/bigcreateautocrafter-<version>.jar`.
- `./gradlew runGameTestServer`: **21 GameTests**, headless. Laufen auch in der CI (`.github/workflows/build.yml`: build + GameTests bei jedem Push/PR).
- Client-Smoke-Test (lokal, nicht in CI, ca. 4 Minuten):
  `LIBGL_ALWAYS_SOFTWARE=1 xvfb-run -a -s "-screen 0 1920x1080x24" ./gradlew runClientSmokeTest`
  - Fenster 1600×900, GUI-Skalierung 3. Ergebnis in `run-smoketest/bcac-smoketest.txt` (`RESULT PASS`), Screenshots in `run-smoketest/screenshots/`.
  - **Ablauf:**
    1. Flachwelt anlegen.
    2. 21er-Gruppe und 3×3-Feld bauen.
    3. Profil über `ProfileService.handleCrafterClick` anlegen.
    4. Welt-Aufnahme: die 21er-Gruppe wird über Creates Entpacker mit den Crushing-Wheel-Zutaten befüllt.
    5. Echten Stock Keeper öffnen (Stock Ticker plus Blaze Burner, sonst schließt Create das Fenster).
    6. JEI-Handler prüfen.
    7. Anhänger mit Tooltip, Panel, Profilfenster (Umbenennen und Adresse über den Server).
    8. Echte Factory Gauge: „Use Mechanical Crafting“ klicken, fremde Adresse eintragen, auf dem Server 21 Slots und 16/4/1 prüfen.
    9. Item-Renders fürs Logo.
  - Die Maus wird über Minecrafts Cursor-Callback bewegt, nicht per GLFW-Warp (GLFW meldet ältere Warps nach). Das Tutorial-Toast ist abgeschaltet.
- `./gradlew runClient` zum manuellen Testen.

## 7. Code-Landkarte (`src/main/java/com/blaublue1303/bigcreateautocrafter/`)

| Bereich | Dateien | Aufgabe |
|---|---|---|
| Einstieg | `BigCreateAutoCrafter.java`, `client/BigCreateAutoCrafterClient.java`, `Config.java` | Events registrieren (Mod-Bus / NeoForge-Bus), Server-Config, Client-Listener |
| Profile | `profile/CrafterRegion.java`, `CrafterProfile.java` (Records mit Codec + StreamCodec), `ProfileManager.java` (SavedData), `ProfileService.java` (Strg-Klick, Umbenennen, Adresse, Löschen, aktives Profil), `Feedback.java`, `BcacCommands.java` (`/bcac list/select/rename/address/delete`), `RegionScanner.java` (Gruppe → Layout wie `".###."`), `ProfileSync.java` |
| Netzwerk | `net/BcacNetwork.java`, `SyncProfilesPayload` (S2C), `CrafterClickPayload` (C2S), `EditProfilePayload` (C2S: RENAME, DELETE, SET_ADDRESS, REMOVE_REGION) |
| Create-Anbindung | `compat/create/CreateCompat.java`, `ConnectedCrafterGroup.java`, `mixin/ConnectedInputAccessor.java` | verbundene Crafter-Gruppe in Creates Inventar-Reihenfolge lesen |
| Rezepte | `recipe/GridRecipe.java`, `RegionPlacer.java` (Platzierung, Auto = kleinster passender Bereich), `Placement.java`, `PatternBuilder.java`, `RegionCraftable.java`, `FactoryArrangements.java` (Gauge: Zielbereich, Muster, Rezeptsuche) |
| Pakete | `order/LargePatternPackaging.java` | große Muster nach Itemtyp verpacken, Start erst bei vollem Raster |
| Mixins | `mixin/PackageRepackageHelperMixin.java`, `CrafterUnpackingHandlerMixin.java`, `client/StockKeeperRequestScreenMixin.java`, `client/FactoryPanelScreenMixin.java`; Config `bigcreateautocrafter.mixins.json` (`defaultRequire: 1`) |
| JEI | `compat/jei/BcacJeiPlugin.java`, `RegionTransferHandler.java`, `JeiSmokeCheck.java` |
| Client-UI | `client/ClientSelection.java`, `ProfileSelector.java` (Anhänger + Panel am Stock Keeper über ScreenEvents), `ProfileManagerScreen.java`, `CreateUi.java`, `ClientProfileCache.java`, `CrafterClickHandler.java`, `BcacKeyMappings.java` (Strg-Modifier, Taste fürs Profilfenster, standardmäßig nicht belegt) |
| Tests | `gametest/*` (6 Klassen + `TestCrafters`, Arena `data/bigcreateautocrafter/structure/empty7.nbt`), `client/smoketest/ClientSmokeTest.java`, `Showcase.java` |
| Sprache | `assets/bigcreateautocrafter/lang/de_de.json`, `en_us.json` (Schlüssel `message.*`, `bigcreateautocrafter.selector.*`, `.manager.*`, `.factory.*`, `.transfer.*`, `key.*`) |

## 8. Wie es funktioniert (mit Create-Interna)

### Stock Keeper (JEI „+“)
1. Der Spieler wählt am Anhänger eine Option (`ClientSelection`): Standard, Profil · Auto oder ein Bereich.
2. `RegionTransferHandler` ist für `minecraft:crafting`, `create:automatic_shaped` und `create:mechanical_crafting` registriert. Handler für einen konkreten Rezepttyp schlagen Creates universellen Handler. Bei „Standard“ gibt er an Creates Handler weiter, sonst legt er ein `RegionCraftable` mit `Placement` an und setzt die Bereichsadresse ins Adressfeld.
3. `StockKeeperRequestScreenMixin` wickelt `convertRecipeToPackageOrderContext` in `sendIt`. Für ein `RegionCraftable` baut `PatternBuilder` das Muster für den Bereich, eine Liste in Creates Inventar-Reihenfolge.
4. **Server:**
   - Creates `LogisticsManager` hängt den Crafting-Kontext nur an die Pakete des **ersten** Packagers.
   - `PackageRepackageHelperMixin` (WrapOperation auf `List.add` in `repackBasedOnRecipes`) packt große Muster nach Itemtyp über `LargePatternPackaging`.
   - `CrafterUnpackingHandlerMixin` wickelt `checkCompletedRecipe` beim Entpacken ein. Gestartet wird nur, wenn alle Musterfelder gefüllt sind. Create startet mit `poweredStart`, leere Crafter sind dabei erlaubt.

### Factory Gauge (`FactoryPanelScreenMixin`)
- **TAIL von `searchForCraftingRecipe`:** bietet auch Mechanical-Crafting-Rezepte an, die in einen eigenen Bereich passen.
- **WrapOperation in `updateConfigs`:** Bei Rezepten größer als 3×3 zeigt die Vorschau jede Zutat einmal mit der Menge pro Craft.
- **Injektion nach `renderItem` in `renderInputItem`:** zeichnet die Menge. **Nicht TAIL**, die Methode hat mehrere `return`.
- **WrapOperation auf den ersten `renderComponentTooltip` in `renderInputItem`:** Tooltip mit Bereich.
- **TAIL von `init`:** Bereich im Button-Tooltip. Ein leeres Adressfeld wird mit der Bereichsadresse gefüllt.
- **ModifyArg am Konstruktor von `FactoryPanelConfigurationPacket` in `sendIt`:** Index 2 = Mengen pro Eingang, Index 3 = Muster. Beim Speichern erscheint eine Meldung über der Hotbar.
- **Bereichswahl (`FactoryArrangements.target`):**
  1. Der Bereich, dessen Paketadresse in der Gauge steht.
  2. Sonst, nur bei Rezepten größer als 3×3: der kleinste passende Bereich des Spielers. Die Gauge-Adresse bestimmt dann nur das Ziel der Pakete.
  3. 3×3-Rezepte bleiben bei Create, außer die Adresse passt zu einem Bereich.
- **Server:** `FactoryPanelBehaviour.tickRequests` fordert `connection.amount` pro Eingang an und schickt `PackageOrderWithCrafts.singleRecipe(activeCraftingArrangement)` mit. Gespeichert wird das Muster in NBT als `"Craft"`.

### Wann ein Repackager nötig ist
Wie bei Create selbst: Kommen die Zutaten von mehreren Packagern oder enthält ein Auftrag mehrere Crafts, braucht man einen Repackager. Eine Gauge fordert immer einen Craft pro Auftrag an.

## 9. Oberfläche im Create-Stil (wichtig für jede UI-Änderung)

- **Texturen:** werden zur Laufzeit aus Creates JAR gelesen, **nie kopieren**.
  - `create:textures/gui/stock_keeper_categories.png`, UVs in `CreateUi`:
    - Karte `ENTRY` (38,159) 171×18: links 12 px, Mitte Spalte u=100 gestreckt, rechts 3 px (u 206) oder mit „×“ 17 px (u 192)
    - Panel-Kopf v=0 (18 hoch), Körper v=32 (20 hoch), Unterkante v=79 (2 hoch), jeweils 4 px Ränder (u 32 / u 220)
  - `AllGuiTextures.STOCK_KEEPER_CATEGORY_*`
- **Farben:**
  - Kartentext `0x656565`, dunkles Braun `0x714A40`, Platzhalter `0xB59370`
  - Kopftitel `0x3D3C48`, Markierung `0x8C5D4B`
  - Tooltip-Kopf `AbstractSimiWidget.HEADER_RGB` (`0x5391E1`), Hinweis `HINT_RGB`, Steuerung dunkelgrau kursiv
- **Klänge:** UI-Klick, Scrollen mit `AllSoundEvents.SCROLL_VALUE`.
- **Stock-Keeper-Geometrie (Create 6.0.10):**
  - Fenster wird ab `guiLeft - 15` gezeichnet, der sichtbare Rahmen beginnt bei `guiLeft + 9`.
  - Suchzeile bei `guiTop + 17`, 18 hoch.
  - Unser Anhänger hängt links am Rahmen. Seine Fläche (und die des offenen Panels) steht in `screen.getExtraAreas()`, damit JEI sie freihält.
  - Unter dem offenen Panel werden Creates Item-Tooltips unterdrückt (`RenderTooltipEvent.Pre`).
- **Profilfenster:**
  - Aufgebaut wie Creates Kategorie-Editor (`AbstractSimiScreen`): blaue Kopfleiste, Holzzeilen, graue Fußleiste mit ✓ (`IconButton` mit `AllIcons.I_CONFIRM`), rechts ein „sprechender“ 3D-Crafter.
  - Karten, Bereiche am Faden.
  - Name/Adresse anklicken = Inline-`EditBox` (Enter speichert, Esc bricht ab, Klick daneben speichert).
  - „×“ zweimal klicken (3 s).
  - Die Höhe richtet sich nach dem Inhalt.
- **Standardname neuer Profile:** „Anlage n“ bei deutscher Client-Sprache, sonst „Setup n“ (`clientInformation().language()`). GameTests nutzen FakePlayer, also en_us und damit „Setup 1“.

## 10. Fehlerhistorie (Lehren)

- **0.0.2:** `PatternBuilder` hat die Bestellliste des Aufrufers heruntergezählt. Create schickte den Auftrag deshalb ohne Crafting-Info, und die Crafter wurden der Reihe nach gefüllt. **Lehre:** Eingaben nie verändern, sondern mit `BigItemStack.duplicateWrappers` kopieren. Regressionstest in `LogisticsGameTests`.
- **0.1.2:** Die Gauge bestellte 1/1/1. Die Gauge-Adresse musste exakt die Bereichsadresse sein, sonst wurde das Crafting still abgeschaltet. **Lehre:** keine stillen Abschaltungen, Mengen sichtbar machen, Bereich automatisch wählen. Zusätzlich: `@At("TAIL")` erreicht nur den letzten `return` (vorher im Bytecode mit `javap -c` prüfen).
- **Smoke-Test-Erkenntnisse:**
  - Der Stock-Keeper-Screen braucht einen Blaze Burner.
  - Der Client kennt ein frisch gesetztes BlockEntity erst nach dem Sync, also ein paar Ticks warten, bevor sein Fenster geöffnet wird.
  - `canInteractWithBlock` begrenzt die Reichweite bei Strg-Klick.
  - Ein FakePlayer hat keinen Netzwerkkanal, deshalb überspringt `ProfileSync` ihn.

## 11. Git und GitHub

- **Arbeitszweig:** `claude/create-mechanical-crafter-mod-48gom2` (steht auf `main`).
- **Neue Arbeit:**
  1. Zweig von `main` neu aufsetzen, falls der PR des Zweigs schon gemerged ist.
  2. Committen und pushen.
  3. Draft-PR öffnen, CI abwarten.
  4. Mergen nur, wenn der Nutzer das will. Bisher wollte er alles auf `main`, damit der CurseForge-Source-Link vollständig ist.
- **Nicht im Repo:** fremde JARs, `run*/`, `build/`, `out/`, `libs/*`.
- **Flüchtig:** Die Modpack-ZIP `CreateExta-2.0.zip` lag nur im Upload-Ordner einer alten Session. Für Pack-Fakten den Nutzer erneut fragen; die Eckdaten stehen in `docs/01-phase1-analysis.md`.
