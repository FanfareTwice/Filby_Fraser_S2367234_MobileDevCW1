package org.me.gcu.filby_fraser_s2367234.ui;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import org.me.gcu.filby_fraser_s2367234.data.LoadStatus;
import org.me.gcu.filby_fraser_s2367234.data.TrafficRepository;
import org.me.gcu.filby_fraser_s2367234.model.Situation;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class MainViewModel extends AndroidViewModel {

    public enum Filter { ALL, INCIDENTS, ROADWORKS }

    private final TrafficRepository repository;

    private final MutableLiveData<Filter> filter = new MutableLiveData<>(Filter.ALL);
    private final MediatorLiveData<List<Situation>> filteredSituations = new MediatorLiveData<>();
    private final MediatorLiveData<Situation> selected = new MediatorLiveData<>();

    public MainViewModel(@NonNull Application application) {
        super(application);
        repository = new TrafficRepository(application);

        filteredSituations.addSource(repository.getSituations(), list -> updateFiltered());
        filteredSituations.addSource(filter, f -> updateFiltered());

        // After a refresh, show the updated version of the selected situation
        selected.addSource(repository.getSituations(), list -> {
            Situation current = selected.getValue();
            if (current == null || list == null) return;
            for (Situation s : list) {
                if (s.getKey().equals(current.getKey())) {
                    selected.setValue(s);
                    return;
                }
            }
        });

        repository.startAutoUpdate();
    }

    public LiveData<List<Situation>> getSituations() { return repository.getSituations(); }
    public LiveData<List<Situation>> getFilteredSituations() { return filteredSituations; }
    public LiveData<Filter> getFilter() { return filter; }
    public LiveData<LoadStatus> getStatus() { return repository.getStatus(); }
    public LiveData<Situation> getSelected() { return selected; }

    public void setFilter(Filter f) { filter.setValue(f); }
    public void refresh() { repository.refresh(); }
    public void select(Situation s) { selected.setValue(s); }
    public void clearSelection() { selected.setValue(null); }

    private void updateFiltered() {
        List<Situation> all = repository.getSituations().getValue();
        Filter f = filter.getValue();
        List<Situation> out = new ArrayList<>();
        if (all != null) {
            for (Situation s : all) {
                if (f == Filter.ALL
                        || (f == Filter.INCIDENTS && s.getCategory() == Situation.Category.INCIDENT)
                        || (f == Filter.ROADWORKS && s.getCategory() == Situation.Category.ROADWORKS)) {
                    out.add(s);
                }
            }
        }
        // Incidents first, then newest first
        out.sort(Comparator.comparing(Situation::getCategory)
                .thenComparing(Situation::getStartTime,
                        Comparator.nullsLast(Comparator.<OffsetDateTime>reverseOrder())));
        filteredSituations.setValue(out);
    }

    @Override
    protected void onCleared() {
        repository.shutdown();
        super.onCleared();
    }
}
