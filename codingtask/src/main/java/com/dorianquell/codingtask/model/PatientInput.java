package com.dorianquell.codingtask.model;

import java.time.LocalDate;
import java.util.Date;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;

public class PatientInput {

    @Getter
    @JsonProperty("firstname")
    private String firstname;

    @Getter
    @JsonProperty("lastname")
    private String lastname;

    @Getter
    @JsonProperty("gender")
    private String gender;

    @Getter
    @JsonProperty("birthdate")
    private LocalDate birthdate;

    @Override
    public String toString() {
        return "Patient [firstname=" + firstname + ", lastname=" + lastname + ", gender=" + gender
                + ", birthdate=" + birthdate + "]";
    }
    
}