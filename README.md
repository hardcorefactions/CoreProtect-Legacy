# CoreProtect-Legacy 2.15.2 — buildable Gradle project

A Gradle build around the decompiled sources of `CoreProtect-Legacy-2.15.2.jar`.

```
./gradlew build          # -> build/libs/CoreProtect-Legacy-2.15.2.jar
```

Requires network access on the first run (Spigot and EngineHub repositories).
Any JDK from 8 up works; the build pins `--release 8` so the output runs on a
Java 8 server regardless of the JDK used to compile it.

CI builds on every push and pull request via `.github/workflows/build.yml`, and
attaches the jar to the workflow run as an artifact named `CoreProtect-Legacy`
(Actions → the run → Artifacts). It can also be triggered by hand from the
Actions tab.

## Dependencies

All dependencies are `compileOnly` — the server provides them at runtime.

| Artifact | Version | Why |
| --- | --- | --- |
| `org.spigotmc:spigot-api` | `1.12.2-R0.1-SNAPSHOT` | Last release with the legacy `Material` / data-value API the plugin targets |
| `com.sk89q.worldedit:worldedit-bukkit` | `6.1.5` | `WorldEditPlugin`, `BukkitWorld` |
| `com.sk89q.worldedit:worldedit-core` | `6.1` | `Vector`, `BaseBlock`, `Actor`, `AbstractLoggingExtent` (last 6.x release of this artifact) |
| gson, json-simple, commons-lang, guava, snakeyaml | — | Shipped inside the Spigot server jar |

`spigot-api` is resolved with `transitive = false`: its `net.md-5:bungeecord-chat`
dependency points at a `bungeecord-parent` POM that is no longer published, which
breaks resolution. The libraries it would have pulled in are declared explicitly
instead.

SQLite and MySQL JDBC drivers are loaded reflectively (`Class.forName`) and are
never on the compile classpath.

## Fixes applied to the decompiled sources

The decompiler output did not compile as-is. The changes below are the only
edits made to it, and all of them restore syntax the decompiler mangled — no
behaviour was changed.

1. **Invalid array-type imports.** `import [Ljava.lang.String;;` and
   `import [Lorg.bukkit.inventory.ItemStack;;` are not Java. Removed
   (`CommandHandler`, `Functions`).

2. **Mangled array clones.** `(String[])((String;)args_input).clone()` →
   `args_input.clone()` — 17 sites in `CommandHandler`, 1 in `Functions`.

3. **Raw casts in enhanced-for.** `for (BlockState b : (List) map.get(id))`
   dropped the type argument and no longer type-checks; the redundant cast was
   removed (`consumer/Process.java`, 2 sites).

4. **`short` → `Integer`.** `EntityType.getTypeId()` returns `short`, which does
   not box to `Integer`; added an explicit `(int)` cast
   (`patch/script/__2_11_0.java`).

5. **`final` locals that are reassigned.** The decompiler collapsed each
   "mutable local + final copy captured by a local class" pair into a single
   `final` variable that it then assigned to. Each was split back apart: the
   mutable variable keeps its computation under a `…0` name, and a `final` alias
   under the original name is declared immediately before the local class that
   captures it, so the captured value is unchanged. Affected:
   `LookupCommand` (`type`, `p`, `pa`, `re`, `page`, `x`, `y`, `z`, `wid`, and
   the `arg_*` / `ts` set), `RollbackRestoreCommand` (`lo`, `arg_wid`,
   `preview`), `PurgeCommand` (`optimizeCheck`), `PlayerListener`
   (`check_block`, `interact_block`), and `database/Lookup` (`user_string`,
   where the original's own `final_user_string` copy was still present and is
   now used inside the anonymous `Runnable`).

`plugin.yml` is the only resource; its `version` is templated from the Gradle
project version.

## Verification

The rebuilt jar has the same 80 entries as the original — every class, including
every anonymous and local class, matches by name. Total bytecode is within 0.16%
of the original (582,396 vs 583,320 bytes), the spread being normal
`LineNumberTable` / `StackMapTable` variation between javac versions.

This is structural equivalence, not behavioural: the plugin has not been run on a
live Spigot server as part of this work.

## Credits

CoreProtect is written by **Intelli**. This repository contains none of that
original work as authored — only decompiler output derived from a compiled
release, with the compile errors in that output repaired. All plugin design and
implementation credit belongs upstream.

The Gradle build, the dependency resolution, and the source repairs listed above
were done by **Claude (Opus 5)** via [Claude Code](https://claude.com/claude-code).

## Licensing

Check CoreProtect's license before relying on or redistributing this repository.
Decompiled sources are a derivative work of the compiled jar, and republishing
them may not be permitted regardless of how the build around them is licensed.
