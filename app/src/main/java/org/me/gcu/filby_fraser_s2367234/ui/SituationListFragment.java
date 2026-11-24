package org.me.gcu.filby_fraser_s2367234.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import org.me.gcu.filby_fraser_s2367234.R;
import org.me.gcu.filby_fraser_s2367234.model.Situation;

public class SituationListFragment extends Fragment {

    private MainViewModel vm;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        vm = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        RecyclerView recycler = view.findViewById(R.id.recycler);
        TextView empty = view.findViewById(R.id.empty);
        ChipGroup chips = view.findViewById(R.id.filter_chips);
        Chip chipAll = view.findViewById(R.id.chip_all);
        Chip chipIncidents = view.findViewById(R.id.chip_incidents);
        Chip chipRoadworks = view.findViewById(R.id.chip_roadworks);
        View legend = view.findViewById(R.id.delay_legend);

        // Details screen is added in the next stage
        SituationAdapter adapter = new SituationAdapter(s ->
                Toast.makeText(requireContext(), s.getTitle(), Toast.LENGTH_SHORT).show());
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        recycler.setAdapter(adapter);

        MainViewModel.Filter current = vm.getFilter().getValue();
        if (current == MainViewModel.Filter.INCIDENTS) chipIncidents.setChecked(true);
        else if (current == MainViewModel.Filter.ROADWORKS) chipRoadworks.setChecked(true);
        else chipAll.setChecked(true);

        chips.setOnCheckedStateChangeListener((group, ids) -> {
            if (ids.isEmpty()) return;
            int id = ids.get(0);
            if (id == R.id.chip_incidents) vm.setFilter(MainViewModel.Filter.INCIDENTS);
            else if (id == R.id.chip_roadworks) vm.setFilter(MainViewModel.Filter.ROADWORKS);
            else vm.setFilter(MainViewModel.Filter.ALL);
        });

        vm.getSituations().observe(getViewLifecycleOwner(), all -> {
            int incidents = 0, roadworks = 0;
            if (all != null) {
                for (Situation s : all) {
                    if (s.getCategory() == Situation.Category.INCIDENT) incidents++;
                    else roadworks++;
                }
            }
            chipAll.setText(getString(R.string.filter_all, incidents + roadworks));
            chipIncidents.setText(getString(R.string.filter_incidents, incidents));
            chipRoadworks.setText(getString(R.string.filter_roadworks, roadworks));
        });

        vm.getFilteredSituations().observe(getViewLifecycleOwner(), list -> {
            adapter.submit(list);
            empty.setVisibility(list == null || list.isEmpty() ? View.VISIBLE : View.GONE);
        });

        // The colour key only applies to roadworks
        vm.getFilter().observe(getViewLifecycleOwner(), f ->
                legend.setVisibility(f == MainViewModel.Filter.INCIDENTS ? View.GONE : View.VISIBLE));
    }
}
