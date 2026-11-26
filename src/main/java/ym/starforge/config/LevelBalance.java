package ym.starforge.config;

import org.bukkit.configuration.ConfigurationSection;
import java.util.*;

/** Reads the balance fields and overlays old sets of defaults in memory. */
record LevelBalance(Map<String,Double> chances,String requiredStone,double downgrade,double crack) {
    static LevelBalance read(ConfigurationSection level,ConfigurationSection bundled,Map<String,ForgeSettings.MaterialSpec> materials,int target,double downgrade){
        String path="levels."+target+".";
        var source=level.contains("stone-chances")?level:bundled;
        var chances=new LinkedHashMap<String,Double>();
        if(source!=null&&source.contains("stone-chances")){
            var section=source.getConfigurationSection("stone-chances");
            if(section==null)throw invalid(path+"stone-chances",source.get("stone-chances"));
            for(String id:section.getKeys(false)){
                if(source==bundled&&!materials.containsKey(id))continue;
                if(!isStone(materials,id))throw invalid(path+"stone-chances."+id,id);
                chances.put(id,probability(section.get(id),path+"stone-chances."+id));
            }
        }
        Object raw=level.contains("required-stone")?level.get("required-stone"):bundled==null?"":bundled.getString("required-stone","");
        if(!(raw instanceof String required)||(!required.isEmpty()&&!isStone(materials,required)))throw invalid(path+"required-stone",raw);
        double down=target<=10?0:downgrade;
        if(target>10&&down==0.35&&bundled!=null&&!level.contains("stone-chances")&&!level.contains("required-stone")&&!level.contains("crack-chance"))
            down=bundled.getDouble("downgrade-chance",down);
        double crack=level.contains("crack-chance")?probability(level.get("crack-chance"),path+"crack-chance")
            :bundled==null?0:bundled.getDouble("crack-chance");
        if(crack>0&&(target!=15||!isStone(materials,ym.starforge.transaction.ForgeRepair.MATERIAL)))throw invalid(path+"crack-chance",crack+" (requires +15 and intermediate_stone)");
        if(down+crack>1)throw invalid(path+"crack-chance",crack+" + downgrade-chance "+down);
        return new LevelBalance(chances,required,down,crack);
    }
    private static boolean isStone(Map<String,ForgeSettings.MaterialSpec> materials,String id){
        return !Set.of("lucky","protect").contains(id)&&materials.containsKey(id);
    }
    private static double probability(Object raw,String path){
        if(raw instanceof Number n&&Double.isFinite(n.doubleValue())&&n.doubleValue()>=0&&n.doubleValue()<=1)return n.doubleValue();
        throw invalid(path,raw);
    }
    private static IllegalArgumentException invalid(String path,Object raw){
        return new IllegalArgumentException("levels.yml:"+path+": invalid value '"+raw+"'");
    }
}
