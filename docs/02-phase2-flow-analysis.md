# Phase 2 – Analyse des aktuellen Ablaufs (Create 6.0.10, dekompiliert)

Methode: JAR mit Vineflower dekompiliert, nur lokal gelesen (Create-Quelltext wird nicht eingecheckt). Klassen unter `com.simibubi.create.…`.

## Antworten auf die 21 Fragen

1. **Welche Mod?** Create 6.0.10 selbst (kein separates Storage-Keeper-Addon).
2. **GUI?** `StockKeeperRequestScreen` / `StockKeeperRequestMenu` (Stock Ticker, Paket `content.logistics.stockTicker`).
3. **Klasse des Buttons?** Der "+"-Button gehört **JEI** (`mezz.jei.gui.recipes.RecipeTransferButtonController`, Sprite `recipe_plus_sign.png`), nicht Create. `StockKeeperRequestScreen` enthält keinen eigenen Plus-Button. Die Farbe kommt von `IRecipeTransferError.getButtonHighlightColor()` bzw. `JeiGuiColors.RECIPE_TRANSFER_BUTTON_HIGHLIGHT`. *Das Blau ist damit ein JEI-Button-Zustand; ich habe noch nicht im Spiel verifiziert, welcher Zustand blau ist.* JEI-Button und Stock-Keeper-Logik sind getrennt zu behandeln: JEI zeigt den Button, Create liefert den Handler.
4. **Methode beim Klick?** JEI ruft `StockKeeperTransferHandler.transferRecipe(...)` (`compat.jei`, registriert in `CreateJEI.registerRecipeTransferHandlers` als `addUniversalRecipeTransferHandler`) → `transferRecipeOnClient(...)`.
5. **Rezeptobjekt?** `RecipeHolder<Recipe<?>>` aus JEI; daraus `CraftableBigItemStack(result, recipe)`.
6. **Interne Speicherung?** Client: `StockKeeperRequestScreen.recipesToOrder: List<CraftableBigItemStack>` (+ `itemsToOrder: List<BigItemStack>`). Netzwerk: `PackageOrderWithCrafts(PackageOrder orderedStacks, List<CraftingEntry(PackageOrder pattern, int count)> orderedCrafts)`. Das **Muster ist eine flache Liste**, kein 2D-Objekt.
7. **Auftrag?** `StockKeeperRequestScreen.sendIt()` baut je Rezept `pattern = FactoryPanelScreen.convertRecipeToPackageOrderContext(cr, mutableOrder, true)` und sendet `PackageOrderRequestPacket` → `StockTickerBlockEntity.broadcastPackageRequest` → `LogisticsManager.broadcastPackageRequest`.
8. **Zutaten anfordern?** `LogisticsManager.findPackagersForRequest` verteilt `orderedStacks` auf `LogisticallyLinkedBehaviour`/Packager; der Crafting-Kontext wird nur an den ersten benutzten Link angehängt.
9. **Verteilung an die Crafter?** Packager → Repackager: `PackageRepackageHelper.repackBasedOnRecipes` erzeugt je Craft ein Paket mit `PackageOrderWithCrafts.singleRecipe(pattern)`. Beim Entpacken über einen Packager an einem Crafter: `UnpackingHandler.REGISTRY` → `CrafterUnpackingHandler` (registriert in `AllUnpackingHandlers`), der Item `i` des Musters in Inventar `i` von `ConnectedInput.getInventories()` legt und danach `crafter.checkCompletedRecipe(true)` aufruft.
10. **Wo 3×3?** Mehrere, voneinander unabhängige Stellen (siehe Tabelle unten).
11. **Nur Anzeige?** Nein.
12. **Rezept intern auf 3×3?** Ja: Muster ist auf **9 Einträge** normiert (`convertRecipeToPackageOrderContext`), Paket hat 9 Slots.
13. **Crafter-Anlage selbst?** **Nein.** Verbundene Mechanical Crafter bilden beliebig große Gruppen (`ConnectedInput.data`), `MechanicalCraftingRecipe extends ShapedRecipe` (Typ `create:mechanical_crafting`) unterstützt größere Formen; JEI hat dafür `MechanicalCraftingCategory`.
14. **Größere Matrizen unterstützt?** Create-Crafter ja, Stock-Keeper-Kette nein.
15. **API erweiterbar?** Teilweise: `UnpackingHandler` ist `@Experimental` public API (`UnpackingHandler.REGISTRY`, `SimpleRegistry`). `register` wirft bei Duplikat (`IllegalArgumentException`) → den bestehenden Crafter-Handler kann man nicht einfach ersetzen.
16. **Event-Hook?** Kein Event für Rezept-Transfer oder `sendIt`. JEI-seitig kann ein eigener `IRecipeTransferHandler` registriert werden (Auswahl durch JEI nach Registrierungsreihenfolge – Risiko R2).
17. **Mixin nötig?** Für `StockKeeperRequestScreen` (private Logik in `sendIt`/`maxCraftable`/`requestCraftable`, harte `9`) und für `FactoryPanelScreen.convertRecipeToPackageOrderContext` (statisch, harte 3/9) voraussichtlich ja, oder Ersatz der Aufrufer über eigenen Handler (Option A im Plan).
18. **Eigener Auftragstyp?** Nicht zwingend: `CraftingEntry.pattern` darf längere Listen codieren (Codec ohne Längenlimit gesehen, **muss getestet werden**).
19. **Eigener Rezepttyp?** Nein. `MechanicalCraftingRecipe` ist ein `CraftingRecipe`/`ShapedRecipe`, `sendIt` filtert mit `instanceof CraftingRecipe` → wird bereits akzeptiert.
20. **Blauen Plus-Button erweiterbar?** Der Button ist JEI. Erweiterbar nur über die eigene `IRecipeTransferError`/Handler-Logik, nicht über Create.
21. **Profil-Auswahl neben dem Button?** Nicht im JEI-Button. Realistisch: eigener Screen-Button im `StockKeeperRequestScreen` (NeoForge `ScreenEvent.Init.Post` + `addRenderableWidget`) oder Shift-Klick/Tooltip. Siehe Plan.

## Wo die Begrenzung entsteht

| # | Stelle | Wirkung |
|---|---|---|
| L1 | `StockKeeperTransferHandler.transferRecipeOnClient`: `recipe.getIngredients().size() > 9` → `RecipeTransferErrorInternal` | JEI zeigt kein Transfer für >9 Zutaten |
| L2 | selbe Methode: 9 Dummy-`Slot`s für `RecipeTransferUtil.getRecipeTransferOperations` | Bestandsprüfung kennt nur 9 Positionen |
| L3 | `screen.itemsToOrder.size() >= 9` und `maxCraftable(... 9 - itemsToOrder.size())` | höchstens 9 verschiedene Item-Typen je Auftrag (auch in 3×3 limitierend) |
| L4 | `FactoryPanelScreen.convertRecipeToPackageOrderContext`: `min(3, …)`, Padding auf 9 | Muster immer 3×3 |
| L5 | `PackageRepackageHelper.repackBasedOnRecipes`: `ItemStackHandler(9)`, `Math.min(stacks.size(), 9)` | Paketinhalt auf 9 Slots abgeschnitten |
| L6 | `CrafterUnpackingHandler`: Muster-Index `i` ↔ `getInventories().get(i)` | Zuordnung nur über Listenindex, **keine** 2D-Geometrie |
| L7 | `ConnectedInput.getInventories`: Sortierung nach Y absteigend, dann Achsenrichtung | Reihenfolge reihenweise, aber ohne Breite → Rechtecke OK, Lücken/L-Formen mehrdeutig |
| L8 | `PackageOrderWithCrafts.hasCraftingInformation`: genau 1 `CraftingEntry` pro Paket | je Craft ein Paket mit Voll-Muster |

Nicht begrenzt: `MechanicalCraftingRecipe.matches` (bis zur Gruppengröße), `RecipeGridHandler` (arbeitet auf der realen Crafter-Gruppe).

## Fazit

Der Anzeige-Teil (JEI) und die Auftragskette sind bei 3×3 hart codiert, die Crafter-Anlage nicht. Für größere Anlagen müssen mindestens L1, L3, L4, L5 und L6 geändert werden; L7 bestimmt, welche Anlagenformen zuverlässig adressierbar sind.
