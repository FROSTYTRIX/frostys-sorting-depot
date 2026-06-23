# Art Brief — Sorting Depot

Texture specs for the mod's items. Drop finished PNGs into
`src/main/resources/assets/frostyssortingdepot/textures/item/<name>.png`.
The model JSONs already point at these paths, so the items render as soon as the PNGs exist.

## Shared style rules

- **Size:** 16×16, 32-bit RGBA, transparent background.
- **Shading:** vanilla conventions — 1px near-black outline (`#33312f`), light source top-left,
  2–3 shades per material.
- **Signature accent:** a sparing **cyan/teal** (`#4fd0d6`, shadow `#2f9aa0`) on each item — a stripe,
  node, or glint — so the three read as one family. This is the mod's "Frosty" identity color.
- **Material palette:**
  - Iron: hi `#d8dde3`, mid `#aeb6bf`, shadow `#6e7681`
  - Redstone: glow `#ff5a4d`, mid `#c91e1e`, shadow `#7d1212`
  - Wood: hi `#6b4f2a`, shadow `#4a3519`
  - Paper: hi `#ece7d6`, shadow `#c8c1aa`

---

## 1. Filter Card — `filter_card.png`

**Reads as:** a punched index/luggage card used to filter items. Crafted from paper + a metal nugget.

- Portrait paper card filling most of the frame, slight tilt optional.
- A **punched hole** near the top (transparent dot) so it reads as a tag, not a sheet of paper.
- An **iron clip/corner** on one edge (the nugget reference).
- Center: a small neutral **filter glyph** — a downward funnel or three stacked sort-bars — kept simple.
- The **cyan accent** as a thin stripe down one side or a colored hole-grommet.
- One texture covers all three modes (Item/Tag/Overflow); mode is shown in the tooltip. *(Optional later:
  per-mode tinted variants — we can add texture overrides if you want distinct looks.)*

## 2. Priority Stamp — `priority_stamp.png`

**Reads as:** an upright office-style rubber stamp you press down to imprint a 1–5 priority. Crafted from
an iron ingot + a redstone torch. In-world it's right-clicked onto a Linker Node to set its priority, and
held normally (flat item, no handheld).

Layout (upright, occupying the center column of the 16×16):
- **Top — handle knob:** a rounded iron knob/cap at rows ~2–4, ~6px wide, centered. Iron shading
  (hi `#d8dde3` top-left, shadow `#6e7681` bottom-right). This is what the hand "presses."
- **Middle — shaft:** a narrow iron post (~3–4px wide) from the knob down to the base, rows ~5–10.
- **Bottom — stamp base/foot:** a wider iron block, ~10px across, rows ~11–14, sitting flat (the part that
  stamps). Give it a 1px dark underline so it reads as resting on a surface.
- **Redstone detail:** a single **glowing redstone-red gem** (`#ff5a4d` core, `#c91e1e` edge) embedded
  where the shaft meets the base — the "ink/charge" that does the imprint. One pixel of glow bleed is plenty.
- **Frosty accent:** one small **cyan** glint pixel (`#4fd0d6`) on the iron knob's top-left highlight.
- Keep the silhouette readable at inventory scale: knob ▸ shaft ▸ foot, clearly three stacked masses.

## 3. Linker — `linker.png` *(handheld — drawn for in-hand rendering)*

**Reads as:** a pointer/wand tool you aim at blocks to wire them up. Crafted from iron ingot + redstone +
stick. **Now a handheld item** (`minecraft:item/handheld`), so it renders gripped in the hand like a sword
or wand — **draw it on the standard lower-left ➜ upper-right diagonal** with the **grip at the bottom-left**
corner (that's where the hand holds it) and the **business end at the top-right**.

- **Handle (bottom-left):** a short brown **wooden stick** grip (hi `#6b4f2a`, shadow `#4a3519`), maybe with
  one iron band/ferrule where it meets the head.
- **Head (top-right):** a tapered **iron** tip/emitter.
- **Tip node:** a **redstone-red** node/lens at the very tip (`#ff5a4d`/`#c91e1e`) — the part that "links."
- **Frosty accent:** 2–3 small **cyan** dots trailing off the tip, suggesting a beam/link being cast.
- Because it's handheld, avoid important detail in the extreme bottom-left ~2px (partly hidden by the fist).

---

## Block textures

Block textures go in `src/main/resources/assets/frostyssortingdepot/textures/block/`, **16×16 per face**.
The models are now **multi-face** (filenames listed per block below). Keep the **iron palette** and the
**cyan Frosty accent** consistent so the set reads as one machine family; use **redstone-red** as the
status/active color. *(`_top`/`_side` files may safely duplicate art if you don't want them distinct.)*

### Linker Node — `linker_node_front.png`, `linker_node_side.png`, `linker_node_top.png`

**Facing-aware:** the `_front` faces the chest it serves. Reads as a compact iron junction box holding one
Filter Card.
- **`_front`:** iron housing with a riveted border; a thin recessed **card slot** (dark rectangle) with a
  sliver of paper-cream, plus a **redstone-red status pip** and a **cyan** edge accent.
- **`_side`:** plain riveted iron panel (used on the 4 non-front sides and the bottom).
- **`_top`:** riveted iron lid; can mirror `_side` if you like.

### Depot Controller — `depot_controller_top.png`, `depot_controller_side.png`, `depot_controller_bottom.png`

The brain/hub — beefier than the others. Items feed in from **above**, so the **top** is the intake.
- **`_top`:** a **hopper-like intake funnel/grate** (darker recess in the center) — clearly where items go in.
- **`_side`:** heavy riveted iron with a **central redstone-red indicator lens** (slightly glowing) and a
  faint comparator-notch vibe; **cyan** trim.
- **`_bottom`:** plain iron plate (can copy `_side` or `_top`).

### Depot Terminal — `depot_terminal_front.png`, `depot_terminal_side.png`, `depot_terminal_top.png`

**Facing-aware:** the `_front` (screen) faces the player who placed it.
- **`_front`:** iron frame around a dark **screen** (`#1a1f24`) with faint **cyan** scanlines / tiny list
  glyphs and one **redstone-red** power pip.
- **`_side` / `_top`:** plain iron casing (thin top edge); may share one texture.

### Overflow Chest — `textures/entity/chest/overflow.png`

It's now a **real (entity-rendered) chest** — same model, lid animation, double-chest joining, and **UV
layout** as a vanilla chest. So the texture is a **chest-sheet entity texture**, **not** a block face. Until
the PNGs exist the chest renders as the missing-texture placeholder. You need **three** files (like vanilla),
each in the matching UV layout:
- `overflow.png` (64×64) — single chest, like `minecraft:textures/entity/chest/normal.png`.
- `overflow_left.png` and `overflow_right.png` (64×64) — the two halves of a double chest, like
  `normal_left.png` / `normal_right.png`. (Without these, double chests render with the wrong UVs.)
- Wooden chest body (wood palette) with **iron banding/latch**; an **overflow motif** (downward funnel /
  chevrons) on the front, and a **cyan** tag so it reads as part of the family.
- Registered into the chest atlas via `assets/minecraft/atlases/chest.json` and drawn by the
  `OverflowChestRenderer` (custom-sprite hook).

### Depot Controller — `depot_controller.png`

**Reads as:** the brain/hub of the network — beefier and busier than the Linker Node.
- Darker, heavier iron block; thicker frame, more rivets — clearly the "main unit."
- A **central indicator**: a round redstone-red lens/eye or a small 2×2 LED cluster, slightly glowing.
- A hint of a **top intake** (a hopper-like notch/funnel along the top edge) since items feed in from above,
  and a faint **comparator notch** vibe on one side.
- **Cyan** accent trim to match. Should feel like the most important block of the set.

### Overflow Chest — `overflow_chest.png`

**Reads as:** a rugged catch-all bin — the one **wood-toned** block in the set (it's the "chest").
- Wooden crate/barrel body (wood palette: hi `#6b4f2a`, shadow `#4a3519`) with **iron banding/corners**.
- An **overflow motif**: a downward iron funnel or chevrons on the front, or a slightly open lid, signaling
  "things fall in here."
- A small **cyan** tag/label so it still reads as part of the family despite the wood.

### Depot Terminal — `depot_terminal.png`

**Reads as:** a wall-mounted monitor/dashboard panel.
- Iron frame around a dark **screen** face (near-black `#1a1f24`) showing faint **cyan** scanlines or a few
  tiny cyan list-row glyphs (echoing the dashboard it opens).
- One small **redstone-red power pip** in a corner.
- Reads clearly as a "screen on a box."

## GUI container backgrounds

`textures/gui/container/<name>.png`, **256×256 canvas**, artwork anchored top-left at the given size; the
slot/text positions below are where the code draws controls/labels.

- `linker_node.png` — **176×166**. One Filter Card slot centered at ~(80,35); leave clear space top-left for
  "Priority: N" and a linked/unlinked status line (~rows 20 & 31).
- `depot_controller.png` — **176×133** (compact, *not* chest-height). One input-buffer slot at ~(80,18);
  player inventory begins at y≈51. Leave label room near the slot for "Buffer".
- `depot_terminal.png` — **200×180** dashboard panel, **no slots**. The list area holds **up to 12 text rows**
  (10px each) from y≈18 down to y≈138; two footer lines (overflow %, buffer) sit at y≈148 and y≈158. A
  scroll-arrow may draw at the top-right when the list overflows. A subtle ruled-list look helps; keep the
  y≈18–138 band clear of busy art so the rows stay readable.
- The **Overflow Chest GUI needs no texture** — it reuses vanilla `generic_54.png`.

All custom GUIs render with the missing-texture placeholder until these exist, but are fully functional.
