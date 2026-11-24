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

    public MainViewModel(@NonNull Application application) {
        super(application);
        repository = new TrafficRepository(application);

        filteredSituations.addSource(repository.getSituations(), list -> updateFiltered());
        filteredSituations.addSource(filter, f -> updateFiltered());

        repository.startAutoUpdate();
    }

    public LiveData<List<Situation>> getSituations() { return repository.getSituations(); }
    public LiveData<List<Situation>> getFilteredSituations() { return filteredSituations; }
    public LiveData<Filter> getFilter() { return filter; }
    public LiveData<LoadStatus> getStatus() { return repository.getStatus(); }

    public void setFilter(Filter f) { filter.setValue(f); }
    public void refresh() { repository.refresh(); }

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
