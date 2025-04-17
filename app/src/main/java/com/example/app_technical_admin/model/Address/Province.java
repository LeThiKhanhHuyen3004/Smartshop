package com.example.app_technical_admin.model.Address;

import java.util.List;

public class Province {
    private String Id;
    private String Name;
    private List<District> Districts;

    // Getters, setters
    public String getId() {
        return Id;
    }

    public void setId(String id) {
        this.Id = id;
    }

    public String getName() {
        return Name;
    }

    public void setName(String name) {
        this.Name = name;
    }

    public List<District> getDistricts() {
        return Districts;
    }

    public void setDistricts(List<District> districts) {
        this.Districts = districts;
    }

    @Override
    public String toString() {
        return Name;
    }
}