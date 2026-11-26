package ym.starforge.command;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import ym.starforge.crafting.*;
import ym.starforge.text.Texts;
import java.util.*;
import java.util.function.Supplier;
import java.util.logging.Level;

/** Admin unlock entry point; disk work belongs to RecipeUnlocks. */
public final class RecipeCommand {
    private final Plugin plugin;
    private final Supplier<CraftingConfig> config;
    private final RecipeUnlocks unlocks;
    private final Texts texts;
    public RecipeCommand(Plugin plugin, Supplier<CraftingConfig> config, RecipeUnlocks unlocks, Texts texts) {
        this.plugin = plugin; this.config = config; this.unlocks = unlocks; this.texts = texts;
    }
    public void execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("starforge.admin")) { texts.send(sender, "no-permission"); return; }
        if (args.length != 4 || !args[1].equalsIgnoreCase("unlock")) { texts.send(sender, "recipe-help"); return; }
        Player target = Bukkit.getPlayerExact(args[2]);
        if (target == null) { texts.send(sender, "offline"); return; }
        var recipe = config.get().recipes().stream().filter(r -> r.id().equals(args[3])).findFirst().orElse(null);
        if (recipe == null) { texts.send(sender, "recipe-unknown"); return; }
        UUID playerId = target.getUniqueId();
        unlocks.grant(playerId, recipe.id()).whenComplete((v, error) -> {
            if (error != null) plugin.getLogger().log(Level.SEVERE, "Recipe unlock failed player=" + playerId + " recipe=" + recipe.id(), error);
            if (!plugin.isEnabled()) return;
            Runnable feedback = () -> texts.send(sender, error == null ? "recipe-unlocked" : "error");
            if (sender instanceof Player p) p.getScheduler().execute(plugin, feedback, null, 1);
            else Bukkit.getGlobalRegionScheduler().execute(plugin, feedback);
        });
    }
    public List<String> complete(CommandSender sender, String[] args) {
        if (!sender.hasPermission("starforge.admin")) return List.of();
        List<String> values = switch (args.length) {
            case 2 -> List.of("unlock");
            case 3 -> Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
            case 4 -> config.get().recipes().stream().map(CraftingRecipe::id).toList();
            default -> List.of();
        };
        return values.stream().filter(s -> s.startsWith(args[args.length - 1])).toList();
    }
}
