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

import org.me.gcu.filby_fraser_s2367234.data.SituationXmlParser;
import org.me.gcu.filby_fraser_s2367234.model.LocationPoint;
import org.me.gcu.filby_fraser_s2367234.model.Situation;
import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;


public class MainActivity extends AppCompatActivity implements OnClickListener {
    private static final String TAG = "MainActivity";
    private static final int EXAMPLES_SHOWN = 3;

    private final SituationXmlParser parser = new SituationXmlParser();
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
            display.append(summarise("Unplanned events", UNPLANNED_EVENTS_URL, Situation.Category.INCIDENT));
            display.append("\n\n");
            display.append(summarise("Current roadworks", ROADWORKS_URL, Situation.Category.ROADWORKS));

            // Temporary - replaced with a ViewModel and LiveData in a later stage
            MainActivity.this.runOnUiThread(() -> rawDataDisplay.setText(display.toString()));
        }

        private String summarise(String name, String address, Situation.Category category) {
            List<Situation> situations;
            try {
                situations = parser.parse(download(address), category);
            } catch (IOException e) {
                Log.e(TAG, "Download failed: " + address, e);
                return "=== " + name + " ===\nDownload failed: " + e.getMessage();
            } catch (XmlPullParserException e) {
                Log.e(TAG, "Parsing failed: " + address, e);
                return "=== " + name + " ===\nCould not parse the data: " + e.getMessage();
            }

            StringBuilder sb = new StringBuilder();
            sb.append("=== ").append(name).append(": ").append(situations.size()).append(" ===\n");

            Map<String, Integer> byType = new TreeMap<>();
            for (Situation s : situations) {
                byType.merge(s.getTypeLabel(), 1, Integer::sum);
            }
            sb.append("By type: ").append(byType).append("\n");

            for (int i = 0; i < Math.min(EXAMPLES_SHOWN, situations.size()); i++) {
                sb.append("\n").append(describe(situations.get(i)));
            }
            return sb.toString();
        }

        private String describe(Situation s) {
            StringBuilder sb = new StringBuilder();
            sb.append("- ").append(s.getTitle()).append(" [").append(s.getRecordId()).append("]\n");
            sb.append("  Road: ").append(s.getRoadName());
            if (s.getDirection() != null) sb.append(" (").append(s.getDirection()).append(")");
            sb.append("\n  ").append(s.getComment()).append("\n");
            sb.append("  Start: ").append(s.getStartTime()).append("\n");
            if (s.getEndTime() != null) sb.append("  End: ").append(s.getEndTime()).append("\n");
            if (s.getImpact().getDelaysType() != null) {
                sb.append("  Delays: ").append(s.getImpact().getDelaysType()).append("\n");
            }
            for (LocationPoint p : s.getLocations()) {
                sb.append(String.format(Locale.UK, "  %s: %s (%.5f, %.5f)%n",
                        p.getRoleLabel(), p.getDisplayName(), p.getLatitude(), p.getLongitude()));
            }
            return sb.toString();
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
