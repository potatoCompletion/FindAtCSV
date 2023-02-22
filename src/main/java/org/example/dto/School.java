package org.example.dto;

import com.opencsv.bean.CsvBindByName;

public class School {
    @CsvBindByName
    private String name;
    @CsvBindByName
    private String address;

    public School() {}
    public School(String name, String address) {
        this.name = name;
        this.address = address;
    }

    public String getName() {
        return name;
    }
    public String getAddress() {
        return address;
    }
}
