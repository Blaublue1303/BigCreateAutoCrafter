# Entwicklungs- und Testumgebung

- JDK 21, Gradle Wrapper 9.2.1, NeoForge ModDevGradle `2.0.148`, NeoForge **21.1.250**, MC 1.21.1, Parchment 2024.11.17.
- Mod-ID `bigcreateautocrafter`, Gruppe `com.blaublue1303.bigcreateautocrafter`, Version `0.0.1`.
- Build: `./gradlew build` → `build/libs/bigcreateautocrafter-0.0.1.jar` (**in Phase 2 erfolgreich gebaut**, Mod enthält keine Gameplay-Logik).
- Referenz-JARs: `scripts/fetch-dev-jars.sh` lädt Create 6.0.10 (SHA-1 geprüft) und JEI nach `libs/` (gitignored). Einbindung in `build.gradle` (`compileOnly`/`localRuntime`) erfolgt in TODO-Schritt 1, da Create Flywheel/Ponder/Catnip als Transitive braucht.
- Start: `./gradlew runClient` / `runServer` / `runGameTestServer` (benötigt Display bzw. headless-Server; Client-Start in dieser Cloud-Umgebung nicht ausgeführt).
- Vollständige Testumgebung: Create 6.0.10 + Flywheel + Ponder + Catnip + JEI in `run/mods` bzw. via `localRuntime`; danach Pack-Mods schrittweise ergänzen (R7).
- CI: GitHub Actions `build.yml` (`./gradlew build`).
