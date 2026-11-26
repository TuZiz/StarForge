package ym.starforge.item;

import net.kyori.adventure.text.*;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;
import ym.starforge.text.Texts;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ForgeNamePresentationTest {
    private static final net.kyori.adventure.key.Key QUALITY=net.kyori.adventure.key.Key.key("itemhub:quality");
    private static final net.kyori.adventure.key.Key DEFAULT=net.kyori.adventure.key.Key.key("minecraft:default");
    private void visibleFonts(Component c,net.kyori.adventure.key.Key inherited,net.kyori.adventure.key.Key expected){
        var font=c.font()==null?inherited:c.font();
        if(c instanceof TextComponent t&&!t.content().isEmpty())assertEquals(expected,font,"visible text="+t.content());
        c.children().forEach(child->visibleFonts(child,font,expected));
    }
    @Test void suffixResetsQualityFontForSymbolsAndLevelDigits(){
        var suffix=ForgeNamePresentation.tag(Texts.parse(" <#FDE047>✦ +{level}",Map.of("level","2")));
        visibleFonts(suffix,QUALITY,DEFAULT);
        var base=Component.text("\uE600").font(QUALITY).append(base().font(DEFAULT));
        var enhanced=Texts.plainStyle(base.append(suffix));
        assertEquals(QUALITY,enhanced.font());assertEquals(DEFAULT,enhanced.children().getFirst().font());
        assertEquals(base,ForgeNamePresentation.strip(enhanced,base,enhanced,suffix));
        visibleFonts(cycle(suffix),QUALITY,DEFAULT);
    }
    @Test void configuredSuffixFontRemainsExplicit(){
        var font=net.kyori.adventure.key.Key.key("starforge:ui");
        var tagged=ForgeNamePresentation.tag(Texts.parse("<font:starforge:ui><#FDE047>+2</font>",Map.of()));
        visibleFonts(tagged,QUALITY,font);
    }
    Component base(){return Texts.parse("<#86EFAC>星芽短剑",Map.of());}
    Component suffix(int level){return Texts.parse(" <#FDE047>+{level}",Map.of("level",""+level));}
    Component cycle(Component c){var json=GsonComponentSerializer.gson();return json.deserialize(json.serialize(c));}
    String plain(Component c){return PlainTextComponentSerializer.plainText().serialize(c);}
    @Test void fiftyPreservedNameRefreshesHaveExactlyOneSuffix(){
        Component base=base(),last=null,current=base,own=null;
        for(int i=0;i<50;i++){
            base=ForgeNamePresentation.strip(cycle(current),base,last,own);
            own=ForgeNamePresentation.tag(suffix(1));current=Texts.plainStyle(base.append(own));last=cycle(current);
            assertEquals("星芽短剑 +1",plain(current));
        }
    }
    @Test void existingRecordedDuplicatesAreRemovedFromPollutedBase(){
        var suffix=suffix(1);var base=base().append(suffix).append(suffix);var last=Texts.plainStyle(base.append(suffix));
        assertEquals("星芽短剑",plain(ForgeNamePresentation.strip(last,base,last,suffix)));
    }
    @Test void newConfigurationNameAndUnmarkedNaturalNumbersAreRetained(){
        var last=base().append(ForgeNamePresentation.tag(suffix(1)));
        var configured=Texts.parse("<#86EFAC>新名字 +1",Map.of());
        assertEquals(configured,ForgeNamePresentation.strip(configured,base(),last,ForgeNamePresentation.tag(suffix(1))));
        assertEquals(configured,ForgeNamePresentation.strip(configured,null,null,null));
    }
    @Test void foreignRenameAndOtherComponentsSurviveWhileOwnNestedMarkerIsRemoved(){
        var changed=Component.text("玩家改名").append(Component.text("外部前缀").append(ForgeNamePresentation.tag(suffix(3)))).append(Component.text(" 保留"));
        assertEquals("玩家改名外部前缀 保留",plain(ForgeNamePresentation.strip(changed,base(),base(),ForgeNamePresentation.tag(suffix(3)))));
    }
    @Test void unnamedVanillaSnapshotRestoresNoCustomName(){
        var name=Component.translatable("item.minecraft.iron_sword").append(ForgeNamePresentation.tag(suffix(1)));
        assertNull(ForgeNamePresentation.strip(name,null,name,ForgeNamePresentation.tag(suffix(1))));
    }
}
