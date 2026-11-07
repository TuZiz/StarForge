package ym.starforge.integration;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import java.util.Map;
import java.lang.reflect.InvocationTargetException;

/** Optional, noncached lookup. AsterStats' base-only service never calls enhancement contribution. */
public final class AsterStatsItems {
    public static Map<String,Double> base(ItemStack item){
        var plugin=Bukkit.getPluginManager().getPlugin("AsterStats");if(plugin==null||!plugin.isEnabled())return Map.of();
        try{
            Class<?> api=plugin.getClass().getClassLoader().loadClass("dev.aster.asterstats.api.AsterStatsItemApi");
            Object provider=Bukkit.getServicesManager().load(api);if(provider==null)return Map.of();
            Object result=api.getMethod("readBase",ItemStack.class,String.class).invoke(provider,item.clone(),"mainhand");
            var values=new java.util.HashMap<String,Double>();
            for(var e:((Map<?,?>)result).entrySet()){
                if(!(e.getKey() instanceof String key)||!(e.getValue() instanceof Number n)||!Double.isFinite(n.doubleValue()))throw new IllegalStateException("AsterStats invalid base item attributes");
                values.put(key,n.doubleValue());
            }
            return Map.copyOf(values);
        }catch(ClassNotFoundException ex){return Map.of();}
        catch(InvocationTargetException ex){throw new IllegalStateException("AsterStats readBase failed",ex.getCause());}
        catch(ReflectiveOperationException ex){throw new IllegalStateException("AsterStats item API incompatible",ex);}
    }
}
