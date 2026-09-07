package me.krunsh.kcraft.api.execution;

/**
 * Origine normalisee d'une execution KCraft.
 *
 * Le fork pourra réutiliser directement ces valeurs dans son bridge V2.
 */
public enum CraftExecutionSource {

    CUSTOM_GUI_SINGLE,
    SHIFT_BATCH,
    VANILLA_BRIDGE,
    API_FORCE,
    COMMAND_FORCE,
    UNKNOWN;

    public boolean isBatch() {
        return this == SHIFT_BATCH;
    }

    public boolean isForced() {
        return this == API_FORCE
            || this == COMMAND_FORCE;
    }
}
