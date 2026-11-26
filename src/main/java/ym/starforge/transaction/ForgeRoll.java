package ym.starforge.transaction;

/** Target level controls its failure policy. Equipment is never destroyed. */
public record ForgeRoll(int level,int failures,boolean success,boolean downgraded,boolean cracked) {
    public record Odds(double success,double unchanged,double downgrade,double crack) {}
    public static ForgeRoll resolve(int old,int failures,double chance,double downChance,boolean protect,double successRoll,double downRoll){
        return resolveTarget(old,old+1,failures,chance,downChance,protect,successRoll,downRoll);
    }
    public static ForgeRoll resolveTarget(int old,int target,int failures,double chance,double downChance,boolean protect,double successRoll,double downRoll){
        return resolveTarget(old,target,failures,chance,downChance,0,protect,successRoll,downRoll);
    }
    public static ForgeRoll resolveTarget(int old,int target,int failures,double chance,double downChance,double crackChance,boolean protect,double successRoll,double failureRoll){
        boolean success=successRoll<chance;
        double down=downRate(old,target,downChance,protect),crack=crackRate(old,target,crackChance,protect);
        boolean downgraded=!success&&failureRoll<down;
        boolean cracked=!success&&failureRoll>=down&&failureRoll<down+crack;
        return new ForgeRoll(success?target:downgraded?old-1:old,success?0:Math.min(1000000,Math.max(0,failures)+1),success,downgraded,cracked);
    }
    public static Odds odds(int old,ForgePlan plan,boolean protect){
        double failure=1-plan.chance();
        double down=downRate(old,plan.target(),plan.downgrade(),protect);
        double crack=crackRate(old,plan.target(),plan.crack(),protect);
        return new Odds(plan.chance(),failure*(1-down-crack),failure*down,failure*crack);
    }
    private static double downRate(int old,int target,double chance,boolean protect){
        return !protect&&old>10&&target>10?chance:0;
    }
    private static double crackRate(int old,int target,double chance,boolean protect){
        return !protect&&old==14&&target==15?chance:0;
    }
}
