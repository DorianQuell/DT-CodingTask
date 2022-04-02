package com.dorianquell.codingtask.api;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dorianquell.codingtask.model.Patient;

@RestController
public class PatientController {

    @PostMapping
    public void createPatient(@RequestBody Patient patient) {
        System.out.println(patient);
    }

    @GetMapping("getPatient")
    public void getPatient(@RequestBody String id) {
        System.out.println("Get Patient: " + id);
    }

    @GetMapping("search")
    public List searchFor(@RequestBody Patient patient) {
        List patients = new ArrayList();
        System.out.println(patient);
        return patients;
    }

    @DeleteMapping
    public void deletePatient(@RequestBody String id) {
        System.out.println("Delete Patient: " + id);
    }

}
