package ym.starforge.config;

import org.bukkit.configuration.ConfigurationSection;
import java.util.*;

/** Async-only validation of optional per-weapon overrides. */
public final class WeaponRulesLoader {
    public Map<String,ForgeSettings.Weapon> load(ConfigurationSection yaml,int max,
            Map<Integer,ForgeSettings.Level> defaults,Map<String,ForgeSettings.MaterialSpec> materials){
        Object version=yaml.get("schema-version");
        require(Objects.equals(version,1)||Objects.equals(version,2),"schema-version",version);
        var root=yaml.getConfigurationSection("weapons");
        if(root==null&&yaml.get("weapons")==null)return Map.of();
        require(root!=null,"weapons",yaml.get("weapons"));
        var result=new LinkedHashMap<String,ForgeSettings.Weapon>();var selectors=new HashSet<String>();
        for(String key:root.getKeys(false)){
            var s=root.getConfigurationSection(key);require(s!=null,"weapons."+key,root.get(key));String path="weapons."+key;
            String hub=text(s,"itemhub",path),material=text(s,"vanilla",path),marker=text(s,"marker",path);
            require((hub==null?0:1)+(material==null?0:1)+(marker==null?0:1)==1,path,"exactly one of itemhub/vanilla/marker required");
            if(material!=null){try{org.bukkit.Material.valueOf(material);require(!Set.of("AIR","CAVE_AIR","VOID_AIR").contains(material),path+".vanilla",material);}catch(IllegalArgumentException ex){throw invalid(path+".vanilla",material);}}
            require(selectors.add(hub!=null?"hub:"+hub:material!=null?"vanilla:"+material:"marker:"+marker),path,"duplicate selector");
            boolean blocked=bool(s,"blacklist",false,path);Integer cap=s.contains("max-level")?integer(s,"max-level",1,max,path):null;
            double growth=num(s,"growth-multiplier",1,0,100,path),fallback=num(s,"base-attack",0,0,1000000,path);
            var levels=new LinkedHashMap<Integer,ForgeSettings.Level>();
            if(s.contains("levels")){
                var overrides=s.getConfigurationSection("levels");require(overrides!=null,path+".levels",s.get("levels"));
                for(String level:overrides.getKeys(false)){
                    int target;try{target=Integer.parseInt(level);}catch(NumberFormatException e){throw invalid(path+".levels."+level,level);}
                    require(target>=1&&target<=max,path+".levels."+level,target);
                    var o=overrides.getConfigurationSection(level);require(o!=null,path+".levels."+level,overrides.get(level));
                    var d=defaults.get(target);String p=path+".levels."+level;
                    for(String field:o.getKeys(false))require(Set.of("chance","money","material","amount","attack-flat","attack-percent","downgrade-chance").contains(field),p+"."+field,o.get(field));
                    String stone=o.contains("material")?text(o,"material",p):d.stone();require(materials.containsKey(stone),p+".material",stone);
                    levels.put(target,new ForgeSettings.Level(num(o,"chance",d.chance(),0,1,p),num(o,"money",d.cost(),0,1e9,p),stone,
                        o.contains("amount")?integer(o,"amount",1,2304,p):d.amount(),num(o,"attack-flat",d.flat(),0,1e6,p),
                        num(o,"attack-percent",d.percent(),0,100,p),target<=10?0:num(o,"downgrade-chance",d.downgrade(),0,1,p)));
                }
            }
            var weapon=new ForgeSettings.Weapon(hub,material,fallback,marker,blocked,cap,growth,levels,s.contains("base-attack"));
            if(!blocked)validateTiers(weapon,max,defaults,materials,path);
            result.put(key,weapon);
        }
        return Collections.unmodifiableMap(result);
    }
    private static void validateTiers(ForgeSettings.Weapon w,int max,Map<Integer,ForgeSettings.Level> defaults,Map<String,ForgeSettings.MaterialSpec> materials,String path){
        int limit=w.max(max);
        for(int current=0;current<limit;current++){
            String id=w.level(current+1,defaults).stone();var rule=materials.get(id).stone();int target=current+(rule==null?1:rule.steps());
            // A multi-step stone may simply be unavailable immediately below a custom cap.
            if(target>limit)continue;
            require(rule==null||rule.allows(target),path+".levels."+(current+1)+".material",id+" cannot enhance +"+current);
        }
    }
    private static String text(ConfigurationSection s,String k,String p){if(!s.contains(k))return null;Object v=s.get(k);require(v instanceof String&&!((String)v).isBlank(),p+"."+k,v);return (String)v;}
    private static boolean bool(ConfigurationSection s,String k,boolean d,String p){if(!s.contains(k))return d;Object v=s.get(k);require(v instanceof Boolean,p+"."+k,v);return (Boolean)v;}
    private static int integer(ConfigurationSection s,String k,int min,int max,String p){double n=num(s,k,0,min,max,p);require(n==Math.rint(n),p+"."+k,n);return (int)n;}
    private static double num(ConfigurationSection s,String k,double d,double min,double max,String p){if(!s.contains(k))return d;Object raw=s.get(k);require(raw instanceof Number,p+"."+k,raw);double n=((Number)raw).doubleValue();require(Double.isFinite(n)&&n>=min&&n<=max,p+"."+k,raw);return n;}
    private static void require(boolean ok,String path,Object value){if(!ok)throw invalid(path,value);}
    private static IllegalArgumentException invalid(String p,Object v){return new IllegalArgumentException("weapons.yml:"+p+": invalid value '"+v+"'");}
}
