package ym.starforge.crafting;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class CraftingWorkstationConfigTest {
    @Test void wideGuiArtExplicitlyOptsOutOfSingleSlotClipping() throws Exception {
        Path base=Path.of("craftengine/强化石/resourcepack/assets/starforge");
        for(String name:List.of("name_aster_head","name_aster_chest","name_aster_legs","name_aster_feet",
                "name_iron_sword","name_star_sword","craft_progress_empty","craft_progress_locked","craft_progress_pending",
                "craft_progress_000","craft_progress_100","count")) {
            String json=Files.readString(base.resolve("items/crafting/"+name+".json"));
            assertTrue(json.contains("\"oversized_in_gui\":true"),name+" must render beyond one 16px slot");
        }
    }
    @Test void ceButtonsValidateWithoutVanillaMaterialAndRejectInvalidIds() throws Exception {
        var y = menu();
        CraftingConfig.validateMenu(CraftingScreen.ORDERS, y);
        assertFalse(y.contains("elements.B.material"));
        for (Object value : List.of("stone", "Bad:ID", "", 123, "test:bad id")) {
            y.set("elements.B.craft-engine", value);
            assertTrue(assertThrows(IllegalArgumentException.class, () -> CraftingConfig.validateMenu(CraftingScreen.ORDERS,y))
                    .getMessage().contains("elements.B.craft-engine="));
        }
    }
    @Test void progressPairingFollowsShapeAndRejectsMissingOrShiftedBars() throws Exception {
        var y = menu(); var rows = new ArrayList<>(y.getStringList("shape"));
        rows.set(1,"FGFFFFFGF"); y.set("shape",rows);
        assertTrue(assertThrows(IllegalArgumentException.class, () -> CraftingConfig.validateMenu(CraftingScreen.ORDERS,y)).getMessage().contains("count must match"));
        rows.set(1,"FFGFGFFGF"); y.set("shape",rows);
        assertTrue(assertThrows(IllegalArgumentException.class, () -> CraftingConfig.validateMenu(CraftingScreen.ORDERS,y)).getMessage().contains("directly below"));
    }
    @Test void ceProgressDefinitionsCoverEveryPercentAndAllStates() throws Exception {
        for (int p = 0; p <= 100; p++) assertDecoration("craft_progress_" + String.format(Locale.ROOT,"%03d",p));
        for (String id : List.of("craft_slot_empty","craft_slot_locked","craft_progress_empty","craft_progress_locked","craft_progress_pending"))
            assertDecoration(id);
        for (String id : List.of("stone", "lucky", "gui_blank", "craft_progress_101", "craft_page_9_1"))
            assertNull(CraftingDecorationModels.model("starforge:" + id), id);
        assertNull(CraftingDecorationModels.model("other:craft_progress_050"));
        for (String name : List.of("iron_sword", "star_sword", "aster_head", "aster_chest", "aster_legs", "aster_feet"))
            assertDecoration("craft_name_" + name);
    }
    private void assertDecoration(String id) throws Exception {
        var model = CraftingDecorationModels.model("starforge:" + id);
        assertNotNull(model, id);
        String name = id.startsWith("craft_name_") ? "name_" + id.substring(11) : id;
        assertEquals("starforge:crafting/" + name, model.toString());
        Path base = Path.of("craftengine/强化石/resourcepack/assets/starforge");
        String definition = Files.readString(base.resolve("items/crafting/" + name + ".json"));
        assertTrue(definition.contains("\"type\":\"minecraft:model\""));
        assertTrue(definition.contains("starforge:item/crafting/" + name));
        assertTrue(Files.isRegularFile(base.resolve("models/item/crafting/" + name + ".json")));
    }
    private YamlConfiguration menu() throws Exception {
        var y = new YamlConfiguration(); y.load(Path.of("src/main/resources/gui/crafting-orders.yml").toFile()); return y;
    }

    @Test void centeredBackHasMatchingHitboxAndNoCloseAction() throws Exception {
        var y=menu(); CraftingConfig.validateMenu(CraftingScreen.ORDERS,y);
        assertEquals('B',y.getStringList("shape").get(4).charAt(4));
        assertEquals(List.of(40),CraftingOrderLayout.slots(y,"back"));
        assertTrue(CraftingOrderLayout.slots(y,"close").isEmpty());
        assertFalse(y.contains("elements.X"));
        assertTrue(CraftingOrderLayout.slots(y,"info").isEmpty());
        assertFalse(y.contains("elements.Q"));
        assertEquals(List.of(39),CraftingOrderLayout.slots(y,"previous"));
        assertEquals(List.of(41),CraftingOrderLayout.slots(y,"next"));
    }

    @Test void footerHidesUnavailableArrowsAndKeepsNavigationAcrossPages() {
        assertFalse(CraftingOrderMenuRenderer.navigationVisible("previous",0,1));
        assertFalse(CraftingOrderMenuRenderer.navigationVisible("next",0,1));
        assertTrue(CraftingOrderMenuRenderer.navigationVisible("info",0,1));
        assertTrue(CraftingOrderMenuRenderer.navigationVisible("back",0,1));
        assertTrue(CraftingOrderMenuRenderer.navigationVisible("close",0,1));
        assertTrue(CraftingOrderMenuRenderer.navigationVisible("next",0,3));
        assertTrue(CraftingOrderMenuRenderer.navigationVisible("previous",1,3));
        assertTrue(CraftingOrderMenuRenderer.navigationVisible("next",1,3));
        assertFalse(CraftingOrderMenuRenderer.navigationVisible("next",2,3));
    }

    @Test void pageIconDefinitionsMatchEverySupportedPageAndOverflow() throws Exception {
        assertDecoration("craft_nav_previous"); assertDecoration("craft_nav_next");
        for (int pages=1; pages<=9; pages++) for (int page=0; page<pages; page++) {
            String id=CraftingOrderMenuRenderer.pageIcon("starforge:craft_page_","starforge:craft_page_more",page,pages);
            assertDecoration(id.substring("starforge:".length()));
        }
        assertEquals("starforge:craft_page_more",CraftingOrderMenuRenderer.pageIcon("starforge:craft_page_","starforge:craft_page_more",11,12));
        assertDecoration("craft_page_more");
        for (String filename : List.of("crafting-orders-icons.yml", "crafting-orders-navigation.yml", "crafting-pages-icons.yml"))
            assertFalse(Files.exists(Path.of("craftengine/强化石/configuration", filename)));
    }

    @Test void pageIconConfigurationRejectsInvalidPrefixOrMissingOverflow() throws Exception {
        var y=menu();
        var rows=new ArrayList<>(y.getStringList("shape")); rows.set(4,"FFFPBNFQF"); y.set("shape",rows);
        y.createSection("elements.Q",Map.of("action","info","craft-engine","starforge:gui_blank",
                "page-craft-engine-prefix","starforge:craft_page_","overflow-craft-engine","starforge:craft_page_more"));
        y.set("elements.Q.page-craft-engine-prefix","Bad:ID");
        assertTrue(assertThrows(IllegalArgumentException.class,()->CraftingConfig.validateMenu(CraftingScreen.ORDERS,y))
                .getMessage().contains("elements.Q.page-craft-engine-prefix"));
        y.set("elements.Q.page-craft-engine-prefix","starforge:craft_page_");
        final var missing=y; missing.set("elements.Q.overflow-craft-engine",null);
        assertTrue(assertThrows(IllegalArgumentException.class,()->CraftingConfig.validateMenu(CraftingScreen.ORDERS,missing))
                .getMessage().contains("elements.Q.overflow-craft-engine"));
    }
}
