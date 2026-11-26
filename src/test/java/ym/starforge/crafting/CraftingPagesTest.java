package ym.starforge.crafting;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import java.nio.file.Path;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class CraftingPagesTest {
    private YamlConfiguration menu(CraftingScreen screen) throws Exception {
        var y = new YamlConfiguration(); y.load(Path.of("src/main/resources/"+screen.file).toFile()); return y;
    }
    @Test void equipmentAndMachinesHaveSeparateFullCardHitAreas() throws Exception {
        var y=menu(CraftingScreen.CATEGORIES); CraftingConfig.validateMenu(CraftingScreen.CATEGORIES,y);
        assertEquals(List.of(0,1,2,3,4,5,9,10,11,12,13,14),CraftingOrderLayout.slots(y,"category"));
        assertEquals("weapons",CraftingCategories.resolve(y.getConfigurationSection("elements.E"),java.util.Set.of()));
        assertEquals("machines",CraftingCategories.resolve(y.getConfigurationSection("elements.M"),java.util.Set.of()));
        for (int center:List.of(7,19,22,25))
            assertEquals("none", y.getString("elements."+CraftingOrderLayout.symbol(y,center)+".action"));
        assertEquals(List.of(42,43,44),CraftingOrderLayout.slots(y,"orders"));
        assertEquals(List.of(39,40,41),CraftingOrderLayout.slots(y,"close"));
    }
    @Test void materialLabelsFollowMovedShapeAndRejectMismatches() throws Exception {
        var y=menu(CraftingScreen.DETAIL); var rows=new java.util.ArrayList<>(y.getStringList("shape"));
        rows.set(2,"FDFFDDFFF");y.set("shape",rows);
        assertTrue(assertThrows(IllegalArgumentException.class,()->CraftingConfig.validateMenu(CraftingScreen.DETAIL,y))
                .getMessage().contains("directly below"));
    }
    @Test void numericLabelsRetainExactCountsForSmallAndLargeRequirements() {
        assertEquals(List.of(0f,0f,0f,9f,11f,9f,0f,0f,0f),CraftingCountDisplay.digits(8,8));
        assertEquals(List.of(3f,4f,1f,5f,11f,3f,4f,1f,5f),CraftingCountDisplay.digits(2304,2304));
        assertEquals(9,CraftingCountDisplay.digits(0,1).size());
    }
    @Test void captionMappingsPointToEquipmentNamesAndCanBeOmitted() throws Exception {
        var y=menu(CraftingScreen.RECIPES); CraftingConfig.validateMenu(CraftingScreen.RECIPES,y);
        assertEquals("starforge:craft_name_iron_sword",y.getString("elements.S.captions.iron_sword"));
        assertEquals("starforge:craft_name_fuel_press",y.getString("elements.S.captions.fuel_press"));
        y.set("elements.S.captions.iron_sword","Bad:ID");
        assertThrows(IllegalArgumentException.class,()->CraftingConfig.validateMenu(CraftingScreen.RECIPES,y));
        y.set("elements.S.captions",null);
        assertDoesNotThrow(()->CraftingConfig.validateMenu(CraftingScreen.RECIPES,y));
    }
}
