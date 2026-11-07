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
        execute(player,expected,lucky,protect,null,-1,List.of());
    }
    public void execute(Player player,Inventory inputs,int weaponSlot,List<Integer> stoneSlots,boolean lucky,boolean protect){
        execute(player,null,lucky,protect,inputs,weaponSlot,stoneSlots);
    }
    private void execute(Player player,ItemStack expected,boolean lucky,boolean protect,Inventory inputs,int weaponSlot,List<Integer> stoneSlots){
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
            int stoneCount=stones.values().stream().mapToInt(ItemStack::getAmount).sum();
            var cfg=config.get();var rule=weapons.match(original);int old=weapons.level(original);if(old>=rule.max(cfg.max())){texts.send(player,"max");return;}
            if(cfg.backend().equals("asterstats")&&!Bukkit.getPluginManager().isPluginEnabled("AsterStats")){texts.send(player,"backend-missing");return;}
            var basePlan=ForgePlan.prepare(cfg,rule,old,weapons.failures(original),lucky);
            if(basePlan==null){texts.send(player,"stone-unavailable");return;}
            var plan=inputs==null?basePlan:basePlan.withStones(cfg,rule,old,weapons.failures(original),lucky,
                    materials.countStones(stones.values(),basePlan.material()));
            if(plan==null){texts.send(player,"materials-missing");return;}
            if(plan.money()>0&&!Bukkit.getPluginManager().isPluginEnabled("Vault")){texts.send(player,"economy-missing");return;}
            Economy economy=Bukkit.getPluginManager().isPluginEnabled("Vault")?Bukkit.getServicesManager().load(Economy.class):null;
            if(plan.money()>0&&economy==null){texts.send(player,"economy-missing");return;}
            ItemStack[] before=cloneContents(player.getInventory().getStorageContents()),after=cloneContents(before);
            int hand=inputs==null?player.getInventory().getHeldItemSlot():-1;
            if((inputs==null&&!take(after,plan.material(),plan.amount(),hand))||(lucky&&!take(after,"lucky",1,hand))||(protect&&!take(after,"protect",1,hand))){texts.send(player,"materials-missing");return;}
            var roll=ForgeRoll.resolveTarget(old,plan.target(),weapons.failures(original),plan.chance(),plan.downgrade(),protect,java.util.concurrent.ThreadLocalRandom.current().nextDouble(),java.util.concurrent.ThreadLocalRandom.current().nextDouble());
            boolean success=roll.success(),down=roll.downgraded();int result=roll.level();
            ItemStack target=original.clone();ForgeData.write(target,result,success?0:Math.min(1000000,weapons.failures(original)+1));target=weapons.refresh(target);if(inputs==null)after[hand]=target;
            String tx=UUID.randomUUID().toString();boolean paid=false,writing=false,writingInputs=false;
            log.record("PREPARED tx="+tx+" player="+uid+" old="+old+" new="+result+" money="+plan.money()+" stones="+(inputs==null?plan.amount():stoneCount));
            try {
                if(plan.money()>0){EconomyResponse response=economy.withdrawPlayer(player,plan.money());if(!response.transactionSuccess()){texts.send(player,"money-missing");log.record("DECLINED tx="+tx+" reason="+response.errorMessage);return;}paid=true;}
                // Re-entrant economy hooks must not have changed any storage slot or configuration.
                if(!plugin.isEnabled()||!player.isOnline()||config.get()!=cfg||!Arrays.equals(before,player.getInventory().getStorageContents()))throw new IllegalStateException("Inventory/config changed during economy call");
                if(inputs!=null&&(player.getOpenInventory().getTopInventory()!=inputs||!original.equals(inputs.getItem(weaponSlot))||!stones.equals(snapshotStones(inputs,stoneSlots))))throw new IllegalStateException("Forge inputs changed during economy call");
                writing=true;
                player.getInventory().setStorageContents(after);
                if(inputs!=null){
                    writingInputs=true;
                    inputs.setItem(weaponSlot,target);
                    stones.keySet().forEach(slot->inputs.setItem(slot,null));
                }
                log.record("COMMITTED tx="+tx+" player="+uid+" result="+(success?"SUCCESS":down?"DOWNGRADE":"FAIL")+" weapon="+target.getPersistentDataContainer().get(ForgeData.UID,org.bukkit.persistence.PersistentDataType.STRING));
            } catch(Exception ex) {
                // On re-entrant changes do not overwrite unrelated new inventory contents.
                boolean unchanged=Arrays.equals(before,player.getInventory().getStorageContents());
                if(writingInputs)try{inputs.setItem(weaponSlot,original);stones.forEach(inputs::setItem);}catch(Exception restore){plugin.getLogger().log(Level.SEVERE,"FORGE_INPUT_ROLLBACK_FAILED tx="+tx,restore);}
                if(writing)try{player.getInventory().setStorageContents(before);}catch(Exception restore){plugin.getLogger().log(Level.SEVERE,"INVENTORY_ROLLBACK_FAILED tx="+tx,restore);log.record("INVENTORY_ROLLBACK_FAILED tx="+tx);}
                if(paid)try{var refund=economy.depositPlayer(player,plan.money());if(!refund.transactionSuccess())log.record("REFUND_FAILED tx="+tx+" money="+plan.money()+" reason="+refund.errorMessage);}catch(Exception refund){plugin.getLogger().log(Level.SEVERE,"REFUND_FAILED tx="+tx,refund);log.record("REFUND_FAILED tx="+tx+" money="+plan.money());}
                log.record("ROLLED_BACK tx="+tx+" unchanged="+unchanged);
                plugin.getLogger().log(Level.SEVERE,"Forge transaction failed player="+uid+" tx="+tx+"; inspect account/inventory if rollback incomplete",ex);texts.send(player,"error");return;
            }
            texts.send(player,success?"success":down?"downgraded":"failed");
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
