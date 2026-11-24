/*  Starter project for Mobile Platform Development - 1st diet 26/27
    You should use this project as the starting point for your assignment.
    This project simply reads the data from the required source and displays the
    raw data in a TextField
*/

//
// Name                 Fraser Filby
// Student ID           S2367234
// Programme of Study   _________________
//

package org.me.gcu.filby_fraser_s2367234;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.view.View.OnClickListener;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;


public class MainActivity extends AppCompatActivity implements OnClickListener {
    private static final String TAG = "MainActivity";
    private static final int PREVIEW_LENGTH = 3000;

    private TextView rawDataDisplay;
    private Button startButton;

    // Read from local.properties via the manifest, so they are not in the source code
    private String clientId;
    private String clientKey;

    private static final String BASE_URL = "https://datex2.trafficscotland.org/rest/2.3/publications/";
    private static final String UNPLANNED_EVENTS_URL = BASE_URL + "UnplannedEvents/Content.xml";
    private static final String ROADWORKS_URL = BASE_URL + "CurrentRoadworks/Content.xml";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        rawDataDisplay = findViewById(R.id.rawDataDisplay);
        startButton = findViewById(R.id.startButton);
        startButton.setOnClickListener(this);

        clientId = readKey("TRAFFIC_CLIENT_ID");
        clientKey = readKey("TRAFFIC_CLIENT_KEY");
    }

    private String readKey(String name) {
        try {
            ApplicationInfo info = getPackageManager()
                    .getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA);
            Object value = info.metaData == null ? null : info.metaData.get(getPackageName() + "." + name);
            return value == null ? "" : String.valueOf(value);
        } catch (PackageManager.NameNotFoundException e) {
            return "";
        }
    }

    public void onClick(View aview) {
        startProgress();
    }

    public void startProgress() {
        rawDataDisplay.setText("Downloading...");
        // Network access must not run on the UI thread
        new Thread(new Task()).start();
    }

    private class Task implements Runnable {
        @Override
        public void run() {
            StringBuilder display = new StringBuilder();
            display.append(describe("Unplanned events", UNPLANNED_EVENTS_URL));
            display.append("\n\n");
            display.append(describe("Current roadworks", ROADWORKS_URL));

            // Temporary - replaced with a ViewModel and LiveData in a later stage
            MainActivity.this.runOnUiThread(() -> rawDataDisplay.setText(display.toString()));
        }

        private String describe(String name, String address) {
            try {
                String xml = download(address);
                String preview = xml.length() > PREVIEW_LENGTH
                        ? xml.substring(0, PREVIEW_LENGTH) + "\n..."
                        : xml;
                return "=== " + name + " (" + xml.length() + " characters) ===\n" + preview;
            } catch (IOException e) {
                Log.e(TAG, "Download failed: " + address, e);
                return "=== " + name + " ===\nDownload failed: " + e.getMessage();
            }
        }
    }

    private String download(String address) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(address).openConnection();
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(30000);
        String credentials = clientId + ":" + clientKey;
        connection.setRequestProperty("Authorization",
                Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8)));
        try {
            int code = connection.getResponseCode();
            if (code != HttpURLConnection.HTTP_OK) {
                throw new IOException("HTTP " + code + " " + connection.getResponseMessage());
            }
            StringBuilder result = new StringBuilder();
            try (BufferedReader in = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = in.readLine()) != null) {
                    result.append(line).append('\n');
                }
            }
            // Remove anything before the XML declaration
            int start = result.indexOf("<?");
            return start > 0 ? result.substring(start) : result.toString();
        } finally {
            connection.disconnect();
        }
    }
}