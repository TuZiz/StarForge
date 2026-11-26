package ym.starforge.transaction;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.Plugin;
import net.milkbowl.vault.economy.*;
import ym.starforge.config.ForgeSettings;
import ym.starforge.integration.MaterialItems;
import ym.starforge.item.*;
import ym.starforge.text.Texts;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.logging.Level;

/** Synchronous Vault transaction on the player's ownership thread. No delayed inventory writes.
 * All detached results are built before withdrawing. */
public final class ForgeTransaction {
    private final Plugin plugin; private final Supplier<ForgeSettings> config; private final WeaponService weapons;
    private final MaterialItems materials; private final Texts texts; private final OperationLog log;
    private final Set<UUID> busy=ConcurrentHashMap.newKeySet();
    private final Map<UUID,Long> cooldown=new ConcurrentHashMap<>();
    public ForgeTransaction(Plugin p,Supplier<ForgeSettings> c,WeaponService w,MaterialItems m,Texts t,OperationLog l){plugin=p;config=c;weapons=w;materials=m;texts=t;log=l;}
    public void execute(Player player,ItemStack expected,boolean lucky,boolean protect){
        execute(player,expected,lucky,protect,null,-1,List.of(),()->{},item->{},null);
    }
    public void execute(Player player,Inventory inputs,int weaponSlot,List<Integer> stoneSlots,boolean lucky,boolean protect){
        execute(player,inputs,weaponSlot,stoneSlots,lucky,protect,()->{});
    }
    public void execute(Player player,Inventory inputs,int weaponSlot,List<Integer> stoneSlots,
                        boolean lucky,boolean protect,Runnable beforeResult){
        execute(player,inputs,weaponSlot,stoneSlots,lucky,protect,beforeResult,item->{});
    }
    public void execute(Player player,Inventory inputs,int weaponSlot,List<Integer> stoneSlots,
                        boolean lucky,boolean protect,Runnable beforeResult,java.util.function.Consumer<ItemStack> onSuccess){
        execute(player,null,lucky,protect,inputs,weaponSlot,stoneSlots,beforeResult,onSuccess,null);
    }
    public void executeSelection(Player player,Inventory inputs,int weaponSlot,Map<String,Integer> selected,
                                 boolean lucky,boolean protect,Runnable beforeResult,java.util.function.Consumer<ItemStack> onSuccess){
        execute(player,null,lucky,protect,inputs,weaponSlot,List.of(),beforeResult,onSuccess,
                Collections.unmodifiableMap(new LinkedHashMap<>(selected)));
    }
    private void execute(Player player,ItemStack expected,boolean lucky,boolean protect,Inventory inputs,
                         int weaponSlot,List<Integer> stoneSlots,Runnable beforeResult,java.util.function.Consumer<ItemStack> onSuccess,
                         Map<String,Integer> selected){
        UUID uid=player.getUniqueId();long now=System.nanoTime();
        if(!busy.add(uid))return;
        try {
            if(now-cooldown.getOrDefault(uid,0L)<500_000_000L)return;
            cooldown.put(uid,now);
            if(!player.hasPermission("starforge.use")||!player.isOnline())return;
            ItemStack original=inputs==null?player.getInventory().getItemInMainHand():inputs.getItem(weaponSlot);
            if(original==null||original.getType().isAir()){texts.send(player,"insert-weapon");return;}
            if((inputs==null&&!original.equals(expected))||weapons.match(original)==null){texts.send(player,"changed");return;}
            original=original.clone();
            var stones=inputs==null?Map.<Integer,ItemStack>of():snapshotStones(inputs,stoneSlots);
            var cfg=config.get();
            if(ForgeData.cracked(original)){
                if(inputs==null||selected==null){texts.send(player,"crack-repair-required");return;}
                repair(player,inputs,weaponSlot,original,cfg);
                return;
            }
            var rule=weapons.match(original);int old=weapons.level(original);if(old>=rule.max(cfg.max())){texts.send(player,"max");return;}
            if(cfg.backend().equals("asterstats")&&!Bukkit.getPluginManager().isPluginEnabled("AsterStats")){texts.send(player,"backend-missing");return;}
            ItemStack[] before=cloneContents(player.getInventory().getStorageContents()),after=cloneContents(before);
            int hand=inputs==null?player.getInventory().getHeldItemSlot():-1;
            boolean useLucky=lucky&&take(after,"lucky",1,hand);
            boolean useProtect=protect&&take(after,"protect",1,hand);
            var basePlan=ForgePlan.prepare(cfg,rule,old,weapons.failures(original),useLucky);
            if(basePlan==null){texts.send(player,"stone-unavailable");return;}
            ForgePlan plan;
            var stoneWrites=new LinkedHashMap<Integer,ItemStack>();
            if(selected!=null){
                var selection=ForgeStoneSelection.prepare(cfg,rule,old,weapons.failures(original),useLucky,selected,
                        materials.countSelected(after,selected.keySet()));
                if(!selection.enough()){texts.send(player,"selection-shortage");return;}
                if(!basePlan.requiredStone().isEmpty()&&selected.getOrDefault(basePlan.requiredStone(),0)==0){
                    player.sendMessage(texts.get("required-stone-missing",Map.of("material",plainMaterial(basePlan.requiredStone()))));
                    return;
                }
                plan=selection.plan();
                if(plan!=null)for(var debit:selection.consumed().entrySet())
                    if(!take(after,debit.getKey(),debit.getValue(),hand)){texts.send(player,"selection-shortage");return;}
            }else if(inputs!=null){
                var spend=basePlan.spend(cfg,rule,old,weapons.failures(original),useLucky,
                    materials.describeStoneStacks(stones.values(),basePlan.material()));
                plan=spend==null?null:spend.plan();
                if(spend!=null){
                    int i=0;
                    for(var entry:stones.entrySet()){
                        int used=spend.quantities().get(i++);
                        if(used==0)continue;
                        var leftover=entry.getValue().clone();
                        leftover.setAmount(leftover.getAmount()-used);
                        stoneWrites.put(entry.getKey(),leftover.getAmount()==0?null:leftover);
                    }
                }
            }else{
                var amounts=new LinkedHashMap<String,Integer>();
                if(!basePlan.requiredStone().isEmpty())amounts.put(basePlan.requiredStone(),1);
                amounts.merge(basePlan.material(),basePlan.amount()-amounts.size(),Integer::sum);
                var spend=basePlan.spend(cfg,rule,old,weapons.failures(original),useLucky,
                    amounts.entrySet().stream().filter(entry->entry.getValue()>0).toList());
                plan=spend==null?null:spend.plan();
                if(plan!=null)for(var debit:amounts.entrySet())
                    if(!take(after,debit.getKey(),debit.getValue(),hand)){texts.send(player,"materials-missing");return;}
            }
            if(plan==null){texts.send(player,"materials-missing");return;}
            if(plan.money()>0&&!Bukkit.getPluginManager().isPluginEnabled("Vault")){texts.send(player,"economy-missing");return;}
            Economy economy=Bukkit.getPluginManager().isPluginEnabled("Vault")?Bukkit.getServicesManager().load(Economy.class):null;
            if(plan.money()>0&&economy==null){texts.send(player,"economy-missing");return;}
            var random=java.util.concurrent.ThreadLocalRandom.current();
            var roll=ForgeRoll.resolveTarget(old,plan.target(),weapons.failures(original),plan.chance(),plan.downgrade(),
                plan.crack(),useProtect,random.nextDouble(),random.nextDouble());
            boolean success=roll.success(),down=roll.downgraded();int result=roll.level();
            ItemStack target=original.clone();
            ForgeData.write(target,result,roll.failures());
            if(roll.cracked())ForgeData.setCracked(target,true);
            target=weapons.refresh(target);
            if(inputs==null)after[hand]=target;
            String tx=UUID.randomUUID().toString();
            log.record("PREPARED tx="+tx+" player="+uid+" old="+old+" new="+result+" money="+plan.money()+" stones="+plan.amount());
            String outcome=success?"SUCCESS":down?"DOWNGRADE":roll.cracked()?"CRACK":"FAIL";
            if(!commit(player,cfg,inputs,weaponSlot,original,target,before,after,stoneSlots,stones,stoneWrites,economy,plan.money(),tx,outcome))return;
            // Post-commit cosmetics are isolated: no visual failure can roll back or retry a trade.
            if(success)try{onSuccess.accept(target.clone());}
            catch(RuntimeException visual){plugin.getLogger().log(Level.WARNING,"Forge success visual failed player="+uid+" tx="+tx,visual);}
            beforeResult.run();
            texts.send(player,success?"success":down?"downgraded":roll.cracked()?"cracked":"failed");
            player.playSound(player.getLocation(),success?Sound.BLOCK_ANVIL_USE:Sound.BLOCK_ANVIL_LAND,1,success?1.3f:0.7f);
            player.spawnParticle(success?Particle.ENCHANT:Particle.SMOKE,player.getLocation().add(0,1,0),20,0.4,0.4,0.4,0.1);
            player.showTitle(net.kyori.adventure.title.Title.title(texts.get(success?"success-title":"failed-title",Map.of()),texts.get("result-title",Map.of("level",""+result))));
            if(success&&result>=cfg.announce()) {
                var message=texts.get("announce",Map.of("player",player.getName(),"level",""+result));
                Bukkit.getGlobalRegionScheduler().execute(plugin,()->Bukkit.getOnlinePlayers().forEach(p->p.getScheduler().execute(plugin,()->{if(p.isOnline())p.sendMessage(message);},null,1)));
            }
        }catch(Exception e){plugin.getLogger().log(Level.SEVERE,"Forge preparation failed player="+uid,e);texts.send(player,"error");}
        finally{busy.remove(uid);}
    }
    private void repair(Player player,Inventory inputs,int slot,ItemStack original,ForgeSettings cfg){
        var before=cloneContents(player.getInventory().getStorageContents());
        var after=cloneContents(before);
        if(!take(after,ForgeRepair.MATERIAL,ForgeRepair.AMOUNT,-1)){
            texts.send(player,"crack-repair-shortage");
            return;
        }
        var repaired=original.clone();
        ForgeData.setCracked(repaired,false);
        repaired=weapons.refresh(repaired);
        String tx=UUID.randomUUID().toString();
        log.record("PREPARED tx="+tx+" player="+player.getUniqueId()+" action=REPAIR stones="+ForgeRepair.AMOUNT);
        if(commit(player,cfg,inputs,slot,original,repaired,before,after,List.of(),Map.of(),Map.of(),null,0,tx,"REPAIR"))
            texts.send(player,"crack-repaired");
    }
    private String plainMaterial(String id){
        return net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(materials.displayName(id));
    }
    private boolean commit(Player player,ForgeSettings cfg,Inventory inputs,int weaponSlot,ItemStack original,ItemStack target,
                           ItemStack[] before,ItemStack[] after,List<Integer> stoneSlots,Map<Integer,ItemStack> stones,Map<Integer,ItemStack> stoneWrites,
                           Economy economy,double money,String tx,String outcome){
        var uid=player.getUniqueId();
        boolean paid=false,writing=false,writingInputs=false;
        try {
            if(money>0){
                EconomyResponse response=economy.withdrawPlayer(player,money);
                if(!response.transactionSuccess()){
                    texts.send(player,"money-missing");
                    log.record("DECLINED tx="+tx+" reason="+response.errorMessage);
                    return false;
                }
                paid=true;
            }
            if(!plugin.isEnabled()||!player.isOnline()||!player.hasPermission("starforge.use")||config.get()!=cfg
                ||!Arrays.equals(before,player.getInventory().getStorageContents()))
                throw new IllegalStateException("Inventory/config changed during forge commit");
            if(inputs!=null&&(player.getOpenInventory().getTopInventory()!=inputs||!original.equals(inputs.getItem(weaponSlot))
                ||!stones.equals(snapshotStones(inputs,stoneSlots))))
                throw new IllegalStateException("Forge inputs changed before commit");
            writing=true;
            player.getInventory().setStorageContents(after);
            if(inputs!=null){
                writingInputs=true;
                inputs.setItem(weaponSlot,target);
                stoneWrites.forEach(inputs::setItem);
            }
            log.record("COMMITTED tx="+tx+" player="+uid+" result="+outcome+" weapon="+target.getPersistentDataContainer().get(ForgeData.UID,org.bukkit.persistence.PersistentDataType.STRING));
            return true;
        }catch(Exception ex){
            if(writingInputs)try{inputs.setItem(weaponSlot,original);stones.forEach(inputs::setItem);}
            catch(Exception restore){plugin.getLogger().log(Level.SEVERE,"FORGE_INPUT_ROLLBACK_FAILED tx="+tx,restore);}
            if(writing)try{player.getInventory().setStorageContents(before);}
            catch(Exception restore){plugin.getLogger().log(Level.SEVERE,"INVENTORY_ROLLBACK_FAILED tx="+tx,restore);log.record("INVENTORY_ROLLBACK_FAILED tx="+tx);}
            if(paid)try{
                var refund=economy.depositPlayer(player,money);
                if(!refund.transactionSuccess())log.record("REFUND_FAILED tx="+tx+" money="+money+" reason="+refund.errorMessage);
            }catch(Exception refund){
                plugin.getLogger().log(Level.SEVERE,"REFUND_FAILED tx="+tx,refund);
                log.record("REFUND_FAILED tx="+tx+" money="+money);
            }
            log.record("ROLLED_BACK tx="+tx);
            plugin.getLogger().log(Level.SEVERE,"Forge transaction failed player="+uid+" tx="+tx+"; inspect account/inventory if rollback incomplete",ex);
            texts.send(player,"error");
            return false;
        }
    }
    private boolean take(ItemStack[] stacks,String id,int amount,int hand){for(int i=0;i<stacks.length&&amount>0;i++){if(i==hand||!materials.matches(stacks[i],id))continue;int count=Math.min(amount,stacks[i].getAmount());amount-=count;stacks[i].setAmount(stacks[i].getAmount()-count);if(stacks[i].getAmount()==0)stacks[i]=null;}return amount==0;}
    public static Map<Integer,ItemStack> snapshotStones(Inventory inventory,List<Integer> slots){
        Map<Integer,ItemStack> result=new LinkedHashMap<>();
        for(int slot:slots){
            ItemStack stack=inventory.getItem(slot);
            if(stack!=null&&!stack.getType().isAir())result.put(slot,stack.clone());
        }
        return result;
    }
    public static ItemStack[] cloneContents(ItemStack[] c){return Arrays.stream(c).map(i->i==null?null:i.clone()).toArray(ItemStack[]::new);}
    public void quit(UUID id){cooldown.remove(id);busy.remove(id);}
    public void close(){cooldown.clear();busy.clear();}
}
