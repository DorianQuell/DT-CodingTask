package com.dorianquell.codingtask.api;

import java.util.List;

import org.hl7.fhir.r4.model.Patient;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.dorianquell.codingtask.dao.DummyPatientDataAccessService;
import com.dorianquell.codingtask.model.PatientInput;
import com.dorianquell.codingtask.processor.FHIRPatientProcessor;

@RestController
public class PatientController {

    @Autowired
    DummyPatientDataAccessService pda;

    @PostMapping
    public ResponseEntity<String> createPatient(@RequestBody PatientInput patInput) {
        Patient patient = FHIRPatientProcessor.createFHIRPatient(patInput);
        if (pda.addPatient(patient))
            return new ResponseEntity<>("Patient stored with ID: " + patient.getId(), HttpStatus.OK);
        return new ResponseEntity<>("Patient could not be stored!", HttpStatus.BAD_REQUEST);
    }

    @GetMapping
    public ResponseEntity<String> getPatient(@RequestBody String id) {
        Patient patient = pda.getPatient(id);
        if (patient != null)
            return new ResponseEntity<>(FHIRPatientProcessor.parseFHIR(patient), HttpStatus.OK);
        return new ResponseEntity<>("Patient could not be found!", HttpStatus.BAD_REQUEST);
    }

    @GetMapping("search")
    public ResponseEntity<String> searchFor(@RequestParam String gender) {
        List<Patient> patients = pda.findAllPatientsByGender(gender);
        JSONArray response = new JSONArray();
        for (Patient patient : patients) {
            response.put(new JSONObject(FHIRPatientProcessor.parseFHIR(patient)));
        }
        return new ResponseEntity<>(response.toString(), HttpStatus.OK);
    }

    @DeleteMapping
    public ResponseEntity<String> deletePatient(@RequestBody String id) {
        if (pda.deletePatient(id))
            return new ResponseEntity<>("Patient " + id + " deleted!", HttpStatus.OK);
        return new ResponseEntity<>("Could not delete Patient " + id, HttpStatus.BAD_REQUEST);
    }

}
