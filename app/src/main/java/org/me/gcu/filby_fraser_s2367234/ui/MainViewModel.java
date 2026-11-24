package org.me.gcu.filby_fraser_s2367234.ui;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import org.me.gcu.filby_fraser_s2367234.data.LoadStatus;
import org.me.gcu.filby_fraser_s2367234.data.TrafficRepository;
import org.me.gcu.filby_fraser_s2367234.model.Situation;

import java.util.List;

// Survives rotation, so data is not downloaded again when the screen turns
public class MainViewModel extends AndroidViewModel {

    private final TrafficRepository repository;

    public MainViewModel(@NonNull Application application) {
        super(application);
        repository = new TrafficRepository(application);
        repository.startAutoUpdate();
    }

    public LiveData<List<Situation>> getSituations() { return repository.getSituations(); }
    public LiveData<LoadStatus> getStatus() { return repository.getStatus(); }

    public void refresh() { repository.refresh(); }

    @Override
    protected void onCleared() {
        repository.shutdown();
        super.onCleared();
    }
}
