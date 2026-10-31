# Phase 3 – Technischer Gesamtplan

## Ziel
Der Stock-Keeper-Craftingablauf (JEI "+" → Zutaten anfordern → Pakete → Crafter) soll auch für **größere verbundene Mechanical-Crafter-Anlagen** (z. B. 5×5, 4×6, 9×9) funktionieren – serverseitig validiert, kompatibel mit dem unveränderten 3×3-Verhalten.

## Benutzerbedienung (Soll)
1. Spieler baut Crafter-Anlage, verbindet sie (Create-Standard) und legt einen **Profil-Eintrag** an: Wrench-Rechtsklick auf Crafter-Controller (oder `/bcac profile create <name>`), Mod scannt die Gruppe → Profil `{name, Breite, Höhe, Maske, Packager-Adresse}`.
2. Im Stock-Keeper-Screen erscheint neben der Suchleiste ein **Profil-Auswahlbutton** (Standard: "3×3 / Create-Standard").
3. JEI öffnet ein Rezept (auch `create:mechanical_crafting`), Klick auf "+" → unser Handler legt den Eintrag in `recipesToOrder` an und konvertiert das Rezept auf das aktive Profil.
4. "Anfordern" sendet die Order; Server validiert, Pakete laufen zur Adresse des Profils.

## Architektur-Entscheidung
- **Variante A (bevorzugt, ohne Mixin):** eigener JEI-`IRecipeTransferHandler` für `StockKeeperRequestMenu` (Priorität vor Create, siehe R2) + eigener Auftrag über dieselbe `PackageOrderRequestPacket`-Kette, aber mit selbst erzeugtem Muster. Der Create-`CrafterUnpackingHandler` lässt sich nicht ersetzen (`register` wirft bei Duplikaten). Deshalb wird das Muster so erzeugt, dass die Create-Zuordnung (Muster-Index i ↔ Inventar i aus `getInventories()`) bereits korrekt ist: Rezept in Row-Major-Reihenfolge der Profil-Maske abbilden. Damit bleibt `CrafterUnpackingHandler` unangetastet.
- **Variante B (Fallback):** Mixins in `StockKeeperRequestScreen.sendIt/requestCraftable`, `FactoryPanelScreen.convertRecipeToPackageOrderContext`, `PackageRepackageHelper.repackBasedOnRecipes` (L3–L5). Nur wenn A an L3/L5 scheitert.
- Entscheidung erst nach Prototyp-Test T3 (Paket mit >9 Muster-Einträgen).

## Pakete / Klassen (`com.blaublue1303.bigcreateautocrafter`)
| Paket | Klassen | Seite |
|---|---|---|
| `.` | `BigCreateAutoCrafter` (Entry), `Config` | beide |
| `.profile` | `CrafterProfile` (record), `ProfileManager` (SavedData pro Level), `ProfileScanner` | Server |
| `.net` | `ProfileSyncPayload`, `ProfileCreatePayload`, `ProfileSelectPayload`, `OrderValidationPayload` (NeoForge Payload-API) | beide |
| `.recipe` | `GridRecipe` (W×H, `List<Ingredient>`), `RecipeConverter` (Rezept→Profil-Muster) | beide |
| `.order` | `OrderPlanner` (Bestand→Zutatenzuordnung, Paketaufteilung ≤9 Slots), `OrderValidator` | Server (Planer auch Client zur Anzeige) |
| `.client` | `ProfileButton`, `ClientEvents`, `BigTransferHandler` | Client |
| `.compat.jei` | `JeiPlugin` (`@JeiPlugin`) | Client |
| `.compat.create` | Zugriff auf Create-Klassen (nur hier importieren) | beide |
| `.mixin` | nur Variante B | beide |

## Datenmodelle
- `CrafterProfile{ UUID id, String name, int width, int height, boolean[] mask, BlockPos controller, ResourceKey<Level> dim, String address, UUID owner }`
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
