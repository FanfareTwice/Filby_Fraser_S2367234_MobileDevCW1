package org.me.gcu.filby_fraser_s2367234.ui;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import org.me.gcu.filby_fraser_s2367234.data.AppConfig;
import org.me.gcu.filby_fraser_s2367234.data.LoadStatus;
import org.me.gcu.filby_fraser_s2367234.data.TrafficRepository;
import org.me.gcu.filby_fraser_s2367234.model.Situation;
import org.me.gcu.filby_fraser_s2367234.util.GeoUtils;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

public class MainViewModel extends AndroidViewModel {

    public enum Tab { LIST, SEARCH }
    public enum Filter { ALL, INCIDENTS, ROADWORKS }
    public enum SearchMode { ROAD, DISTANCE }

    private final TrafficRepository repository;

    private final MutableLiveData<Filter> filter = new MutableLiveData<>(Filter.ALL);
    private final MediatorLiveData<List<Situation>> filteredSituations = new MediatorLiveData<>();
    private final MediatorLiveData<Situation> selected = new MediatorLiveData<>();
    private final MutableLiveData<Tab> currentTab = new MutableLiveData<>(Tab.LIST);

    private final MutableLiveData<SearchMode> searchMode = new MutableLiveData<>(SearchMode.ROAD);
    private final MutableLiveData<String> roadQuery = new MutableLiveData<>("");
    private final MutableLiveData<Integer> distanceKm = new MutableLiveData<>(25);
    private final MutableLiveData<SearchOrigin> origin = new MutableLiveData<>(defaultOrigin());
    private final MediatorLiveData<List<Situation>> searchResults = new MediatorLiveData<>();
    private final MediatorLiveData<List<String>> roadNames = new MediatorLiveData<>();

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

        searchResults.addSource(repository.getSituations(), x -> updateSearch());
        searchResults.addSource(searchMode, x -> updateSearch());
        searchResults.addSource(roadQuery, x -> updateSearch());
        searchResults.addSource(distanceKm, x -> updateSearch());
        searchResults.addSource(origin, x -> updateSearch());

        roadNames.addSource(repository.getSituations(), this::updateRoadNames);

        repository.startAutoUpdate();
    }

    public LiveData<List<Situation>> getSituations() { return repository.getSituations(); }
    public LiveData<List<Situation>> getFilteredSituations() { return filteredSituations; }
    public LiveData<Filter> getFilter() { return filter; }
    public LiveData<LoadStatus> getStatus() { return repository.getStatus(); }
    public LiveData<Situation> getSelected() { return selected; }
    public LiveData<Tab> getCurrentTab() { return currentTab; }
    public LiveData<SearchMode> getSearchMode() { return searchMode; }
    public LiveData<String> getRoadQuery() { return roadQuery; }
    public LiveData<Integer> getDistanceKm() { return distanceKm; }
    public LiveData<SearchOrigin> getOrigin() { return origin; }
    public LiveData<List<Situation>> getSearchResults() { return searchResults; }
    public LiveData<List<String>> getRoadNames() { return roadNames; }

    public void setFilter(Filter f) { filter.setValue(f); }
    public void refresh() { repository.refresh(); }
    public void select(Situation s) { selected.setValue(s); }
    public void clearSelection() { selected.setValue(null); }
    public void setTab(Tab tab) { if (tab != currentTab.getValue()) currentTab.setValue(tab); }
    public void setSearchMode(SearchMode m) { searchMode.setValue(m); }
    public void setRoadQuery(String q) { roadQuery.setValue(q == null ? "" : q.trim()); }
    public void setDistanceKm(int km) { distanceKm.setValue(km); }
    public void setOrigin(SearchOrigin o) { origin.setValue(o); }
    public void useDefaultOrigin() { origin.setValue(defaultOrigin()); }

    private static SearchOrigin defaultOrigin() {
        return new SearchOrigin(AppConfig.DEFAULT_LAT, AppConfig.DEFAULT_LON,
                AppConfig.DEFAULT_LOCATION_NAME, false);
    }

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

    private void updateSearch() {
        List<Situation> all = repository.getSituations().getValue();
        List<Situation> out = new ArrayList<>();
        if (all == null) {
            searchResults.setValue(out);
            return;
        }
        if (searchMode.getValue() == SearchMode.ROAD) {
            String q = roadQuery.getValue();
            if (q != null && !q.isEmpty()) {
                for (Situation s : all) {
                    if (s.isOnRoad(q)) out.add(s);
                }
            }
        } else {
            SearchOrigin o = origin.getValue();
            Integer km = distanceKm.getValue();
            if (o != null && km != null) {
                for (Situation s : all) {
                    if (GeoUtils.distanceToSituationKm(o.lat, o.lon, s) <= km) out.add(s);
                }
                out.sort(Comparator.comparingDouble(s -> GeoUtils.distanceToSituationKm(o.lat, o.lon, s)));
            }
        }
        searchResults.setValue(out);
    }

    private void updateRoadNames(List<Situation> list) {
        TreeSet<String> names = new TreeSet<>(MainViewModel::compareRoads);
        if (list != null) {
            for (Situation s : list) names.addAll(s.getRoads());
        }
        roadNames.setValue(new ArrayList<>(names));
    }

    // Motorways first, then B and A roads, each in number order (M8 before M74)
    static int compareRoads(String a, String b) {
        int byLetter = Character.compare(b.charAt(0), a.charAt(0));
        if (byLetter != 0) return byLetter;
        int byNumber = Integer.compare(roadNumber(a), roadNumber(b));
        return byNumber != 0 ? byNumber : a.compareTo(b);
    }

    private static int roadNumber(String road) {
        String digits = road.replaceAll("[^0-9]", "");
        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @Override
    protected void onCleared() {
        repository.shutdown();
        super.onCleared();
    }

    // The point that distance searches are measured from
    public static class SearchOrigin {
        public final double lat;
        public final double lon;
        public final String label;
        public final boolean fromDevice;

        public SearchOrigin(double lat, double lon, String label, boolean fromDevice) {
            this.lat = lat;
            this.lon = lon;
            this.label = label;
            this.fromDevice = fromDevice;
        }
    }
}
