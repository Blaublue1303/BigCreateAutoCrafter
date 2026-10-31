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

## Verifizierungsstatus der Annahmen
- Aus Code belegt: L1–L8, Aufrufkette, Registry-Duplikat-Exception, JEI-Handler-Registrierung.
- **Nur abgeleitet, nicht getestet:** Teilpakete (R3), Codec ohne Längenlimit für `PackageOrder`, Verhalten bei Lücken, Button-Farbzustand.
