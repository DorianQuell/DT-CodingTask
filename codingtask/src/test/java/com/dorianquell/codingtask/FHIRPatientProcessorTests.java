package com.dorianquell.codingtask;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Date;

import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.Patient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.dorianquell.codingtask.model.PatientInput;
import com.dorianquell.codingtask.processor.FHIRPatientProcessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
public class FHIRPatientProcessorTests {

    private static JSONArray input;

    @BeforeAll
    public static void init() throws IOException, JSONException {
        // Load test data and store it in a json array
        String file = new String(Files.readAllBytes(Paths.get("src/test/resources/testPatients.json")),
            Charset.defaultCharset());
        input = new JSONArray(file);
    }

    @Test
    public void testCreateFHIRPatient() throws JsonMappingException, JsonProcessingException, JSONException {

        // Create first test patient - all fields filled correctly
        JSONObject json = input.getJSONObject(0);
        PatientInput pat = new PatientInput(json.getString("firstname"), json.getString("lastname"),
            json.getString("gender"), LocalDate.parse(json.getString("birthdate")));
        Patient result = FHIRPatientProcessor.createFHIRPatient(pat);

        // Check all fields
        assertEquals(true, result.hasId());
        assertEquals(pat.getFirstname(), result.getName().get(0).getGiven().get(0).toString());
        assertEquals(pat.getLastname(), result.getName().get(0).getFamily().toString());
        assertEquals(new DateType(pat.getBirthdate().toString()).toString(), result.getBirthDateElement().toString());
        assertEquals(pat.getGender(), result.getGender().toString().toLowerCase());
        
        // Create second test patient - all fields null
        PatientInput pat2 = new PatientInput(null, null, null, null);
        Patient result2 = FHIRPatientProcessor.createFHIRPatient(pat2);

        // Check all fields
        assertEquals(true, result2.hasId());
        assertEquals(false, result2.getName().get(0).hasGiven());
        assertEquals(false, result2.getName().get(0).hasFamily());
        assertEquals(false, result2.hasBirthDate());
        assertEquals("UNKNOWN", result2.getGender().toString());
        
    }

}
