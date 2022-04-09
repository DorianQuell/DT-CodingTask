# DT-CodingTask

Simple API which stores Patients in a SQLite database.  
If run, the jar will open the API on localhost:8080.

### Available Functions
#### PUT
Given a json with patient data, the patient will be added to the database.  
If information to this patient already exists, the data will be updated.  
Example input data:  
```
{
  "firstname":"Testodor",
  "lastname":"Testington",
  "gender":"Male",
  "birthdate":"1990-01-01"
}
```  
Example REST call:  
``` curl -X PUT 'http://localhost:8080' -H 'Content-Type: application/json' -d '{"firstname":"Testodor","lastname":"Testington","gender":"Male","birthdate":"1990-01-01"}'```  
If the patient was added successfully, the call will return the patients ID.  
*Note: Patients under the age of 18 will not be added!*

#### GET
**/search** allows the user the create a SQL search query. Using it without any parameters will return all patients.  
The fields ``` id ```, ``` firstname ```, ``` lastname ```, and ``` gender ``` can be searched.  
The search results are returned as a JSON array containing patient FHIR resources.  
Multiple results will also be returned ordered alphabetically by their last names.  
Example REST call:  
``` curl -X GET 'http://localhost:8080/search?gender=female' ``` will return all female patients.  
``` curl -X GET 'http://localhost:8080/search?gender=female&firstname=Anna' ``` will return all female patients called Anna.  
*Note: Invalid search parameters will be ignored and the search query will be built without them!*

#### DELETE
Allows the user to delete patients based on their ID.  
Example REST call:
``` curl -X DELETE 'http://localhost:8080?id=7ec087de-aa9c-4947-975b-3f9e24b1499e' ```

#### Clean Up
Once a day the program will remove all patient resources from the database which were created and not updated for the last year.

#### Building and running the project
The project uses Maven and can be built by using ``` mvn clean install ```.  
The jar (in the target folder) can be started by using ``` java -jar codingtask-0.0.1-SNAPSHOT.jar ```
