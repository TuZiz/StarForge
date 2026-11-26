package ym.starforge.crafting;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import ym.starforge.text.Texts;
import ym.starforge.integration.MenuArt;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/** Presentation only: never consumes inputs, creates orders or grants their displayed copies. */
final class CraftingMenuRenderer {
    private final Plugin plugin;
    private final CraftingOrders orders;
    private final Texts texts;
    private final RecipeUnlocks unlocks;
    private final CraftingOrderMenuRenderer orderRenderer;
    private final CraftingItems items = new CraftingItems();
    private final Set<String> reported = ConcurrentHashMap.newKeySet();

    CraftingMenuRenderer(Plugin plugin, CraftingOrders orders, Texts texts, RecipeUnlocks unlocks) {
        this.plugin = plugin; this.orders = orders; this.texts = texts; this.unlocks = unlocks;
        orderRenderer = new CraftingOrderMenuRenderer(plugin, orders, texts);
    }
    Component title(Player player, CraftingSession h) {
        if(h.screen == CraftingScreen.ORDERS) return orderRenderer.title(player, h);
        var menu=h.revision.menu(h.screen);
        String title = h.screen == CraftingScreen.RECIPES ? h.revision.galleryTitle(h.category) : menu.getString("title");
        return MenuArt.title(Texts.parse(title,Map.of()),menu.getString("background-image"));
    }
    CraftingRecipe recipe(CraftingSession h) {
        return h.revision.recipes().stream().filter(r -> r.id().equals(h.recipeId)).findFirst().orElse(null);
    }
    CraftingJob job(Player player, CraftingSession h) {
        return orders.job(player.getUniqueId(), h.jobKey);
    }
    void paint(Player player, CraftingSession h) {
        if (h.screen == CraftingScreen.ORDERS) { orderRenderer.paint(player, h); return; }
        var y = h.revision.menu(h.screen);
        var catalog = unlocks.catalog(player.getUniqueId(),
                h.screen == CraftingScreen.DETAIL ? List.of() : h.revision.recipes());
        var recipes = h.screen==CraftingScreen.RECIPES
            ? catalog.entries(h.category) : List.<CraftingCatalog.Entry>of();
        var jobs = orders.jobs(player.getUniqueId()); var selected = recipe(h); var job = job(player, h);
        h.displayedJob = job;
        var ingredients = selected == null ? List.<CraftingRecipe.Ingredient>of() : selected.ingredients();
        int capacity = h.screen == CraftingScreen.CATEGORIES ? 1 : (int)y.getStringList("shape").stream().flatMapToInt(String::chars)
                .filter(c -> h.screen.contentAction.equals(y.getString("elements." + (char)c + ".action"))).count();
        int count = switch (h.screen) {
            case CATEGORIES -> 1;
            case RECIPES -> recipes.size();
            case ORDERS -> jobs.size();
            case DETAIL -> job == null ? h.jobKey == null ? ingredients.size() : 0 : 1;
        };
        int pages = Math.max(1, (count + capacity - 1) / capacity);
        h.page = Math.max(0, Math.min(h.page, pages - 1));
        var vars = new HashMap<>(values(selected, job));
        vars.put("page", "" + (h.page + 1)); vars.put("pages", "" + pages);
        h.inventory.clear(); h.actions.clear(); h.recipes.clear(); h.jobs.clear();
        int index = h.page * capacity, slot = 0;
        List<Integer> contentSlots = new ArrayList<>();
        Set<String> lockedCards = new HashSet<>();
        Map<String, CraftingCatalog.Progress> categoryProgress = new HashMap<>();
        for (String row : y.getStringList("shape")) for (char symbol : row.toCharArray()) {
            int target = slot++; var e = y.getConfigurationSection("elements." + symbol);
            if (e == null) continue;
            String action = e.getString("action", "none"); h.actions.put(target, action);
            if (action.equals(h.screen.contentAction)) contentSlots.add(target);
            try {
                if (!CraftingOrderMenuRenderer.navigationVisible(action, h.page, pages)) {
                    h.actions.remove(target); continue;
                }
                ItemStack item;
                switch (action) {
                    case "none" -> { continue; }
                    case "recipe" -> {
                        if (index >= count) continue;
                        var entry = recipes.get(index++);
                        var recipe = entry.recipe();
                        h.recipes.put(target, recipe.id());
                        item = items.preview(recipe.result());
                        if (entry.unlocked()) {
                            displayAmount(item, recipe.result().amount());
                            decorate(item, CraftingRecipeDisplay.lore(player, recipe, items, e, values(recipe, null)));
                        } else {
                            decorate(item, List.of(Component.empty(),
                                    texts.get("crafting-not-learned", Map.of()),
                                    texts.get(RecipeUnlocks.requirementKey(recipe), Map.of()),
                                    texts.get("crafting-locked-view", Map.of())));
                            if (CraftingUnlockArt.galleryCard(h, target)) {
                                var locked = CraftingUnlockArt.recipeIcon(e, recipe.id());
                                CraftingRecipeCards.applyText(locked, item.effectiveName(), item.getItemMeta().lore());
                                item = locked;
                                lockedCards.add(recipe.id());
                            }
                        }
                    }
                    case "order" -> {
                        if (index >= count) continue;
                        var j = jobs.get(index++); h.jobs.put(target, j);
                        item = items.orderPreview(j); decorate(item, e.getStringList("lore"), values(null, j));
                    }
                    case "ingredient" -> {
                        if (index >= count) continue;
                        if (job != null) {
                            if (contentSlots.size() != capacity / 2 + 1) continue;
                            index++; item = new ItemStack(Material.CLOCK);
                            item.editMeta(m -> m.displayName(texts.get("crafting-order-status", Map.of())));
                            decorate(item, e.getStringList("order-lore"), vars);
                        } else {
                            var input = ingredients.get(index++); item = items.create(input);
                            var inputVars = new HashMap<>(vars);
                            int owned = Arrays.stream(player.getInventory().getStorageContents()).filter(i -> items.matches(i, input))
                                    .mapToInt(ItemStack::getAmount).sum();
                            inputVars.put("owned", "" + owned); inputVars.put("required", "" + input.amount());
                            displayAmount(item, input.amount()); decorate(item, e.getStringList("lore"), inputVars);
                        }
                    }
                    case "recipe-caption" -> {
                        String id = h.recipes.get(target - 9);
                        if (id == null) { h.actions.remove(target); continue; }
                        String caption = e.getString("captions." + id);
                        if (caption == null) { h.actions.remove(target); continue; }
                        h.recipes.put(target, id); item = CraftingGuiIcons.ce(caption);
                        if (lockedCards.contains(id)) CraftingUnlockArt.lockedCaption(item, caption);
                    }
                    case "ingredient-count" -> {
                        if (job != null || h.jobKey != null) continue;
                        var slots = CraftingOrderLayout.slots(y, "ingredient");
                        int at = h.page * capacity + slots.indexOf(target - 9);
                        if (at < 0 || at >= ingredients.size()) continue;
                        var input = ingredients.get(at);
                        int owned = Arrays.stream(player.getInventory().getStorageContents())
                                .filter(i -> items.matches(i, input)).mapToInt(ItemStack::getAmount).sum();
                        item = CraftingCountDisplay.create(owned, input.amount());
                        var inputVars = new HashMap<>(vars);
                        inputVars.put("owned", "" + owned); inputVars.put("required", "" + input.amount());
                        item.editMeta(m -> m.displayName(Texts.parse(e.getString("name", ""), inputVars)));
                        decorate(item, e.getStringList("lore"), inputVars);
                    }
                    case "result" -> {
                        if (job != null) item = items.orderPreview(job);
                        else if (selected != null && h.jobKey == null) { item = items.preview(selected.result()); displayAmount(item, selected.result().amount()); }
                        else continue;
                        decorate(item, e.getStringList(job == null ? "lore" : "order-lore"), vars);
                    }
                    default -> {
                        item = CraftingGuiIcons.create(e);
                        String category = h.screen == CraftingScreen.CATEGORIES ? h.revision.categoryAt(target) : null;
                        if (category != null) {
                            renderCategory(item, h, target, category, e, categoryProgress.computeIfAbsent(category, catalog::progress));
                            break;
                        }
                        String state = job == null ? h.jobKey == null ? "idle" : "empty"
                                : !job.state().equals("running") ? "pending" : job.remaining() == 0 ? "ready" : "running";
                        String name = action.equals("confirm") ? e.getString(state + "-name", e.getString("name", "")) : e.getString("name", "");
                        if (!e.contains("craft-engine") || "starforge:gui_blank".equals(e.getString("craft-engine"))
                                || CraftingDecorationModels.model(e.getString("craft-engine")) != null)
                            item.editMeta(m -> m.displayName(Texts.parse(name, vars)));
                        String lorePath = action.equals("confirm") ? job != null ? "order-lore" : h.jobKey != null ? "empty-lore" : "lore" : "lore";
                        if(action.equals("confirm") && job==null && h.jobKey==null && selected!=null
                                && !unlocks.unlocked(player.getUniqueId(),selected)) {
                            item.editMeta(m -> m.displayName(texts.get("crafting-not-learned",Map.of())));
                            decorate(item,List.of(texts.get(RecipeUnlocks.requirementKey(selected),Map.of())));
                        } else decorate(item,e.getStringList(lorePath),vars);
                    }
                }
                h.inventory.setItem(target, item);
            } catch (Exception ex) {
                String key = h.screen + ":" + symbol + ":" + h.recipeId + ":" + ex.getMessage();
                if (reported.add(key)) plugin.getLogger().log(Level.WARNING,
                        "Crafting menu unavailable file=" + h.screen.file + " element=" + symbol + " player=" + player.getUniqueId(), ex);
                var barrier = new ItemStack(Material.BARRIER);
                barrier.editMeta(m -> m.displayName(texts.get("crafting-unavailable", Map.of()))); h.inventory.setItem(target, barrier);
            }
        }
        if (count == 0 && !contentSlots.isEmpty()) {
            var empty = new ItemStack(Material.BOOK);
            empty.editMeta(m -> m.displayName(texts.get(h.screen == CraftingScreen.RECIPES ? "crafting-empty" : "crafting-orders-empty", Map.of())));
            h.inventory.setItem(contentSlots.get(contentSlots.size() / 2), empty);
        }
        if (h.screen == CraftingScreen.RECIPES && "starforge:craft_gallery_six".equals(y.getString("background-image"))) {
            try {
                CraftingRecipeCards.paint(h,y,()->CraftingGuiIcons.ce("starforge:gui_blank"));
            } catch (RuntimeException ex) {
                if(reported.add("recipe-card:"+ex.getMessage()))plugin.getLogger().log(Level.WARNING,
                        "Crafting recipe card unavailable file="+h.screen.file+" player="+player.getUniqueId(),ex);
            }
        }
    }
    private void renderCategory(ItemStack item, CraftingSession session, int slot, String category,
                                ConfigurationSection element, CraftingCatalog.Progress progress) {
        if (element.isString("name"))
            item.editMeta(meta -> meta.displayName(Texts.parse(element.getString("name"), Map.of())));
        var vars = Map.of("unlocked", Integer.toString(progress.unlocked()), "visible", Integer.toString(progress.visible()));
        var lore = new ArrayList<Component>();
        lore.add(texts.get("crafting-category-progress", vars));
        if (progress.locked()) lore.add(texts.get(progress.visible() == 0 ? "crafting-category-empty" : "crafting-category-locked", Map.of()));
        else element.getStringList("lore").stream().map(line -> Texts.parse(line, vars)).forEach(lore::add);
        decorate(item, lore);
        if (CraftingUnlockArt.categoryCard(session, slot - 1, category)) CraftingUnlockArt.count(item, progress);
        else if (progress.locked() && CraftingUnlockArt.categoryCard(session, slot, category))
            CraftingUnlockArt.categoryIcon(item, element, slot, category);
    }
    private Map<String, String> values(CraftingRecipe recipe, CraftingJob job) {
        int progress = job == null ? 0 : job.progress();
        var vars = new HashMap<String, String>();
        vars.put("money", recipe == null ? "0" : "" + recipe.money());
        vars.put("duration", "" + (job != null ? (job.ready() - job.started()) / 1000 : recipe == null ? 0 : recipe.durationSeconds()));
        vars.put("seconds", "" + (job == null ? 0 : job.remaining())); vars.put("progress", "" + progress);
        vars.put("bar", "|".repeat(progress / 10) + "-".repeat(10 - progress / 10));
        vars.put("amount", "" + (job != null ? job.amount() : recipe == null ? 0 : recipe.result().amount()));
        vars.put("state", PlainTextComponentSerializer.plainText().serialize(texts.get(job == null ? "crafting-idle"
                : !job.state().equals("running") ? "crafting-pending" : job.remaining() == 0 ? "crafting-ready" : "crafting-running", Map.of())));
        return vars;
    }
    private static void displayAmount(ItemStack item, int amount) { item.setAmount(Math.min(amount, item.getMaxStackSize())); }
    static void decorate(ItemStack item, List<String> lines, Map<String, String> vars) {
        decorate(item, lines.stream().map(line -> Texts.parse(line, vars)).toList());
    }
    private static void decorate(ItemStack item, List<Component> lines) {
        item.editMeta(meta -> {
            var lore = meta.lore() == null ? new ArrayList<Component>() : new ArrayList<>(meta.lore());
            lore.addAll(lines);
            meta.lore(lore.stream().map(Texts::plainStyle).toList());
            if (meta.hasDisplayName()) meta.displayName(Texts.plainStyle(meta.displayName()));
            if (meta.hasItemName()) meta.itemName(Texts.plainStyle(meta.itemName()));
        });
    }
}
