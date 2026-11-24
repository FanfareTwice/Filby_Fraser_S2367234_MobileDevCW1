package org.me.gcu.filby_fraser_s2367234.data;

public class LoadStatus {

    public enum State { LOADING, SUCCESS, OFFLINE_CACHED, ERROR }

    private final State state;
    private final String message;
    private final long dataTimestamp;   // when the data on screen was downloaded, 0 if unknown

    public LoadStatus(State state, String message, long dataTimestamp) {
        this.state = state;
        this.message = message;
        this.dataTimestamp = dataTimestamp;
    }

    public State getState() { return state; }
    public String getMessage() { return message; }
    public long getDataTimestamp() { return dataTimestamp; }
}
