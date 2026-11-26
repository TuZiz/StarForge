package ym.starforge.transaction;

import ym.starforge.config.ForgeSettings;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** One shared preview/commit policy; tier rules never mutate existing weapon data. */
public record ForgePlan(int target,String material,int amount,double money,double chance,double downgrade,String requiredStone,double crack) {
    record Spend(ForgePlan plan,List<Integer> quantities) {}
    public ForgePlan withStones(ForgeSettings config,ForgeSettings.Weapon weapon,int current,int failures,
                                boolean lucky,List<Map.Entry<String,Integer>> stones){
        var spend=spend(config,weapon,current,failures,lucky,stones);
        return spend==null?null:spend.plan();
    }
    Spend spend(ForgeSettings config,ForgeSettings.Weapon weapon,int current,int failures,
                boolean lucky,List<Map.Entry<String,Integer>> stones){
        int requiredIndex=-1;
        var increments=new java.util.ArrayList<BigDecimal>();
        var quantities=new java.util.ArrayList<Integer>();
        var level=weapon.level(target,config.levels());
        for(int i=0;i<stones.size();i++){
            var entry=stones.get(i);
            String id=entry.getKey();
            var spec=config.materials().get(id);
            if(spec==null||id.equals("lucky")||id.equals("protect"))return null;
            var rule=spec.stone();
            if(rule==null&&!id.equals(material))return null;
            if(rule!=null&&(!rule.allows(target)||rule.steps()!=target-current))return null;
            if(requiredIndex<0&&id.equals(requiredStone)&&entry.getValue()>0)requiredIndex=i;
            increments.add(BigDecimal.valueOf(level.stoneChance(id,rule)));
            quantities.add(0);
        }
        if(!requiredStone.isEmpty()&&requiredIndex<0)return null;
        BigDecimal probability=BigDecimal.valueOf(config.pity()).multiply(BigDecimal.valueOf(Math.max(0,failures)));
        if(lucky)probability=probability.add(BigDecimal.valueOf(config.lucky()));
        int consumed=0;
        if(requiredIndex>=0){
            quantities.set(requiredIndex,1);
            probability=probability.add(increments.get(requiredIndex));
            consumed++;
        }
        for(int i=0;i<stones.size();i++){
            int used=quantities.get(i);
            while(used<stones.get(i).getValue()&&(consumed<amount||probability.compareTo(BigDecimal.ONE)<0)){
                probability=probability.add(increments.get(i));
                used++;
                consumed++;
            }
            quantities.set(i,used);
        }
        if(consumed<amount)return null;
        var plan=new ForgePlan(target,material,consumed,money,probability.min(BigDecimal.ONE).doubleValue(),downgrade,requiredStone,crack);
        return new Spend(plan,List.copyOf(quantities));
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
        double base=level.stoneChance(id,rule);
        return new ForgePlan(target,id,level.amount(),level.cost(),
            Math.min(1,base*Math.max(0,stones)+Math.max(0,failures)*config.pity()+(lucky?config.lucky():0)),
            level.downgrade(),level.requiredStone(),current==14&&target==15?level.crackChance():0);
    }
}
