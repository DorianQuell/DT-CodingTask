package com.dorianquell.codingtask.api;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.dorianquell.codingtask.dao.DummyPatientDataAccessService;
import com.dorianquell.codingtask.model.Patient;

@RestController
public class PatientController {

    @Autowired
    DummyPatientDataAccessService pda;

    @PostMapping
    public ResponseEntity<String> createPatient(@RequestBody Patient patient) {
        if (pda.addPatient(patient))
            return new ResponseEntity<>("Patient stored", HttpStatus.OK);
        return new ResponseEntity<>("Patient could not be stored!", HttpStatus.BAD_REQUEST);
    }

    @GetMapping
    public ResponseEntity<String> getPatient(@RequestBody int id) {
        Patient pat = pda.getPatient(id);
        if (pat != null)
            return new ResponseEntity<>(pat.toString(), HttpStatus.OK);
        return new ResponseEntity<>("Patient could not be found!", HttpStatus.BAD_REQUEST);
    }

    @GetMapping("search")
    public List searchFor(@RequestBody Patient patient) {
        List patients = new ArrayList();
        pda.printDB();
        return patients;
    }

    @DeleteMapping
    public ResponseEntity<String> deletePatient(@RequestBody int id) {
        if (pda.deletePatient(id))
            return new ResponseEntity<>("Patient " + id + " deleted!", HttpStatus.OK);
        return new ResponseEntity<>("Could not delete Patient " + id, HttpStatus.BAD_REQUEST);
    }

}
