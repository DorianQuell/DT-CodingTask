package com.dorianquell.codingtask.dao;

import java.util.HashMap;

import org.springframework.stereotype.Repository;

import com.dorianquell.codingtask.model.Patient;

@Repository("DummyPatientDAO")
public class DummyPatientDataAccessService {

    private static HashMap<Integer, Patient> DB = new HashMap<Integer, Patient>();

    public Boolean addPatient(Patient patient) {
        DB.put(patient.getId(), patient);
        return true;
    }

    public Patient getPatient(int id) {
        if (DB.containsKey(id))
            return DB.get(id);
        return null;
    }

    public Boolean deletePatient(int id) {
        if (DB.containsKey(id)) {
            DB.remove(id);
            return true;
        }
        return false;
    }

    public void printDB() {
        System.out.println(DB);
    }

}
