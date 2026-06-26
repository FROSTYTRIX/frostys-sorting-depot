# Frosty's Sorting Depot

**Mechanical item sorting that anyone can set up in five minutes. No power. No pipes. No fuss.**

Sorting Depot adds a lightweight, fully mechanical sorting network to Minecraft. Feed items into a
**Depot Controller** and they route themselves into the right inventories based on **Filter Cards** and
**Priority Stamps** — and it works with *any* storage block, vanilla or modded, that exposes the standard
NeoForge item capability (chests, barrels, shulker boxes, Create vaults, Mekanism crates, and more).

No energy systems. No conduits of its own. Just place, link, and sort.

---

## Quick start

1. **Place a Depot Controller.** Items pushed into its top — by a hopper, dropper, pipe, or an
   inventory sitting directly above it — enter its buffer.
2. **Put a Linker Node** against a chest (or any inventory) and drop a **Filter Card** inside it.
3. **Right-click the Filter Card** to open its config screen and pick what it should match
   (up to 5 items, 3 tags, a whole mod, or wildcard).
4. **Wire it up** with the **Linker** tool: right-click the node, then right-click the Controller.
5. Done. Items flow to the highest-matching destination by **priority** (set with a **Priority
   Stamp**), and anything unmatched falls into the **Overflow Chest** — never lost, never voided.

For a full walkthrough see **[DOCS.md](DOCS.md)**.

---

## Features

| Block / item | What it does |
|---|---|
| 🧭 **Depot Controller** | The network hub. Routes ~4× a hopper's speed; pulls from the inventory above it; exposes its buffer as a comparator signal. |
| 🔗 **Linker Node** | A placed destination marker. Holds a Filter Card and a priority, and feeds the inventory it faces. |
| 🪪 **Filter Card** | Right-click to configure. Four modes: **Item** (exact, up to 5; optional strict NBT), **Mod** (everything from `create:`, etc.), **Tag** (up to 3, e.g. `#minecraft:logs`), **Any** (catch-all). |
| 🔢 **Priority Stamp** | Right-click a Linker Node to set its priority (1–5). Higher fills first; ties break by registration order — or in rotation with round-robin. |
| 📦 **Overflow Chest** | A real double-able, animated chest that catches whatever no other destination accepted. |
| 🖥️ **Depot Terminal** | A live dashboard showing every destination's target, filter, and fill at a glance. |
| 🛠️ **Linker tool** | The wand you use to register Linker Nodes into a Controller. Holds a beam overlay that highlights the selected node and can draw existing wires (toggle with **B**). |

### Highlights

- **Cross-mod by design** — built on the standard NeoForge item-handler capability, so it talks to
  anything that does.
- **Never voids items** — if no destination accepts an item, it waits in the Controller's buffer.
- **Round-robin distribution** — toggle in the Controller GUI to load-balance across
  equal-priority destinations.
- **Live Terminal** — fill levels refresh in real time while the dashboard is open.
- **Tunable** — a server-side config exposes transfer rate, batch size, and network size.

---

## Requirements

- **Minecraft 26.2** (NeoForge 26.2.0.x-beta), or one of the backport builds for
  **1.21.8 / 1.21.5 / 1.21.1**.
- **No required dependencies.** Optional integration with **JEI** (drag items straight from
  the recipe viewer into a Filter Card's ghost slots).

---

## Install

1. Install NeoForge for your Minecraft version.
2. Drop the matching `frostyssortingdepot-<mod-version>-<mc-version>.jar` into your `mods/`
   folder. (The jar filename includes the MC version — e.g. `…-1.1.0-26.2.jar` is the 26.2
   build; the version *inside* the jar is the same `1.1.0` everywhere.)
3. Launch the game. There's no further setup.

---

## Links

- **Documentation:** [DOCS.md](DOCS.md)
- **Changelog:** [CHANGELOG.md](CHANGELOG.md)
- **Modrinth page:** updated alongside each release
- **Source / issues:** this repository

---

## License

See [LICENSE](LICENSE) — defaults to "All rights reserved" unless changed by the author.
