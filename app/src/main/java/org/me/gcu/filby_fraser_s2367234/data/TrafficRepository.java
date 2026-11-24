package org.me.gcu.filby_fraser_s2367234.data;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import org.me.gcu.filby_fraser_s2367234.model.Situation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class TrafficRepository {

    private static final String TAG = "TrafficRepository";
    private static final String CACHE_UNPLANNED = "unplanned_cache.xml";
    private static final String CACHE_ROADWORKS = "roadworks_cache.xml";

    private final Context appContext;
    private final String clientId;
    private final String clientKey;
    private final SituationXmlParser parser = new SituationXmlParser();

    // A single background thread, so downloads never overlap
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> autoUpdateTask;

    private final MutableLiveData<List<Situation>> situations =
            new MutableLiveData<>(Collections.emptyList());
    private final MutableLiveData<LoadStatus> status =
            new MutableLiveData<>(new LoadStatus(LoadStatus.State.LOADING, "Loading...", 0));

    private final ConnectivityManager connectivityManager;
    private final ConnectivityManager.NetworkCallback networkCallback =
            new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(@NonNull Network network) {
                    // Connection is back - reload if we are showing saved data or an error
                    LoadStatus s = status.getValue();
                    if (s != null && (s.getState() == LoadStatus.State.OFFLINE_CACHED
                            || s.getState() == LoadStatus.State.ERROR)) {
                        refresh();
                    }
                }
            };

    public TrafficRepository(Context context) {
        appContext = context.getApplicationContext();
        clientId = readKey("TRAFFIC_CLIENT_ID");
        clientKey = readKey("TRAFFIC_CLIENT_KEY");
        connectivityManager = (ConnectivityManager)
                appContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        try {
            connectivityManager.registerDefaultNetworkCallback(networkCallback);
        } catch (Exception e) {
            Log.w(TAG, "Could not register network callback", e);
        }
    }

    public LiveData<List<Situation>> getSituations() { return situations; }
    public LiveData<LoadStatus> getStatus() { return status; }

    // Loads now, then again every REFRESH_INTERVAL_MINUTES
    public void startAutoUpdate() {
        if (autoUpdateTask != null) return;
        autoUpdateTask = executor.scheduleWithFixedDelay(this::loadData,
                0, AppConfig.REFRESH_INTERVAL_MINUTES, TimeUnit.MINUTES);
    }

    public void refresh() {
        executor.execute(this::loadData);
    }

    public void shutdown() {
        if (autoUpdateTask != null) autoUpdateTask.cancel(true);
        executor.shutdownNow();
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback);
        } catch (Exception ignored) { }
    }

    // Runs on the background thread; results are sent to the UI with postValue
    private void loadData() {
        long previousTime = status.getValue() != null ? status.getValue().getDataTimestamp() : 0;
        status.postValue(new LoadStatus(LoadStatus.State.LOADING, "Updating...", previousTime));

        boolean online = isOnline();
        FeedResult unplanned = loadFeed(AppConfig.UNPLANNED_EVENTS_URL, CACHE_UNPLANNED, online);
        FeedResult roadworks = loadFeed(AppConfig.ROADWORKS_URL, CACHE_ROADWORKS, online);

        if (unplanned.xml == null && roadworks.xml == null) {
            String msg = online
                    ? "Could not download data" + (unplanned.error != null ? " (" + unplanned.error + ")" : "")
                    : "No internet connection and no saved data";
            status.postValue(new LoadStatus(LoadStatus.State.ERROR, msg, 0));
            return;
        }

        List<Situation> all = new ArrayList<>();
        try {
            if (unplanned.xml != null) all.addAll(parser.parse(unplanned.xml, Situation.Category.INCIDENT));
            if (roadworks.xml != null) all.addAll(parser.parse(roadworks.xml, Situation.Category.ROADWORKS));
        } catch (Exception e) {
            Log.e(TAG, "Parsing failed", e);
            status.postValue(new LoadStatus(LoadStatus.State.ERROR,
                    "Could not read the traffic data", previousTime));
            return;
        }

        situations.postValue(all);
        long dataTime = Math.min(orMax(unplanned.timestamp), orMax(roadworks.timestamp));
        if (unplanned.fromCache || roadworks.fromCache) {
            String reason = online ? "Download failed" : "Offline";
            status.postValue(new LoadStatus(LoadStatus.State.OFFLINE_CACHED,
                    reason + " - showing saved data from " + formatTime(dataTime), dataTime));
        } else {
            status.postValue(new LoadStatus(LoadStatus.State.SUCCESS,
                    "Updated " + formatTime(dataTime), dataTime));
        }
    }

    private static long orMax(long t) { return t == 0 ? Long.MAX_VALUE : t; }

    // Downloads a feed and saves a copy; if that fails, uses the last saved copy
    private FeedResult loadFeed(String url, String cacheName, boolean online) {
        FeedResult r = new FeedResult();
        if (online) {
            try {
                r.xml = download(url);
                r.timestamp = System.currentTimeMillis();
                writeCache(cacheName, r.xml);
                return r;
            } catch (IOException e) {
                Log.e(TAG, "Download failed: " + url, e);
                r.error = e.getMessage();
            }
        }
        File cache = new File(appContext.getFilesDir(), cacheName);
        if (cache.exists()) {
            try {
                r.xml = readCache(cache);
                r.timestamp = cache.lastModified();
                r.fromCache = true;
            } catch (IOException e) {
                Log.e(TAG, "Cache read failed", e);
            }
        }
        return r;
    }

    private String download(String address) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(address).openConnection();
        connection.setConnectTimeout(AppConfig.CONNECT_TIMEOUT_MS);
        connection.setReadTimeout(AppConfig.READ_TIMEOUT_MS);
        String credentials = clientId + ":" + clientKey;
        connection.setRequestProperty("Authorization",
                Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8)));
        try {
            int code = connection.getResponseCode();
            if (code == HttpURLConnection.HTTP_UNAUTHORIZED || code == HttpURLConnection.HTTP_FORBIDDEN) {
                throw new IOException("access refused - check client ID and key");
            }
            if (code != HttpURLConnection.HTTP_OK) {
                throw new IOException("HTTP " + code);
            }
            try (Reader in = new BufferedReader(new InputStreamReader(
                    connection.getInputStream(), StandardCharsets.UTF_8))) {
                return readAll(in);
            }
        } finally {
            connection.disconnect();
        }
    }

    private static String readAll(Reader in) throws IOException {
        StringBuilder sb = new StringBuilder();
        char[] buffer = new char[8192];
        int n;
        while ((n = in.read(buffer)) != -1) sb.append(buffer, 0, n);
        return sb.toString();
    }

    private void writeCache(String name, String xml) {
        File f = new File(appContext.getFilesDir(), name);
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            w.write(xml);
        } catch (IOException e) {
            Log.e(TAG, "Cache write failed", e);
        }
    }

    private String readCache(File f) throws IOException {
        try (Reader in = new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8)) {
            return readAll(in);
        }
    }

    private boolean isOnline() {
        if (connectivityManager == null) return true;
        Network net = connectivityManager.getActiveNetwork();
        if (net == null) return false;
        NetworkCapabilities caps = connectivityManager.getNetworkCapabilities(net);
        return caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    private String formatTime(long millis) {
        if (millis == 0 || millis == Long.MAX_VALUE) return "unknown time";
        return DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(new Date(millis));
    }

    // Keys come from local.properties through the manifest meta-data
    private String readKey(String name) {
        try {
            ApplicationInfo info = appContext.getPackageManager().getApplicationInfo(
                    appContext.getPackageName(), PackageManager.GET_META_DATA);
            Object value = info.metaData == null ? null
                    : info.metaData.get(appContext.getPackageName() + "." + name);
            return value == null ? "" : String.valueOf(value);
        } catch (PackageManager.NameNotFoundException e) {
            return "";
        }
    }

    private static class FeedResult {
        String xml;
        long timestamp;
        boolean fromCache;
        String error;
    }
}
