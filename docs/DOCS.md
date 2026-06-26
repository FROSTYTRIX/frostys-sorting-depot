# Frosty's Sorting Depot — Documentation

This is the long-form companion to the [README](README.md). If you just want to start
sorting items, the **[Quick start](#quick-start)** section at the top of the README is
all you need. Read on for the full reference.

- [Quick start (5 minutes)](#quick-start-5-minutes)
- [Filter Cards](#filter-cards)
- [Depot Controller](#depot-controller)
- [Linker Node](#linker-node)
- [Linker tool & beam](#linker-tool--beam)
- [Depot Terminal](#depot-terminal)
- [Priority Stamp](#priority-stamp)
- [Overflow Chest](#overflow-chest)
- [Recipes](#recipes)
- [Configuration](#configuration)
- [Compatibility](#compatibility)
- [Troubleshooting / FAQ](#troubleshooting--faq)

---

## Quick start (5 minutes)

1. Craft a **Depot Controller** (Iron Block + Hopper + Comparator) and place it.
2. Craft a **Linker Node** (iron + redstone) and place it **facing a chest** (or barrel, or
   anything that holds items, vanilla or modded). The face *pointing toward* the chest is
   where items will go.
3. Craft a **Filter Card** (paper + iron nugget) and drop it in the Linker Node's GUI.
4. **Right-click the Filter Card** to open its config screen. Click items in your inventory
   to add them, or switch to **Mod / Tag** mode.
5. Craft a **Linker** (iron + redstone + stick). Right-click the Linker Node, then the
   Controller. The node is now registered.
6. Push items into the Controller's top face (hopper, dropper, or any inventory directly
   above it). They route automatically.

Optional but recommended:
- An **Overflow Chest** (Chest + Iron Ingot + Hopper) placed adjacent to the Controller
  catches anything no destination accepted.
- A **Depot Terminal** (Controller + Glass Pane + Redstone) placed adjacent to the
  Controller shows a live dashboard of every destination's fill.

---

## Filter Cards

Each Filter Card stores **one** filter. To configure it, hold the card and right-click in
the air — a dedicated screen opens. There are four modes:

| Mode | What it matches |
|---|---|
| **Item** | Any of the items you added (up to 5). Click an item in your inventory to add it; click a ghost slot to remove. By default it matches on item id alone; tick **Strict (match NBT)** to also compare components — useful for "this specific enchanted book" vs "all books". |
| **Mod** | Any item sharing a namespace with the items you added. Drop one Create item in to route everything from `create:`. |
| **Tag** | Up to 3 tags, ticked from the union of the tags on every item you added. Includes vanilla tags like `#minecraft:logs` and any modded tag the items belong to. |
| **Any** | The wildcard — accepts everything. Used implicitly by the Overflow Chest. |

The card's tooltip summarises its current configuration so you can identify cards at a
glance from your inventory.

### Why an item *also* needs to be added in Tag mode

Minecraft doesn't ship a built-in "browse every tag" picker, so Sorting Depot derives the
available tags from items you add to the card. Add an oak log, switch to Tag, and you can
tick `#minecraft:logs` / `#minecraft:oak_logs` / etc. Selected tags stay visible even
after you remove the source item, so you can build a tag-only filter and then clear the
ghost slots.

---

## Depot Controller

The hub. One per network.

- Push items into its **top face** — hoppers, droppers, pipes, an inventory directly above
  it (vanilla hopper behaviour), or any modded transport feeding into it.
- The Controller pulls one item per tick cycle (~4× a hopper) and routes one item per
  cycle into the highest-matching registered Linker Node.
- **Comparator output** on every face: the signal scales with how full the buffer is.
- **GUI:** right-click the Controller to see and retrieve the buffered item, plus a toggle
  between **First-match** and **Round-robin** distribution.
- **Never voids items** — anything no destination accepts (or anything that's full)
  stays buffered, never deleted.

### First-match vs Round-robin

The default is **First-match**: at any moment, the eligible destination with the
**highest priority** wins; ties break by registration order. Switch to **Round-robin** and
the Controller still respects priority (it only ever uses the top-priority tier that has
room), but among those equal-priority destinations it rotates through them in order — so
N furnaces all set to priority 3 get filled evenly instead of the first one filling first.

---

## Linker Node

A placeable destination marker. Each Linker Node:

- Holds **one** Filter Card (open the node's GUI to put one in).
- Has a **priority** (1–5; default 3). Apply with the Priority Stamp; shown in the GUI.
- **Faces** an inventory. The block on the face the node is pointing at is where items
  flow. (The default behaviour: when you place the node, it points at whatever you
  clicked on.)
- **Drops its card** when broken.

You can have as many Linker Nodes as you want pointing at the same chest — they're cheap
on purpose.

---

## Linker tool & beam

The Linker is the wand you use to register destinations. It's **handheld** (like a
sword) so it reads as a tool in your hand.

### Wiring nodes into a network

1. Right-click a **Linker Node** → the tool stores its position.
2. Right-click a **Depot Controller** → the stored node is registered into that
   Controller's network.

You can repeat freely; one node can only be registered to one Controller at a time.

### The beam overlay

While holding the Linker:

- The currently-selected node is **outlined in cyan** with a short vertical beam, so you
  can see what your next Controller click will register.
- Press **B** (rebindable in Controls → Gameplay → *Toggle Linker Wiring*) to turn on the
  **wiring overlay**: every nearby Controller draws a wire to each of its registered
  nodes. Useful to debug a big network; **off by default** so you don't get flash-banged.

Both can be tuned in **Mods → Frosty's Sorting Depot → Client Settings**:
- show / hide the beam
- colour (hex RGB or ARGB)
- line width (only takes effect on MC 26.2)
- outline width (only takes effect on MC 26.2)
- default state of the wiring overlay

---

## Depot Terminal

A read-only dashboard. Place it adjacent to a Controller, right-click to open.

You'll see, for each registered Linker Node:
- the **target** block name,
- the **filter** in human-readable form (`Items: oak_log, stone`, `Mods: create`,
  `Items (NBT): …` for strict cards, `Tags: #minecraft:logs`, or `Overflow`),
- the **fill percentage** of the target inventory.

Plus the overall **Overflow Chest** fill and the Controller's **buffer count**.

The dashboard **updates live** while the menu is open (~twice per second). Long rows wrap
to fit; if you have more destinations than fit on screen, scroll with the mouse wheel.

---

## Priority Stamp

A handheld stamp set to a value 1–5 (default 3).

- **Right-click in air** to cycle the value up (Shift to cycle down — both wrap).
- **Right-click a Linker Node** to apply the current value as that node's priority.

Higher priorities are tried first. Ties break by registration order (or rotate, in
round-robin mode).

---

## Overflow Chest

A normal-looking chest that's the network's catch-all. Anything no registered Linker Node
accepted lands here. If you don't place one, unmatched items just stay buffered in the
Controller.

- It's a **real chest**: double-able, animated lid, vanilla chest GUI. The renderer uses
  the mod's custom chest texture.
- It must be **adjacent** to its Controller (any of the six faces).
- A **double** Overflow Chest currently exposes only one half to routing — single chests
  recommended for now.

---

## Recipes

| Item | Recipe |
|---|---|
| **Linker** | Iron Ingot + Redstone + Stick (vertical, "I/R/S") |
| **Linker Node** | 3 × Iron Ingot top + Iron Ingot + Redstone + Iron Ingot middle |
| **Filter Card** | Paper + Iron Nugget (side by side) |
| **Priority Stamp** | Redstone Torch on top of Iron Ingot |
| **Depot Controller** | Hopper / Iron Block / Comparator (vertical) |
| **Depot Terminal** | Redstone / Glass Pane / Depot Controller (vertical) |
| **Overflow Chest** | Chest / Iron Ingot / Hopper (vertical) |

(Recipes are visible in JEI / EMI / your recipe viewer of choice.)

---

## Configuration

There are two config files, both editable from **Mods → Frosty's Sorting Depot** (or by
hand under `config/`).

### Client (`frostyssortingdepot-client.toml`)

Visual-only settings for the Linker beam overlay. See the **Linker tool & beam** section
above.

### Common (server-side, `frostyssortingdepot-common.toml`)

These are tuned by server admins. On a dedicated server, the server's values take effect.

| Key | Default | What it does |
|---|---|---|
| `transferCooldown` | `2` | Ticks between transfer cycles. Lower = faster (`2` is ~4× a hopper). Range 1–40. |
| `batchSize` | `1` | How many items the Controller can move per cycle. `1` matches hopper feel; raise for large packs. Range 1–64. |
| `maxNetworkSize` | `64` | Hard cap on the number of Linker Nodes a single Controller can have registered. Range 1–1024. |
| `validDestinationTag` | *(empty)* | Optional block tag. When set, the Controller will refuse to route into a block that isn't in this tag — useful in modpacks to keep items out of hoppers / pipes by accident. Leave empty to allow any item-handler. |

---

## Compatibility

- **Cross-mod by design.** Sorting Depot only ever speaks the standard NeoForge
  item-handler capability. Anything that exposes that capability is a valid destination.
  This includes vanilla chests/barrels/shulker boxes, Create vaults, Mekanism storage,
  Ironchests, Storage Drawers, AE2 interface side, etc.
- **No power**, **no pipes of our own**. We deliberately don't reimplement transport;
  hoppers, droppers, Create funnels, etc. are how you feed the Controller.
- **JEI** integration — if JEI is installed, you can drag items straight from the recipe
  viewer into a Filter Card's ghost slots.

---

## Troubleshooting / FAQ

**Items sit in the Controller and never leave.**
Either no Linker Node's filter accepts them (and you have no Overflow Chest), or every
matching destination is full. Open the Controller GUI to retrieve the buffered item, or
open the Terminal to see who's full. Items never void — they just wait.

**I broke a chest and the Linker Node is "still linked" but going nowhere.**
The node tracks the *space* it's pointing at, not the chest. Place a new inventory in
front of it and routing resumes. If you also broke the node, take the dropped Filter
Card with you.

**My Linker Node is feeding a hopper underneath the target chest.**
Set the **`validDestinationTag`** config option to a tag that only includes the storage
blocks you actually want to use — `#c:chests` is a reasonable default in many packs.

**The Terminal shows `(no inventory)` for a row.**
The Linker Node is registered but the block it's facing isn't (anymore) something with
an item capability. Either replace the block or break + re-place the node facing the
right thing.

**JEI drag doesn't work.**
Make sure you're on a recent JEI build for your MC version and that you're dragging into
one of the **five ghost slots** in the Filter Card config screen (not the inventory
slots below).

**Where do I report bugs / suggest features?**
Open an issue on the GitHub repository linked from the Modrinth page.
