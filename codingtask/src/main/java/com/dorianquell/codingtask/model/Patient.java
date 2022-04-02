package com.dorianquell.codingtask.model;

import java.util.Date;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;

public class Patient {

    @Getter
    @JsonProperty("id")
    private int id;

    @Getter
    @JsonProperty("firstname")
    private String firstname;

    @Getter
    @JsonProperty("lastname")
    private String lastname;

    @Getter
    @JsonProperty("gender")
    private char gender;

    @Getter
    @JsonProperty("birthdate")
    private Date birthdate;

    @Override
    public String toString() {
        return "Patient [id=" + id + ", firstname=" + firstname + ", lastname=" + lastname + ", gender=" + gender
                + ", birthdate=" + birthdate + "]";
    }
    
}
