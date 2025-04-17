package com.example.app_technical_admin.model.Address;

public class Ward {
    private String Id;
    private String Name;
    private String Level;

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

    public String getLevel() {
        return Level;
    }

    public void setLevel(String level) {
        this.Level = level;
    }

    @Override
    public String toString() {
        return Name;
    }

}