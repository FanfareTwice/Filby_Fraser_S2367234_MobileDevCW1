package org.me.gcu.filby_fraser_s2367234.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import org.me.gcu.filby_fraser_s2367234.R;
import org.me.gcu.filby_fraser_s2367234.model.LocationPoint;
import org.me.gcu.filby_fraser_s2367234.model.Situation;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TrafficMapFragment extends Fragment {

    private static final LatLng SCOTLAND = new LatLng(56.8, -4.2);

    private MainViewModel vm;
    private GoogleMap map;
    private final Map<Marker, Situation> markerSituations = new HashMap<>();
    private final Map<Situation.Type, BitmapDescriptor> iconCache = new EnumMap<>(Situation.Type.class);
    private boolean firstFit = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        vm = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        firstFit = savedInstanceState == null;

        View banner = view.findViewById(R.id.map_banner);
        TextView bannerText = view.findViewById(R.id.map_banner_text);
        view.findViewById(R.id.map_show_all).setOnClickListener(v -> {
            firstFit = true;
            vm.showAllOnMap();
        });

        vm.getMapSelectionTitle().observe(getViewLifecycleOwner(), title -> {
            banner.setVisibility(title == null ? View.GONE : View.VISIBLE);
            if (title != null) bannerText.setText(getString(R.string.map_showing, title));
        });

        SupportMapFragment mapFragment = (SupportMapFragment)
                getChildFragmentManager().findFragmentById(R.id.google_map);
        if (mapFragment == null) return;
        mapFragment.getMapAsync(googleMap -> {
            if (getView() == null) return;
            map = googleMap;
            map.getUiSettings().setZoomControlsEnabled(true);
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(SCOTLAND, 6f));
            map.setOnInfoWindowClickListener(marker -> {
                Situation s = markerSituations.get(marker);
                if (s != null) vm.select(s);
            });
            vm.getMapSituations().observe(getViewLifecycleOwner(), this::draw);
        });

        // A new selection (e.g. search results) re-zooms the map to fit it
        vm.getMapSelectionTitle().observe(getViewLifecycleOwner(), t -> firstFit = true);
    }

    private void draw(@Nullable List<Situation> list) {
        if (map == null) return;
        map.clear();
        markerSituations.clear();
        if (list == null || list.isEmpty()) return;

        LatLngBounds.Builder bounds = new LatLngBounds.Builder();
        int plotted = 0;
        for (Situation s : list) {
            List<LocationPoint> pts = s.getMappablePoints();
            if (pts.isEmpty()) continue;

            if (s.isLinear()) {
                PolylineOptions line = new PolylineOptions()
                        .width(10f)
                        .color(SituationStyle.stripColour(requireContext(), s));
                for (LocationPoint p : pts) {
                    if (p.getRole() == LocationPoint.Role.FROM || p.getRole() == LocationPoint.Role.TO) {
                        line.add(new LatLng(p.getLatitude(), p.getLongitude()));
                    }
                }
                map.addPolyline(line);
            }
            LocationPoint c = s.getCentre();
            LatLng pos = new LatLng(c.getLatitude(), c.getLongitude());
            Marker m = map.addMarker(new MarkerOptions()
                    .position(pos)
                    .title(s.getTitle())
                    .snippet(snippet(s))
                    .anchor(0.5f, 0.5f)
                    .icon(iconFor(s)));
            if (m != null) markerSituations.put(m, s);
            for (LocationPoint p : pts) bounds.include(new LatLng(p.getLatitude(), p.getLongitude()));
            plotted++;
        }

        if (plotted > 0 && firstFit) {
            firstFit = false;
            View v = getView();
            int padding = Math.round(48 * getResources().getDisplayMetrics().density);
            if (plotted == 1) {
                LatLng only = bounds.build().getCenter();
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(only, 13f));
            } else if (v != null && v.getWidth() > 0) {
                map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(), padding));
            } else if (v != null) {
                v.post(() -> map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(), padding)));
            }
        }
    }

    private static String snippet(Situation s) {
        String text = s.getComment() == null ? "" : s.getComment();
        if (text.length() > 80) text = text.substring(0, 77) + "…";
        return text + "  (tap for details)";
    }

    private BitmapDescriptor iconFor(Situation s) {
        BitmapDescriptor d = iconCache.get(s.getType());
        if (d == null) {
            int size = Math.round(32 * getResources().getDisplayMetrics().density);
            d = BitmapDescriptorFactory.fromBitmap(SituationStyle.markerBitmap(requireContext(), s, size));
            iconCache.put(s.getType(), d);
        }
        return d;
    }
}
