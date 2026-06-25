package net.frostytrix.sortingdepot.client;

import net.frostytrix.sortingdepot.gui.DepotTerminalMenu;
import net.frostytrix.sortingdepot.network.TerminalUpdatePayload;
import net.minecraft.client.Minecraft;

/**
 * Client-only handling for {@link TerminalUpdatePayload}. Kept separate from the (common) network
 * registration so a dedicated server never classloads {@link Minecraft}.
 */
public final class TerminalClientHandler {

    private TerminalClientHandler() {
    }

    /** Pushes a fresh snapshot into the open Terminal menu, if one is open. Runs on the client main thread. */
    public static void handle(TerminalUpdatePayload payload) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.containerMenu instanceof DepotTerminalMenu menu) {
            menu.setSnapshot(payload.snapshot());
        }
    }
}
