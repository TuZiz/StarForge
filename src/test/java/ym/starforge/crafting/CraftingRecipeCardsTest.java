package ym.starforge.crafting;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;
import static org.junit.jupiter.api.Assertions.*;

class CraftingRecipeCardsTest {
    private YamlConfiguration menu() throws Exception {
        var y=new YamlConfiguration();y.load(Path.of("src/main/resources/gui/crafting.yml").toFile());return y;
    }
    static final class Icon extends ItemStack {
        final String model;
        Component name;
        List<Component> lore=List.of();
        @SuppressWarnings("unchecked")
        final ItemMeta meta=(ItemMeta)Proxy.newProxyInstance(ItemMeta.class.getClassLoader(),new Class<?>[]{ItemMeta.class},(p,m,a)->switch(m.getName()){
            case "lore" -> {if(a!=null&&a.length==1){lore=(List<Component>)a[0];yield null;}yield lore;}
            case "displayName" -> {if(a!=null&&a.length==1){name=(Component)a[0];yield null;}yield name;}
            default -> throw new UnsupportedOperationException(m.getName());
        });
        Icon(String model,Component name){super();this.model=model;this.name=name;}
        @Override public Component effectiveName(){return name;}
        @Override public ItemMeta getItemMeta(){return meta;}
        @Override public boolean editMeta(Consumer<? super ItemMeta> consumer){consumer.accept(meta);return true;}
    }
    static Inventory inventory(ItemStack[] slots){
        return (Inventory)Proxy.newProxyInstance(Inventory.class.getClassLoader(),new Class<?>[]{Inventory.class},(p,m,a)->switch(m.getName()){
            case "getItem" -> slots[(int)a[0]];
            case "setItem" -> {slots[(int)a[0]]=(ItemStack)a[1];yield null;}
            default -> throw new UnsupportedOperationException(m.getName());
        });
    }
    private void noItalic(Component c){assertEquals(TextDecoration.State.FALSE,c.decoration(TextDecoration.ITALIC));c.children().forEach(this::noItalic);}
    @Test void sixCardsCoverDisjointTwoByThreeAreas() throws Exception {
        var y=menu();var all=new HashSet<Integer>();
        for(int anchor:CraftingOrderLayout.slots(y,"recipe")){
            var cells=CraftingRecipeCards.cells(y,anchor);assertEquals(6,cells.size());
            for(int cell:cells)assertTrue(all.add(cell));
        }
        assertEquals(36,all.size());assertTrue(all.stream().allMatch(i->i<36));
        assertEquals(List.of(0,1,2,9,10,11),CraftingRecipeCards.cells(y,1));
        assertEquals(List.of(24,25,26,33,34,35),CraftingRecipeCards.cells(y,25));
    }
    @Test void userControlsInsideACardAreNotOverwritten() throws Exception {
        var y=menu();var rows=new ArrayList<>(y.getStringList("shape"));rows.set(1,"BSFFSFFSF");y.set("shape",rows);
        assertFalse(CraftingRecipeCards.cells(y,1).contains(9));
        assertEquals(List.of(0),CraftingRecipeCards.cells(y,0));
    }
    @Test void allSixCellsShareRealNameLoreAndClickWhileKeepingTheirModels() throws Exception {
        var y=menu();var s=new CraftingSession(null,CraftingScreen.RECIPES,CraftingScreen.CATEGORIES,null,null);
        ItemStack[] slots=new ItemStack[45];s.inventory=inventory(slots);s.recipes.put(1,"iron_sword");
        var weapon=new Icon("actual-result-model",Component.translatable("item.minecraft.iron_sword").decorate(TextDecoration.ITALIC));
        weapon.lore=List.of(Component.text("材料需求").append(Component.text("拥有 1 / 需要 8").decorate(TextDecoration.ITALIC)));
        var caption=new Icon("caption-model",Component.text("old CE caption"));caption.lore=List.of(Component.text("old caption lore"));
        slots[1]=weapon;slots[10]=caption;
        CraftingRecipeCards.paint(s,y,()->new Icon("transparent",Component.empty()));
        assertSame(weapon,slots[1]);assertSame(caption,slots[10]);
        for(int cell:List.of(0,1,2,9,10,11)){
            var item=(Icon)slots[cell];assertEquals(weapon.name,item.name);assertEquals(weapon.lore,item.lore);
            assertEquals("iron_sword",s.recipes.get(cell));noItalic(item.name);item.lore.forEach(this::noItalic);
        }
        assertEquals("actual-result-model",((Icon)slots[1]).model);assertEquals("caption-model",((Icon)slots[10]).model);
        assertNull(slots[4]);assertNull(slots[13]);assertFalse(s.recipes.containsKey(4));
        assertEquals(6,s.recipes.size());
    }
    @Test void emptyRecipeCardsNeverGainHoverItemsOrActions() throws Exception {
        var y=menu();var s=new CraftingSession(null,CraftingScreen.RECIPES,CraftingScreen.CATEGORIES,null,null);
        ItemStack[] slots=new ItemStack[45];s.inventory=inventory(slots);
        CraftingRecipeCards.paint(s,y,()->{throw new AssertionError("empty card must not create buttons");});
        assertTrue(s.recipes.isEmpty());assertTrue(Arrays.stream(slots).allMatch(Objects::isNull));
    }
}
