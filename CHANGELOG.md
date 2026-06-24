# Changelog

All notable changes to Frosty's Sorting Depot are documented here.

## 1.0.1 — 2026-06-24

### Changed
- **Filter Cards are now configured in a dedicated GUI.** Right-click a Filter Card to open its
  configuration screen. The old "right-click the card onto an item" interaction is gone.
- **One card can now match up to 5 items _or_ up to 3 tags.** In the screen, click items in your
  inventory to add them (up to five), switch to **Tag** mode to tick up to three of those items' tags
  from a scrolling checklist, or pick **Overflow** to accept everything.
- **Filter Cards now stack to 1**, since each carries its own configuration.

### Removed
- The one-tag-at-a-time limitation on tag filters.

## 1.0.0 — 2026-06-23

First release: a complete, purely mechanical sorting network.

### Added
- **Depot Controller** — routes buffered items to registered destinations on a hopper-style cadence
  (~4× a hopper) and pulls items from an inventory placed directly above it. Exposes its buffer as a
  comparator signal and as an item capability on its top face.
- **Linker Node** — a placeable destination marker that holds a Filter Card and a priority and feeds the
  inventory it faces.
- **Linker** (tool) — right-click a Linker Node, then a Controller, to register it into the network.
- **Filter Cards** — three modes, all configurable in-world by right-clicking the card onto an item:
  - *Item* — match an exact item.
  - *Tag* — match a tag; click an item to cycle through its tags.
  - *Overflow* — accept anything.
- **Priority Stamps** — set a destination's priority (1–5); right-click a Linker Node to apply.
- **Overflow Chest** — an entity-rendered chest (double-chest joining + lid animation) that catches any
  item no destination accepted.
- **Depot Terminal** — a read-only dashboard listing each destination's target, filter, and fill level,
  plus overflow fill and buffer count; scrolls for large networks.
- **Advancements** — a "Sorting Depot" progression tree.
- Recipes, loot tables (blocks drop their contents), and an in-game config screen.

### Notes
- Items are **never voided**: anything with no available destination waits safely in the Controller's buffer.
- Works with any storage that exposes the standard NeoForge item capability — vanilla and modded alike.
- Built on the 26.2 transfer API (`ResourceHandler<ItemResource>`).

### Known limitations
- A *double* Overflow Chest currently exposes one half to routing (single chests are unaffected).
- Tag filters bind one tag at a time.
