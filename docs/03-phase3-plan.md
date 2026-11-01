# Phase 3 – Technischer Gesamtplan

> **Stand Prototyp 1:** umgesetzt mit Variante B (Mixins für L4, L5 und den Startzeitpunkt) plus eigenem JEI-Handler für L1. Ein Bereich ist eine ganze Create-Crafter-Gruppe. Die Klassen weichen im Detail von der Tabelle unten ab (z. B. `ProfileService`, `RegionScanner`, `LargePatternPackaging`, `ProfileSelector`); maßgeblich sind der Code und `docs/07-prototyp-1.md`.

## Ziel
Der Stock-Keeper-Craftingablauf (JEI "+" → Zutaten anfordern → Pakete → Crafter) soll auch für **größere verbundene Mechanical-Crafter-Anlagen** (z. B. 5×5, 4×6, 9×9) funktionieren – serverseitig validiert, kompatibel mit dem unveränderten 3×3-Verhalten.

## Benutzerbedienung (Soll, Stand Nutzerwunsch)
1. **Profil anlegen:** Strg + Rechtsklick auf einen Mechanical Crafter. Der Client erkennt die Taste und sendet ein Payload; der Server scannt die verbundene Crafter-Gruppe und legt ein neues **Profil** an (Name per Eingabe, Adresse des Packagers).
2. **Mehrere Bereiche pro Profil:** Ein Profil ist eine ganze Anlage und enthält mehrere **Bereiche** (Regions). Beispiel: Anlage mit 21 Crafter-Feldern. Erst wählt man den großen Bereich (alle 21), danach per weiterem Strg + Rechtsklick auf einen anderen Crafter / Teilgruppe einen kleinen **3×3-Bereich**, in dem kleine Rezepte gecraftet werden.
3. **Im Stock-Keeper-Screen:** Profil-Auswahl neben der Suchleiste, darunter Bereichsauswahl. Standard "Automatisch": der **kleinste passende Bereich** wird für das Rezept genommen (3×3-Rezept → 3×3-Bereich, 5×4-Rezept → großer Bereich). Manuelle Wahl überschreibt.
4. JEI "+" legt den Eintrag an, der Rezept-Konverter nutzt den gewählten Bereich, "Anfordern" sendet die Order, der Server validiert.

## Architektur-Entscheidung
- **Variante A (bevorzugt, ohne Mixin):** eigener JEI-`IRecipeTransferHandler` für `StockKeeperRequestMenu` (Priorität vor Create, siehe R2) + eigener Auftrag über dieselbe `PackageOrderRequestPacket`-Kette, aber mit selbst erzeugtem Muster. Der Create-`CrafterUnpackingHandler` lässt sich nicht ersetzen (`register` wirft bei Duplikaten). Deshalb wird das Muster so erzeugt, dass die Create-Zuordnung (Muster-Index i ↔ Inventar i aus `getInventories()`) bereits korrekt ist: Rezept in Row-Major-Reihenfolge der Profil-Maske abbilden. Damit bleibt `CrafterUnpackingHandler` unangetastet.
- **Variante B (Fallback):** Mixins in `StockKeeperRequestScreen.sendIt/requestCraftable`, `FactoryPanelScreen.convertRecipeToPackageOrderContext`, `PackageRepackageHelper.repackBasedOnRecipes` (L3–L5). Nur wenn A an L3/L5 scheitert.
- Entscheidung erst nach Prototyp-Test T3 (Paket mit >9 Muster-Einträgen).

## Pakete / Klassen (`com.blaublue1303.bigcreateautocrafter`)
✅ = im Gerüst vorhanden, ⬜ = folgt (Nummer = Schritt in docs/06-todo.md)

| Paket | Klassen | Seite |
|---|---|---|
| `.` | ✅ `BigCreateAutoCrafter` (Entry), ✅ `Config` (Server-Config) | beide |
| `.profile` | ✅ `CrafterRegion`, ✅ `CrafterProfile`, ✅ `ProfileManager` (SavedData), ✅ `ProfileSync`; ⬜ `ProfileService` (5) | Server |
| `.net` | ✅ `BcacNetwork`, ✅ `SyncProfilesPayload`; ⬜ `CreateProfilePayload`, `AddRegionPayload` (5), `CraftOrderPayload` (8) | beide |
| `.recipe` | ⬜ `GridRecipe`, `RecipeConverter` (6) | beide |
| `.order` | ⬜ `OrderPlanner` (7), `OrderValidator` (8) | Server (Planer auch Client) |
| `.client` | ✅ `BigCreateAutoCrafterClient`, ✅ `BcacKeyMappings`, ✅ `ClientProfileCache`; ⬜ `CrafterClickHandler` (5), `ProfileButton` (10) | Client |
| `.compat.create` | ✅ `CreateCompat`, ✅ `ConnectedCrafterGroup` (einziger Zugriff auf Create-Interna) | beide |
| `.compat.jei` | ✅ `BcacJeiPlugin` (noch ohne Handler); ⬜ `BigTransferHandler` (9) | Client |
| `.mixin` | ✅ `ConnectedInputAccessor`; weitere nur bei Variante B | beide |
| `.gametest` | ✅ `CreateCompatGameTests`, ✅ `ProfileCodecGameTests` | Server (nur Testmodus) |

## Datenmodelle
- `CrafterProfile{ UUID id, String name, ResourceKey<Level> dim, String address, UUID owner, List<CrafterRegion> regions }`
- `CrafterRegion{ String name, BlockPos controller, Direction facing, List<String> layout }`. `layout` sind Zeilen von oben nach unten, `#` = Crafter, `.` = Lücke (lesbar in NBT und später in Datapack-JSON). Ein Bereich ist im Gerüst eine ganze verbundene Create-Gruppe. Teilbereiche lassen sich später als optionales Feld ergänzen, ohne gespeicherte Daten zu brechen (R12).
- `GridRecipe{ int w, h, List<Ingredient> cells }` aus `ShapedRecipe`/`MechanicalCraftingRecipe` (`getWidth/getHeight/getIngredients`); Shapeless → Row-Major füllen.
- Persistenz: `SavedData` (Codec), Sync per Payload; Datapack-Unterstützung optional: Profile aus `data/<ns>/bcac_profiles/*.json` (Phase 8).

## Crafter-Erkennung
`ProfileScanner`: Controller `MechanicalCrafterBlockEntity` → `ConnectedInput` (package-private `data`; Zugriff via `getInput().getInventories(level,pos)` public + Positionen der `MechanicalCrafterBlockEntity`). Bounding-Box in Blickrichtung (`HORIZONTAL_FACING`), Maske für Lücken; Profil ungültig, wenn Gruppe nicht rechteckig (v1) oder nicht verbunden.

## Rezeptumwandlung
1. `GridRecipe` (w×h) in Profil (W×H) einpassen, Anker oben-links (Create-`matches` akzeptiert verschobene Muster nur bei `acceptMirrored=false` per Offset-Suche → Anker konfigurierbar).
2. Ausgabe: Liste der Länge `W*H` Row-Major, Leerfelder = `ItemStack.EMPTY` (entspricht `getInventories()`-Sortierung, L7).
3. Passt das Rezept nicht (w>W oder h>H) → Fehlertooltip (`RecipeTransferErrorTooltip`).

## Lager-/Zutatenlogik
`OrderPlanner` ersetzt `maxCraftable`: Zutaten-Alternativen (`Ingredient.test`) gegen `InventorySummary` (Stock-Snapshot), **Typlimit 9 entfällt oder wird auf Konfig-Wert gesetzt** (L3). Paketaufteilung: je Craft eine Menge von Paketen mit ≤9 Slots, jedes trägt das Voll-Muster; `CrafterUnpackingHandler` füllt greedy nur leere Positionen (T4 verifiziert das).

## Servervalidierung
Eigenes Payload prüft: Spieler darf Stock Ticker bedienen (`Create.LOGISTICS.mayInteract`), Profil gehört zur Dimension/Reichweite, Rezept existiert serverseitig (`RecipeManager`, per ID statt Client-Objekt!), Muster = serverseitig neu berechnet (Client-Muster wird **nicht** vertraut), Mengenlimits (Konfig), Rate-Limit.

## Fehlerbehandlung
Kein Profil/ungültig, Rezept zu groß, Bestand fehlt, Packager busy (`isTooBusyFor`), Crafter belegt → sprechende Chat-/Tooltip-Meldungen, nie Teil-Order senden.

## Konfiguration
`ModConfigSpec` COMMON: `maxGridSize` (Default 9), `maxItemTypesPerOrder`, `allowShapeless`, `anchor`. CLIENT: Button-Position.

## JEI-Integration
Erforderlich (der "+" ist JEI). `compileOnly jei-…-neoforge-api`, `@JeiPlugin`, `registerRecipeTransferHandlers`. Registrierungsreihenfolge/Priorität gegenüber `CreateJEI` prüfen (T2).

## Tests
- Unit (JUnit): `RecipeConverter`, `OrderPlanner`, Profil-Codec.
- GameTest (`gameTestServer` Run-Config vorhanden): 3×3-Regression, 5×5-Anlage + Stock Ticker + Packager (Struct-NBT).
- Manuell: `runClient` mit Create + JEI + Flywheel + Ponder (`localRuntime`).

## Build / Veröffentlichung
`./gradlew build` (CI: `.github/workflows/build.yml`), JAR `bigcreateautocrafter-<ver>.jar`; Veröffentlichung später über CurseForge/Modrinth (nur Mod-JAR, keine Create-Dateien); Lizenz klären (R9).

## Datenfluss
```text
Storage/Stock Keeper GUI (StockKeeperRequestScreen, Create)
        ↓  Spieler wählt Item, öffnet JEI-Rezept
Profil-Auswahl (ProfileButton, Client)  ──►  aktives CrafterProfile
        ↓
Rezeptanalyse (GridRecipe aus RecipeHolder; Größe, Zutatenalternativen)
        ↓
Ausgewählte Crafter-Anlage (Profil: W×H, Maske, Adresse)
        ↓
Rezeptumwandlung (RecipeConverter → Row-Major-Muster der Länge W*H)
        ↓
Servervalidierung (Payload: Rezept-ID + Profil-ID + Menge; Server berechnet Muster neu)
        ↓
Crafting-Auftrag (PackageOrderWithCrafts → PackageOrderRequestPacket-Kette / LogisticsManager)
        ↓
Zutatenverteilung (Packager → Repackager: Pakete ≤9 Slots, Voll-Muster im Kontext)
        ↓
Create Mechanical Crafters (CrafterUnpackingHandler → Inventar i ↔ Musterindex i → checkCompletedRecipe)
```
