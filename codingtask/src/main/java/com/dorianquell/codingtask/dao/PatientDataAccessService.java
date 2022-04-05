package com.dorianquell.codingtask.dao;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import javax.annotation.PreDestroy;

import org.hl7.fhir.r4.model.Patient;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Repository;

import com.dorianquell.codingtask.processor.FHIRPatientProcessor;

import lombok.Getter;

@Repository("PatientDAO")
public class PatientDataAccessService {

    @Getter
    private Connection dbConnection;

    /**
     * Given a FHIR patient resource, the patient will be inserted into the database
     * 
     * @param patient
     *            The FHIR patient resource to be added
     * @return Boolean which indicates if the patient could be added or not
     */
    public Boolean addPatient(Patient patient, Connection connection) {
        if (calculateAge(patient.getBirthDate()) >= 18) {
            String insertSQL =
                    "INSERT INTO patients (id, firstname, lastname, gender, birthdate, date_created, fhir) VALUES(?,?,?,?,?,?,?)";
            try {
                PreparedStatement pstmt = connection.prepareStatement(insertSQL);
                pstmt.setString(1, patient.getId());
                pstmt.setString(2, patient.getName().get(0).getGivenAsSingleString());
                pstmt.setString(3, patient.getName().get(0).getFamily());
                pstmt.setString(4, patient.getGender().toString().toLowerCase());
                pstmt.setDate(5, new java.sql.Date(patient.getBirthDate().getTime()));
                pstmt.setDate(6, new java.sql.Date(patient.getMeta().getLastUpdated().getTime()));
                pstmt.setString(7, FHIRPatientProcessor.parseFHIR(patient));
                pstmt.executeUpdate();
                return true;
            } catch (SQLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return false;
    }

    /**
     * Returns the FHIR resource for a patient given it's ID
     * 
     * @param id
     *            of the patient to be returned
     * @param connection
     *            to the database
     * @return String containing the FHIR resource in a json format
     */
    public String getPatient(String id, Connection connection) {
        String searchSQL = "SELECT fhir FROM patientsView WHERE id = ?";
        try {
            PreparedStatement pstmt = connection.prepareStatement(searchSQL);
            pstmt.setString(1, id);
            ResultSet res = pstmt.executeQuery();
            if (res.next())
                return res.getString("fhir");
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Deletes a patient given its ID
     * 
     * @param id
     *            of the patient to be deleted
     * @param connection
     *            to the database
     * @return boolean showing if the deletion was successful
     */
    public Boolean deletePatient(String id, Connection connection) {
        String deleteSQL = "DELETE FROM patients WHERE id = ?";
        try {
            PreparedStatement pstmt = connection.prepareStatement(deleteSQL);
            pstmt.setString(1, id);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Writes the content of the database to the command line Debugging only - Remove later
     * 
     * @param connection
     *            to the database
     */
    public void printDB(Connection connection) {
        String printSQL = "SELECT * FROM patientsView";
        try {
            Statement stmt = connection.createStatement();
            ResultSet res = stmt.executeQuery(printSQL);
            while (res.next()) {
                System.out.println(res.getString("fhir"));
            }
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Given a gender (male, female, other, unknown, ...) the function will return all patients of the given gender
     * 
     * @param gender
     * @param connection
     *            to the database
     * @return JSONArray containing all FHIR resources of the found patients as JSONObjects
     */
    public JSONArray findAllPatientsByGender(String gender, Connection connection) {
        gender = gender.toLowerCase();
        String searchGenderSQL = "SELECT * FROM patientsView WHERE gender = ?";
        try {
            PreparedStatement pstmt = connection.prepareStatement(searchGenderSQL);
            pstmt.setString(1, gender);
            ResultSet res = pstmt.executeQuery();
            JSONArray resArr = new JSONArray();
            while (res.next()) {
                resArr.put(new JSONObject(res.getString("fhir")));
            }
            return resArr;
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Given a birth date the function will return the age of the person as an integer
     * 
     * @param birthdate
     * @return int age - in years
     */
    private int calculateAge(Date birthdate) {
        if (birthdate != null) {
            LocalDate bd = Instant.ofEpochMilli(birthdate.getTime()).atZone(ZoneId.systemDefault()).toLocalDate();
            return Period.between(bd, LocalDate.now()).getYears();
        }
        return 0;
    }

    /**
     * Given a date the function will delete all resources which were created before that date
     * 
     * @param deletionDate
     * @param connection
     *            to the database
     */
    private void deletePatientRecordsOlderThan(Date deletionDate, Connection connection) {
        String deleteSQL = "DELETE FROM patients WHERE date_created < ?";
        try {
            PreparedStatement pstmt = connection.prepareStatement(deleteSQL);
            pstmt.setDate(1, new java.sql.Date(deletionDate.getTime()));
            pstmt.executeUpdate();
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Will be run on Start up, used for all initialization calls
     */
    @EventListener(ApplicationReadyEvent.class)
    private void onStartUp() {

        // Create the DB, tables etc. if they don't exist yet
        dbConnection = initDB();

        // Start the daily clean up process
        dbCleanUp();
    }

    /*
     * Will be run right before shut down, used to close all connections
     */
    @PreDestroy
    private void onClose() {
        try {
            dbConnection.close();
            System.out.println("Connection closed!");
        } catch (SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Opens the connection to the Database and creates the tables and views if needed
     * 
     * @return Connection to the SQLite DB
     */
    private Connection initDB() {
        Connection connection = null;
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:patientdata.db");
            System.out.println("Database connection opened!");
            // Check if table exists
            DatabaseMetaData dmd = connection.getMetaData();
            ResultSet tables = dmd.getTables(null, null, "patients", null);

            // If it doesn't exist create the table and the view
            if (!tables.next()) {
                createPatientsTable(connection);
                createPatientsView(connection);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return connection;
    }

    /*
     * Creates the Patients table
     */
    private void createPatientsTable(Connection connection) throws SQLException {
        String createTableSQL = "CREATE TABLE patients (" + "    id varchar(255), " + "    firstname varchar(255), "
                + "    lastname varchar(255), " + "    gender varchar(10), " + "    birthdate date, "
                + "    date_created date, " + "fhir text, " + "PRIMARY KEY (id)" + ");";
        connection.createStatement().execute(createTableSQL);
    }

    /*
     * Creates the View of the Patients table to show data ordered by last name
     */
    private void createPatientsView(Connection connection) throws SQLException {
        String createView = "CREATE VIEW patientsView AS SELECT * FROM patients ORDER BY lastname;";
        connection.createStatement().execute(createView);
    }

    /*
     * Starts a thread which will delete all data older than a year, once a day
     */
    private void dbCleanUp() {

        Thread dailyCleanUp = new Thread() {
            public void run() {
                LocalDate today;
                while (true) {

                    today = LocalDate.now().minusYears(1);
                    deletePatientRecordsOlderThan(Date.from(today.atStartOfDay(ZoneId.systemDefault()).toInstant()),
                        dbConnection);

                    try {
                        sleep(TimeUnit.DAYS.toMillis(1));
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                }
            }
        };

        dailyCleanUp.start();
    }

}
