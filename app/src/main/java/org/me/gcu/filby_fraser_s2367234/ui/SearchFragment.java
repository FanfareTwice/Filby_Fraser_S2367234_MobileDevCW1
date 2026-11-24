package org.me.gcu.filby_fraser_s2367234.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.slider.Slider;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;

import org.me.gcu.filby_fraser_s2367234.R;
import org.me.gcu.filby_fraser_s2367234.model.Situation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SearchFragment extends Fragment {

    private MainViewModel vm;
    private MaterialSwitch useLocationSwitch;
    private LocationManager locationManager;
    private LocationListener oneShotListener;

    private final ActivityResultLauncher<String> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    requestDeviceLocation();
                } else {
                    Toast.makeText(requireContext(), R.string.location_denied, Toast.LENGTH_LONG).show();
                    useLocationSwitch.setChecked(false);
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        vm = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        locationManager = (LocationManager) requireContext().getSystemService(Context.LOCATION_SERVICE);

        MaterialButtonToggleGroup modeToggle = view.findViewById(R.id.search_mode);
        View roadPanel = view.findViewById(R.id.road_panel);
        View distancePanel = view.findViewById(R.id.distance_panel);
        MaterialAutoCompleteTextView roadInput = view.findViewById(R.id.road_input);
        Slider slider = view.findViewById(R.id.distance_slider);
        TextView distanceLabel = view.findViewById(R.id.distance_label);
        TextView originLabel = view.findViewById(R.id.origin_label);
        useLocationSwitch = view.findViewById(R.id.use_location);
        TextView resultCount = view.findViewById(R.id.result_count);
        MaterialButton mapButton = view.findViewById(R.id.show_results_map);
        RecyclerView recycler = view.findViewById(R.id.results);

        SituationAdapter adapter = new SituationAdapter(vm::select);
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        recycler.setAdapter(adapter);

        modeToggle.check(vm.getSearchMode().getValue() == MainViewModel.SearchMode.DISTANCE
                ? R.id.mode_distance : R.id.mode_road);
        modeToggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            vm.setSearchMode(checkedId == R.id.mode_distance
                    ? MainViewModel.SearchMode.DISTANCE : MainViewModel.SearchMode.ROAD);
        });
        vm.getSearchMode().observe(getViewLifecycleOwner(), mode -> {
            boolean distance = mode == MainViewModel.SearchMode.DISTANCE;
            roadPanel.setVisibility(distance ? View.GONE : View.VISIBLE);
            distancePanel.setVisibility(distance ? View.VISIBLE : View.GONE);
            adapter.setOrigin(distance ? vm.getOrigin().getValue() : null);
        });

        // Suggestions come from the roads in the current data
        roadInput.setText(vm.getRoadQuery().getValue(), false);
        vm.getRoadNames().observe(getViewLifecycleOwner(), names -> roadInput.setAdapter(
                new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line,
                        names == null ? new ArrayList<String>() : names)));
        roadInput.setOnItemClickListener((parent, v, position, id) -> {
            vm.setRoadQuery((String) parent.getItemAtPosition(position));
            hideKeyboard(roadInput);
        });
        roadInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH
                    || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                vm.setRoadQuery(v.getText().toString());
                hideKeyboard(v);
                return true;
            }
            return false;
        });
        view.findViewById(R.id.road_search_button).setOnClickListener(v -> {
            vm.setRoadQuery(roadInput.getText().toString());
            hideKeyboard(roadInput);
        });

        Integer km = vm.getDistanceKm().getValue();
        slider.setValue(km == null ? 25 : km);
        distanceLabel.setText(getString(R.string.within_km, (int) slider.getValue()));
        slider.addOnChangeListener((s, value, fromUser) -> {
            distanceLabel.setText(getString(R.string.within_km, (int) value));
            if (fromUser) vm.setDistanceKm((int) value);
        });

        MainViewModel.SearchOrigin o = vm.getOrigin().getValue();
        useLocationSwitch.setChecked(o != null && o.fromDevice);
        useLocationSwitch.setOnClickListener(b -> {
            if (useLocationSwitch.isChecked()) {
                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED) {
                    requestDeviceLocation();
                } else {
                    permissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION);
                }
            } else {
                vm.useDefaultOrigin();
            }
        });
        vm.getOrigin().observe(getViewLifecycleOwner(), origin -> {
            originLabel.setText(getString(R.string.searching_from, origin.label));
            if (vm.getSearchMode().getValue() == MainViewModel.SearchMode.DISTANCE) adapter.setOrigin(origin);
        });

        vm.getSearchResults().observe(getViewLifecycleOwner(), (List<Situation> results) -> {
            adapter.submit(results);
            boolean hasQuery = vm.getSearchMode().getValue() == MainViewModel.SearchMode.DISTANCE
                    || !vm.getRoadQuery().getValue().isEmpty();
            int n = results == null ? 0 : results.size();
            resultCount.setText(hasQuery
                    ? getResources().getQuantityString(R.plurals.results_found, n, n)
                    : getString(R.string.search_hint_text));
            mapButton.setEnabled(n > 0);
        });


        mapButton.setOnClickListener(v -> {
            List<Situation> results = vm.getSearchResults().getValue();
            if (results == null || results.isEmpty()) return;
            String title = vm.getSearchMode().getValue() == MainViewModel.SearchMode.ROAD
                    ? getString(R.string.map_title_road, vm.getRoadQuery().getValue())
                    : getString(R.string.map_title_distance, vm.getDistanceKm().getValue(),
                        vm.getOrigin().getValue().label);
            vm.showOnMap(results, title);
        });
    }

    // Use the last known location straight away if it is recent, and ask every
    // enabled provider for a fresh fix (the emulator only sends fixes through GPS)
    @SuppressLint("MissingPermission")
    private void requestDeviceLocation() {
        stopLocationUpdates();
        List<String> providers = new ArrayList<>(locationManager.getProviders(true));
        providers.remove(LocationManager.PASSIVE_PROVIDER);
        if (providers.isEmpty()) {
            Toast.makeText(requireContext(), R.string.location_unavailable, Toast.LENGTH_LONG).show();
            useLocationSwitch.setChecked(false);
            vm.useDefaultOrigin();
            return;
        }

        Location best = null;
        for (String provider : providers) {
            Location l = locationManager.getLastKnownLocation(provider);
            if (l != null && (best == null || l.getTime() > best.getTime())) best = l;
        }
        if (best != null && System.currentTimeMillis() - best.getTime() < 2 * 60 * 1000) {
            setDeviceOrigin(best);
        } else {
            Toast.makeText(requireContext(), R.string.location_waiting, Toast.LENGTH_SHORT).show();
        }

        // Not a lambda: before API 30 all four methods are abstract
        oneShotListener = new LocationListener() {
            @Override public void onLocationChanged(@NonNull Location location) {
                setDeviceOrigin(location);
                stopLocationUpdates();
            }
            @Override public void onStatusChanged(String provider, int status, Bundle extras) { }
            @Override public void onProviderEnabled(@NonNull String provider) { }
            @Override public void onProviderDisabled(@NonNull String provider) { }
        };
        for (String provider : providers) {
            locationManager.requestLocationUpdates(provider, 0, 0, oneShotListener, Looper.getMainLooper());
        }
    }

    private void setDeviceOrigin(Location l) {
        if (!isAdded()) return;
        String label = String.format(Locale.UK, "%s (%.4f, %.4f)",
                getString(R.string.my_location), l.getLatitude(), l.getLongitude());
        vm.setOrigin(new MainViewModel.SearchOrigin(l.getLatitude(), l.getLongitude(), label, true));
    }

    private void stopLocationUpdates() {
        if (oneShotListener != null) {
            locationManager.removeUpdates(oneShotListener);
            oneShotListener = null;
        }
    }

    private void hideKeyboard(View v) {
        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
        v.clearFocus();
    }

    @Override
    public void onDestroyView() {
        stopLocationUpdates();
        super.onDestroyView();
    }
}
