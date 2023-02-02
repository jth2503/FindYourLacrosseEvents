package de.joristhiele.findyourlacrosseevents.ui;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.content.pm.PackageManager;
import android.os.Bundle;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import de.joristhiele.findyourlacrosseevents.MyViewModel;
import de.joristhiele.findyourlacrosseevents.R;

public class MainNavigationActivity extends AppCompatActivity {

    private MyViewModel viewModel;

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
                    selectedFragment = new NewEventFragment();
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
            }
            return true;
        });

        // manually load initial fragment
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.frame_layout, permissionGranted ? new MapFragment() : new RequestPermissionsFragment())
                .commit();
    }

    public void replaceMapFragment() {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.frame_layout, new MapFragment())
                .commit();
    }
}