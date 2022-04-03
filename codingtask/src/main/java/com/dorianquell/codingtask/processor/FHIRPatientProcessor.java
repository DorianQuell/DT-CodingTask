package com.dorianquell.codingtask.processor;

import java.time.LocalDate;
import java.util.Date;
import java.util.UUID;

import org.apache.tomcat.jni.Time;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Enumerations.AdministrativeGender;

import com.dorianquell.codingtask.model.PatientInput;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;

public interface FHIRPatientProcessor {
    
    FhirContext ctx = new FhirContext().forR4();
    IParser fhirParser = ctx.newJsonParser();
    
    public static Patient createFHIRPatient(PatientInput pat) {
        Patient patient = new Patient();
        
        patient.setId(UUID.randomUUID().toString());
        patient.addName(new HumanName().setFamily(pat.getLastname()).addGiven(pat.getFirstname()));
        patient.setBirthDateElement(new DateType(pat.getBirthdate().toString()));
        
        String gender = pat.getGender().toLowerCase();
        if("male".equals(gender)) {
            patient.setGender(AdministrativeGender.MALE);
        } else if ("female".equals(gender)) {
            patient.setGender(AdministrativeGender.FEMALE);
        } else if ("other".equals(gender)) {
            patient.setGender(AdministrativeGender.OTHER);
        } else {
            patient.setGender(AdministrativeGender.UNKNOWN);
        }
        
        patient.getMeta().setLastUpdated(new Date());
        
        return patient;
    }
    
    public static String parseFHIR(Patient patient) {
        return ctx.newJsonParser().encodeResourceToString(patient);
    }
    
}
