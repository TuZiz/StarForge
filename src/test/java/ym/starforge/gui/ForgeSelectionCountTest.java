package ym.starforge.gui;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ForgeSelectionCountTest {
    @Test void iconsAndNumbersShareIncrementActionsAndUnsupportedClicksDoNothing(){
        assertEquals(1,ForgeStoneCards.delta(org.bukkit.event.inventory.ClickType.LEFT));
        assertEquals(-1,ForgeStoneCards.delta(org.bukkit.event.inventory.ClickType.RIGHT));
        assertEquals(5,ForgeStoneCards.delta(org.bukkit.event.inventory.ClickType.SHIFT_LEFT));
        assertEquals(-5,ForgeStoneCards.delta(org.bukkit.event.inventory.ClickType.SHIFT_RIGHT));
        for(var click:org.bukkit.event.inventory.ClickType.values())
            if(!java.util.Set.of("LEFT","RIGHT","SHIFT_LEFT","SHIFT_RIGHT").contains(click.name()))assertEquals(0,ForgeStoneCards.delta(click));
    }
    @Test void everySupportedQuantityUsesItsCenteredModelWithoutChangingDigits(){
        for(int number=0;number<=2304;number++){
            String text=Integer.toString(number);
            assertEquals(text.length()%2==0?"enhancement/count_even":"crafting/count",ForgeStoneCards.countModel(number));
            var values=ForgeStoneCards.digits(number);assertEquals(9,values.size());
            var visible=new StringBuilder();double sum=0;int count=0;
            for(int i=0;i<values.size();i++)if(values.get(i)>0){
                visible.append((int)(values.get(i)-1));
                sum+=-8+4*i+(text.length()%2==0?2:0);count++;
            }
            assertEquals(text,visible.toString());assertEquals(8,sum/count,.0001,"quantity "+number);
        }
    }
}
