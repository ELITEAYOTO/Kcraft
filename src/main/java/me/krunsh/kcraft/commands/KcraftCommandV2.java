package me.krunsh.kcraft.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import me.krunsh.kcraft.Kcraft;
import me.krunsh.kcraft.diagnostics.KcraftDiagnostics;

/**
 * Routeur V2.8 léger au-dessus de KcraftCommand historique.
 * Seules perf/status sont interceptées, le reste est délégué sans duplication.
 */
public final class KcraftCommandV2 extends KcraftCommand {

    private final KcraftDiagnostics diagnostics;

    public KcraftCommandV2(Kcraft plugin) {
        super(plugin);
        this.diagnostics = new KcraftDiagnostics(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command,
                             String label, String[] args) {
        if (args.length > 0) {
            String sub = args[0].toLowerCase();

            if ("perf".equals(sub)) {
                return handlePerf(sender, args);
            }

            if ("status".equals(sub)) {
                return handleStatus(sender);
            }
        }

        return super.onCommand(sender, command, label, args);
    }

    private boolean handlePerf(CommandSender sender, String[] args) {
        if (!sender.hasPermission("kcraft.admin")) {
            sender.sendMessage("§cVous n'avez pas la permission.");
            return true;
        }

        if (args.length >= 2 && "reset".equalsIgnoreCase(args[1])) {
            diagnostics.resetPerformance();
            sender.sendMessage("§aKCraft: compteurs performance remis à zéro.");
            return true;
        }

        for (String line : diagnostics.performanceLines()) {
            sender.sendMessage(line);
        }

        sender.sendMessage("§8Reset: /kcraft perf reset");
        return true;
    }

    private boolean handleStatus(CommandSender sender) {
        if (!sender.hasPermission("kcraft.admin")) {
            sender.sendMessage("§cVous n'avez pas la permission.");
            return true;
        }

        for (String line : diagnostics.statusLines()) {
            sender.sendMessage(line);
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command,
                                      String alias, String[] args) {
        if (args.length == 1 && sender.hasPermission("kcraft.admin")) {
            List<String> base = new ArrayList<String>(
                super.onTabComplete(sender, command, alias, args)
            );
            addIfMissing(base, "perf");
            addIfMissing(base, "status");
            return filter(base, args[0]);
        }

        if (args.length == 2
                && "perf".equalsIgnoreCase(args[0])
                && sender.hasPermission("kcraft.admin")) {
            return filter(
                new ArrayList<String>(Arrays.asList("reset")),
                args[1]
            );
        }

        return super.onTabComplete(sender, command, alias, args);
    }

    private static void addIfMissing(List<String> values, String value) {
        if (!values.contains(value)) values.add(value);
    }

    private static List<String> filter(List<String> values, String current) {
        String prefix = current == null ? "" : current.toLowerCase();
        List<String> out = new ArrayList<String>();
        for (String value : values) {
            if (value.toLowerCase().startsWith(prefix)) out.add(value);
        }
        return out;
    }
}
