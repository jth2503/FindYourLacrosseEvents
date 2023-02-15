package de.joristhiele.findyourlacrosseevents.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import de.joristhiele.findyourlacrosseevents.MyViewModel;
import de.joristhiele.findyourlacrosseevents.R;
import de.joristhiele.findyourlacrosseevents.data.Event;
import de.joristhiele.findyourlacrosseevents.data.FavoriteListAdapter;

public class FavoritesFragment extends Fragment {

    private MyViewModel viewModel;
    private SharedPreferences preferences;
    private SharedPreferences.Editor prefEditor;

    private RecyclerView rvFavoritesList;
    private FavoriteListAdapter adapter;

    private Set<String> favoriteIds;
    private final List<Event> favoriteEvents = new ArrayList<>();

    public FavoritesFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_favorites, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(MyViewModel.class);
        preferences = getActivity().getPreferences(Context.MODE_PRIVATE);
        prefEditor = preferences.edit();

        // initialize view
        rvFavoritesList = view.findViewById(R.id.favorites_list);

        // initialize recyclerview adapter
        FavoriteListAdapter adapter = new FavoriteListAdapter(favoriteEvents);
        rvFavoritesList.setAdapter(adapter);

        // get dataset of favorite events
        favoriteIds = preferences.getAll().keySet();
        viewModel.getAllEvents().observe(getViewLifecycleOwner(), events -> {
            favoriteEvents.clear();
            favoriteEvents.addAll(events.stream().filter(event -> favoriteIds.contains(event.getId())).collect(Collectors.toList()));
            adapter.notifyDataSetChanged();
        });
    }
}