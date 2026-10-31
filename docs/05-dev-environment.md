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
- `./gradlew build`: kompiliert gegen Create und JEI und baut `build/libs/bigcreateautocrafter-0.0.1.jar`.
- `./gradlew runGameTestServer`: startet einen headless Server mit Create, Ponder, Flywheel, Registrate, JEI und der Mod, führt alle GameTests aus und beendet sich mit dem Ergebnis als Exit-Code. **Läuft lokal und in der CI.**
- `./gradlew runClient` / `runServer`: manuelles Testen. Der Client braucht ein Display und wurde in der Cloud-Umgebung nicht gestartet. `runServer` verlangt `run/eula.txt`.

## GameTests (`src/main/java/.../gametest`, Arena `data/bigcreateautocrafter/structure/empty7.nbt`)
| Test | prüft |
|---|---|
| `singleCrafterIsItsOwnGroup` | Mixin-Accessor und `CreateCompat.findConnectedGroup` für einen einzelnen Crafter bzw. Luft |
| `readsConnectedGroupInCreateOrder` | 2×2-Gruppe, verbunden über Creates `ConnectedInputHandler.toggleConnection`, gleiche Gruppe von jedem Mitglied aus, obere Reihe zuerst. Loggt die Reihenfolge als Daten für T5 |
| `profileSurvivesNbtAndNetwork` | `CrafterProfile`-Codec (Speichern) und StreamCodec (Netzwerk), 21er-Bereich mit 5×5-Rahmen |
| `invalidRegionsAreRejected` | Validierung der Bereichs-Layouts |

Die Arena erzeugt `scripts/make-empty-structure.py` (Vanilla-Struktur-NBT, DataVersion 3955).

GameTests liegen im Haupt-Sourceset. NeoForge lädt sie nur auf einem GameTest-Server bzw. bei aktiviertem GameTest-Modus, im normalen Spiel nicht.

## Gemessene Fakten aus den GameTests
- Bei `facing = NORTH` liefert Create die Inventare in dieser Reihenfolge: obere Reihe zuerst, innerhalb der Reihe von **Osten nach Westen** (absolute Koordinaten). Ob das vom Spieler aus gesehen links→rechts ist, hängt davon ab, welche Seite die Vorderseite des Crafters ist. Das wird im Client geprüft (T5).
