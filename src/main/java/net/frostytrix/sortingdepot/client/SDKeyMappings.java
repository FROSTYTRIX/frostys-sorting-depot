package net.frostytrix.sortingdepot.client;

import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

/**
 * Client key bindings. Registered on {@code RegisterKeyMappingsEvent}; rebindable in Options → Controls.
 */
public final class SDKeyMappings {

    /** Toggles the Controller → node wiring overlay (see {@link SDLinkerBeams}). Defaults to B. */
    public static final KeyMapping TOGGLE_WIRING = new KeyMapping(
            "key.frostyssortingdepot.toggle_wiring", GLFW.GLFW_KEY_B, "key.categories.frostyssortingdepot");

    private SDKeyMappings() {
    }
}
