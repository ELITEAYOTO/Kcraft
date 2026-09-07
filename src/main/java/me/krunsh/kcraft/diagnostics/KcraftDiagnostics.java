package me.krunsh.kcraft.diagnostics;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import me.krunsh.kcraft.Kcraft;
import me.krunsh.kcraft.batch.CraftBatchMetrics;
import me.krunsh.kcraft.gui.GuiRefreshMetrics;
import me.krunsh.kcraft.logging.LoggingMetrics;
import me.krunsh.kcraft.managers.IndexedCraftManager;
import me.krunsh.kcraft.matching.MatchingMetrics;
import me.krunsh.kcraft.matching.RecipeIndex;

/**
 * Diagnostics V2.8.
 *
 * Lecture uniquement : aucune opération lourde, aucun scan d'ItemStack.
 */
public final class KcraftDiagnostics {

    private final Kcraft plugin;

    public KcraftDiagnostics(Kcraft plugin) {
        this.plugin = plugin;
    }

    public List<String> performanceLines() {
        List<String> lines = new ArrayList<String>();

        IndexedCraftManager manager = plugin.getIndexedCraftManager();
        MatchingMetrics matching = manager.getMatchingMetrics();
        RecipeIndex index = manager.getRecipeIndex();

        CraftBatchMetrics batch = plugin.getCraftGUIListener().getBatchMetrics();
        GuiRefreshMetrics gui = plugin.getCraftGUIListener().getGuiRefreshMetrics();
        LoggingMetrics logging = plugin.getLoggingManager().getMetrics();

        lines.add("§6=== KCraft V2.8 Performance ===");
        lines.add("§7Recipes: §e" + plugin.getRecipeCatalog().size()
            + " §7| compiled: §e" + plugin.getCompiledRecipeCatalog().size()
            + " §7| tables: §e" + plugin.getTableManager().getAllTables().size());

        if (index != null) {
            lines.add("§7Index: buckets=§e" + index.getBucketCount()
                + " §7avg=§e" + f(index.getAverageBucketSize())
                + " §7max=§e" + index.getLargestBucket());
        }

        lines.add("§bMatching §7attempts=§e" + matching.getAttempts()
            + " §7matches=§e" + matching.getMatches()
            + " §7candidates avg=§e" + f(matching.getAverageCandidates())
            + " §7exact avg=§e" + f(matching.getAverageExactChecks()));

        lines.add("§bMatching §7time avg/max=§e" + f(matching.getAverageMicros())
            + "§7/§e" + f(matching.getMaxMicros()) + "us"
            + " §7NBT snapshots=§e" + matching.getNbtSnapshots());

        lines.add("§dBatch §7plans=§e" + batch.getPlans()
            + " §7planned=§e" + batch.getPlannedCrafts()
            + " §7executed=§e" + batch.getExecutedCrafts()
            + " §7failed/cancel=§e" + batch.getFailedCrafts()
            + "§7/§e" + batch.getCancelledCrafts());

        lines.add("§dBatch §7planning avg/max=§e" + f(batch.getAveragePlanningMicros())
            + "§7/§e" + f(batch.getMaxPlanningMicros()) + "us");

        lines.add("§aGUI §7marks=§e" + gui.getMarks()
            + " §7coalesced=§e" + gui.getCoalesced()
            + " §7rate=§e" + f(gui.getCoalesceRate()) + "%"
            + " §7refresh=§e" + gui.getRefreshed()
            + " §7stale=§e" + gui.getStaleSkipped());

        lines.add("§aGUI §7dirtyNow=§e" + plugin.getCraftGUIListener().getDirtyGuiCount()
            + " §7locksNow=§e" + plugin.getCraftGUIListener().getPreviewLockCount()
            + " §7preview create/reuse/use=§e" + gui.getPreviewCreated()
            + "§7/§e" + gui.getPreviewReused()
            + "§7/§e" + gui.getPreviewConsumed());

        lines.add("§3Logging §7queued/written=§e" + logging.getQueued()
            + "§7/§e" + logging.getWritten()
            + " §7pending=§e" + plugin.getLoggingManager().getPendingLogCount()
            + " §7batches=§e" + logging.getBatches()
            + " §7dropped/fail=§e" + logging.getDropped()
            + "§7/§e" + logging.getFailures());

        lines.add("§3Logging §7queue max=§e" + logging.getMaxQueue()
            + " §7write avg/max=§e" + f(logging.getAverageWriteMicros())
            + "§7/§e" + f(logging.getMaxWriteMicros()) + "us");

        lines.add("§9Tables IO §7writes=§e" + plugin.getTableManager().getPersistenceWrites()
            + " §7coalesced=§e" + plugin.getTableManager().getPersistenceCoalesced()
            + " §7failures=§e" + plugin.getTableManager().getPersistenceFailures());

        return lines;
    }

    public List<String> statusLines() {
        List<String> lines = new ArrayList<String>();
        lines.add("§6=== KCraft V2.8 Status ===");
        lines.add("§7API: §e" + Kcraft.getAPI().getAPIVersion()
            + " §7| runtime config: §aIMMUTABLE"
            + " §7| legacy cache: §cOFF");
        lines.add("§7Recipes: §e" + plugin.getRecipeCatalog().size()
            + " §7| compiled: §e" + plugin.getCompiledRecipeCatalog().size());

        RecipeIndex index = plugin.getIndexedCraftManager().getRecipeIndex();
        lines.add("§7RecipeIndex: "
            + (index == null ? "§cOFF" : "§aON §7(" + index.getBucketCount() + " buckets)"));

        lines.add("§7Kfaction: " + yn(plugin.getHookManager().isKfactionEnabled()));
        lines.add("§7Vault: " + yn(plugin.getHookManager().isPluginAvailable("Vault")));
        lines.add("§7PlaceholderAPI: " + yn(plugin.getHookManager().isPluginAvailable("PlaceholderAPI")));
        lines.add("§7KHope bridge: §e" + plugin.getVanillaDebugManager().healthSummary());

        Map<String, Object> global = plugin.getLoggingManager().getGlobalStats();
        lines.add("§7Craft stats: total=§e" + global.get("total_crafts")
            + " §7failed=§e" + global.get("failed_crafts")
            + " §7unique=§e" + global.get("unique_players"));

        return lines;
    }

    public void resetPerformance() {
        plugin.getIndexedCraftManager().getMatchingMetrics().reset();
        plugin.getCraftGUIListener().getBatchMetrics().reset();
        plugin.getCraftGUIListener().getGuiRefreshMetrics().reset();
        plugin.getLoggingManager().getMetrics().reset();
        plugin.getTableManager().resetPersistenceMetrics();
    }

    private static String yn(boolean value) {
        return value ? "§aON" : "§cOFF";
    }

    private static String f(double value) {
        return String.format(Locale.US, "%.2f", value);
    }
}
