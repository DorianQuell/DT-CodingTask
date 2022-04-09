package com.dorianquell.codingtask.api;

import java.util.Map;

import org.hl7.fhir.r4.model.Patient;
import org.json.JSONArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
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

    @GetMapping("search")
    public ResponseEntity<String> search(@RequestParam Map<String,String> allRequestParams){
        JSONArray patients = pda.search(allRequestParams, pda.getDbConnection());
        return new ResponseEntity<>(patients.toString(4), HttpStatus.OK); 
    }

    @DeleteMapping
    public ResponseEntity<String> deletePatient(@RequestParam String id) {
        if (pda.deletePatient(id, pda.getDbConnection()))
            return new ResponseEntity<>("Patient " + id + " deleted!", HttpStatus.OK);
        return new ResponseEntity<>("Could not delete Patient " + id, HttpStatus.BAD_REQUEST);
    }
    
}
