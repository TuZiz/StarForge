package ym.starforge.crafting;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class HarvestRecipeUnlockTest {
    @TempDir Path root;

    @Test void onlyThePlayerWhoHarvestedTheConfiguredCropLearnsTheRecipe() throws Exception {
        UUID player=UUID.randomUUID();
        var machine=new CraftingRecipe.Ingredient("craft-engine","genlite:fuel_press",1);
        var recipe=new CraftingRecipe("fuel_press","Press",List.of(),machine,0,30,"",false,"machines","golden_rice");
        try(var unlocks=new RecipeUnlocks(root.resolve("unlocks.yml"))) {
            assertFalse(unlocks.unlocked(player,recipe));
            var equipment=new CraftingRecipe("example","Equipment",List.of(),machine,0,30,"",true,"weapons");
            var otherMachine=new CraftingRecipe("machine_ii","Press II",List.of(),machine,0,30,"",false,"machines","wheat");
            var hidden=new CraftingRecipe("hidden","Hidden",List.of(),machine,0,30,"",false,"machines","");
            var all=List.of(equipment,recipe);
            var locked=unlocks.catalog(player,List.of(equipment,recipe,otherMachine,hidden));
            assertEquals(List.of(new CraftingCatalog.Entry(recipe,false),new CraftingCatalog.Entry(otherMachine,false)),
                    locked.entries("machines"));
            assertEquals(new CraftingCatalog.Progress(0,2),locked.progress("machines"));
            assertTrue(locked.progress("machines").locked());
            assertEquals(List.of(new CraftingCatalog.Entry(equipment,true)),locked.entries("weapons"));
            assertEquals("crafting-harvest-rice",RecipeUnlocks.requirementKey(recipe));
            unlocks.harvestMilestones((id,crop) -> id.equals(player) && crop.equals("golden_rice"));
            assertTrue(unlocks.unlocked(player,recipe));
            assertFalse(unlocks.unlocked(UUID.randomUUID(),recipe));
            var partial=unlocks.catalog(player,List.of(equipment,recipe,otherMachine,hidden));
            assertFalse(partial.progress("machines").locked());
            assertEquals(new CraftingCatalog.Progress(1,2),partial.progress("machines"));
            assertEquals(List.of(new CraftingCatalog.Entry(recipe,true),new CraftingCatalog.Entry(otherMachine,false)),
                    partial.entries("machines"));
            assertEquals(new CraftingCatalog.Progress(0,0),partial.progress("empty"));
            assertEquals(new CraftingCatalog.Progress(1,1),unlocks.catalog(player,all).progress("machines"));
            assertEquals(List.of(0f,0f,0f,0f,2f,11f,3f),CraftingUnlockArt.digits(partial.progress("machines")));
        }
    }
}
