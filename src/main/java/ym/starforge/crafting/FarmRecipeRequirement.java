package ym.starforge.crafting;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiPredicate;
import java.util.logging.Level;
import org.bukkit.plugin.Plugin;

public final class FarmRecipeRequirement {
    private FarmRecipeRequirement() {}

    public static BiPredicate<UUID,String> connect(Plugin forge) {
        var farm = forge.getServer().getPluginManager().getPlugin("NextDoorFarm");
        if (farm == null) return (player, crop) -> false;
        try {
            var query = farm.getClass().getMethod("hasHarvested", UUID.class, String.class);
            var reported = new AtomicBoolean();
            return (player, crop) -> {
                try {
                    return farm.isEnabled() && Boolean.TRUE.equals(query.invoke(farm, player, crop));
                } catch (ReflectiveOperationException error) {
                    if (reported.compareAndSet(false, true)) forge.getLogger().log(Level.WARNING, "NextDoorFarm harvest lookup failed", error);
                    return false;
                }
            };
        } catch (NoSuchMethodException error) {
            forge.getLogger().warning("NextDoorFarm has no harvest milestone API; harvest-gated recipes remain locked");
            return (player, crop) -> false;
        }
    }
}
