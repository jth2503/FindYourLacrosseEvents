package de.joristhiele.findyourlacrosseevents.ui;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import de.joristhiele.findyourlacrosseevents.MyViewModel;
import de.joristhiele.findyourlacrosseevents.R;
import de.joristhiele.findyourlacrosseevents.data.EditEventState;

public class MainNavigationActivity extends AppCompatActivity {

    private MyViewModel viewModel;

    private EditEventState editEventState = EditEventState.NONE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_navigation);

        // viewModel to later be used by fragments
        viewModel = new ViewModelProvider(this).get(MyViewModel.class);

        // check whether permission for location is granted
        boolean permissionGranted = ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;

        // set up BottomNavigationView
        BottomNavigationView bottomNavigationView = findViewById(R.id.navigation);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            switch (item.getItemId()) {
                case R.id.map_item:
                    selectedFragment = permissionGranted ? new MapFragment() : new RequestPermissionsFragment();
                    break;
                case R.id.new_event_item:
                    switch (editEventState) {
                        case NEW_EVENT:
                            selectedFragment = new NewEventFragment();
                            break;
                        case EDIT_EVENT:
                            selectedFragment = new EditEventFragment();
                            break;
                        case NONE:
                            selectedFragment = new AddOrEditFragment();
                            break;
                    }
                    break;
                case R.id.favorites_item:
                    selectedFragment = new FavoritesFragment();
                    break;
            }

            if (selectedFragment != null) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.frame_layout, selectedFragment)
                        .commit();
                getSupportActionBar().setDisplayHomeAsUpEnabled(selectedFragment instanceof NewEventFragment || selectedFragment instanceof EditEventFragment);
            }
            return true;
        });

        // manually load initial fragment
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.frame_layout, permissionGranted ? new MapFragment() : new RequestPermissionsFragment())
                .commit();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home && editEventState != EditEventState.NONE) {
            if (editEventState == EditEventState.NEW_EVENT)
                viewModel.clearNewEventData();
            else if (editEventState == EditEventState.EDIT_EVENT) {
                viewModel.clearEditEventData();
            }
            setWorkingOnEvent(EditEventState.NONE);
        }

        return super.onOptionsItemSelected(item);
    }

    public void replaceMapFragment() {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.frame_layout, new MapFragment())
                .commit();
    }

    public void setWorkingOnEvent(EditEventState editEventState) {
        this.editEventState = editEventState;
        switch(editEventState) {
            case NEW_EVENT:
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.frame_layout, new NewEventFragment())
                        .commit();
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                break;
            case EDIT_EVENT:
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.frame_layout, new EditEventFragment())
                        .commit();
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                break;
            case NONE:
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.frame_layout, new AddOrEditFragment())
                        .commit();
                getSupportActionBar().setDisplayHomeAsUpEnabled(false);
                break;
        }
    }
}