# Phase 1 – Modpack- und Versionsanalyse

Quelle: `CreateExta+-2.0.zip` (CurseForge-Modpack, `manifestType: minecraftModpack`).

## Verifizierte Fakten

| Punkt | Wert | Beleg |
|---|---|---|
| Modpack | Create Exta+ 2.0, Autor blaublue1303 | `manifest.json` |
| Minecraft | 1.21.1 | `manifest.json` |
| NeoForge | 21.1.250 | `manifest.json` (`neoforge-21.1.250`) |
| Create | **6.0.10** (`create-1.21.1-6.0.10.jar`) | CurseForge-Fileinfo zu projectID 328085 / fileID 7963363; Größe 19123767 B identisch mit der Modrinth-JAR (SHA-1 `0e97e498…acc353`) |
| Create Mod-ID | `create` | `META-INF/neoforge.mods.toml` der JAR |
| JEI Mod-ID | `jei` | JAR |
| Storage-/Stock-Keeper-System | **Teil von Create 6.0.x** ("Stock Keeper", Block `StockTicker`, Paket `com.simibubi.create.content.logistics.stockTicker`). Keine separate Mod. | Klassen in der Create-JAR; `create-client.toml` erwähnt "Stock Keepers" |
| Create-Abhängigkeiten | NeoForge ≥ 21.1.219, Flywheel [1.0,2.0), Ponder ≥ 1.0.82 | `neoforge.mods.toml` |
| Seiten | Create: BOTH; JEI: Client-UI, Server braucht JEI nicht | Create-Code (JEI-Handler ist rein clientseitig) |

Das Archiv enthält **keine JARs**, nur 89 `projectID/fileID`-Paare, `modlist.html` (89 Einträge) und `overrides/config`.

## Nicht verifiziert / offen

- **Exakte JEI-Version des Packs** (manifest: projectID 238222, fileID 8857687). Die CurseForge-Datei-API konnte ich nicht massenhaft abfragen (Abfrage mit gefälschtem User-Agent wurde zu Recht blockiert, wird nicht umgangen). Für Entwicklung wird JEI 19.70.0.468 (Modrinth) genutzt. **Bitte die JEI-Dateiname aus dem `mods`-Ordner nennen.**
- Die restlichen 87 Mods wurden nur namentlich gesichtet, nicht gegen Dateien geprüft.

## Relevanz der übrigen Mods

- **Create Aeronautics: Automated Logistics**: betrifft Luftschiff-Routen, *nicht* das Stock-Keeper-System (Konfig geprüft).
- Create-Addons (Steam 'n' Rails, Big Cannons, Design n' Decor, Smart Bounds, Threaded Trains, Liquid Fuel/Blueprints, …): keine Hinweise auf Mechanical-Crafter- oder Stock-Keeper-Änderungen in Konfigs; **Mixin-Konflikte wurden noch nicht geprüft** (JARs fehlen) – siehe Risiko R7.
- **Just Enough Threads**, **ModernFix**, **FerriteCore**: ändern JEI-Startup bzw. Speicher, kein bekannter Eingriff in Transfer-Handler.
- Alle anderen Rezeptviewer (REI/EMI): nicht im Pack.

## Create-Konfiguration im Pack (relevant)

`create-server.toml`: `allowRegularCraftingInCrafter = true`, `maxFireworkIngredientsInCrafter = 9`. Keine Größenbegrenzung für verbundene Crafter-Gruppen gefunden.
