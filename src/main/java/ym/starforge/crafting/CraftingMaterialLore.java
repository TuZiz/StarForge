package ym.starforge.crafting;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import ym.starforge.text.Texts;
import java.util.List;
import java.util.Map;

/** Formats recipe requirements without flattening custom names or vanilla translations. */
final class CraftingMaterialLore {
    record Requirement(Component name, int required, int owned) {}

    static List<Component> render(List<Requirement> requirements, String enough, String missing) {
        return requirements.stream().map(r -> {
            String template = r.owned() >= r.required() ? enough : missing;
            return Texts.plainStyle(Texts.parse(template, Map.of("required", "" + r.required(), "owned", "" + r.owned(),
                            "missing", "" + Math.max(0, r.required() - r.owned())))
                    .replaceText(TextReplacementConfig.builder().matchLiteral("{material}")
                            .replacement(Texts.plainStyle(r.name())).build()));
        }).toList();
    }
}
