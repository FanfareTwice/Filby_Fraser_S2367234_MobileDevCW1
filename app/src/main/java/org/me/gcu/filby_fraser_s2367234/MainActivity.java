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

import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import org.me.gcu.filby_fraser_s2367234.data.LoadStatus;
import org.me.gcu.filby_fraser_s2367234.model.LocationPoint;
import org.me.gcu.filby_fraser_s2367234.model.Situation;
import org.me.gcu.filby_fraser_s2367234.ui.MainViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class MainActivity extends AppCompatActivity {

    private static final int EXAMPLES_SHOWN = 3;

    private TextView statusText;
    private TextView rawDataDisplay;
    private MainViewModel viewModel;

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

        statusText = findViewById(R.id.statusText);
        rawDataDisplay = findViewById(R.id.rawDataDisplay);
        Button refreshButton = findViewById(R.id.startButton);

        // Data loads automatically; the button is only for a manual refresh
        viewModel = new ViewModelProvider(this).get(MainViewModel.class);
        refreshButton.setOnClickListener(v -> viewModel.refresh());

        viewModel.getStatus().observe(this, this::showStatus);
        viewModel.getSituations().observe(this, this::showSummary);
    }

    private void showStatus(LoadStatus status) {
        statusText.setText(status.getMessage());
        int colour;
        switch (status.getState()) {
            case SUCCESS: colour = Color.rgb(46, 125, 50); break;
            case OFFLINE_CACHED: colour = Color.rgb(230, 81, 0); break;
            case ERROR: colour = Color.rgb(198, 40, 40); break;
            default: colour = Color.GRAY; break;
        }
        statusText.setBackgroundColor(colour);
    }

    private void showSummary(List<Situation> situations) {
        List<Situation> incidents = new ArrayList<>();
        List<Situation> roadworks = new ArrayList<>();
        for (Situation s : situations) {
            if (s.getCategory() == Situation.Category.INCIDENT) incidents.add(s);
            else roadworks.add(s);
        }
        if (situations.isEmpty()) {
            rawDataDisplay.setText("No data yet");
            return;
        }
        rawDataDisplay.setText(summarise("Unplanned events", incidents)
                + "\n\n" + summarise("Current roadworks", roadworks));
    }

    private String summarise(String name, List<Situation> situations) {
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
