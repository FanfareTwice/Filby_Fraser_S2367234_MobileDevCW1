//
// Name                 Fraser Filby
// Student ID           S2367234
// Programme of Study   Software Development
//

package org.me.gcu.filby_fraser_s2367234;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.navigation.NavigationBarView;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import org.me.gcu.filby_fraser_s2367234.data.LoadStatus;
import org.me.gcu.filby_fraser_s2367234.ui.DetailFragment;
import org.me.gcu.filby_fraser_s2367234.ui.MainViewModel;
import org.me.gcu.filby_fraser_s2367234.ui.SearchFragment;
import org.me.gcu.filby_fraser_s2367234.ui.SituationListFragment;
import org.me.gcu.filby_fraser_s2367234.ui.TrafficMapFragment;

// Single activity; each screen is a fragment that shares MainViewModel
public class MainActivity extends AppCompatActivity {

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

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle(R.string.toolbar_title);

        TextView statusText = findViewById(R.id.status_text);
        LinearProgressIndicator progress = findViewById(R.id.progress);

        viewModel = new ViewModelProvider(this).get(MainViewModel.class);

        // On rotation the fragment manager restores the fragment itself
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.detail_container, new DetailFragment())
                    .commit();
        }

        NavigationBarView navigation = findViewById(R.id.navigation);
        // In portrait the details cover the tabs, so changing tab closes them
        navigation.setOnItemSelectedListener(item -> {
            closeDetailsIfCovering();
            viewModel.setTab(tabFor(item.getItemId()));
            return true;
        });
        navigation.setOnItemReselectedListener(item -> closeDetailsIfCovering());
        viewModel.getCurrentTab().observe(this, tab -> {
            showTab(tab);
            int id = menuIdFor(tab);
            if (navigation.getSelectedItemId() != id) navigation.setSelectedItemId(id);
        });

        // In portrait the details cover the list, so Back closes them first
        boolean twoPane = getResources().getBoolean(R.bool.two_pane);
        View detailContainer = findViewById(R.id.detail_container);
        OnBackPressedCallback closeDetails = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
                viewModel.clearSelection();
            }
        };
        getOnBackPressedDispatcher().addCallback(this, closeDetails);

        viewModel.getSelected().observe(this, s -> {
            if (!twoPane) {
                detailContainer.setVisibility(s != null ? View.VISIBLE : View.GONE);
                closeDetails.setEnabled(s != null);
            }
        });

        viewModel.getStatus().observe(this, status -> {
            progress.setVisibility(status.getState() == LoadStatus.State.LOADING
                    ? View.VISIBLE : View.GONE);
            statusText.setText(status.getMessage());
            int colour;
            switch (status.getState()) {
                case OFFLINE_CACHED: colour = R.color.status_offline; break;
                case ERROR: colour = R.color.status_error; break;
                default: colour = R.color.status_ok; break;
            }
            statusText.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, colour)));
        });
    }

    private void closeDetailsIfCovering() {
        if (!getResources().getBoolean(R.bool.two_pane)) viewModel.clearSelection();
    }

    private void showTab(MainViewModel.Tab tab) {
        String tag = tab.name();
        if (getSupportFragmentManager().findFragmentByTag(tag) != null) return;  // already showing
        Fragment f;
        switch (tab) {
            case SEARCH: f = new SearchFragment(); break;
            case MAP: f = new TrafficMapFragment(); break;
            default: f = new SituationListFragment(); break;
        }
        getSupportFragmentManager().beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.main_container, f, tag)
                .commit();
    }

    private static MainViewModel.Tab tabFor(int menuId) {
        if (menuId == R.id.nav_search) return MainViewModel.Tab.SEARCH;
        if (menuId == R.id.nav_map) return MainViewModel.Tab.MAP;
        return MainViewModel.Tab.LIST;
    }

    private static int menuIdFor(MainViewModel.Tab tab) {
        switch (tab) {
            case SEARCH: return R.id.nav_search;
            case MAP: return R.id.nav_map;
            default: return R.id.nav_list;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_refresh) {
            viewModel.refresh();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}