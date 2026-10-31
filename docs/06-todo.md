# Umsetzungsliste

Status: ✅ erledigt · 🟡 teilweise · ⬜ offen

0. ⬜ **Offen vom Nutzer:** JEI-Dateiname aus `mods/`; soll der kleine 3×3-Bereich eine eigene verbundene Crafter-Gruppe sein oder Teil der großen Gruppe (R12)?; maximale Anlagengröße; Lizenz. *Entschieden:* Profile per Strg + Rechtsklick, mehrere Bereiche pro Profil.
1. 🟡 **Gerüst und Umgebung:** Create 6.0.10 (exakter Build 280), Ponder, Flywheel, Registrate und JEI in `build.gradle`; `mods.toml` mit Abhängigkeiten und Mixin-Config; GameTest-Server mit 4 grünen Tests, auch in der CI. *Offen:* `runClient` einmal lokal mit Display starten.
2. ⬜ **Prototypen/Tests:** T1 Button-Zustände im Spiel, T2 JEI-Handler-Priorität, T3 Paket/Muster >9 (Codec, `PackageItem.setOrder`), T4 Teilpakete im `CrafterUnpackingHandler` (GameTest).
   🟡 T5: Reihenfolge der Crafter-Inventare gemessen (NORTH: oben zuerst, Ost→West). *Offen:* kleineres Rezept in größerer Gruppe, Blickrichtung (R12).
3. ⬜ Entscheidung Variante A oder B und Bereichsmodell (eigene Gruppe vs. Teilbereich).
4. 🟡 Datenmodell: ✅ `CrafterRegion`, `CrafterProfile` (Codec + StreamCodec, validiert), ✅ `ProfileManager` (SavedData), ✅ `SyncProfilesPayload` (beim Login). *Offen:* Sync nach jeder Änderung.
5. 🟡 Profilerstellung: ✅ `CreateCompat.findConnectedGroup` (Mixin-Accessor, Create-Reihenfolge), ✅ Tastenbelegung "Crafter-Profil" (Standard Strg, umbelegbar). *Offen:* Rechtsklick-Event (Client) → `CreateProfilePayload`/`AddRegionPayload` → Server-Scan → `CrafterRegion` aus Gruppe; Namenseingabe; Bereiche entfernen; Validierung rechteckig/disjunkt; Limits aus `Config`.
6. ⬜ `RecipeConverter` + Tests (3×3-Regression, 5×5, shapeless, Mirrored).
7. ⬜ `OrderPlanner` inkl. Paketaufteilung + Tests.
8. ⬜ Server-Payload + `OrderValidator` (Rezept-ID, Rechte, Limits).
9. 🟡 JEI: ✅ leeres `BcacJeiPlugin` registriert. *Offen:* Transfer-Handler, Fehlertooltips.
10. ⬜ `ProfileButton` + Bereichsauswahl (Auto = kleinster passender Bereich) im Stock-Keeper-Screen.
11. 🟡 Config: ✅ Server-Config (Bereichsgrößen, Limits, Auto-Auswahl), de/en-Sprachdateien. *Offen:* Datapack-Profile (optional).
12. 🟡 GameTests: ✅ Grundgerüst + 4 Tests. *Offen:* 5×5 End-to-End, manuelle Tests im vollständigen Pack, Mixin-Konfliktcheck (R7).
13. ⬜ Release: Version, Changelog, Veröffentlichung.
