# Contributing

Thanks for your interest in H&H Armors!

## Reporting bugs & suggesting features

Please use the [issue templates](https://github.com/Henny263462/HHA/issues/new/choose). For bugs, always include your mod/loader versions and the relevant log excerpt.

## Development setup

- JDK 21, Fabric Loom (fetched by Gradle automatically)
- Build: `./gradlew build` — the remapped jar lands in `build/libs/`
- Run a dev client: `./gradlew runClient`

## Code layout

- `logic/` — server-side ability logic (one object per feature)
- `item/`, `entity/` — custom items and the thrown mace entity
- `client/` — HUD, particles, renderers (client entrypoint only)
- `config/` — live config (`/hha`), custom recipe datapack, commands
- Assets follow vanilla conventions; animated item textures are 16×N sprite sheets with `.mcmeta`

## Ground rules

- Kotlin for logic, Java only where type inference requires it (mixins/renderers)
- Every gameplay feature needs a config toggle in `HhaConfig`
- **Do not touch assets listed in `HEAVEN_ARMOR_LOCK.md`** — their look is frozen by the maintainer
- Keep vanilla tag files additive (`"replace": false`)

## Pull requests

Small, focused PRs are easiest to review. Make sure `./gradlew build` passes; the CI workflow builds every PR.
