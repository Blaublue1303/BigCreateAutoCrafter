# Umsetzungsliste (erst nach Freigabe der Phasen 1–3)

0. **Offen vom Nutzer:** JEI-Dateiname aus `mods/`; Wunschformen der Anlagen (max. Größe? nur Rechtecke?); Lizenzwunsch; Profile werden per Strg+Rechtsklick angelegt (entschieden).
1. `build.gradle`: Create (+Flywheel, Ponder, Catnip) und JEI-API einbinden, `./gradlew runClient` mit Create+JEI starten.
2. **Prototypen/Tests T1–T4** (ohne Features): T1 Button-Zustände im Spiel, T2 JEI-Handler-Priorität, T3 Paket/Muster >9 (Codec, `PackageItem.setOrder`), T4 Teilpakete im `CrafterUnpackingHandler` (GameTest).
   T5: Verhalten von Create, wenn ein kleineres Rezept in einer größeren verbundenen Gruppe (z. B. 21 Felder) nur teilweise befüllt wird, bzw. mit separater 3×3-Gruppe (R12).
3. Entscheidung Variante A oder B und Bereichsmodell (eigene Gruppe vs. Teilbereich).
4. Datenmodell: `CrafterProfile`, Codec, `ProfileManager` (SavedData), Sync-Payloads.
5. `ProfileScanner` + Erstellung per **Strg + Rechtsklick** (Client-Event → Payload → Server-Scan), Bereiche hinzufügen/entfernen, Validierung rechteckig und Disjunktheit.
6. `RecipeConverter` + Unit-Tests (3×3-Regression, 5×5, shapeless, Mirrored).
7. `OrderPlanner` inkl. Paketaufteilung + Unit-Tests.
8. Server-Payload + `OrderValidator` (Rezept-ID, Rechte, Limits).
9. JEI-Plugin / Transfer-Handler, Fehlertooltips.
10. `ProfileButton` + Bereichsauswahl (Auto = kleinster passender Bereich) im Stock-Keeper-Screen (+ Lokalisierung de/en).
11. Config, Datapack-Profile (optional).
12. GameTests (5×5 End-to-End), manuelle Tests im vollständigen Pack, Mixin-Konfliktcheck (R7).
13. Release: Version, Changelog, `mods.toml`-Abhängigkeiten (`create [6.0.10,6.1)`, `jei` optional/client), Veröffentlichung.
