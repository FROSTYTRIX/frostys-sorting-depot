# Frosty's Sorting Depot

**Mechanical item sorting that anyone can set up in five minutes. No power. No pipes. No fuss.**

Sorting Depot adds a lightweight, fully mechanical sorting network to Minecraft. Feed items into a
**Depot Controller** and they route themselves into the right inventories based on **Filter Cards** and
**Priority Stamps** — and it works with *any* storage block, vanilla or modded, that exposes the standard
NeoForge item capability (chests, barrels, shulker boxes, Create vaults, Mekanism crates, and more).

No energy systems. No conduits of its own. Just place, link, and sort.

## How it works

1. **Place a Depot Controller.** Items pushed into its top — by a hopper, dropper, pipe, or an inventory
   sitting directly above it — enter its buffer.
2. **Put a Linker Node** against a chest (or any inventory) and drop a **Filter Card** inside it.
3. **Configure the card** by right-clicking it to open its screen: *Item* mode accepts up to **5 exact
   items** (click them in your inventory to add them), *Tag* mode accepts up to **3 tags** ticked from
   those items' tags (e.g. `#minecraft:logs`), and *Overflow* mode catches everything.
4. **Wire it up** with the **Linker** tool: right-click the node, then right-click the Controller.
5. Done. Items flow to the first matching destination by **priority** (set with a **Priority Stamp**),
   and anything unmatched falls into the **Overflow Chest** — never lost, never voided.

## Features

- 🧭 **Depot Controller** — the hub. Routes ~4× a hopper's speed and pulls from the inventory above it.
- 🏷️ **Filter Cards** — Item (up to 5), Tag (up to 3), and Overflow modes, configured in a right-click GUI.
- 🔢 **Priority Stamps** — 1–5 priority per destination; higher fills first.
- 📦 **Overflow Chest** — a real double-able, animated chest that catches the unmatched.
- 🖥️ **Depot Terminal** — a dashboard showing every destination's filter and fill at a glance.
- 🔌 **Comparator output** — read how full the Controller's buffer is for redstone automation.
- ♻️ **Never voids items** — if nothing can take an item, it waits safely in the buffer.
- 🤝 **Cross-mod by design** — built entirely on the standard item-transfer capability.

## Requirements

- Minecraft 26.2 · NeoForge
- No dependencies.

> Version 1.0.0 — feedback and bug reports very welcome!
