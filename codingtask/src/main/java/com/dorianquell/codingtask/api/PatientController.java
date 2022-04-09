package com.dorianquell.codingtask.api;

import org.hl7.fhir.r4.model.Patient;
import org.json.JSONArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.dorianquell.codingtask.dao.PatientDataAccessService;
import com.dorianquell.codingtask.model.PatientInput;
import com.dorianquell.codingtask.processor.FHIRPatientProcessor;

@RestController
public class PatientController {

    @Autowired
    PatientDataAccessService pda;

    @PutMapping
    public ResponseEntity<String> createPatient(@RequestBody PatientInput patInput) {
        Patient patient = FHIRPatientProcessor.createFHIRPatient(patInput);
        if (pda.updatePatient(patient, pda.getDbConnection()))
            return new ResponseEntity<>("Patient stored with ID: " + patient.getId(), HttpStatus.OK);
        return new ResponseEntity<>("Patient could not be stored!", HttpStatus.BAD_REQUEST);
    }

    @GetMapping
    public ResponseEntity<String> getPatient(@RequestParam String id) {
        String patient = pda.getPatient(id, pda.getDbConnection());
        if (patient != null)
            return new ResponseEntity<>(patient, HttpStatus.OK);
        return new ResponseEntity<>("Patient could not be found!", HttpStatus.BAD_REQUEST);
    }

    @GetMapping("search")
    public ResponseEntity<String> searchFor(@RequestParam String gender) {
        JSONArray patients = pda.findAllPatientsByGender(gender, pda.getDbConnection());
        return new ResponseEntity<>(patients.toString(), HttpStatus.OK);
    }

    @DeleteMapping
    public ResponseEntity<String> deletePatient(@RequestParam String id) {
        if (pda.deletePatient(id, pda.getDbConnection()))
            return new ResponseEntity<>("Patient " + id + " deleted!", HttpStatus.OK);
        return new ResponseEntity<>("Could not delete Patient " + id, HttpStatus.BAD_REQUEST);
    }
    
    @GetMapping("print")
    public void printAllPatients() {
        pda.printDB(pda.getDbConnection());
    }

}
