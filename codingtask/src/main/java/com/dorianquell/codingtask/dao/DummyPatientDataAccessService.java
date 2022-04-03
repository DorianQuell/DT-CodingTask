package com.dorianquell.codingtask.dao;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.hl7.fhir.r4.model.Patient;
import org.springframework.stereotype.Repository;

@Repository("DummyPatientDAO")
public class DummyPatientDataAccessService {

    // private static HashMap<Integer, PatientInput> DB = new HashMap<Integer, PatientInput>();
    private static List<Patient> DB = new ArrayList<Patient>();

    public Boolean addPatient(Patient patient) {
        if (calculateAge(patient.getBirthDate()) >= 18)
            return DB.add(patient);
        return false;
    }

    public Patient getPatient(String id) {
        return findInDB(id);
    }

    public Boolean deletePatient(String id) {
        Patient toDelete = findInDB(id);
        if (toDelete != null) {
            return DB.remove(toDelete);
        }
        return false;
    }

    public void printDB() {
        System.out.println(DB);
    }

    private int calculateAge(Date birthdate) {
        if (birthdate != null) {
            LocalDate bd = Instant.ofEpochMilli(birthdate.getTime()).atZone(ZoneId.systemDefault()).toLocalDate();
            return Period.between(bd, LocalDate.now()).getYears();
        }
        return 0;
    }

    private Patient findInDB(String id) {
        for (Patient patient : DB) {
            if (patient.getId().equals(id))
                return patient;
        }
        return null;
    }
}
