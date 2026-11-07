package ym.starforge.transaction;

/** Target level controls its failure policy. Equipment is never destroyed. */
public record ForgeRoll(int level,int failures,boolean success,boolean downgraded) {
    public static ForgeRoll resolve(int old,int failures,double chance,double downChance,boolean protect,double successRoll,double downRoll){
        return resolveTarget(old,old+1,failures,chance,downChance,protect,successRoll,downRoll);
    }
    public static ForgeRoll resolveTarget(int old,int target,int failures,double chance,double downChance,boolean protect,double successRoll,double downRoll){
        boolean success=successRoll<chance;
        boolean down=!success&&!protect&&target>10&&downRoll<downChance;
        return new ForgeRoll(success?target:down?old-1:old,success?0:Math.min(1000000,failures+1),success,down);
    }
}
