# CLAUDE.md — Sorting Depot

## Project identity

- **Mod name:** Frosty's Sorting Depot
- **Mod ID:** `frostyssortingdepot`
- **Package root:** `net.frostytrix.sortingdepot`
- **Loader:** NeoForge (latest stable)
- **Minecraft version:** 26.2 (NeoForge `26.2.0.6-beta`)
- **Java version:** 25 (Mojang ships Java 25 to end users in 26.2; the Gradle toolchain targets 25)

## What this mod does

Sorting Depot adds a purely mechanical item-sorting network to Minecraft. Players route items through a **Depot Controller**, which pushes them into registered inventories based on **Filter Cards** and optional **Priority Stamps**. No power, no pipes of its own, no energy systems. Works with any mod that exposes the standard NeoForge `IItemHandler` capability (Create, Mekanism, vanilla chests/barrels/shulker boxes, etc.).

## Repository layout

```
sortingdepot/
├── src/main/java/net/frostytrix/sortingdepot/
│   ├── FrostysSortingDepot.java       # Mod entrypoint (@Mod)
│   ├── FrostysSortingDepotClient.java # Client-only entrypoint (@Mod dist=CLIENT)
│   ├── Config.java                    # ModConfigSpec
│   ├── registry/
│   │   ├── SDBlocks.java              # DeferredRegister for blocks
│   │   ├── SDItems.java               # DeferredRegister for items
│   │   └── SDBlockEntities.java       # DeferredRegister for block entities
│   ├── block/
│   │   ├── DepotControllerBlock.java
│   │   ├── LinkerBlock.java
│   │   ├── OverflowChestBlock.java
│   │   └── DepotTerminalBlock.java
│   ├── blockentity/
│   │   ├── DepotControllerBlockEntity.java   # Core routing logic lives here
│   │   ├── LinkerBlockEntity.java            # Holds FilterCard + Priority
│   │   └── OverflowChestBlockEntity.java
│   ├── item/
│   │   ├── FilterCardItem.java        # Item / Tag / Overflow modes
│   │   ├── PriorityStampItem.java
│   │   └── LinkerItem.java            # Right-click tool to register inventories
│   ├── network/
│   │   └── DepotNetwork.java          # Logical grouping of Controller + Linkers
│   ├── routing/
│   │   ├── RoutingEngine.java         # Pure routing logic — no Minecraft imports
│   │   └── FilterMatcher.java         # Item/tag/overflow matching — no Minecraft imports
│   ├── gui/
│   │   ├── DepotTerminalScreen.java
│   │   └── DepotTerminalMenu.java
│   └── datagen/
│       ├── SDRecipeProvider.java
│       ├── SDBlockStateProvider.java
│       └── SDItemModelProvider.java
├── src/main/resources/
│   ├── META-INF/neoforge.mods.toml
│   └── assets/frostyssortingdepot/
│       ├── lang/en_us.json
│       ├── models/
│       └── textures/
└── ROADMAP.md
```

## Architecture rules

### Routing package is Minecraft-free
`net.frostytrix.sortingdepot.routing` must have **zero Minecraft imports**. `RoutingEngine` and `FilterMatcher` operate on abstract item representations (e.g. a simple `RoutableItem` record wrapping an `ItemStack` snapshot) so they can be unit-tested without launching a game instance. Block entities adapt between Minecraft types and these abstractions.

### IItemHandler everywhere
Never access inventories by casting to `Container` or `WorldlyContainer`. Always obtain the `IItemHandler` capability from the target `BlockEntity`. This is what makes cross-mod compatibility automatic.

### One Controller per logical network
A `DepotNetwork` is identified by the position of its `DepotControllerBlockEntity`. Linkers store the Controller's `BlockPos` when registered. On world load, Linkers re-resolve their Controller via `level.getBlockEntity(pos)` — no UUIDs, no persistent network IDs.

### No energy, no ticks on idle
The Controller's `serverTick` only runs when its input buffer is non-empty. If there's nothing to route, exit immediately. Do not implement any power interface (`IEnergyStorage`, etc.).

### Filter Card modes
`FilterCardItem` has three modes stored in its `DataComponents`:
- `ITEM` — exact item match (optionally strict NBT/component matching via a boolean flag)
- `TAG` — `TagKey<Item>` match, resolves at runtime against the item registry
- `OVERFLOW` — wildcard, matches anything

### Priority
Priority is an integer 1–5 stored in the `LinkerBlockEntity`. Default is 3. Higher value = tried first. When two Linkers share the same priority and both accept an item, the one registered first (by insertion order in the Controller's list) wins.

## Key implementation notes

- Use `ItemHandlerHelper.insertItemStacked` (or the NeoForge equivalent) when pushing items into target inventories — never assume the whole stack will fit.
- The Controller's input slot is a single `ItemStackHandler` slot. Hoppers, pipes, and droppers insert here naturally.
- The `LinkerItem` right-click handler must work in both `USE_ON` and off-hand. Store the target `BlockPos` + `BlockEntity` class name in the held `LinkerItem`'s data components so the Controller can validate the target is still a valid inventory on tick.
- `DepotTerminalScreen` is client-side only. All actual routing state lives server-side in `DepotControllerBlockEntity`.
- Use NeoForge's `RegisterCapabilitiesEvent` to expose `IItemHandler` on the Controller's input face.

## Crafting recipes (target costs)

| Item | Ingredients | Notes |
|---|---|---|
| Linker | Iron Ingot + Redstone + Stick | Cheap — players need many |
| Filter Card (Item) | Paper + Iron Nugget | Very cheap |
| Filter Card (Tag) | Paper + Gold Nugget | Slightly rarer |
| Filter Card (Overflow) | Paper only | Trivial |
| Priority Stamp | Iron Ingot + Redstone Torch | One per chest max |
| Depot Controller | Iron Block + Hopper + Comparator | Mid-game cost |
| Depot Terminal | Controller + Glass Pane + Redstone | Optional QoL |
| Overflow Chest | Chest + Hopper + Iron Ingot | One per network |

## Testing checklist (run before any release)

- [ ] Vanilla chest: item routes correctly
- [ ] Barrel: item routes correctly
- [ ] Shulker box: item routes correctly
- [ ] Create vault (if Create is present): item routes correctly
- [ ] Tag filter catches all variants (e.g. all log types across mods)
- [ ] Priority ordering: higher priority chest fills first
- [ ] Full chest fallback: routes to next valid chest, never voids
- [ ] Overflow Chest catches unmatched items
- [ ] Controller with no Overflow Chest queues items safely (no void, no crash)
- [ ] Linker survives chunk unload/reload (BlockPos re-resolution works)
- [ ] DepotTerminalScreen shows correct fill levels
- [ ] No item duplication under rapid input (stress test with hopper minecart)
- [ ] `RoutingEngine` unit tests pass without Minecraft on classpath

## Code style

- Standard Java 21 style, no Lombok
- `record` types for immutable data (filter snapshots, routing decisions)
- `sealed interface` for filter card mode variants if it aids exhaustive matching
- All registry objects in `SDBlocks`, `SDItems`, `SDBlockEntities` — never registered ad hoc
- Lang keys follow `block.frostyssortingdepot.<name>` / `item.frostyssortingdepot.<name>`

## What NOT to do

- Do not add your own pipe/conduit blocks — integrate with existing transport
- Do not add a power system of any kind
- Do not use `Container`/`WorldlyContainer` for cross-mod compatibility
- Do not void items under any routing failure condition
- Do not add decorative blocks or world generation
- Do not register the `RoutingEngine` or `FilterMatcher` with any Minecraft event system — they are pure logic
