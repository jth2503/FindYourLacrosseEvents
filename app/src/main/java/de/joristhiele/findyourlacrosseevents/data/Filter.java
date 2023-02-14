package de.joristhiele.findyourlacrosseevents.data;

import com.parse.ParseObject;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Filter {

    private String filterName = "";
    private LocalDate filterStartDate = null;
    private LocalDate filterEndDate = null;
    private final List<ParseObject> filterEventTypes = new ArrayList<>();
    private final List<ParseObject> filterGenders = new ArrayList<>();
    private final List<ParseObject> filterDisciplines = new ArrayList<>();

    public String getFilterName() {
        return filterName;
    }

    public void setFilterName(String filterName) {
        this.filterName = filterName;
    }

    public LocalDate getFilterStartDate() {
        return filterStartDate;
    }

    public void setFilterStartDate(LocalDate filterStartDate) {
        this.filterStartDate = filterStartDate;
    }

    public LocalDate getFilterEndDate() {
        return filterEndDate;
    }

    public void setFilterEndDate(LocalDate filterEndDate) {
        this.filterEndDate = filterEndDate;
    }

    public List<ParseObject> getFilterEventTypes() {
        return filterEventTypes;
    }

    public void addEventType(ParseObject eventType) {
        this.filterEventTypes.add(eventType);
    }

    public void removeEventType(ParseObject eventType) {
        this.filterEventTypes.remove(eventType);
    }

    public List<ParseObject> getFilterGenders() {
        return filterGenders;
    }

    public void addGender(ParseObject gender) {
        this.filterGenders.add(gender);
    }

    public void removeGender(ParseObject gender) {
        this.filterGenders.remove(gender);
    }

    public List<ParseObject> getFilterDisciplines() {
        return filterDisciplines;
    }

    public void addDiscipline(ParseObject discipline) {
        this.filterDisciplines.add(discipline);
    }

    public void removeDiscipline(ParseObject discipline) {
        this.filterDisciplines.remove(discipline);
    }
}
