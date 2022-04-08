package com.dorianquell.codingtask;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.time.LocalDate;
import java.util.Date;

import org.hl7.fhir.r4.model.Patient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import com.dorianquell.codingtask.dao.PatientDataAccessService;
import com.dorianquell.codingtask.model.PatientInput;
import com.dorianquell.codingtask.processor.FHIRPatientProcessor;

@SpringBootTest
public class PatientDataAccessServiceTest {

    @Value("${tablename}")
    private String tablename;

    @Value("${viewname}")
    private String viewname;

    @Autowired
    PatientDataAccessService pda;

    private static JSONArray input;

    @BeforeAll
    public static void init() throws IOException, JSONException {
        // Load test data and store it in a json array
        String file = new String(Files.readAllBytes(Paths.get("src/test/resources/testPatients.json")),
            Charset.defaultCharset());
        input = new JSONArray(file);
    }

    @BeforeEach
    public void deleteDB() throws SQLException {
        System.out.println("Connection closed!");
        pda.getDbConnection().close();
        Connection connection = DriverManager.getConnection("jdbc:sqlite:patientdata.db");
        connection.createStatement().executeUpdate("DROP VIEW IF EXISTS " + viewname);
        connection.createStatement().executeUpdate("DROP TABLE IF EXISTS " + tablename);
        pda.onStartUp();
    }

    @Test
    public void testAddPatient() throws JSONException, SQLException, ParseException {
        JSONObject json = input.getJSONObject(0);

        // Check if table is empty
        String sql = "SELECT COUNT(*) AS count FROM " + tablename;
        ResultSet result = pda.getDbConnection().createStatement().executeQuery(sql);
        assertTrue(0 == result.getInt("count"));

        // Add patient
        PatientInput pat = new PatientInput(json.getString("firstname"), json.getString("lastname"),
            json.getString("gender"), LocalDate.parse(json.getString("birthdate")));
        Patient patient = FHIRPatientProcessor.createFHIRPatient(pat);

        assertTrue(pda.addPatient(patient, pda.getDbConnection()));

        // Check if patient was added correctly
        sql = "SELECT * FROM " + tablename + " WHERE firstname = ?";
        PreparedStatement pstmt = pda.getDbConnection().prepareStatement(sql);
        pstmt.setString(1, json.getString("firstname"));
        result = pstmt.executeQuery();

        assertEquals(json.getString("firstname"), result.getString("firstname"));
        assertEquals(json.getString("lastname"), result.getString("lastname"));
        assertEquals(json.getString("gender"), result.getString("gender"));
        assertEquals(json.getString("birthdate"), result.getDate("birthdate").toString());
        assertEquals(FHIRPatientProcessor.parseFHIR(patient), result.getString("fhir"));
        
        // Try adding an underaged patient
        json = input.getJSONObject(1);
        pat = new PatientInput(json.getString("firstname"), json.getString("lastname"),
            json.getString("gender"), LocalDate.parse(json.getString("birthdate")));
        patient = FHIRPatientProcessor.createFHIRPatient(pat);
        
        assertFalse(pda.addPatient(patient, pda.getDbConnection()));
    }

    @Test
    public void testGetPatient() throws JSONException, SQLException {
        JSONObject json = input.getJSONObject(0);

        // Check if table is empty
        String sql = "SELECT COUNT(*) AS count FROM " + tablename;
        ResultSet result = pda.getDbConnection().createStatement().executeQuery(sql);
        assertTrue(0 == result.getInt("count"));

        // Add patient
        PatientInput pat = new PatientInput(json.getString("firstname"), json.getString("lastname"),
            json.getString("gender"), LocalDate.parse(json.getString("birthdate")));
        Patient patient = FHIRPatientProcessor.createFHIRPatient(pat);

        pda.addPatient(patient, pda.getDbConnection());

        // Check if get function works
        assertEquals(FHIRPatientProcessor.parseFHIR(patient),
            pda.getPatient(patient.getId().toString(), pda.getDbConnection()));

        // Try reading a non-existent patient
        assertEquals(null, pda.getPatient("123", pda.getDbConnection()));
    }

    @Test
    public void testDeletePatient() throws SQLException, JSONException {
        JSONObject json = input.getJSONObject(0);

        // Check if table is empty
        String sql = "SELECT COUNT(*) AS count FROM " + tablename;
        ResultSet result = pda.getDbConnection().createStatement().executeQuery(sql);
        assertTrue(0 == result.getInt("count"));

        // Add patient
        PatientInput pat = new PatientInput(json.getString("firstname"), json.getString("lastname"),
            json.getString("gender"), LocalDate.parse(json.getString("birthdate")));
        Patient patient = FHIRPatientProcessor.createFHIRPatient(pat);

        pda.addPatient(patient, pda.getDbConnection());

        // Check if table has one patient
        sql = "SELECT COUNT(*) AS count FROM " + tablename;
        result = pda.getDbConnection().createStatement().executeQuery(sql);
        assertTrue(1 == result.getInt("count"));

        // Delete Patient
        assertTrue(pda.deletePatient(patient.getId().toString(), pda.getDbConnection()));

        // Check if deleted
        sql = "SELECT COUNT(*) AS count FROM " + tablename;
        result = pda.getDbConnection().createStatement().executeQuery(sql);
        assertTrue(0 == result.getInt("count"));

        // Try deleting a non-existing patient - returns true since patient already doesn't exist
        assertTrue(pda.deletePatient("123", pda.getDbConnection()));
    }

    @Test
    public void testFindAllPatientsByGender() throws JSONException, SQLException {
        JSONObject json = input.getJSONObject(0);

        // Check if table is empty
        String sql = "SELECT COUNT(*) AS count FROM " + tablename;
        ResultSet result = pda.getDbConnection().createStatement().executeQuery(sql);
        assertTrue(0 == result.getInt("count"));

        // Add patients
        PatientInput pat = new PatientInput(json.getString("firstname"), json.getString("lastname"),
            json.getString("gender"), LocalDate.parse(json.getString("birthdate")));
        Patient patient = FHIRPatientProcessor.createFHIRPatient(pat);
        Patient patient2 = FHIRPatientProcessor.createFHIRPatient(pat);

        pda.addPatient(patient, pda.getDbConnection());
        pda.addPatient(patient2, pda.getDbConnection());

        // Check if function finds 2 male patients and no females
        assertTrue(2 == pda.findAllPatientsByGender("male", pda.getDbConnection()).length());
        assertTrue(0 == pda.findAllPatientsByGender("female", pda.getDbConnection()).length());
    }
    
    @Test
    public void testDeletePatientRecordsOlderThan() throws JSONException, SQLException, InterruptedException {
        JSONObject json = input.getJSONObject(0);

        // Check if table is empty
        String sql = "SELECT COUNT(*) AS count FROM " + tablename;
        ResultSet result = pda.getDbConnection().createStatement().executeQuery(sql);
        assertTrue(0 == result.getInt("count"));

        // Add patient
        PatientInput pat = new PatientInput(json.getString("firstname"), json.getString("lastname"),
            json.getString("gender"), LocalDate.parse(json.getString("birthdate")));
        Patient patient = FHIRPatientProcessor.createFHIRPatient(pat);
        pda.addPatient(patient, pda.getDbConnection());
        
        // Wait 10 seconds
        Thread.currentThread().sleep(10000);
        
        // Create deletion date
        Date deletionDate = new Date();
        
        // Add second patient
        Patient patient2 = FHIRPatientProcessor.createFHIRPatient(pat);
        pda.addPatient(patient2, pda.getDbConnection());

        // Check if two patients in table
        sql = "SELECT COUNT(*) AS count FROM " + tablename;
        result = pda.getDbConnection().createStatement().executeQuery(sql);
        assertTrue(2 == result.getInt("count"));
        
        // Delete all "old" patients
        pda.deletePatientRecordsOlderThan(deletionDate, pda.getDbConnection());
        
        //Since one patient was created before and one after the deletion date, only one patient should be left
        sql = "SELECT COUNT(*) AS count FROM " + tablename;
        result = pda.getDbConnection().createStatement().executeQuery(sql);
        assertTrue(1 == result.getInt("count"));
        
        // Check if it's the correct patient
        assertEquals(FHIRPatientProcessor.parseFHIR(patient2), pda.getPatient(patient2.getId().toString(), pda.getDbConnection()));
        
    }

}
