package ym.starforge.transaction;

import ym.starforge.config.ForgeSettings;
import java.util.Map;

/** One shared preview/commit policy; tier rules never mutate existing weapon data. */
public record ForgePlan(int target,String material,int amount,double money,double chance,double downgrade) {
    public ForgePlan withStones(ForgeSettings config,ForgeSettings.Weapon weapon,int current,int failures,
                                boolean lucky,Map<String,Integer> stones){
        int total=0;
        double probability=0;
        for(var entry:stones.entrySet()){
            String id=entry.getKey();
            var spec=config.materials().get(id);
            if(spec==null||id.equals("lucky")||id.equals("protect"))return null;
            var rule=spec.stone();
            if(rule==null&&!id.equals(material))return null;
            if(rule!=null&&(!rule.allows(target)||rule.steps()!=target-current))return null;
            total+=entry.getValue();
            double perStone=rule!=null&&rule.chance()!=null?rule.chance():weapon.level(target,config.levels()).chance();
            probability+=perStone*entry.getValue();
        }
        if(total<amount)return null;
        return new ForgePlan(target,material,total,money,
                Math.min(1,probability+Math.max(0,failures)*config.pity()+(lucky?config.lucky():0)),downgrade);
    }
    public static ForgePlan prepare(ForgeSettings config,int current,int failures,boolean lucky){
        return prepare(config,ForgeSettings.defaults(),current,failures,lucky);
    }
    public static ForgePlan prepare(ForgeSettings config,ForgeSettings.Weapon weapon,int current,int failures,boolean lucky){
        return prepare(config,weapon,current,failures,lucky,1);
    }
    public static ForgePlan prepare(ForgeSettings config,ForgeSettings.Weapon weapon,int current,int failures,boolean lucky,int stones){
        if(weapon==null||current>=weapon.max(config.max()))return null;
        String id=weapon.level(current+1,config.levels()).stone();
        var spec=config.materials().get(id);
        var rule=spec.stone();
        int target=current+(rule==null?1:rule.steps());
        if(target>weapon.max(config.max())||(rule!=null&&!rule.allows(target)))return null;
        var level=weapon.level(target,config.levels());
        double base=rule!=null&&rule.chance()!=null?rule.chance():level.chance();
        return new ForgePlan(target,id,level.amount(),level.cost(),Math.min(1,base*Math.max(0,stones)+Math.max(0,failures)*config.pity()+(lucky?config.lucky():0)),level.downgrade());
    }
}
