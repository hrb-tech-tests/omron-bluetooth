1. from now on, the code present in folder @common/src/main/java/com/example/omronapp/omron should be used only to view how the old approach that didn't work was done and for reuse some ideas or structures that can   │
   be useful in the new hybrid approach                                                                                                                                                                                    │

2. Complete this the new hybrid approach. It should be coded always in the  @common/src/main/java/com/example/omronapp/omromwebbluetooth folder, as it is already done so far. Or in the @common/src/main/java/com/example/omronapp when it makes sense.

3. The capturedDataButton in OmronWebBluetoothForm should be initially disabled. After the JSON structure with all measures data in the device's memory is ready, this button should be enabled. Take care of using appropriated background colors to indicate that if the button is disabled or enabled.

4. The abortButton in OmronWebBluetoothForm should always be enabled.                                                                                                                              │

5. implement the action for the connection button inside the HTML form. in This action all the bluetooth communication with the OMROM device should be done using the internal browser bluetooth api. All the health         │
   measures from the device should be captured and converted to a JSON structured string and placed in the multiline text labeled as jsonData (index.html - via AngularJS two way binding). At the same time, log all important messages about the bluetooth communication. This log should be appended directly in the multiline text labeled as Logs (also use AngularJS 2 way binding for that). To identify details of how the bluetooth communications between the cellphone and Omron device should be done, you also can consult the files (@docs/nRF-app-data.csv and @docs/nRF-app-log.txt) and the code present into @common/src/main/java/com/example/omronapp/omron folder (the older approach)

6. when the jsonData response is completed, the capturedDataButton should be enabled in the OmronWebBluetoothForm (use javascritp-to-java bridge for that). When capturedDataButton is enabled, after pressed the form should be closed, and it should have two new methods, One to get the content of JSON Data in index.html via bridge. Another to get the content of Logs in index.html (also via Brifge provided by Code Name Oone). Then, the developer can get this data and send them to the IwServer using the way he considers better. This sending isn't part of this project.

7. when necessary, update .md files present into the project, EXCEPT this file.
                                     
