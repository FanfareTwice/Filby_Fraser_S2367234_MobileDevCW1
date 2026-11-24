package org.me.gcu.filby_fraser_s2367234.ui;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;

import org.me.gcu.filby_fraser_s2367234.R;
import org.me.gcu.filby_fraser_s2367234.model.Impact;
import org.me.gcu.filby_fraser_s2367234.model.LocationPoint;
import org.me.gcu.filby_fraser_s2367234.model.Situation;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class DetailFragment extends Fragment {

    private MainViewModel vm;
    private View content, placeholder;
    private ImageView icon;
    private TextView title, comment;
    private Chip categoryChip, delayChip;
    private LinearLayout sections;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        vm = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        boolean twoPane = getResources().getBoolean(R.bool.two_pane);

        content = view.findViewById(R.id.detail_content);
        placeholder = view.findViewById(R.id.detail_placeholder);
        icon = view.findViewById(R.id.detail_icon);
        title = view.findViewById(R.id.detail_title);
        comment = view.findViewById(R.id.detail_comment);
        categoryChip = view.findViewById(R.id.detail_category);
        delayChip = view.findViewById(R.id.detail_delay);
        sections = view.findViewById(R.id.detail_sections);
        MaterialButton close = view.findViewById(R.id.detail_close);
        MaterialButton showOnMap = view.findViewById(R.id.detail_show_map);

        close.setVisibility(twoPane ? View.GONE : View.VISIBLE);
        close.setOnClickListener(v -> vm.clearSelection());

        showOnMap.setOnClickListener(v -> {
            Situation s = vm.getSelected().getValue();
            if (s == null) return;
            vm.showOnMap(Collections.singletonList(s), s.getTitle());
            if (!twoPane) vm.clearSelection();   // portrait: reveal the map underneath
        });


        vm.getSelected().observe(getViewLifecycleOwner(), this::show);
    }

    private void show(@Nullable Situation s) {
        content.setVisibility(s == null ? View.GONE : View.VISIBLE);
        placeholder.setVisibility(s == null ? View.VISIBLE : View.GONE);
        if (s == null) return;

        int typeColour = SituationStyle.colourFor(requireContext(), s.getType());
        icon.setImageResource(SituationStyle.iconFor(s.getType()));
        icon.setBackgroundTintList(ColorStateList.valueOf(typeColour));
        title.setText(s.getTitle());
        comment.setText(s.getComment() != null ? s.getComment() : "No description provided.");

        categoryChip.setText(s.getCategory() == Situation.Category.ROADWORKS ? "Roadworks" : "Incident");

        Impact.DelayLevel level = s.getImpact().getDelayLevel();
        delayChip.setText(SituationStyle.delayLabel(level));
        delayChip.setChipBackgroundColor(ColorStateList.valueOf(SituationStyle.delayColour(requireContext(), level)));
        delayChip.setVisibility(s.getCategory() == Situation.Category.ROADWORKS || level != Impact.DelayLevel.NONE
                ? View.VISIBLE : View.GONE);

        sections.removeAllViews();

        // --- When
        section("When");
        row("Starts", SituationStyle.formatTime(s.getStartTime()));
        if (s.getEndTime() != null) row("Ends", SituationStyle.formatTime(s.getEndTime()));
        String duration = SituationStyle.formatDuration(s.getDuration());
        if (duration != null) row("Duration", duration);
        row("Validity", Situation.humanise(s.getValidityStatus()));

        // --- Impact
        Impact im = s.getImpact();
        section("Impact");
        row("Type", s.getTypeLabel());
        if (im.getNumberOfOperationalLanes() != null) {
            row("Lanes open", im.getNumberOfOperationalLanes() == 0
                    ? "None – road closed" : String.valueOf(im.getNumberOfOperationalLanes()));
        }
        if (im.getNumberOfLanesRestricted() != null) {
            row("Lanes restricted", String.valueOf(im.getNumberOfLanesRestricted()));
        }
        if (im.getCapacityRemaining() != null) row("Capacity remaining", im.getCapacityRemaining() + "%");
        row("Delays", im.getDelaysType() != null ? Situation.humanise(im.getDelaysType()) : "None reported");
        String delayTime = SituationStyle.formatDelaySeconds(im.getDelayTimeSeconds());
        if (delayTime != null) row("Expected delay", delayTime);
        if (s.getComplianceOption() != null) row("Compliance", Situation.humanise(s.getComplianceOption()));

        // --- Where
        section("Where");
        Set<String> roads = s.getRoads();
        if (!roads.isEmpty()) row(roads.size() > 1 ? "Roads" : "Road", String.join(", ", roads));
        if (s.getDirection() != null) row("Direction", Situation.humanise(s.getDirection()));
        if (s.getLocationType() != null) row("Location type", Situation.humanise(s.getLocationType()));
        for (LocationPoint p : s.getLocations()) {
            StringBuilder sb = new StringBuilder();
            if (p.getDisplayName() != null) sb.append(p.getDisplayName());
            if (p.getJunction() != null && p.getName() != null && !p.getName().contains(p.getJunction())) {
                sb.append(" (").append(p.getJunction()).append(")");
            }
            if (!p.getCrossingRoads().isEmpty()) {
                sb.append(sb.length() > 0 ? "\n" : "").append("Meets ").append(String.join(", ", p.getCrossingRoads()));
            }
            if (p.hasCoordinates()) {
                sb.append(sb.length() > 0 ? "\n" : "")
                  .append(String.format(Locale.UK, "%.5f, %.5f", p.getLatitude(), p.getLongitude()));
            }
            row(p.getRoleLabel(), sb.toString());
        }

        // --- About this report
        section("About this report");
        if (s.getSource() != null) row("Source", s.getSource());
        if (s.getProbabilityOfOccurrence() != null) row("Probability", Situation.humanise(s.getProbabilityOfOccurrence()));
        if (s.getInformationStatus() != null) row("Information status", Situation.humanise(s.getInformationStatus()));
        if (s.getSeverity() != null) row("Severity", Situation.humanise(s.getSeverity()));
        if (s.getRecordType() != null) row("Record type", Situation.humanise(s.getRecordType()));
        if (s.getCreationTime() != null) row("First reported", formatIso(s.getCreationTime()));
        if (s.getVersionTime() != null) row("Last updated", formatIso(s.getVersionTime()));
        row("Reference", s.getRecordId() + (s.getVersion() != null ? " (v" + s.getVersion() + ")" : ""));

        // Anything else in the feed that isn't shown above
        for (Map.Entry<String, String> e : s.getOtherFields().entrySet()) {
            String key = e.getKey();
            if (key.startsWith("situationRecord") || key.equals("sourceName") || key.endsWith("Type")) continue;
            String value = e.getValue();
            if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
                value = "true".equalsIgnoreCase(value) ? "Yes" : "No";
            }
            row(Situation.humanise(key), value);
        }
    }

    private static String formatIso(String iso) {
        try {
            return SituationStyle.formatTime(java.time.OffsetDateTime.parse(iso));
        } catch (Exception e) {
            return iso;
        }
    }

    private void section(String heading) {
        TextView tv = (TextView) getLayoutInflater().inflate(R.layout.detail_section, sections, false);
        tv.setText(heading);
        sections.addView(tv);
    }

    private void row(String label, String value) {
        if (value == null || value.isEmpty()) return;
        View row = getLayoutInflater().inflate(R.layout.detail_row, sections, false);
        ((TextView) row.findViewById(R.id.row_label)).setText(label);
        ((TextView) row.findViewById(R.id.row_value)).setText(value);
        sections.addView(row);
    }
}
