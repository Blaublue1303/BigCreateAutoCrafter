# Umsetzungsliste

Status: ✅ erledigt · 🟡 teilweise · ⬜ offen

0. 🟡 **Nutzer:** Stock-Keeper-Ablauf mit 0.0.2 im Spiel bestätigt. *Offen:* Test von 0.1.0 (Menü, Factory Gauge), JEI-Dateiname aus `mods/`.
   *Entschieden:* Profile per Strg + Rechtsklick, mehrere Bereiche pro Profil.
1. ✅ **Gerüst und Umgebung:** Create 6.0.10 (Build 280), Ponder, Flywheel, Registrate, JEI; GameTests in der CI; Client-Smoke-Test lokal (`xvfb-run ./gradlew runClientSmokeTest`).
2. ✅ **Prototyp-Tests:**
   - T2 (JEI-Priorität): aus JEI-Code geklärt und im Client bestätigt. Handler für konkrete Rezepttypen schlagen Creates universellen Handler.
   - T3 (Muster > 9 über Netzwerk): GameTest `largePatternSurvivesNetwork`.
   - T4 (Entpacken in große Gruppe, Teilpakete): GameTests `unpacksRecipeIntoRightCrafters` und `splitPackagesFillGridBeforeStart`.
   - T5 (kleines Rezept in großer Gruppe): aus Create-Code geklärt. Der Entpack-Handler startet wie mit Redstone, leere Crafter sind erlaubt.
   - 🟡 T1 (Farbe des Buttons): nur im Spiel prüfbar.
3. ✅ Entscheidung: Variante B (Mixins) für L4/L5 und den Startzeitpunkt, eigener JEI-Handler für L1. Ein Bereich ist eine ganze Create-Gruppe.
4. ✅ Datenmodell, Speicherung, Sync (auch nach jeder Änderung).
5. ✅ Profilerstellung per Strg + Rechtsklick (Shift = entfernen/neu), `/bcac`-Befehle, Limits aus `Config`, Fenster „Crafter-Profile“ (umbenennen, löschen, Adressen, Bereiche entfernen). *Offen:* Adresse automatisch vom Packager lesen.
6. ✅ `GridRecipe`, `RegionPlacer` (Auto = kleinster passender Bereich), `PatternBuilder`.
7. ✅ Paketierung großer Muster nach Itemtyp (`LargePatternPackaging`), Start erst bei vollem Raster.
8. ⬜ Eigene Servervalidierung (R6). Create vertraut dem Client-Muster bereits, deshalb nicht Teil von Prototyp 1.
9. ✅ JEI-Transfer-Handler für `minecraft:crafting`, `create:automatic_shaped` und `create:mechanical_crafting`, Fehlertooltips, Standard leitet an Create weiter.
10. ✅ Auswahl am Stock Keeper als Aufklappliste mit „Profile verwalten…“ (JEI-Sperrzone).
10b. ✅ Factory Gauge auf Crafter-Bereichen (Bereich über die Gauge-Adresse, Mechanical-Crafting-Rezepte). 🟡 Im Spiel testen.
11. 🟡 Config (Server). *Offen:* Datapack-Profile (optional), Client-Config für die Button-Position.
12. 🟡 Tests: 20 GameTests und ein Client-Smoke-Test. *Offen:* echter Crafting-Durchlauf mit Rotation und Paketwegen im Spiel, Test im vollständigen Modpack (Mixin-Konflikte R7).
13. ⬜ Release: Version, Changelog, Veröffentlichung.
14. ⬜ Danach: mehr als 9 Itemtypen pro Auftrag (L3), robuste Reihenfolge bei geteilten Paketen (R14).
