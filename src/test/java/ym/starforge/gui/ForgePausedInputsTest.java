package ym.starforge.gui;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class ForgePausedInputsTest {
    record Inputs(String weapon,int firstStone,int secondStone){}
    @Test void successAndFailureRetainWholeOriginalInventoryUntilResume(){
        for(String result:new String[]{"success:+2","failure:+1"}){
            var states=new ForgePausedInputs<Inputs>();var id=UUID.randomUUID();
            var inputs=new Inputs(result,54,8);states.pause(id,inputs);
            assertTrue(states.contains(id,inputs));assertSame(inputs,states.take(id));
            assertNull(states.take(id));assertFalse(states.contains(id,inputs));
        }
    }
    @Test void cancelAndLateCallbackCannotOwnOrReturnSameInputsTwice(){
        var states=new ForgePausedInputs<Inputs>();var id=UUID.randomUUID();var inputs=new Inputs("weapon",54,4);
        states.pause(id,inputs);assertSame(inputs,states.take(id));
        assertFalse(states.take(id,inputs));assertNull(states.take(id));
    }
    @Test void staleSessionCannotRemoveNewSessionOrOverwriteExistingInputs(){
        var states=new ForgePausedInputs<Inputs>();var id=UUID.randomUUID();
        var old=new Inputs("old",1,0);var current=new Inputs("new",54,6);
        states.pause(id,old);assertThrows(IllegalStateException.class,()->states.pause(id,current));
        assertSame(old,states.take(id));states.pause(id,current);
        assertFalse(states.take(id,old));assertTrue(states.contains(id,current));
        assertTrue(states.take(id,current));assertNull(states.take(id));
    }
    @Test void playersAreIndependent(){
        var states=new ForgePausedInputs<Inputs>();var first=UUID.randomUUID();var second=UUID.randomUUID();
        var a=new Inputs("a",64,0);var b=new Inputs("b",12,4);
        states.pause(first,a);states.pause(second,b);assertSame(a,states.take(first));assertSame(b,states.take(second));
    }
}
