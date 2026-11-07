package ym.starforge.integration;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import ym.starforge.api.StarForgeApi;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;

/** PAPI reads immutable cached values; it never accesses an off-thread inventory. */
public final class ForgePlaceholders extends PlaceholderExpansion implements PlaceholderCache {
    private final ConcurrentHashMap<UUID,int[]> cache=new ConcurrentHashMap<>();
    public String getIdentifier(){return "starforge";}public String getAuthor(){return "ym";}public String getVersion(){return "1.0.0";}public boolean persist(){return true;}
    public void update(UUID id,int level,int fails){cache.put(id,new int[]{level,fails});}
    public void remove(UUID id){cache.remove(id);}public void clear(){cache.clear();}
    public void close(){unregister();cache.clear();}
    public String onRequest(OfflinePlayer player,String key){if(player==null)return "0";int[] v=cache.getOrDefault(player.getUniqueId(),new int[2]);return switch(key){case "level"->""+v[0];case "failures"->""+v[1];default->null;};}
}
