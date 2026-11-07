package ym.starforge.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.util.*;

/** Async-only rule catalog stored beside the CraftEngine resource pack, not in StarForge. */
public final class MaterialCatalogLoader {
    public Map<String,ForgeSettings.MaterialSpec> load(File file,int max)throws Exception {
        var yaml=new YamlConfiguration();yaml.load(file);
        check(Objects.equals(yaml.get("schema-version"),1),file+":schema-version",yaml.get("schema-version"));
        var ms=yaml.getConfigurationSection("materials");check(ms!=null,file+":materials",yaml.get("materials"));
        var materials=new LinkedHashMap<String,ForgeSettings.MaterialSpec>();
        for(String id:ms.getKeys(false)) {
            String path=file+":materials."+id;
            var s=ms.getConfigurationSection(id);check(s!=null,path,ms.get(id));
            String vanilla=s.getString("vanilla"),ce=s.getString("craft-engine"),bind=s.getString("binding");
            check((vanilla!=null?1:0)+(ce!=null?1:0)+(bind!=null?1:0)==1,path,s.getValues(false));
            // Display metadata belongs exclusively to CE; reject accidentally copied legacy names/Lore.
            check(!s.contains("name")&&!s.contains("lore"),path,"name/lore must be maintained in CE item definitions");
            ForgeSettings.StoneRule rule=null;
            if(s.contains("enhancement")){
                var e=s.getConfigurationSection("enhancement");check(e!=null,path+".enhancement",s.get("enhancement"));
                int min=e.contains("min-target")?integer(e,"min-target",max,path):1;
                int top=e.contains("max-target")?integer(e,"max-target",max,path):max;
                int steps=integer(e,"steps",max,path);
                check(min<=top,path+".enhancement",e.getValues(false));
                Double chance=null;
                if(e.contains("chance")){
                    Object raw=e.get("chance");check(raw instanceof Number,path+".enhancement.chance",raw);chance=((Number)raw).doubleValue();
                    check(Double.isFinite(chance)&&chance>=0&&chance<=1,path+".enhancement.chance",raw);
                }
                rule=new ForgeSettings.StoneRule(min,top,steps,chance);
            }
            materials.put(id,new ForgeSettings.MaterialSpec(vanilla,ce,bind,rule));
        }
        check(materials.containsKey("lucky")&&materials.containsKey("protect"),file+":materials",materials.keySet());
        return Map.copyOf(materials);
    }
    private static int integer(ConfigurationSection s,String key,int max,String path){Object raw=s.get(key);check(raw instanceof Integer&&((Integer)raw)>=1&&((Integer)raw)<=max,path+".enhancement."+key,raw);return (Integer)raw;}
    private static void check(boolean ok,String path,Object value){if(!ok)throw new IllegalArgumentException(path+": invalid value '"+value+"'");}
}
