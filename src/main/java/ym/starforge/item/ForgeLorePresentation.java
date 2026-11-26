package ym.starforge.item;

import net.kyori.adventure.text.*;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import ym.starforge.config.ForgeSettings;
import ym.starforge.text.Texts;
import java.util.*;

/** Reserved translation keys tag our exact suffix/fallback components, independently of PDC snapshots. */
public final class ForgeLorePresentation {
    private static final String MARKER="starforge.lore.attack_bonus";
    private static final String DEFENSE_MARKER="starforge.lore.defense_bonus";
    private static final String CRACK_MARKER="starforge.lore.crack";
    public static List<Component> strip(List<Component> input){
        var result=new ArrayList<Component>();
        for(Component line:input){
            if(own(line))continue;
            result.add(clean(line));
        }
        return result;
    }
    private static boolean own(Component c){return c instanceof TranslatableComponent t&&(t.key().equals(MARKER)||t.key().equals(DEFENSE_MARKER)||t.key().equals(CRACK_MARKER));}
    static List<Component> withCrack(List<Component> lore,boolean cracked,ForgeSettings cfg){
        if(cracked)lore.add(tag(Texts.parse(cfg.messages().get("crack-lore"),Map.of()),CRACK_MARKER));
        return lore;
    }
    private static Component clean(Component c){return c.children(c.children().stream().filter(child->!own(child)).map(ForgeLorePresentation::clean).toList());}
    public static List<Component> decorate(List<Component> input,int level,double bonus,ForgeSettings cfg){
        return decorate(input,level,bonus,cfg,EnhancementStat.ATTACK);
    }
    public static List<Component> decorate(List<Component> input,int level,double bonus,ForgeSettings cfg,EnhancementStat stat){
        var result=strip(input);if(level<=0||bonus==0)return result;
        var values=Map.of("level",""+level,"bonus",compact(bonus));
        boolean armor=stat==EnhancementStat.DEFENSE;
        String prefix=stat.messagePrefix(),marker=armor?DEFENSE_MARKER:MARKER;
        Component suffix=tag(Texts.parse(cfg.messages().get(armor?"armor-defense-suffix":"weapon-attack-suffix"),values),marker);
        for(int i=0;i<result.size();i++){
            String text=PlainTextComponentSerializer.plainText().serialize(result.get(i));
            if((armor?cfg.loreStyle().defenseLine():cfg.loreStyle().attackLine()).matcher(text.trim()).matches()){
                result.set(i,Texts.plainStyle(result.get(i).append(suffix)));return result;
            }
        }
        result.add(tag(Texts.parse(cfg.messages().get(prefix+"-bonus-line"),values),marker));return result;
    }
    private static Component tag(Component inner,String marker){return Texts.plainStyle(Component.translatable(marker,"").append(inner));}
    public static String compact(double number){return java.math.BigDecimal.valueOf(number).setScale(2,java.math.RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();}
}
