package ym.starforge.text;

import net.kyori.adventure.text.*;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import ym.starforge.config.ForgeSettings;
import java.util.*;
import java.util.function.Supplier;

public final class Texts {
    private final Supplier<ForgeSettings> settings;
    public Texts(Supplier<ForgeSettings> settings){this.settings=settings;}
    public static Component plainStyle(Component c) { return c.decoration(TextDecoration.ITALIC,false).children(c.children().stream().map(Texts::plainStyle).toList()); }
    public static Component parse(String text,Map<String,String> values) {
        Component c=MiniMessage.miniMessage().deserialize(text);
        for(var e:values.entrySet()) c=c.replaceText(TextReplacementConfig.builder().matchLiteral("{"+e.getKey()+"}").replacement(Component.text(e.getValue())).build());
        return plainStyle(c);
    }
    public Component get(String key,Map<String,String> values){return parse(settings.get().messages().getOrDefault(key,"<#FF6B6B>"+key),values);}
    public void send(CommandSender sender,String key){sender.sendMessage(get(key,Map.of()));}
}
