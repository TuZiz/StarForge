package ym.starforge.command;

import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import ym.starforge.gui.ForgeMenus;
import ym.starforge.integration.MaterialItems;
import ym.starforge.item.*;
import ym.starforge.text.Texts;
import java.util.*;

public final class ForgeCommand implements CommandExecutor,TabCompleter {
    private final java.util.function.Consumer<Player> crafting;
    private final Plugin plugin;private final ForgeMenus menus;private final MaterialItems materials;private final WeaponService weapons;private final Texts texts;private final Runnable reload;private final java.util.function.IntSupplier max;
    public ForgeCommand(Plugin p,ForgeMenus g,MaterialItems m,WeaponService w,Texts t,Runnable r,java.util.function.IntSupplier x,java.util.function.Consumer<Player> crafting){this.crafting=crafting;plugin=p;menus=g;materials=m;weapons=w;texts=t;reload=r;max=x;}
    public boolean onCommand(CommandSender s,Command c,String label,String[] a){
        if(a.length==0){if(s instanceof Player p)menus.open(p,false,false,false);else texts.send(s,"player-only");return true;}
        if(a[0].equalsIgnoreCase("craft")){if(!s.hasPermission("starforge.use"))texts.send(s,"no-permission");else if(a.length!=1)texts.send(s,"help");else if(s instanceof Player p)crafting.accept(p);else texts.send(s,"player-only");return true;}
        if(a.length==1&&a[0].equalsIgnoreCase("help")){texts.send(s,s.hasPermission("starforge.admin")?"admin-help":"help");return true;}
        if(!s.hasPermission("starforge.admin")){texts.send(s,"no-permission");return true;}
        try{switch(a[0].toLowerCase(Locale.ROOT)){
            case "admin" -> {if(a.length!=1)throw new IllegalArgumentException();if(s instanceof Player p)menus.open(p,true,false,false);else texts.send(s,"player-only");}
            case "reload" -> {if(a.length!=1)throw new IllegalArgumentException();reload.run();texts.send(s,"reload-requested");}
            case "bind" -> {if(a.length!=2||!(s instanceof Player p))throw new IllegalArgumentException();var item=p.getInventory().getItemInMainHand();if(item.getType().isAir())throw new IllegalArgumentException();String key=a[1];if(!Set.of("stone","intermediate_stone","advanced_stone","lucky","protect").contains(key))throw new IllegalArgumentException();item.editMeta(m->m.getPersistentDataContainer().set(ForgeData.MATERIAL,PersistentDataType.STRING,key));texts.send(s,"bound");}
            case "mark" -> {if((a.length!=2&&a.length!=3)||!(s instanceof Player p)||!Set.of("on","off").contains(a[1])||(a.length==3&&(!a[1].equals("on")||!a[2].matches("[\\p{L}\\p{N}_-]+"))))throw new IllegalArgumentException();var item=weapons.markWeapon(p.getInventory().getItemInMainHand(),a[1].equals("on"));if(a.length==3)item.editMeta(m->m.getPersistentDataContainer().set(ForgeData.RULE,PersistentDataType.STRING,a[2]));p.getInventory().setItemInMainHand(item);plugin.getLogger().info("Admin weapon mark player="+p.getUniqueId()+" enabled="+a[1]+" rule="+(a.length==3?a[2]:"default"));texts.send(s,"weapon-marked");}
            case "set" -> {if(a.length!=2||!(s instanceof Player p))throw new IllegalArgumentException();int level=Integer.parseInt(a[1]);var item=p.getInventory().getItemInMainHand();if(level<0||level>weapons.maxLevel(item)||weapons.match(item)==null)throw new IllegalArgumentException();var replacement=item.clone();ForgeData.write(replacement,level,0);p.getInventory().setItemInMainHand(weapons.refresh(replacement));plugin.getLogger().info("Admin level set player="+p.getUniqueId()+" level="+level);texts.send(s,"set-success");}
            case "give" -> {if(a.length<3||a.length>4)throw new IllegalArgumentException();Player p=Bukkit.getPlayerExact(a[1]);if(p==null){texts.send(s,"offline");return true;}int amount=a.length==4?Integer.parseInt(a[3]):1;if(amount<1||amount>2304)throw new IllegalArgumentException();String id=a[2];p.getScheduler().execute(plugin,()->{try{var template=materials.create(id);int left=amount;while(left>0){var item=template.clone();int count=Math.min(left,item.getMaxStackSize());item.setAmount(count);left-=count;p.getInventory().addItem(item).values().forEach(drop->p.getWorld().dropItemNaturally(p.getLocation(),drop));}texts.send(p,"received");}catch(Exception ex){plugin.getLogger().log(java.util.logging.Level.SEVERE,"Material give failed target="+p.getUniqueId()+" material="+id,ex);texts.send(p,"error");}},null,1);}
            default -> texts.send(s,"admin-help");
        }}catch(IllegalArgumentException ex){texts.send(s,"admin-help");}return true;
    }
    public List<String> onTabComplete(CommandSender s,Command c,String label,String[] a){if(!s.hasPermission("starforge.use")&&!s.hasPermission("starforge.admin"))return List.of();List<String> values=a.length==1?(s.hasPermission("starforge.admin")?List.of("craft","help","admin","give","set","bind","mark","reload"):List.of("craft","help")):!s.hasPermission("starforge.admin")?List.of():a.length==2&&a[0].equals("mark")?List.of("on","off"):a.length==2&&a[0].equals("give")?Bukkit.getOnlinePlayers().stream().map(Player::getName).toList():a.length==2&&a[0].equals("bind")||a.length==3&&a[0].equals("give")?List.of("stone","intermediate_stone","advanced_stone","lucky","protect"):List.of();return values.stream().filter(v->v.startsWith(a[a.length-1])).toList();}
}
