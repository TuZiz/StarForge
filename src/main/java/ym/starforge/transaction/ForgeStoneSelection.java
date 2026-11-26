package ym.starforge.transaction;

import ym.starforge.config.ForgeSettings;
import java.util.*;

/** Immutable ordered quantities, shared by the virtual preview and committed backpack debit. */
public record ForgeStoneSelection(ForgePlan plan, Map<String,Integer> selected, Map<String,Integer> consumed,
                                  Map<String,Integer> owned, boolean enough) {
    public static final int MAX_QUANTITY=2304;
    public ForgeStoneSelection {
        selected=Collections.unmodifiableMap(new LinkedHashMap<>(selected));
        consumed=Collections.unmodifiableMap(new LinkedHashMap<>(consumed));
        owned=Map.copyOf(owned);
    }
    public static ForgeStoneSelection prepare(ForgeSettings cfg,ForgeSettings.Weapon rule,int current,int failures,
                                             boolean lucky,Map<String,Integer> selected,Map<String,Integer> owned) {
        var base=ForgePlan.prepare(cfg,rule,current,failures,lucky);
        var entries=new ArrayList<Map.Entry<String,Integer>>();boolean enough=true;
        for(var e:selected.entrySet()) {
            int n=e.getValue();
            if(n<0||n>MAX_QUANTITY||!cfg.materials().containsKey(e.getKey())
                    ||e.getKey().equals("lucky")||e.getKey().equals("protect"))throw new IllegalArgumentException("Invalid selected material: "+e);
            if(n>owned.getOrDefault(e.getKey(),0))enough=false;
            if(n>0)entries.add(Map.entry(e.getKey(),n));
        }
        var spend=base==null?null:base.spend(cfg,rule,current,failures,lucky,entries);
        var plan=spend==null?null:spend.plan();
        var consumed=new LinkedHashMap<String,Integer>();
        selected.keySet().forEach(id->consumed.put(id,0));
        if(spend!=null&&enough)for(int i=0;i<entries.size();i++)consumed.put(entries.get(i).getKey(),spend.quantities().get(i));
        return new ForgeStoneSelection(plan,selected,consumed,owned,enough);
    }
    public int totalSelected(){return selected.values().stream().mapToInt(Integer::intValue).sum();}
    public boolean ready(){return enough&&plan!=null;}
    public static int adjusted(int current,int delta,int owned) {
        if(delta<0)return Math.max(0,current+delta);
        // A shortage after an earlier craft is retained until the player explicitly reduces it.
        return current>=owned?current:Math.min(Math.min(MAX_QUANTITY,owned),current+delta);
    }
}
