package org.me.gcu.filby_fraser_s2367234.data;

public final class AppConfig {

    private AppConfig() { }

    private static final String BASE_URL = "https://datex2.trafficscotland.org/rest/2.3/publications/";
    public static final String UNPLANNED_EVENTS_URL = BASE_URL + "UnplannedEvents/Content.xml";
    public static final String ROADWORKS_URL = BASE_URL + "CurrentRoadworks/Content.xml";

    // Set to true when recording the video so the auto-update can be seen
    public static final boolean DEMO_MODE = false;
    public static final long REFRESH_INTERVAL_MINUTES = DEMO_MODE ? 2 : 60;

    public static final int CONNECT_TIMEOUT_MS = 15000;
    public static final int READ_TIMEOUT_MS = 30000;
}
