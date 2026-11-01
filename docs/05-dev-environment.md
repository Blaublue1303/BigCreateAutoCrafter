# Entwicklungs- und Testumgebung

## Versionen
- JDK 21, Gradle Wrapper 9.2.1, ModDevGradle `2.0.148`, NeoForge **21.1.250**, MC 1.21.1, Parchment 2024.11.17.
- Mod-ID `bigcreateautocrafter`, Gruppe `com.blaublue1303.bigcreateautocrafter`, Version `0.0.1`.

## Abhängigkeiten (`build.gradle`, Versionen in `gradle.properties`)
| Artefakt | Version | Herkunft / Beleg |
|---|---|---|
| `com.simibubi.create:create-1.21.1` (`slim`) | `6.0.10-280` | maven.createmod.net. Build 280 hat denselben Git-Commit `ac0c444d` wie die Pack-JAR, alle 2692 Klassen byte-identisch |
| `net.createmod.ponder:ponder-neoforge` (enthält Catnip) | `1.0.82+mc1.21.1` | in Create-JAR gebündelt (`META-INF/jarjar/metadata.json`) |
| `dev.engine-room.flywheel:flywheel-neoforge(-api)-1.21.1` | `1.0.6` | ebenso |
| `com.tterrag.registrate:Registrate` | `MC1.21-1.3.0+67` | ebenso, mvn.devos.one/snapshots |
| `mezz.jei:jei-1.21.1-*` | `19.70.0.468` | maven.blamejared.com. **Pack-Version noch unbestätigt** (R8) |

Ponder wird mit `transitive = false` eingebunden, weil seine POM sonst Flywheel 1.0.4 statt 1.0.6 zieht.
`scripts/fetch-dev-jars.sh` wird nur noch zur Analyse (Dekompilieren) gebraucht, nicht für den Build.

## Befehle
- `./gradlew build`: kompiliert gegen Create und JEI und baut `build/libs/bigcreateautocrafter-0.1.0.jar`.
- `./gradlew runGameTestServer`: startet einen headless Server mit Create, Ponder, Flywheel, Registrate, JEI und der Mod, führt alle GameTests aus und beendet sich mit dem Ergebnis als Exit-Code. **Läuft lokal und in der CI.**
- `xvfb-run ./gradlew runClientSmokeTest` (Linux ohne Display; mit Display reicht `./gradlew runClientSmokeTest`): automatischer Client-Test in eigenem Ordner `run-smoketest/`. Er legt eine Flachwelt an, baut eine 21er-Crafter-Gruppe, legt über dieselbe Serverlogik wie Strg + Rechtsklick ein Profil an und öffnet einen echten Stock-Keeper-Screen. Dann prüft er die JEI-Handler, öffnet Aufklappliste und Profilfenster, benennt ein Profil über den Server um und macht Screenshots. Das Ergebnis steht in `run-smoketest/bcac-smoketest.txt` (`RESULT PASS/FAIL`). Nicht in der CI.
- `./gradlew runClient` / `runServer`: manuelles Testen. `runServer` verlangt `run/eula.txt`.

## GameTests (`src/main/java/.../gametest`, Arena `data/bigcreateautocrafter/structure/empty7.nbt`), 20 Tests
| Klasse | prüft |
|---|---|
| `CreateCompatGameTests` | Mixin-Accessor, Lesen verbundener Gruppen in Creates Reihenfolge |
| `ProfileCodecGameTests` | Speicher- und Netzwerk-Codec des Profils, Validierung der Layouts |
| `RegionGameTests` | Scan echter Gruppen (21er, L-Form, beide Achsen), Platzierung inkl. echtem Crushing-Wheel- und Truhen-Rezept |
| `LogisticsGameTests` | T3 (Netzwerk), Repackager (nach Itemtyp, Aufteilung, 3×3 unverändert), T4 (Entpacken in die richtigen Crafter, Teilpakete), Creates Zählschleife aus `sendIt` mit unserem Muster (Regression 0.0.2) |
| `ProfileServiceGameTests` | Strg-Rechtsklick-Ablauf: anlegen, hinzufügen, doppelt, entfernen, löschen, Reichweite; Bearbeiten aus dem Profilfenster inkl. Besitzer-Prüfung |
| `FactoryGameTests` | Factory Gauge: Mechanical-Crafting-Rezept zu den Gauge-Eingängen finden, Muster für einen Bereich (Crushing Wheel, Truhe), Bereich per Adresse |

Die Arena erzeugt `scripts/make-empty-structure.py` (Vanilla-Struktur-NBT, DataVersion 3955).
GameTests und Smoke-Test liegen im Haupt-Sourceset. Sie laufen nur im GameTest-Modus bzw. mit der Systemeigenschaft `bigcreateautocrafter.clientSmokeTest`, im normalen Spiel nicht.

## Gemessene Fakten aus den GameTests
- Bei `facing = NORTH` liefert Create die Inventare in dieser Reihenfolge: obere Reihe zuerst, innerhalb der Reihe von **Osten nach Westen**. `RegionScanner` bildet Creates Sortierung nach und prüft sie bei jedem Scan gegen Creates echte Reihenfolge.
- Creates Stock-Keeper-Screen schließt sich sofort, wenn neben dem Stock Ticker weder ein sitzender Händler noch ein Blaze Burner (mit Blaze) steht. Der Smoke-Test stellt deshalb einen Blaze Burner auf.
