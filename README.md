# Big Create Auto Crafter

NeoForge-Mod (MC 1.21.1, NeoForge 21.1.250) für das Modpack **Create Exta+ 2.0**: erweitert den Create-Stock-Keeper-Crafting-Ablauf auf größere Mechanical-Crafter-Anlagen.

**Status: Gerüst steht.** Abhängigkeiten (Create 6.0.10, JEI), Datenmodell für Profile/Bereiche, Speicherung, Netzwerk-Sync, Config, Tastenbelegung, Create-Zugriff per Mixin-Accessor und GameTests sind fertig. Die eigentliche Crafting-Funktion folgt nach `docs/06-todo.md`. Im Spiel ändert die Mod noch nichts am Stock-Keeper-Ablauf.

- Analyse: `docs/01-phase1-analysis.md`, `docs/02-phase2-flow-analysis.md`
- Plan, Risiken, Umgebung, TODO: `docs/03…06`
- Build: `./gradlew build` (JDK 21)
- Tests: `./gradlew runGameTestServer` (headless, mit Create + JEI)
- Details zur Umgebung: `docs/05-dev-environment.md`

Die Mojang-Mappings (Parchment) unterliegen eigenen Lizenzen: https://github.com/NeoForged/NeoForm/blob/main/Mojang.md
