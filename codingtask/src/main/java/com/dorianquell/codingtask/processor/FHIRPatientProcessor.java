package com.dorianquell.codingtask.processor;

import java.util.Date;
import java.util.UUID;

import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Enumerations.AdministrativeGender;

import com.dorianquell.codingtask.model.PatientInput;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;

public interface FHIRPatientProcessor {

    FhirContext ctx = FhirContext.forR4();
    IParser fhirParser = ctx.newJsonParser();

    /**
     * @param patient
     *            object which should transformed into a FHIR resource
     * @return Patient FHIR resource
     */
    public static Patient createFHIRPatient(PatientInput patient) {
        Patient pat = new Patient();

        pat.setId(UUID.randomUUID().toString());
        pat.addName(new HumanName().setFamily(patient.getLastname()).addGiven(patient.getFirstname()));

        if (patient.getBirthdate() != null)
            pat.setBirthDateElement(new DateType(patient.getBirthdate().toString()));

        String gender = patient.getGender();
        if (gender != null)
            switch (gender.toLowerCase()) {
                case "male":
                    pat.setGender(AdministrativeGender.MALE);
                    break;
                case "female":
                    pat.setGender(AdministrativeGender.FEMALE);
                    break;
                case "other":
                    pat.setGender(AdministrativeGender.OTHER);
                    break;
                default:
                    pat.setGender(AdministrativeGender.UNKNOWN);
                    break;
            }
        else
            pat.setGender(AdministrativeGender.UNKNOWN);

        pat.getMeta().setLastUpdated(new Date());

        return pat;
    }

    /**
     * @param patient
     *            resource to turn into a string
     * @return String containing the FHIR patient resource as a json
     */
    public static String parseFHIR(Patient patient) {
        return ctx.newJsonParser().encodeResourceToString(patient);
    }

}
