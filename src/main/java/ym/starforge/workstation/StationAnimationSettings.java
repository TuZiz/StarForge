package ym.starforge.workstation;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.*;
import java.nio.charset.StandardCharsets;

public record StationAnimationSettings(boolean enabled, int ticks, double rise, double distance) {
    /** Async configuration phase only. Missing optional feature file uses bundled settings, never overwrites files. */
    public static StationAnimationSettings load(JavaPlugin plugin) throws Exception {
        var file=new File(plugin.getDataFolder(),"workstations.yml");
        var yaml=new YamlConfiguration();
        if(file.exists())yaml.load(file);
        else {
            try(var stream=plugin.getResource("workstations.yml")) {
                if(stream==null)throw new FileNotFoundException("Bundled workstations.yml");
                yaml.load(new InputStreamReader(stream,StandardCharsets.UTF_8));
            }
            plugin.getLogger().info("workstations.yml absent; using bundled animation settings without creating a file");
        }
        return parse(yaml);
    }
    public static StationAnimationSettings parse(YamlConfiguration yaml) {
        require(yaml.getInt("schema-version")==1,"schema-version",yaml.get("schema-version"));
        require(yaml.get("animation.enabled") instanceof Boolean,"animation.enabled",yaml.get("animation.enabled"));
        double ticks=number(yaml,"animation.duration-ticks",10,100);
        require(ticks==(int)ticks,"animation.duration-ticks",ticks);
        return new StationAnimationSettings(yaml.getBoolean("animation.enabled"),(int)ticks,
                number(yaml,"animation.rise-height",0.2,2),number(yaml,"animation.max-distance",2,16));
    }
    private static double number(YamlConfiguration yaml,String path,double min,double max) {
        Object value=yaml.get(path);
        require(value instanceof Number,path,value);
        double n=((Number)value).doubleValue();
        require(Double.isFinite(n)&&n>=min&&n<=max,path,value);
        return n;
    }
    private static void require(boolean valid,String path,Object value) {
        if(!valid)throw new IllegalArgumentException("workstations.yml:"+path+" invalid '"+value+"'");
    }
}
