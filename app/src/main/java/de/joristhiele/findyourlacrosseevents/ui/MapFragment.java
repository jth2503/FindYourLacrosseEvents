package de.joristhiele.findyourlacrosseevents.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import android.provider.Settings;
import android.text.Editable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.textfield.TextInputEditText;
import com.parse.ParseObject;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import de.joristhiele.findyourlacrosseevents.Constants;
import de.joristhiele.findyourlacrosseevents.MyViewModel;
import de.joristhiele.findyourlacrosseevents.R;
import de.joristhiele.findyourlacrosseevents.Util;
import de.joristhiele.findyourlacrosseevents.data.Event;
import de.joristhiele.findyourlacrosseevents.data.Filter;
import de.joristhiele.findyourlacrosseevents.data.FilterCategory;

public class MapFragment extends Fragment implements OnMapReadyCallback, GoogleMap.InfoWindowAdapter {

    private MyViewModel viewModel;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private SharedPreferences preferences;
    private SharedPreferences.Editor prefEditor;

    private GoogleMap map;
    private Circle circle;
    private Map<String, Marker> markerMap = new HashMap<>();

    private TextInputEditText etFilterSearchRadius, etFilterName, etFilterStartDate, etFilterEndDate;
    private ChipGroup cgFilterGender, cgFilterDiscipline, cgFilterEventType;

    public MapFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(MyViewModel.class);
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(getContext());
        preferences = getActivity().getPreferences(Context.MODE_PRIVATE);
        prefEditor = preferences.edit();

        // instantiate all the ui components
        etFilterSearchRadius = view.findViewById(R.id.filter_search_radius_et);
        etFilterName = view.findViewById(R.id.filter_event_name_et);
        etFilterStartDate = view.findViewById(R.id.filter_event_dates_start_et);
        etFilterEndDate = view.findViewById(R.id.filter_event_dates_end_et);
        cgFilterGender = view.findViewById(R.id.filter_genders);
        cgFilterDiscipline = view.findViewById(R.id.filter_disciplines);
        cgFilterEventType = view.findViewById(R.id.filter_event_types);

        // Place the bottom sheet containing filters at the correct PeekHeight
        BottomSheetBehavior<View> bottomSheet = BottomSheetBehavior.from(view.findViewById(R.id.filter_bottom_sheet));
        View filterHeader = view.findViewById(R.id.filter_header);
        filterHeader.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                filterHeader.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                bottomSheet.setPeekHeight(filterHeader.getHeight());
            }
        });

        //
        Filter oldFilter = viewModel.getFilter().getValue();
        if (viewModel.getSearchRadius().getValue() != 0.0f)
            etFilterSearchRadius.setText(viewModel.getSearchRadius().getValue().toString());
        etFilterName.setText(oldFilter.getFilterName());
        if (oldFilter.getFilterStartDate() != null)
            etFilterStartDate.setText(oldFilter.getFilterStartDate().format(DateTimeFormatter.ISO_DATE));
        if (oldFilter.getFilterEndDate() != null)
            etFilterEndDate.setText(oldFilter.getFilterEndDate().format(DateTimeFormatter.ISO_DATE));

        // setup text watchers on the filter edit texts to update the viewmodel
        setupEditTextWatchers();

        // setup the datepicker functionality
        setupDatePickers(etFilterStartDate, R.string.filter_event_dates_start_label, oldFilter.getFilterStartDate());
        setupDatePickers(etFilterEndDate, R.string.filter_event_dates_end_label, oldFilter.getFilterEndDate());

        // setup the chip groups with all available event categories from server
        setupChipGroupWithData(cgFilterGender, viewModel.getAllGenders(), "GenderName", FilterCategory.GENDER);
        setupChipGroupWithData(cgFilterDiscipline, viewModel.getAllDisciplines(), "DisciplineName", FilterCategory.DISCIPLINE);
        setupChipGroupWithData(cgFilterEventType, viewModel.getAllEventTypes(), "EventTypeName", FilterCategory.EVENT_TYPE);

        // set up map
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.mapFragment);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (Util.isLocationEnabled(getContext()))
            getDeviceLocation();
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        this.map = googleMap;
        map.setInfoWindowAdapter(this);
        // mark/unmark event as favorite
        map.setOnInfoWindowLongClickListener(marker -> {
            if (preferences.contains((String) marker.getTag())) {
                prefEditor.remove((String) marker.getTag());
                prefEditor.apply();
            } else {
                prefEditor.putString((String) marker.getTag(), "null");
                prefEditor.apply();
            }
            marker.showInfoWindow();
        });

        // enable location layer and set location
        map.setMyLocationEnabled(true);
        map.getUiSettings().setMyLocationButtonEnabled(true);
        map.setOnMyLocationButtonClickListener(() -> {
            if (viewModel.getLastKnownLocation() == null) {                                            // click on location button should direct to settings if not enabled
                startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            }
            return false;
        });
        getDeviceLocation();

        // display markers for events
        viewModel.getFilteredEvents().observe(getViewLifecycleOwner(), events -> {
            List<String> eventIds =  new ArrayList<>();
            for (Event event : events) {
                eventIds.add(event.getId());
                if (!markerMap.containsKey(event.getId())) {
                    Marker marker = map.addMarker(new MarkerOptions()
                            .position(new LatLng(event.getLocation().getLatitude(), event.getLocation().getLongitude()))
                            .title(event.getName())
                    );
                    marker.setTag(event.getId());
                    markerMap.put(event.getId(), marker);
                }
            }
            for (String id : new ArrayList<>(markerMap.keySet())) {
                if (!eventIds.contains(id))
                    markerMap.remove(id).remove();
            }
        });

        // draw circle around user's location
        viewModel.getSearchRadius().observe(getViewLifecycleOwner(), aFloat -> {
            if (viewModel.getLastKnownLocation() != null) {
                if (circle != null)
                    circle.remove();
                circle = map.addCircle(new CircleOptions()
                        .center(new LatLng(viewModel.getLastKnownLocation().getLatitude(), viewModel.getLastKnownLocation().getLongitude()))
                        .radius(aFloat * 1000)
                );
            }
        });
    }

    @SuppressLint("MissingPermission")
    private void getDeviceLocation() {
        try {
            fusedLocationProviderClient.getCurrentLocation(Priority.PRIORITY_PASSIVE, null).addOnCompleteListener(getActivity(), task -> {
                if (task.isSuccessful()) {
                    viewModel.setLastKnownLocation(task.getResult());
                    if (viewModel.getLastKnownLocation() != null) {                        // zoom to location if available
                        map.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                new LatLng(viewModel.getLastKnownLocation().getLatitude(), viewModel.getLastKnownLocation().getLongitude()),
                                Constants.DEFAULT_ZOOM_WITH_LOCATION)
                        );
                        viewModel.updateSearchRadius(viewModel.getSearchRadius().getValue() != null ? viewModel.getSearchRadius().getValue() : 0);
                    } else {                                                // zoom to default viewport if no location available
                        map.moveCamera(CameraUpdateFactory
                                .newLatLngZoom(Constants.MAP_CENTER, Constants.DEFAULT_ZOOM_WITHOUT_LOCATION));
                    }
                }
            });
        } catch (SecurityException e)  {
            Log.e("Exception: %s", e.getMessage(), e);
        }
    }

    private void setupEditTextWatchers() {
        etFilterSearchRadius.addTextChangedListener(new Util.MyTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                if (!Util.StringIsNullOrEmpty(s))
                    viewModel.updateSearchRadius(Float.parseFloat(s.toString()));
                else
                    viewModel.updateSearchRadius(0.0f);
            }
        });
        etFilterName.addTextChangedListener(new Util.MyTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                if (!Util.StringIsNullOrEmpty(s))
                    viewModel.updateFilter(FilterCategory.NAME, s.toString(), false);
                else
                    viewModel.updateFilter(FilterCategory.NAME, Constants.EMPTY_STRING, false);
            }
        });

        etFilterStartDate.addTextChangedListener(new Util.MyTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                if (!Util.StringIsNullOrEmpty(s))
                    viewModel.updateFilter(FilterCategory.START_DATE, LocalDate.parse(s.toString(), DateTimeFormatter.ISO_DATE), false);
                else
                    viewModel.updateFilter(FilterCategory.START_DATE, null, false);
            }
        });

        etFilterEndDate.addTextChangedListener(new Util.MyTextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                if (!Util.StringIsNullOrEmpty(s))
                    viewModel.updateFilter(FilterCategory.END_DATE, LocalDate.parse(s.toString(), DateTimeFormatter.ISO_DATE), false);
                else
                    viewModel.updateFilter(FilterCategory.END_DATE, null, false);
            }
        });
    }

    private void setupDatePickers(TextInputEditText editText, int titleResource, LocalDate preselectedDate) {
        // disable keyboard
        editText.setShowSoftInputOnFocus(false);
        // show datepicker when editText receives focus
        editText.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                        .setTitleText(titleResource)
                        .setSelection(preselectedDate != null
                                ? preselectedDate.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()              // use UTC to always have correct date
                                : LocalDate.now().atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()                 // use UTC to always have correct date)
                        ).build();
                datePicker.addOnPositiveButtonClickListener(selection -> {
                    editText.setText(Instant.ofEpochMilli(selection).atZone(ZoneId.of("UTC")).toLocalDate().toString());    // use UTC to always have correct date
                    v.clearFocus();
                });
                datePicker.addOnCancelListener(_ignored -> v.clearFocus());
                datePicker.addOnDismissListener(_ignored -> v.clearFocus());
                datePicker.show(getParentFragmentManager(), Constants.EMPTY_STRING);
            }
        });
    }

    private void setupChipGroupWithData(ChipGroup chipGroup, LiveData<List<ParseObject>> dataList, String columnName, FilterCategory filterCategory) {
        dataList.observe(getViewLifecycleOwner(), itemList -> {
            // create chip for each list item
            for (ParseObject item : itemList) {
                // inflate layout
                Chip chip = (Chip) getLayoutInflater().inflate(R.layout.chip_category, chipGroup, false);
                chip.setText(item.getString(columnName));
                chipGroup.addView(chip);

                // select values from viewModel
                switch (filterCategory) {
                    case EVENT_TYPE:
                        chip.setChecked(viewModel.getFilter().getValue().getFilterEventTypes().stream().map(ParseObject::getObjectId).collect(Collectors.toList()).contains(item.getObjectId()));
                        break;
                    case GENDER:
                        chip.setChecked(viewModel.getFilter().getValue().getFilterGenders().stream().map(ParseObject::getObjectId).collect(Collectors.toList()).contains(item.getObjectId()));
                        break;
                    case DISCIPLINE:
                        chip.setChecked(viewModel.getFilter().getValue().getFilterDisciplines().stream().map(ParseObject::getObjectId).collect(Collectors.toList()).contains(item.getObjectId()));
                        break;
                }

                chip.setOnCheckedChangeListener((buttonView, isChecked) -> viewModel.updateFilter(filterCategory, item, isChecked));
            }
        });
    }

    @Nullable
    @Override
    public View getInfoContents(@NonNull Marker marker) {
        Log.d("JORIS", "getInfoContents: betreten");
        View view = getLayoutInflater().inflate(R.layout.custom_info_contents, null);
        Event event = viewModel.getEvent(String.valueOf(marker.getTag()));
        ((TextView) view.findViewById(R.id.title)).setText(event.getName());
        if (preferences.contains((String) marker.getTag())) {
            Log.d("JORIS", "getInfoContents: if betreten");
            view.findViewById(R.id.marker_favorite_icon).setVisibility(View.VISIBLE);
            ((TextView) view.findViewById(R.id.marker_favorite_info)).setText(R.string.marker_favorite_marked);
        }
        ((TextView) view.findViewById(R.id.marker_address)).setText(getString(R.string.marker_address, event.getAddress()));
        ((TextView) view.findViewById(R.id.marker_dates)).setText(getString(R.string.marker_dates, event.getStartDate(), event.getEndDate()));
        ((TextView) view.findViewById(R.id.marker_event_type)).setText(getString(R.string.marker_event_type, event.getEventType().getString("EventTypeName")));
        StringBuilder builder = new StringBuilder();
        for (ParseObject gender : event.getGenders()) {
            builder.append(gender.getString("GenderName"));
            builder.append(" ");
        }
        ((TextView) view.findViewById(R.id.marker_genders)).setText(getString(R.string.marker_genders, builder.toString()));
        builder = new StringBuilder();
        for (ParseObject discipline : event.getDisciplines()) {
            builder.append(discipline.getString("DisciplineName"));
            builder.append(" ");
        }
        ((TextView) view.findViewById(R.id.marker_disciplines)).setText(getString(R.string.marker_disciplines, builder.toString()));
        return view;
    }

    @Nullable
    @Override
    public View getInfoWindow(@NonNull Marker marker) {
        return null;
    }
}