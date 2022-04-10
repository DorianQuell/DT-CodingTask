package com.dorianquell.codingtask.dao;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.annotation.PreDestroy;

import org.hl7.fhir.r4.model.Patient;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Repository;

import com.dorianquell.codingtask.processor.FHIRPatientProcessor;

import lombok.Getter;

@Repository("PatientDAO")
public class PatientDataAccessService {

    @Getter
    private Connection dbConnection;

    @Value("${tablename}")
    private String tablename;

    @Value("${viewname}")
    private String viewname;

    /**
     * Given a FHIR patient resource, the patient will be inserted into the database
     * 
     * @param patient
     *            The FHIR patient resource to be added
     * @param conncection
     *            to the database
     * @return Boolean which indicates if the patient could be added or not
     */
    public Boolean addPatient(Patient patient, Connection connection) {
        if (calculateAge(patient.getBirthDate()) >= 18) {
            String insertSQL = "INSERT INTO " + tablename
                    + " (id, firstname, lastname, gender, birthdate, date_created, fhir) VALUES(?,?,?,?,?,?,?)";
            try {
                PreparedStatement pstmt = connection.prepareStatement(insertSQL);
                pstmt.setString(1, patient.getId());
                pstmt.setString(2, patient.getName().get(0).getGivenAsSingleString());
                pstmt.setString(3, patient.getName().get(0).getFamily());
                pstmt.setString(4, patient.getGender().toString().toLowerCase());
                pstmt.setString(5, patient.getBirthDateElement().asStringValue());
                pstmt.setDate(6, new java.sql.Date(patient.getMeta().getLastUpdated().getTime()));
                pstmt.setString(7, FHIRPatientProcessor.parseFHIR(patient));
                pstmt.executeUpdate();
                return true;
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return false;
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
        String deleteSQL = "DELETE FROM " + tablename + " WHERE id = ?";
        try {
            PreparedStatement pstmt = connection.prepareStatement(deleteSQL);
            pstmt.setString(1, id);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Given a FHIR patient resource, the patient will be inserted into the database AND all old version of that patient will be
     * deleted
     * 
     * @param patient
     *            The FHIR patient resource to be added
     * @param conncection
     *            to the database
     * @return Boolean which indicates if the patient could be added or not
     */

    public Boolean updatePatient(Patient patient, Connection connection) {
        String searchSQL =
                "DELETE FROM " + tablename + " WHERE firstname = ? AND lastname = ? AND gender = ? and birthdate = ?";
        try {
            PreparedStatement pstmt = connection.prepareStatement(searchSQL);
            pstmt.setString(1, patient.getName().get(0).getGivenAsSingleString());
            pstmt.setString(2, patient.getName().get(0).getFamily());
            pstmt.setString(3, patient.getGender().toString().toLowerCase());
            pstmt.setString(4, patient.getBirthDateElement().asStringValue());
            pstmt.executeUpdate();

            return (addPatient(patient, connection));

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    /**
     * 
     * @param searchParameters
     *            map with all search parameters as keys and value as the desired fields. Invalid fields will be ignored. An empty
     *            map will return the full data set.
     * @param connection
     *            to database
     * @return JSONArray with all fhir resources
     */
    public JSONArray search(Map<String, String> searchParameters, Connection connection) {
        String templateQuery = "SELECT fhir FROM " + viewname;
        String searchSQL = templateQuery;

        try {
            // If there is atleast one search parameter generate the sql query
            if (searchParameters.size() > 0) {
                searchSQL += " WHERE ";
                // Get a list of all column names
                ArrayList<String> columnNames = getAllColumnNames(connection);

                // Add all search parameters which exist as column names
                for (Map.Entry<String, String> entry : searchParameters.entrySet()) {
                    if (columnNames.contains(entry.getKey()))
                        searchSQL += entry.getKey() + " = '" + entry.getValue() + "' AND ";
                }
                // Clean up the query and remove the last AND
                searchSQL = (searchSQL + ";").replace(" AND ;", "").replace(" WHERE ;", "");
                System.out.println(searchSQL);
            }

            ResultSet res = connection.createStatement().executeQuery(searchSQL);
            JSONArray resArr = new JSONArray();
            while (res.next()) {
                resArr.put(new JSONObject(res.getString("fhir")));
            }
            return resArr;

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * 
     * @param connection
     *            to database
     * @return String List of all column names
     * @throws SQLException
     */
    private ArrayList<String> getAllColumnNames(Connection connection) throws SQLException {
        ArrayList<String> columnNames = new ArrayList<String>();
        String sql = "SELECT * FROM " + tablename + " WHERE 1 = 0";
        ResultSetMetaData rsmd = connection.createStatement().executeQuery(sql).getMetaData();
        int columnCount = rsmd.getColumnCount();
        for (int i = 1; i <= columnCount; i++) {
            columnNames.add(rsmd.getColumnName(i));
        }
        return columnNames;
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
    public void deletePatientRecordsOlderThan(Date deletionDate, Connection connection) {
        String deleteSQL = "DELETE FROM " + tablename + " WHERE date_created < ?";
        try {
            PreparedStatement pstmt = connection.prepareStatement(deleteSQL);
            pstmt.setDate(1, new java.sql.Date(deletionDate.getTime()));
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Will be run on Start up, used for all initialization calls
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onStartUp() {

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
            ResultSet tables = dmd.getTables(null, null, tablename, null);

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
        String createTableSQL = "CREATE TABLE " + tablename + " (" + "    id varchar(255), "
                + "    firstname varchar(255), " + "    lastname varchar(255), " + "    gender varchar(10), "
                + "    birthdate text, " + "    date_created date, " + "fhir text, " + "PRIMARY KEY (id)" + ");";
        connection.createStatement().execute(createTableSQL);
    }

    /*
     * Creates the View of the Patients table to show data ordered by last name
     */
    private void createPatientsView(Connection connection) throws SQLException {
        String createView = "CREATE VIEW " + viewname + " AS SELECT * FROM " + tablename + " ORDER BY lastname;";
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
