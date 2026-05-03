package org.igye.remem3.app.controllers2.counter;

import org.igye.remem3.app.state.StateConstructor;

public class CounterConstructor implements StateConstructor<CounterState> {
    @Override
    public String getName() {
        return "counter";
    }

    @Override
    public CounterState construct() {
        return new CounterState();
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

}
