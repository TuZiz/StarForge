package ym.starforge.config;

import org.bukkit.configuration.file.YamlConfiguration;
import java.util.*;

public record ForgeSettings(int max, double pity, double lucky, int announce, String backend,
                            Map<Integer, Level> levels, Map<String, MaterialSpec> materials,
                            Map<String, Weapon> weapons, Map<String,String> messages,
                            YamlConfiguration gui, YamlConfiguration admin, LoreStyle loreStyle) {
    public record LoreStyle(java.util.regex.Pattern attackLine) {
        public static LoreStyle defaults(){return new LoreStyle(java.util.regex.Pattern.compile("^.*(?:攻击力|攻击伤害)\\s*(?:»|:|：)?\\s*\\+?\\d+(?:\\.\\d+)?\\s*$"));}
    }
    public ForgeSettings(int max,double pity,double lucky,int announce,String backend,Map<Integer,Level> levels,
            Map<String,MaterialSpec> materials,Map<String,Weapon> weapons,Map<String,String> messages,YamlConfiguration gui,YamlConfiguration admin){
        this(max,pity,lucky,announce,backend,levels,materials,weapons,messages,gui,admin,LoreStyle.defaults());
    }
    public record Level(double chance, double cost, String stone, int amount, double flat, double percent, double downgrade) {}
    public record MaterialSpec(String vanilla, String ce, String binding, StoneRule stone) {}
    public record StoneRule(int minTarget,int maxTarget,int steps,Double chance) {
        public boolean allows(int target){return target>=minTarget&&target<=maxTarget;}
    }
    /** A special rule, not the global eligibility list. base is a legacy, last-resort fallback. */
    public record Weapon(String itemHub, String vanilla, double base, String marker,
                         boolean blacklisted, Integer limit, double growth, Map<Integer,Level> overrides,boolean legacyRegistered) {
        public Weapon(String itemHub,String vanilla,double base){this(itemHub,vanilla,base,null,false,null,1,Map.of(),true);}
        public Weapon(String itemHub,String vanilla,double base,String marker,boolean blacklisted,Integer limit,double growth,Map<Integer,Level> overrides){this(itemHub,vanilla,base,marker,blacklisted,limit,growth,overrides,false);}
        public Weapon { overrides=Map.copyOf(overrides); }
        public int max(int global){return limit==null?global:Math.min(limit,global);}
        public Level level(int target,Map<Integer,Level> defaults){return overrides.getOrDefault(target,defaults.get(target));}
    }
    public static Weapon defaults(){return new Weapon(null,null,0,null,false,null,1,Map.of(),false);}
    public static double bonus(double base, double flat, double percent) { return flat + base * percent; }
    public double bonus(double base, int level) {
        return bonus(base,level,defaults());
    }
    public double bonus(double base,int level,Weapon weapon) {
        double flat=0, percent=0;
        for (int i=1;i<=Math.min(level,max);i++) { var rule=weapon.level(i,levels);flat+=rule.flat();percent+=rule.percent(); }
        return bonus(base,flat,percent)*weapon.growth();
    }
    public double chance(int target, int failures, boolean charm) {
        return Math.min(1, levels.get(target).chance() + Math.max(0,failures)*pity + (charm?lucky:0));
    }
}
