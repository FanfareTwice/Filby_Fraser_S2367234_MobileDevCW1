//
// Name                 Fraser Filby
// Student ID           S2367234
// Programme of Study   _________________
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
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import org.me.gcu.filby_fraser_s2367234.data.LoadStatus;
import org.me.gcu.filby_fraser_s2367234.ui.DetailFragment;
import org.me.gcu.filby_fraser_s2367234.ui.MainViewModel;
import org.me.gcu.filby_fraser_s2367234.ui.SituationListFragment;

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
                    .replace(R.id.main_container, new SituationListFragment())
                    .replace(R.id.detail_container, new DetailFragment())
                    .commit();
        }

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
