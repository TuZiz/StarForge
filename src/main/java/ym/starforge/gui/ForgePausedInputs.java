package ym.starforge.gui;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Tracks original GUI inventories during the result screen; take transfers ownership exactly once. */
final class ForgePausedInputs<T> {
    private final ConcurrentHashMap<UUID,T> pending=new ConcurrentHashMap<>();
    void pause(UUID id,T inputs){
        if(pending.putIfAbsent(id,inputs)!=null)throw new IllegalStateException("Forge result already paused player="+id);
    }
    boolean contains(UUID id,T inputs){return pending.get(id)==inputs;}
    T take(UUID id){return pending.remove(id);}
    boolean take(UUID id,T inputs){return pending.remove(id,inputs);}
}
