package org.igye.remem3.app.state;

public interface StateLookup {
    <T> T getState(String stateId);
}
