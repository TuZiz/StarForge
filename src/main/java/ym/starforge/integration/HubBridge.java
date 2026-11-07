package ym.starforge.integration;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import ym.itemhub.api.ItemHubModifierApi;
import ym.itemhub.api.ItemHubLoreDecoratorApi;
import ym.starforge.item.WeaponService;

public final class HubBridge {
    public static AutoCloseable register(Plugin plugin,WeaponService weapons){
        var api=Bukkit.getServicesManager().load(ItemHubModifierApi.class);
        if(api==null)throw new IllegalStateException("Update ItemHub: ItemHubModifierApi is required for safe auto-refresh");
        var lore=Bukkit.getServicesManager().load(ItemHubLoreDecoratorApi.class);
        if(lore==null)throw new IllegalStateException("Update ItemHub: ItemHubLoreDecoratorApi required for inline enhancement Lore");
        AutoCloseable lines=lore.register(plugin,"enhancement",(id,item,base)->weapons.decorateLore(item,java.util.List.copyOf(base)));
        AutoCloseable modifier=api.register(plugin,"enhancement",(id,item)->weapons.render(item,true));
        return ()->{modifier.close();lines.close();};
    }
}
