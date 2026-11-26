package ym.starforge.config;

import org.bukkit.configuration.file.YamlConfiguration;
import java.util.*;

public record ForgeSettings(int max, double pity, double lucky, int announce, String backend,
                            Map<Integer, Level> levels, Map<String, MaterialSpec> materials,
                            Map<String, Weapon> weapons, Map<String,String> messages,
                            YamlConfiguration gui, YamlConfiguration admin, LoreStyle loreStyle) {
    public record LoreStyle(java.util.regex.Pattern attackLine,java.util.regex.Pattern defenseLine) {
        public LoreStyle(java.util.regex.Pattern attackLine){this(attackLine,defenseDefault());}
        private static java.util.regex.Pattern defenseDefault(){return java.util.regex.Pattern.compile("^.*(?<!法术)(?<!魔法)(?:防御力|物理防御|防御)\\s*(?:»|:|：)?\\s*\\+?\\d+(?:\\.\\d+)?\\s*$");}
        public static LoreStyle defaults(){return new LoreStyle(java.util.regex.Pattern.compile("^.*(?:攻击力|攻击伤害)\\s*(?:»|:|：)?\\s*\\+?\\d+(?:\\.\\d+)?\\s*$"));}
    }
    public ForgeSettings(int max,double pity,double lucky,int announce,String backend,Map<Integer,Level> levels,
            Map<String,MaterialSpec> materials,Map<String,Weapon> weapons,Map<String,String> messages,YamlConfiguration gui,YamlConfiguration admin){
        this(max,pity,lucky,announce,backend,levels,materials,weapons,messages,gui,admin,LoreStyle.defaults());
    }
    public record Level(double chance, double cost, String stone, int amount, double flat, double percent, double downgrade,
                        double defenseFlat,double defensePercent,Map<String,Double> stoneChances,String requiredStone,double crackChance) {
        public Level { stoneChances=Map.copyOf(stoneChances); }
        public Level(double chance,double cost,String stone,int amount,double flat,double percent,double downgrade,double defenseFlat,double defensePercent){
            this(chance,cost,stone,amount,flat,percent,downgrade,defenseFlat,defensePercent,Map.of(),"",0);
        }
        public Level(double chance,double cost,String stone,int amount,double flat,double percent,double downgrade){this(chance,cost,stone,amount,flat,percent,downgrade,0.20,0.05);}
        public double stoneChance(String id,StoneRule rule){
            return stoneChances.getOrDefault(id,rule!=null&&rule.chance()!=null?rule.chance():chance);
        }
    }
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
        return bonus(base,level,weapon,false);
    }
    public double bonus(double base,int level,Weapon weapon,boolean armor) {
        double flat=0, percent=0;
        for (int i=1;i<=Math.min(level,max);i++) { var rule=weapon.level(i,levels);flat+=armor?rule.defenseFlat():rule.flat();percent+=armor?rule.defensePercent():rule.percent(); }
        return bonus(base,flat,percent)*weapon.growth();
    }
    public double chance(int target, int failures, boolean charm) {
        return Math.min(1, levels.get(target).chance() + Math.max(0,failures)*pity + (charm?lucky:0));
    }
}
