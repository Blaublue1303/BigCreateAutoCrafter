# Big Create Auto Crafter

NeoForge-Mod (MC 1.21.1, NeoForge 21.1.250) für das Modpack **Create Exta+ 2.0**: erweitert den Create-Stock-Keeper-Crafting-Ablauf auf größere Mechanical-Crafter-Anlagen.

**Status: Version 0.1.2.** Strg + Rechtsklick registriert Crafter-Anlagen mit mehreren Bereichen. Im Stock Keeper wählst du die Anlage über einen Anhänger am Fenster und verwaltest Profile in einem eigenen Fenster (umbenennen, löschen, Adressen), beides im Stil von Creates Stock Keeper. JEIs „+“ und Factory Gauges craften damit auch große Rezepte, z. B. das Crushing Wheel mit 21 Zutaten. Anleitung und Grenzen stehen in `docs/07-prototyp-1.md`.

- Analyse: `docs/01-phase1-analysis.md`, `docs/02-phase2-flow-analysis.md`
- Plan, Risiken, Umgebung, TODO: `docs/03…06`
- Bedienung, Test im Spiel, Grenzen: `docs/07-prototyp-1.md`
- Build: `./gradlew build` (JDK 21)
- Tests: `./gradlew runGameTestServer` (20 GameTests, headless) und `xvfb-run ./gradlew runClientSmokeTest` (Client)
- Details zur Umgebung: `docs/05-dev-environment.md`

Die Mojang-Mappings (Parchment) unterliegen eigenen Lizenzen: https://github.com/NeoForged/NeoForm/blob/main/Mojang.md
