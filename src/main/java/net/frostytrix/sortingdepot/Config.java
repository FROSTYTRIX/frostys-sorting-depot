package net.frostytrix.sortingdepot;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Client-side configuration. Currently just the Linker beam overlay (toggle, colour, line width); these
 * are purely visual, so they live in a CLIENT spec that never touches the server or world save.
 */
public final class Config {

    private Config() {
    }

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue SHOW_BEAM = BUILDER
            .comment("Draw a highlight/beam on the Linker Node selected by a held Linker.")
            .define("linkerBeam.show", true);

    public static final ModConfigSpec.ConfigValue<String> BEAM_COLOR = BUILDER
            .comment("Linker beam colour as hex RGB (\"33CCCC\") or ARGB (\"FF33CCCC\").")
            .define("linkerBeam.color", "33CCCC", Config::isHexColor);

    public static final ModConfigSpec.DoubleValue BEAM_WIDTH = BUILDER
            .comment("Linker beam line width.")
            .defineInRange("linkerBeam.width", 3.0, 0.5, 16.0);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private static boolean isHexColor(Object value) {
        return value instanceof String s && s.matches("(?i)[0-9a-f]{6}([0-9a-f]{2})?");
    }

    /** The configured beam colour as a packed ARGB int (alpha defaults to opaque for 6-digit values). */
    public static int beamColorArgb() {
        String hex = BEAM_COLOR.get();
        long value = Long.parseLong(hex, 16);
        return hex.length() == 8 ? (int) value : 0xFF000000 | (int) value;
    }

    /** The configured beam line width. */
    public static float beamWidth() {
        return (float) (double) BEAM_WIDTH.get();
    }
}
