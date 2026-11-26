package ym.starforge.item;

/** One primary growth attribute per item; health and other quality stats are left untouched. */
public enum EnhancementStat {
    ATTACK("attack", "weapon"), DEFENSE("defense", "armor");
    private final String key, messagePrefix;
    EnhancementStat(String key,String messagePrefix){this.key=key;this.messagePrefix=messagePrefix;}
    public String key(){return key;}
    public String messagePrefix(){return messagePrefix;}
}
