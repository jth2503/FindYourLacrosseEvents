package de.joristhiele.findyourlacrosseevents.data;

import com.parse.ParseGeoPoint;
import com.parse.ParseObject;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Event {
    private String name = "";
    private LocalDate startDate = null;
    private LocalDate EndDate = null;
    private String address = "";
    private ParseGeoPoint location = null;
    private ParseObject eventType = null;
    private final List<ParseObject> genders = new ArrayList<>();
    private final List<ParseObject> disciplines = new ArrayList<>();

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public LocalDate getStartDate() {
        return startDate;
    }
    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }
    public LocalDate getEndDate() {
        return EndDate;
    }
    public void setEndDate(LocalDate endDate) {
        EndDate = endDate;
    }
    public String getAddress() {
        return address;
    }
    public void setAddress(String address) {
        this.address = address;
    }
    public ParseGeoPoint getLocation() {
        return location;
    }
    public void setLocation(ParseGeoPoint location) {
        this.location = location;
    }
    public ParseObject getEventType() {
        return eventType;
    }
    public void setEventType(ParseObject eventType) {
        this.eventType = eventType;
    }
    public List<ParseObject> getGenders() {
        return genders;
    }
    public List<ParseObject> getDisciplines() {
        return disciplines;
    }
}
