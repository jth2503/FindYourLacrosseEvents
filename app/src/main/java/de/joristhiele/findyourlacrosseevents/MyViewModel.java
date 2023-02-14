package de.joristhiele.findyourlacrosseevents;

import android.app.Application;
import android.content.pm.PackageManager;
import android.location.Location;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.Transformations;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import de.joristhiele.findyourlacrosseevents.data.Event;
import de.joristhiele.findyourlacrosseevents.data.Filter;
import de.joristhiele.findyourlacrosseevents.data.FilterCategory;

public class MyViewModel extends AndroidViewModel {

    private final MyBackendConnection backendConnection;

    public MyViewModel(@NonNull Application application) {
        super(application);
        String apiKey = Constants.EMPTY_STRING;
        try {
            apiKey = application
                    .getApplicationContext()
                    .getPackageManager()
                    .getApplicationInfo(application.getPackageName(), PackageManager.GET_META_DATA)
                    .metaData
                    .getString("com.google.android.geo.API_KEY");
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        RequestQueue queue = Volley.newRequestQueue(application);
        this.backendConnection = new MyBackendConnection(queue, apiKey);
    }

    //region EventCategories
    private LiveData<List<ParseObject>> allGenders;
    private LiveData<List<ParseObject>> allDisciplines;
    private LiveData<List<ParseObject>> allEventTypes;

    public LiveData<List<ParseObject>> getAllGenders() {
        // pull the data from the backend unchanged
        if (allGenders == null) {
            allGenders = Transformations.map(backendConnection.getAllGenders(), allGenders -> allGenders);
        }
        return allGenders;
    }
    public LiveData<List<ParseObject>> getAllDisciplines() {
        // pull the data from the backend unchanged
        if (allDisciplines == null) {
            allDisciplines = Transformations.map(backendConnection.getAllDisciplines(), allDisciplines -> allDisciplines);
        }
        return allDisciplines;
    }
    public LiveData<List<ParseObject>> getAllEventTypes() {
        // pull the data from the backend unchanged
        if (allEventTypes == null) {
            allEventTypes = Transformations.map(backendConnection.getAllEventTypes(), allEventTypes -> allEventTypes);
        }
        return allEventTypes;
    }
    //endregion

    //region Map
    private Location lastKnownLocation;
    private final MutableLiveData<Float> searchRadius = new MutableLiveData<>();
    private MutableLiveData<Filter> filter;

    private LiveData<List<Event>> allEvents;
    private MediatorLiveData<List<Event>> filteredEvents;

    public Location getLastKnownLocation() {
        return lastKnownLocation;
    }

    public void setLastKnownLocation(Location lastKnownLocation) {
        this.lastKnownLocation = lastKnownLocation;
    }

    public LiveData<Float> getSearchRadius() {
        if (searchRadius.getValue() == null)
            searchRadius.setValue(0.0f);
        return searchRadius;
    }

    public void updateSearchRadius(Float newRadius) {
        searchRadius.setValue(newRadius);
        filter.setValue(filter.getValue());
    }

    private LiveData<Filter> getFilter() {
        if (filter == null) {
            filter = new MutableLiveData<>();
            filter.setValue(new Filter());
        }

        return filter;
    }

    public void updateFilter(FilterCategory category, Object newValue, boolean addToList) {
        Filter newFilter = filter.getValue();
        if (newFilter == null)
            return;
        switch (category) {
            case NAME:
                newFilter.setFilterName((String) newValue);
                break;
            case START_DATE:
                newFilter.setFilterStartDate((LocalDate) newValue);
                break;
            case END_DATE:
                newFilter.setFilterEndDate((LocalDate) newValue);
                break;
            case EVENT_TYPE:
                if (addToList)
                    newFilter.addEventType((ParseObject) newValue);
                else
                    newFilter.removeEventType((ParseObject) newValue);
                break;
            case GENDER:
                if (addToList)
                    newFilter.addGender((ParseObject) newValue);
                else
                    newFilter.removeGender((ParseObject) newValue);
                break;
            case DISCIPLINE:
                if (addToList)
                    newFilter.addDiscipline((ParseObject) newValue);
                else
                    newFilter.removeDiscipline((ParseObject) newValue);
                break;
        }
        filter.setValue(newFilter);
    }

    private LiveData<List<Event>> getAllEvents() {
        // pull the data from the backend unchanged
        if (allEvents == null) {
            allEvents = Transformations.map(backendConnection.getAllEvents(), allEvents -> allEvents);
        }
        return allEvents;
    }

    public LiveData<List<Event>> getFilteredEvents() {
        if (filteredEvents == null) {
            filteredEvents = new MediatorLiveData<>();
            filteredEvents.setValue(new ArrayList<>());

            filteredEvents.addSource(getAllEvents(), events -> filteredEvents.setValue(events));

            filteredEvents.addSource(getFilter(), filter -> {
                if (allEvents.getValue() != null) {
                    List<Event> allEventsSource = allEvents.getValue();
                    // filter for radius around location
                    if (lastKnownLocation != null && searchRadius.getValue() != 0.0f) {
                        allEventsSource = allEventsSource.stream().filter(event ->
                                event.getLocation().distanceInKilometersTo(new ParseGeoPoint(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude())) <= searchRadius.getValue()
                        ).collect(Collectors.toList());
                    }

                    // filter for other fields
                    allEventsSource = allEventsSource.stream().filter(event ->
                            event.getName().toLowerCase().contains(filter.getFilterName().toLowerCase())
                                    && (filter.getFilterStartDate() == null || event.getStartDate().isAfter(filter.getFilterStartDate()))
                                    && (filter.getFilterEndDate() == null || event.getEndDate().isBefore(filter.getFilterEndDate()))
                                    && (filter.getFilterEventTypes().isEmpty() || filter.getFilterEventTypes().stream().map(ParseObject::getObjectId).collect(Collectors.toList()).contains(event.getEventType().getObjectId()))
                                    && (filter.getFilterGenders().isEmpty() || filter.getFilterGenders().stream().anyMatch(filterGender -> event.getGenders().stream().map(ParseObject::getObjectId).collect(Collectors.toList()).contains(filterGender.getObjectId())))
                                    && (filter.getFilterDisciplines().isEmpty() || filter.getFilterDisciplines().stream().anyMatch(filterDiscipline -> event.getDisciplines().stream().map(ParseObject::getObjectId).collect(Collectors.toList()).contains(filterDiscipline.getObjectId())))
                    ).collect(Collectors.toList());

                    filteredEvents.setValue(allEventsSource);
                }
            });
        }

        return filteredEvents;
    }

    public Event getEvent(String id) {
        return allEvents.getValue().stream().filter(event -> event.getId().equals(id)).collect(Collectors.toList()).get(0);
    }
    //endregion

    //region NewEvent
    private LiveData<Pair<String, ParseGeoPoint>> addressCheckResult;
    private LiveData<Boolean> newEventSaved;
    private Event newEvent;

    public void clearNewEventData() {
        newEvent.resetData();
    }
    public Event getNewEvent() {
        if (newEvent == null)
            newEvent = new Event();
        return newEvent;
    }

    public LiveData<Pair<String, ParseGeoPoint>> getAddressCheckResult() {
        if (addressCheckResult == null)
            addressCheckResult = Transformations.map(backendConnection.getAddressCheckResult(), pair -> pair);
        return addressCheckResult;
    }
    public void validateAddress(String address) {
        backendConnection.validateAddress(address);
    }

    public LiveData<Boolean> getNewEventSaved() {
        if (newEventSaved == null)
            newEventSaved = Transformations.map(backendConnection.getNewEventSaved(), bool -> bool);
        return newEventSaved;
    }
    public void saveNewEvent(Event event) {
        backendConnection.saveNewEvent(event);
    }
    //endregion
}