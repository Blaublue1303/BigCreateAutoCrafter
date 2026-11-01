# Risiko- und Machbarkeitsanalyse

Gesamteinschätzung: **machbar**, da die Crafter-Seite (Create) bereits beliebige Gruppen kann; Aufwand liegt in Auftragskette und Zuordnung.

| ID | Risiko | Wahrsch. | Auswirkung | Gegenmaßnahme |
|---|---|---|---|---|
| R1 | Create-Interna (`@Experimental` API, private Felder) ändern sich bei Updates | hoch | mittel | Create-Version im Pack gepinnt (6.0.10); `compat.create` kapselt Zugriffe; `[6.0.10,6.1)` als Versionsbereich |
| R2 | JEI wählt Create-Handler statt unseren (beide universal für `StockKeeperRequestMenu`) | mittel | hoch | T2: Reihenfolge testen; Fallback Mixin auf `CreateJEI`/`StockKeeperTransferHandler` (Variante B) |
| R3 | Pakete haben nur 9 Slots (L5) → >9 Zutaten | sicher | hoch | Mehrere Pakete je Craft; T3/T4 beweisen, dass Create-Handler Teilpakete nimmt; sonst Mixin |
| R4 | `CrafterUnpackingHandler` kennt nur Listenindex; nicht-rechteckige Gruppen (L7) | mittel | mittel | v1: nur Rechtecke; Maske validieren |
| R5 | `ConnectedInput.data` ist package-private | sicher | niedrig | Zugriff über public `getInventories`; sonst Access Transformer (`META-INF/accesstransformer.cfg`) |
| R6 | Client darf Muster nicht bestimmen (Cheat/Exploit) | mittel | hoch | Server berechnet Muster neu aus Rezept-ID |
| R7 | Mixin-Konflikte mit anderen Pack-Mods (ModernFix, Create-Addons) | unbekannt | mittel | JARs des Packs fehlen → Test mit vollständigem Pack nötig (`mods`-Liste/Dateien anfordern) |
| R8 | JEI-Version im Pack unbekannt (file 8857687) | – | niedrig | nur API-Funktionen nutzen, die seit 19.x stabil sind; Dateiname vom Nutzer erfragen |
| R9 | Lizenz: Create/JEI-Code nicht kopieren | – | hoch | nur Interop, kein eingechecktes Fremdcode, `libs/` ist gitignored |
| R10 | Mojang-Mappings/Parchment-Lizenz | niedrig | niedrig | im README erwähnt |
| R11 | Fehlerhafte Annahme "blauer Button = JEI" | niedrig | mittel | im Spiel verifizieren (T1); Code zeigt keinen Create-Plus-Button |
| R12 | ~~Kleiner Bereich in großer Gruppe blockiert~~: **geklärt.** `CrafterUnpackingHandler` ruft `checkCompletedRecipe(true)` (wie ein Redstone-Start), leere Crafter sind erlaubt, das Rezept ergibt sich aus der Bounding-Box der belegten Crafter | – | – | Kleine Rezepte gehen auch in große Bereiche, eigene 3×3-Gruppen sind optional |
| R13 | Strg + Rechtsklick: Strg-Status nur clientseitig, auf macOS ist Strg+Klick = Rechtsklick; Konflikt mit Create-Wrench/anderen Mods | mittel | niedrig | Taste konfigurierbar, Server validiert Block, Reichweite und Rechte; Alternative: Create-Wrench-Interaktion |
| R14 | Ein Craft mit mehr als 9 Paket-Slots (unstapelbare Zutaten) wird auf mehrere Pakete verteilt. Kommen Pakete verschiedener Crafts vermischt an, nimmt Creates Entpacker das zweite Paket nicht an, und der Packager wartet | niedrig | mittel | Pakete werden pro Craft hintereinander exportiert, der Crafter startet erst bei vollem Raster. Später: Entpacker toleranter machen |
| R15 | Create erlaubt höchstens 9 verschiedene Itemtypen pro Auftrag (L3) | sicher | niedrig | Für Prototyp 1 akzeptiert, die meisten großen Rezepte haben weniger Typen |
| R16 | Mixins in Create-Interna (`StockKeeperRequestScreen.sendIt`, `PackageRepackageHelper`, `CrafterUnpackingHandler`) | mittel bei Create-Updates | hoch | Versionsbereich `[6.0.10,6.1)`, `defaultRequire = 1` (Fehler fallen beim Start auf), GameTests und Smoke-Test |

## Verifizierungsstatus der Annahmen
- Aus Code belegt: L1–L8, Aufrufkette, Registry-Duplikat-Exception, JEI-Handler-Registrierung.
- Durch GameTests belegt: Muster > 9 über das Netzwerk (T3); Paketierung; Entpacken in die richtigen Crafter einer 21er-Gruppe; Teilpakete starten erst bei vollem Raster (T4); Strg-Rechtsklick-Ablauf.
- Im Client belegt (Smoke-Test): Client-Mixin wird angewendet, Profil-Sync, JEI wählt unseren Handler (T2), das Crushing Wheel mit 21 Zutaten wird angenommen.
- **Noch nicht getestet:** echter Crafting-Durchlauf mit Rotation und Paketwegen; Farbzustand des Buttons (T1); Tooltip-Darstellung; Konflikte mit anderen Pack-Mods (R7).
