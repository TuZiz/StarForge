package ym.starforge.workstation;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Proxy;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class StationOriginsTest {
    @SuppressWarnings("unchecked") static <T>T proxy(Class<T> type,java.lang.reflect.InvocationHandler h) {
        return (T)Proxy.newProxyInstance(type.getClassLoader(),new Class<?>[]{type},h);
    }
    static class Fixture {
        UUID id=UUID.randomUUID(),worldId=UUID.randomUUID();int tick=50;
        World world=proxy(World.class,(p,m,a)->m.getName().equals("getUID")?worldId:null);
        Location location=new Location(world,1,64,1);
        Player player=proxy(Player.class,(p,m,a)->switch(m.getName()) {
            case "getUniqueId"->id;
            case "getTicksLived"->tick;
            case "getLocation"->location;
            default->throw new UnsupportedOperationException(m.getName());
        });
        StationOrigins origins=new StationOrigins();
        StationAnchor anchor=StationAnchor.of(UUID.randomUUID(),location,StationAnchor.Kind.ENHANCEMENT);
    }
    @Test void originIsOneShotAndExpiresNextTick() {
        var f=new Fixture();f.origins.offer(f.player,f.anchor);
        assertSame(f.anchor,f.origins.take(f.player,StationAnchor.Kind.ENHANCEMENT));
        assertNull(f.origins.take(f.player,StationAnchor.Kind.ENHANCEMENT));
        f.origins.offer(f.player,f.anchor);f.tick++;
        assertNull(f.origins.take(f.player,StationAnchor.Kind.ENHANCEMENT));
    }
    @Test void wrongKindIsConsumedAndTypedCommandsCanClearOrigin() {
        var f=new Fixture();f.origins.offer(f.player,f.anchor);
        assertNull(f.origins.take(f.player,StationAnchor.Kind.CRAFTING));
        assertNull(f.origins.take(f.player,StationAnchor.Kind.ENHANCEMENT));
        f.origins.offer(f.player,f.anchor);f.origins.clear(f.id);
        assertNull(f.origins.take(f.player,StationAnchor.Kind.ENHANCEMENT));
    }
    @Test void distantOrDifferentWorldNeverAnimatesOldStation() {
        var f=new Fixture();f.origins.offer(f.player,f.anchor);f.location.add(30,0,0);
        assertNull(f.origins.take(f.player,StationAnchor.Kind.ENHANCEMENT));
        f.location=new Location(f.world,1,64,1);f.worldId=UUID.randomUUID();
        f.origins.offer(f.player,f.anchor);
        assertNull(f.origins.take(f.player,StationAnchor.Kind.ENHANCEMENT));
    }
    @Test void reloadClearsAllOriginsAndPlayersAreIsolated() {
        var f=new Fixture();var g=new Fixture();f.origins.offer(f.player,f.anchor);
        assertNull(f.origins.take(g.player,StationAnchor.Kind.ENHANCEMENT));
        f.origins.clear();assertNull(f.origins.take(f.player,StationAnchor.Kind.ENHANCEMENT));
    }
}
