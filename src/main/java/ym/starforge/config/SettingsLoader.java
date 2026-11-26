package ym.starforge.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Async-only resource installation and configuration parsing. */
public final class SettingsLoader {
    private final JavaPlugin plugin;
    public SettingsLoader(JavaPlugin plugin) { this.plugin=plugin; }
    public ForgeSettings load() throws Exception {
        File root=plugin.getDataFolder();
        for (String file : InitialResourceInstaller.install(root.toPath(), plugin::getResource))
            plugin.getLogger().info("Installed default configuration: " + new File(root, file));
        for (String file : GuiResourceInstaller.installMissing(root.toPath(), plugin::getResource))
            plugin.getLogger().info("Restored missing GUI configuration: " + new File(root, file));
        var c=read("config.yml"); var l=read("levels.yml"); var w=read("weapons.yml"); var lang=read("lang/zh_cn.yml");
        var defaultLevels=new YamlConfiguration();
        try(var stream=plugin.getResource("levels.yml")){
            defaultLevels.load(new InputStreamReader(Objects.requireNonNull(stream),java.nio.charset.StandardCharsets.UTF_8));
        }
        int max=c.getInt("max-level"); check(max>=1 && max<=100,"config.yml:max-level",max);
        String materialPath=c.getString("materials-file","../CraftEngine/resources/强化石/starforge-materials.yml");
        File materialFile=new File(root,materialPath).getCanonicalFile();
        var materials=new MaterialCatalogLoader().load(materialFile,max);
        var levels=new LinkedHashMap<Integer,ForgeSettings.Level>();
        for(int i=1;i<=max;i++) {
            String path="levels."+i; var s=section(l,path,"levels.yml");
            double chance=number(s,"chance",0,1,path),cost=number(s,"money",0,1e9,path),flat=number(s,"attack-flat",0,1e6,path),pct=number(s,"attack-percent",0,100,path), down=number(s,"downgrade-chance",0,1,"levels.yml:"+path);
            int count=s.getInt("amount"); String stone=s.getString("material");
            check(count>0 && count<=2304 && materials.containsKey(stone),"levels.yml:"+path,s.getValues(false));
            var balance=LevelBalance.read(s,defaultLevels.getConfigurationSection(path),materials,i,down);
            levels.put(i,new ForgeSettings.Level(chance,cost,stone,count,flat,pct,balance.downgrade(),
                optionalNumber(s,"defense-flat",0.20,0,1e6,path),optionalNumber(s,"defense-percent",0.05,0,100,path),
                balance.chances(),balance.requiredStone(),balance.crack()));
        }
        for(int current=0;current<max;current++){
            String id=levels.get(current+1).stone();var rule=materials.get(id).stone();
            check(rule==null||(current+rule.steps()<=max&&rule.allows(current+rule.steps())),"levels.yml:levels."+(current+1)+".material",id+" tier cannot enhance current +"+current);
            int target=current+(rule==null?1:rule.steps());
            String required=levels.get(target).requiredStone();
            if(!required.isEmpty()){
                var gate=materials.get(required).stone();
                check(gate==null?required.equals(id):gate.allows(target)&&gate.steps()==target-current,
                    "levels.yml:levels."+target+".required-stone",required+" cannot enhance +"+current);
            }
        }
        var weapons=new WeaponRulesLoader().load(w,max,levels,materials);
        Map<String,String> messages=new HashMap<>();
        try(var stream=plugin.getResource("lang/zh_cn.yml")){
            var defaults=new YamlConfiguration();defaults.load(new java.io.InputStreamReader(java.util.Objects.requireNonNull(stream),java.nio.charset.StandardCharsets.UTF_8));
            for(String key:defaults.getKeys(false))if(!key.equals("schema-version"))messages.put(key,defaults.getString(key));
        }
        var bundledMessages=Map.copyOf(messages);
        for(String key:lang.getKeys(false)) if(!key.equals("schema-version")) {
            Object value=lang.get(key); check(value instanceof String,"lang/zh_cn.yml:"+key,value); messages.put(key,(String)value);
        }
        EnhancementTextDefaults.upgrade(messages,bundledMessages);
        String backend=c.getString("attribute-backend"); check(Set.of("asterstats","vanilla","api").contains(backend),"config.yml:attribute-backend",backend);
        var loreStyle=ForgeSettings.LoreStyle.defaults();
        if(w.contains("lore.attack-line-pattern")){
            Object raw=w.get("lore.attack-line-pattern");check(raw instanceof String,"weapons.yml:lore.attack-line-pattern",raw);
            try{loreStyle=new ForgeSettings.LoreStyle(java.util.regex.Pattern.compile((String)raw));}catch(java.util.regex.PatternSyntaxException ex){throw new IllegalArgumentException("weapons.yml:lore.attack-line-pattern: invalid '"+raw+"'",ex);}
        }
        if(w.contains("lore.defense-line-pattern")){
            Object raw=w.get("lore.defense-line-pattern");check(raw instanceof String,"weapons.yml:lore.defense-line-pattern",raw);
            try{loreStyle=new ForgeSettings.LoreStyle(loreStyle.attackLine(),java.util.regex.Pattern.compile((String)raw));}catch(java.util.regex.PatternSyntaxException ex){throw new IllegalArgumentException("weapons.yml:lore.defense-line-pattern: invalid '"+raw+"'",ex);}
        }
        var result=new ForgeSettings(max,number(c,"pity-per-failure",0,1,"config.yml"),number(c,"lucky-bonus",0,1,"config.yml"),c.getInt("announce-level",11),backend,
            Map.copyOf(levels),Map.copyOf(materials),weapons,Map.copyOf(messages),read("gui/main.yml"),read("gui/admin.yml"),loreStyle);
        ym.starforge.gui.ForgeMenus.validate(result.gui());ym.starforge.gui.ForgeMenus.validate(result.admin());
        ym.starforge.gui.ForgeMenus.validateInputs(result.gui());
        for(String id:result.gui().getStringList("virtual-materials.materials"))
            check(materials.containsKey(id)&&!Set.of("lucky","protect").contains(id),"gui/main.yml:virtual-materials.materials",id);
        for(var entry:levels.entrySet()){
            String required=entry.getValue().requiredStone();
            check(required.isEmpty()||result.gui().getStringList("virtual-materials.materials").contains(required),
                "levels.yml:levels."+entry.getKey()+".required-stone (must be selectable in gui/main.yml)",required);
        }
        if(w.getInt("schema-version")==1)WeaponConfigMigration.migrate(new File(root,"weapons.yml").toPath());
        return result;
    }
    private YamlConfiguration read(String path) throws Exception {
        var y=new YamlConfiguration(); y.load(new File(plugin.getDataFolder(),path)); check(Objects.equals(y.get("schema-version"),1)||(path.equals("weapons.yml")&&Objects.equals(y.get("schema-version"),2)),path+":schema-version",y.get("schema-version"));
        if(path.equals("gui/main.yml"))try(var stream=plugin.getResource(path)){
            var bundled=new YamlConfiguration();bundled.load(new InputStreamReader(Objects.requireNonNull(stream),java.nio.charset.StandardCharsets.UTF_8));
            virtualDefaults(y,bundled);
        }
        return y;
    }
    /** Add only new optional presentation fields in memory; never repair invalid user layouts. */
    static void virtualDefaults(YamlConfiguration y,YamlConfiguration bundled){
        var defaults=new YamlConfiguration();defaults.set("virtual-materials",bundled.get("virtual-materials"));
        defaults.set("virtual-materials.target-chance",bundled.getString("virtual-materials.target-chance"));
        var elements=y.getConfigurationSection("elements");
        if(elements!=null)for(String key:elements.getKeys(false)){
            String action=y.getString("elements."+key+".action");
            if("forge".equals(action)){
                defaults.set("elements."+key+".selection-lore",bundled.getStringList("elements.C.selection-lore"));
                defaults.set("elements."+key+".repair-name",bundled.getString("elements.C.repair-name"));
                defaults.set("elements."+key+".repair-lore",bundled.getStringList("elements.C.repair-lore"));
                defaults.set("elements."+key+".risk-lore",bundled.getStringList("elements.C.risk-lore"));
                String legacy="<#CBD5E1>攻击力 <#F8FAFC>{current} <#64748B>→ <#4ADE80>{next-attack}";
                String replacement=bundled.getStringList("elements.C.selection-lore").stream().filter(s->s.contains("{stat-name}")).findFirst().orElseThrow();
                for(String field:List.of("lore","selection-lore")){
                    String path="elements."+key+"."+field;
                    if(y.isSet(path)&&y.getStringList(path).contains(legacy))y.set(path,y.getStringList(path).stream().map(s->s.equals(legacy)?replacement:s).toList());
                }
            }
            if("lucky".equals(action)||"protect".equals(action))
                defaults.set("elements."+key+".availability-lore",bundled.getStringList("elements."+(action.equals("lucky")?"L":"P")+".availability-lore"));
        }
        y.setDefaults(defaults);
    }
    private static ConfigurationSection section(ConfigurationSection s,String key,String path) {
        var result=s.getConfigurationSection(key); check(result!=null,path+":"+key,s.get(key)); return result;
    }
    private static double number(ConfigurationSection s,String key,double min,double max,String path) {
        Object raw=s.get(key); check(raw instanceof Number,path+":"+key,raw); double value=((Number)raw).doubleValue();
        check(Double.isFinite(value)&&value>=min&&value<=max,path+":"+key,raw);return value;
    }
    private static double optionalNumber(ConfigurationSection s,String key,double fallback,double min,double max,String path){return s.contains(key)?number(s,key,min,max,"levels.yml:"+path):fallback;}
    private static void check(boolean ok,String path,Object raw) {if(!ok)throw new IllegalArgumentException(path+": invalid value '"+raw+"'");}
}
