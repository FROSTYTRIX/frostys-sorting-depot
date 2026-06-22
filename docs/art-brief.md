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

**Reads as:** a handheld rubber-stamp tool that imprints a priority. Crafted from iron ingot + redstone torch.

- A **stamp**: short vertical iron handle, wider stamp base at the bottom.
- A **redstone-red glowing dot/gem** set into the base or the top knob (the redstone-torch reference) —
  signals it "stamps" a value.
- Optional: 1–2 tiny red pips on the stamp face hinting at a number/level (keep abstract at 16px).
- A small **cyan glint** on the iron handle to tie it to the set.

## 3. Linker — `linker.png`

**Reads as:** a linking wand/tool that ties an inventory to the network. Crafted from iron ingot + redstone + stick.

- A short diagonal **rod**: brown wooden **stick handle** (lower-left) into an **iron head** (upper-right).
- A **redstone-red node** at the tip that "emits" the link.
- A subtle **cyan connecting motif** — e.g., 2–3 cyan dots trailing from the tip, suggesting a link being made.
- Should feel like a tool you point at a chest.

---

## Blocks (Phase 3)

Block textures go in `src/main/resources/assets/frostyssortingdepot/textures/block/<name>.png` (16×16).

### Linker Node — `linker_node.png`

**Reads as:** a compact iron device you mount facing a chest, with a slot for a Filter Card.

- Iron-housing cube, riveted/paneled like a small machine.
- A visible **card slot** (thin dark rectangle) on the face, hinting the Filter Card goes in.
- A **redstone-red status pip** + the **cyan accent** so it matches the item set.
- Currently rendered as an all-faces cube (one texture). If you want a distinct front, supply
  `_front` / `_side` / `_top` variants and I'll switch the model to a facing-aware one.

### Coming later

Depot Controller, Overflow Chest, and (Phase 6) Depot Terminal block textures — I'll add specs here as
each block lands. The Linker GUI (Phase 3B) will also need a container background:
`textures/gui/container/linker_node.png` (176×166 standard) — spec to follow when we build that screen.
