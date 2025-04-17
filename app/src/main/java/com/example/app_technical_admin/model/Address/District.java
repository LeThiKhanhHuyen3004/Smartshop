package com.example.app_technical_admin.model.Address;

import java.util.List;

public class District {
    private String Id;
    private String Name;
    private List<Ward> Wards;

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

    public List<Ward> getWards() {
        return Wards;
    }

    public void setWards(List<Ward> wards) {
        this.Wards = wards;
    }

    @Override
    public String toString() {
        return Name;
    }

}