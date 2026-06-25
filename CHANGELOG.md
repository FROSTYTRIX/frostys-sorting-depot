# Changelog

All notable changes to Frosty's Sorting Depot are documented here.

## 1.1.0 — 2026-06-25

### Added
- **Filter Card: Mod mode.** A fourth matching mode that catches any item sharing a namespace with the
  items you added to the card — e.g. drop one Create item in to route everything from `create:`.
- **Filter Card: strict NBT matching.** A per-card checkbox in Item mode that also compares item
  components, so you can route, say, a specific enchanted book without matching plain books. Cards now
  store full item stacks (not just ids) to keep the components around.
- **Depot Controller: round-robin distribution.** A toggle button in the Controller GUI that spreads
  items across equal-priority destinations in rotation instead of always filling the first. Priority
  ordering is still respected — only ties are load-balanced. Persists per Controller.
- **Live Depot Terminal.** The dashboard now refreshes ~twice a second while the menu is open instead of
  being a one-shot snapshot. Open the Terminal once, watch fill levels move in real time.
- **Linker beam overlay.** While holding a Linker, the currently-selected node is outlined in cyan with
  a short vertical beam — much easier to see what your next Controller click will register. Optional:
  toggle the **Controller → linked-node wires** with the new **Toggle Linker Wiring** keybind (default
  **B**, rebindable, off by default so you don't get flash-banged with many nodes).
- **Client config screen for the beam.** Mods → Config exposes the beam toggle, colour (hex RGB/ARGB),
  line width, outline width, and the wiring default.

### Changed
- The Filter Card configuration screen now has four mode buttons (Item / Mod / Tag / Any), a strict
  checkbox in Item mode, and a Mod-namespaces line in Mod mode.
- The Depot Terminal's "filter" column now shows `Items (NBT)` for strict cards and lists mod ids for
  Mod-mode cards.

### Notes
- Routing-engine unit tests grew from 21 to 29 (added Mod / strict / round-robin coverage).
- **Per-version caveat:** the `linkerBeam.lineWidth` and `linkerBeam.outlineWidth` config values only
  take effect on the 26.2 build. The 1.21.x builds use the classic immediate-mode line renderer, which
  has a fixed line width — the colour, the on/off, the wiring toggle, and the keybind all work there.

## 1.0.2 — 2026-06-24

### Fixed
- **Depot Terminal no longer overflows its panel.** Long rows (long block names, or filters with many
  items/tags) now wrap onto extra lines instead of running off the edge, with the fill % kept at the end.

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
