package ym.starforge.config;

import java.util.Map;

/** Exact shipped text upgrades in memory only; translations and files remain owned by the user. */
final class EnhancementTextDefaults {
    private static final Map<String,String> LEGACY=Map.ofEntries(
        Map.entry("level-suffix"," <#FDE047>✦ +{level}"),
        Map.entry("insert-weapon","<#FBBF24>⚠ <#475569>│ <#FDE68A>请先将单件武器放入左侧槽位。"),
        Map.entry("changed","<#FBBF24>⚠ <#475569>│ <#FDE68A>检测到主手武器已更换，请重新打开菜单。"),
        Map.entry("not-weapon","<#FBBF24>⚠ <#475569>│ <#FDE68A>请持有单件可强化的武器 <#64748B>(特殊或黑名单物品无法强化)<#FDE68A>。"),
        Map.entry("max","<#FDE047>★ <#475569>│ <#FEF08A>该武器已达到 <#FDE047><bold>最高强化上限</bold><#FEF08A>！"),
        Map.entry("stone-unavailable","<#FBBF24>⚠ <#475569>│ <#FDE68A>该强化石不适用于当前武器等级。"),
        Map.entry("success","<#4ADE80>✔ <#475569>│ <#86EFAC>星力共鸣，武器<#4ADE80><bold>强化成功</bold><#86EFAC>！"),
        Map.entry("failed","<#FBBF24>✦ <#475569>│ <#FDE68A>强化未成功，武器等级不变 <#64748B>(<#93C5FD>保底进度已提升<#64748B>)<#FDE68A>。"),
        Map.entry("downgraded","<#FB923C>▼ <#475569>│ <#FDBA74>强化失败！武器 <#FB923C>降低 1 级<#FDBA74>，本体已完好保留。"),
        Map.entry("announce","<#C084FC>✦ <#475569>│ <#E2E8F0>星芒闪耀！玩家 <#93C5FD>{player} <#E2E8F0>成功将武器强化至 <#FDE047><bold>+{level}</bold> <#E2E8F0>！"),
        Map.entry("set-success","<#4ADE80>✔ <#475569>│ <#86EFAC>已设置主手武器等级，保底进度已重置。"));
    static void upgrade(Map<String,String> loaded,Map<String,String> bundled){
        LEGACY.forEach((key,old)->{if(old.equals(loaded.get(key)))loaded.put(key,bundled.get(key));});
    }
}
