package ym.starforge.crafting;

import net.kyori.adventure.text.Component;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import ym.starforge.text.Texts;
import java.util.*;

/** Builds recipe requirements from real item identities; does not alter inventory. */
final class CraftingRecipeDisplay {
    private CraftingRecipeDisplay() {}
    static List<Component> lore(Player player, CraftingRecipe recipe, CraftingItems items,
                                ConfigurationSection element, Map<String,String> values) {
        var requirements = new ArrayList<CraftingMaterialLore.Requirement>();
        var storage = player.getInventory().getStorageContents();
        for (var input : recipe.ingredients()) {
            var material = items.create(input); var meta = material.getItemMeta();
            Component name = meta.hasDisplayName() ? meta.displayName() : meta.hasItemName()
                    ? meta.itemName() : Component.translatable(material.getType().translationKey());
            int owned = Arrays.stream(storage).filter(i -> items.matches(i, input)).mapToInt(ItemStack::getAmount).sum();
            requirements.add(new CraftingMaterialLore.Requirement(name, input.amount(), owned));
        }
        var lines = CraftingMaterialLore.render(requirements,
                Objects.requireNonNull(element.getString("material-enough")),
                Objects.requireNonNull(element.getString("material-missing")));
        var lore = new ArrayList<Component>();
        for (String line : element.getStringList("lore")) {
            if (line.equals("{materials}")) lore.addAll(lines);
            else lore.add(Texts.parse(line, values));
        }
        return lore;
    }
}
