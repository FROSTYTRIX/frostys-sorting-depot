# ROADMAP.md — Sorting Depot

## Vision

A lightweight, purely mechanical item-sorting mod that any player can set up in five minutes. No power systems, no pipe networks of its own — just a Controller, some Linkers, and Filter Cards. Works transparently with vanilla storage and any mod that uses standard NeoForge inventory capabilities.

---

## Phase 0 — Project scaffold

**Goal:** Compiling, loading mod in-game with no content yet.

- [ ] Generate NeoForge mod project (MDK or ModDevGradle)
- [ ] Set mod ID `sortingdepot`, package root `net.frostytrix.sortingdepot`
- [ ] Configure `neoforge.mods.toml` (name, description, authors, license)
- [ ] Create `SortingDepot.java` entrypoint with `@Mod` annotation
- [ ] Create empty registry classes: `SDBlocks`, `SDItems`, `SDBlockEntities`
- [ ] Confirm mod loads in-game with no errors

**Exit condition:** `/mod list` shows Sorting Depot, no crash on startup.

---

## Phase 1 — Core data model

**Goal:** The pure logic layer exists and is unit-testable without Minecraft.

### 1.1 Routing abstractions
- [ ] Create `RoutableItem` record: wraps item identifier + count + optional component snapshot
- [ ] Create `FilterMode` sealed interface with `ItemFilter`, `TagFilter`, `OverflowFilter` variants
- [ ] Create `FilterMatcher.java` — pure logic, no Minecraft imports
  - `matches(RoutableItem item, FilterMode filter) → boolean`
  - Item mode: exact item ID match, optional strict component comparison
  - Tag mode: checks item against a set of tag keys (passed as strings, resolved externally)
  - Overflow mode: always returns true
- [ ] Create `RoutingEngine.java` — pure logic, no Minecraft imports
  - Input: `RoutableItem`, ordered list of `(FilterMode, priority, slotAvailable)` candidates
  - Output: index of winning candidate, or -1 if no match
  - Tie-breaking: higher priority wins; same priority → insertion order

### 1.2 Unit tests
- [ ] `FilterMatcherTest`: item match, tag match, overflow match, no-match cases
- [ ] `RoutingEngineTest`: single candidate, priority ordering, full-slot fallback, no candidates

**Exit condition:** All unit tests pass with no Minecraft on the classpath.

---

## Phase 2 — Items

**Goal:** Filter Cards and the Priority Stamp exist as craftable items.

### 2.1 Filter Card item
- [ ] Register `FilterCardItem` in `SDItems`
- [ ] Add `DataComponent` to store `FilterMode` (item, tag, overflow) and filter payload
- [ ] Default mode on craft: `ItemFilter` with empty target (unconfigured)
- [ ] Right-click on an item in inventory GUI: sets the card's target item (TBD — can be a held-item interaction or a small GUI)
- [ ] Item tooltip shows current filter mode and target

### 2.2 Priority Stamp item
- [ ] Register `PriorityStampItem` in `SDItems`
- [ ] Stores priority value 1–5 in a `DataComponent`
- [ ] Tooltip shows current priority value
- [ ] Right-clicking while sneaking cycles priority down; right-clicking normally cycles up

### 2.3 Linker item
- [ ] Register `LinkerItem` in `SDItems`
- [ ] `USE_ON` handler: stores the right-clicked block's `BlockPos` in a `DataComponent`
- [ ] Tooltip shows "Linked to: [x, y, z]" or "Unlinked" if no target set
- [ ] Works in off-hand

### 2.4 Recipes
- [ ] `SDRecipeProvider` with shaped recipes for all items (see CLAUDE.md for target costs)
- [ ] Item models + textures (placeholder textures acceptable for Phase 2)
- [ ] Lang entries in `en_us.json`

**Exit condition:** All items craftable in-game, tooltips display correctly, LinkerItem stores a BlockPos on right-click.

---

## Phase 3 — Blocks and block entities

**Goal:** Depot Controller and Linker blocks exist and can be placed.

### 3.1 Linker block + block entity
- [ ] Register `LinkerBlock` and `LinkerBlockEntity` 
- [ ] `LinkerBlockEntity` stores:
  - Controller `BlockPos` (set when registered)
  - Optional `FilterCard` snapshot
  - Priority (int, default 3)
- [ ] `LinkerBlock` has a slot GUI (single slot for Filter Card insertion)
- [ ] Applying a Priority Stamp to a placed Linker sets its priority
- [ ] Linker re-resolves its Controller on world load via `level.getBlockEntity(controllerPos)`
- [ ] Block state: `facing` (4 directions), `linked` (boolean) — drives texture variant

### 3.2 Depot Controller block + block entity
- [ ] Register `DepotControllerBlock` and `DepotControllerBlockEntity`
- [ ] `DepotControllerBlockEntity` holds:
  - Input `ItemStackHandler` (1 slot) — exposed as `IItemHandler` on the top face
  - Ordered list of registered Linker `BlockPos` entries
  - Output `IItemHandler` reference cache (invalidated on chunk unload)
- [ ] `LinkerItem` used on the Controller registers the previously stored `BlockPos` target into the Controller's list
- [ ] `serverTick`: if input slot is non-empty, run routing; otherwise exit immediately
- [ ] Routing tick:
  1. Take one item from input slot
  2. Query each registered Linker (sorted by priority) for its `FilterMode`
  3. Call `FilterMatcher.matches()` for each candidate
  4. Push item into first matching available inventory using `IItemHandler.insertItem`
  5. If no match or all full → send to Overflow Chest if registered, else return item to input slot

### 3.3 Overflow Chest block + block entity
- [ ] Register `OverflowChestBlock` and `OverflowChestBlockEntity`
- [ ] Standard `ItemStackHandler` inventory (27 slots)
- [ ] When placed adjacent to or linked to a Controller, auto-registers as the overflow target
- [ ] Exposes `IItemHandler` on all faces for pipe extraction

### 3.4 Cross-mod inventory support
- [ ] Use `level.getCapability(Capabilities.ItemHandler.BLOCK, pos, facing)` to obtain `IItemHandler` from any target inventory
- [ ] Never cast to `Container` or `WorldlyContainer`
- [ ] Graceful handling when capability is absent (skip that Linker target silently)

**Exit condition:** Items inserted into the Controller's top slot (by hopper) route into a linked chest with a matching Item Filter Card.

---

## Phase 4 — Tag filters and priority routing

**Goal:** Tag-based filtering and priority stamp ordering work end-to-end.

- [ ] `TagFilter` mode in `FilterCardItem` stores a `ResourceLocation` tag key
- [ ] At routing time, resolve the tag key against `BuiltInRegistries.ITEM.getTag(tagKey)` and check membership
- [ ] Priority Stamp interaction with `LinkerBlockEntity` updates priority value
- [ ] Routing engine sorts candidate list by priority (descending) before evaluation
- [ ] Integration test: two chests, same item type, different priorities — higher priority fills first
- [ ] Integration test: tag filter catches all log variants (oak, birch, spruce, etc.)

**Exit condition:** Tag filters work correctly; priority ordering is deterministic.

---

## Phase 5 — Overflow filter and full fallback chain

**Goal:** The entire fallback routing chain is complete and item-safe.

- [ ] `OverflowFilter` mode on a Filter Card makes that Linker's chest accept anything
- [ ] Fallback order: exact match → tag match → overflow filter → Overflow Chest block → queue in Controller
- [ ] Stress test: fill all target chests, confirm no item is ever voided
- [ ] Stress test: rapid hopper minecart insertion — no duplication, no crash
- [ ] Linker registered but its target block removed: Controller skips silently, logs a debug warning

**Exit condition:** No item is ever lost or duplicated under any routing failure condition.

---

## Phase 6 — Depot Terminal (optional QoL)

**Goal:** Players can inspect and manage their network from a single GUI.

- [ ] Register `DepotTerminalBlock` and `DepotTerminalMenu` / `DepotTerminalScreen`
- [ ] Terminal placed adjacent to a Controller auto-connects
- [ ] Screen shows:
  - List of all registered Linkers with their target block name, position, fill %, and active filter
  - Overflow Chest fill %
  - Input queue count
- [ ] Players can reassign a Filter Card from the Terminal GUI (drag and drop to a Linker row)
- [ ] Fill % updated every 20 ticks (1 second) via a server-to-client packet
- [ ] Client-side only: `DepotTerminalScreen` has no routing logic

**Exit condition:** Terminal correctly displays network state and allows filter reassignment without breaking routing.

---

## Phase 7 — Polish and release prep

**Goal:** Mod is ready for a public alpha on Modrinth.

### Assets
- [ ] Final block textures (Controller, Linker, Overflow Chest, Terminal)
- [ ] Final item textures (Filter Cards with distinct look per mode, Priority Stamp, Linker item)
- [ ] Block state JSON + item model JSON for all blocks
- [ ] Sounds: soft click on item routed, error buzz when queue full

### Data generation
- [ ] `SDBlockStateProvider`: all block states and models
- [ ] `SDItemModelProvider`: all item models
- [ ] `SDRecipeProvider`: all shaped recipes
- [ ] Run datagen and verify output before shipping

### Documentation
- [ ] In-game advancement tree: "Sorted Out" (place Controller → link first chest → route first item → use Tag filter → fill Overflow Chest)
- [ ] JEI/REI recipe integration (if those mods are present)
- [ ] Modrinth page description, screenshots, feature list

### Final QA
- [ ] Run full testing checklist from CLAUDE.md
- [ ] Test in a modpack environment with Create and Mekanism present
- [ ] Confirm no `IItemHandler` capability warnings in logs
- [ ] Confirm no item loss under chunk boundary edge cases

**Exit condition:** Public alpha released on Modrinth under `net.frostytrix.sortingdepot`.

---

## Future ideas (post-release)

- **Remote Linker** — links to an inventory in a different dimension or far away via an Ender Pearl crafting component
- **Comparator output** on Controller — emits signal strength based on input queue length
- **Filter Card duplication** — copy a configured card in a crafting grid (paper + existing card)
- **Import mode** — reverse routing: pull items from a chest into the Controller for redistribution
- **Statistics screen** — items sorted per category over time, shown in the Terminal
