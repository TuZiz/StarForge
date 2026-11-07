package ym.starforge.item;

import net.kyori.adventure.text.*;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import ym.starforge.config.ForgeSettings;
import ym.starforge.text.Texts;
import java.util.*;

/** Reserved translation keys tag our exact suffix/fallback components, independently of PDC snapshots. */
public final class ForgeLorePresentation {
    private static final String MARKER="starforge.lore.attack_bonus";
    public static List<Component> strip(List<Component> input){
        var result=new ArrayList<Component>();
        for(Component line:input){
            if(own(line))continue;
            result.add(clean(line));
        }
        return result;
    }
    private static boolean own(Component c){return c instanceof TranslatableComponent t&&t.key().equals(MARKER);}
    private static Component clean(Component c){return c.children(c.children().stream().filter(child->!own(child)).map(ForgeLorePresentation::clean).toList());}
    public static List<Component> decorate(List<Component> input,int level,double bonus,ForgeSettings cfg){
        var result=strip(input);if(level<=0||bonus==0)return result;
        var values=Map.of("level",""+level,"bonus",compact(bonus));
        Component suffix=tag(Texts.parse(cfg.messages().get("weapon-attack-suffix"),values));
        for(int i=0;i<result.size();i++){
            String text=PlainTextComponentSerializer.plainText().serialize(result.get(i));
            if(cfg.loreStyle().attackLine().matcher(text.trim()).matches()){
                result.set(i,Texts.plainStyle(result.get(i).append(suffix)));return result;
            }
        }
        result.add(tag(Texts.parse(cfg.messages().get("weapon-bonus-line"),values)));return result;
    }
    private static Component tag(Component inner){return Texts.plainStyle(Component.translatable(MARKER,"").append(inner));}
    public static String compact(double number){return java.math.BigDecimal.valueOf(number).setScale(2,java.math.RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();}
}
