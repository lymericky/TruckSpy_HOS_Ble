# TruckSpy_HOS_Ble
Currently, TruckSpy_HOS_Ble is a stand-alone application simply utilized for development purposes and will eventually become a library for the TruckSpy Enterprise App. Its purpose is to eventually enable independent HOS functionality via Bluetooth Low Energy within the TruckSpy Enterprise App.
## Software Architecture Pattern (MVVM)
Based on the Eldman app by Pacific Track, TruckSpy_HOS_Ble incorporates Eldman’s core logic to communicate with a tracker. Initially, Eldman’s architecture pattern was primarily Model-View-Presenter; however, my intention is to apply the Model-View-ViewModel pattern within the TruckSpy_HOS_Ble and implement classes to handle the core logic and communication with databases.  
## Requirements to run the application
TruckSpy_HOS_Ble requires a physical device to run on (Not the emulator) and a tracking device, such as the Pacific Track PT-40.
The tracking device must either be connected to a vehicle via OBDII, J1939, or a using vehicle ECU simulator.   
## Installation
Simply open the project in Android Studio and run on an actual device. 
## Usage
Upon running the app, you are welcomed by the Hours of Service activity, press "Let's Go" to get started
Next, press the "Connect" button and wait for the device to scan for local Bluetooth LE devices. Once, your tracker appears, select it and another activity immediately opens with the tracker's information populating the fields automatically.
NOTE: this activity is visible to the user for development purposes only.   
If the tracker has any events recorded, the "Stored Events" fragment will appear with the total number of events at the top. Press "Retrieve Stored Events" to retrieve the last event ID and then "Details" to review the specific details of it. At the bottom of the screen, press the Hours of Service button to view the drivers Hours of Service activity.   
## Hours of Service
The Hours of Service activity will be the primary view for production grade users (i.e., our clients/drivers).
At the top, it displays the driver, vehicle, and trailer ID as well as the country flag and current duty status. Below that, users can select "Begin trip - End Break" to change to active-duty status, get trip distance, and start the counters. Selecting "Start Break" stops and resets the chronometer and timer.
Currently, the only active button is for the "Events" which again displays the last stored event, but with more details.     
 # Project Requirements Breakdown
## TS-241 Implement BLE Connection
The launch activity is the MainActivity, and I imported the Eldman app as a library. I did this because, initially, even after following their instructions, the Eldman app contained a lot of deprecated code, obsolete versions of libraries, and utilized JAVA 8, which wouldn't compile. Moreover, Eldman’s Manifest was missing some of the required permissions and the "uses-feature...ble" statement. Without them, we couldn't publish the app. So, it was easier to create a new application with up-to-date criteria, import Eldman as a library, and start debugging from there. As a result, I can successfully connect to, and extract data from the tracker.   
Upon launch, first and foremost, the MainActivity first checks for Bluetooth support before consuming any more resources. Then, using an intent, the MainActivity “Let’s Go” button opens Eldman’s com.pt.devicemanager TrackerManagerActivity, which contains the fragment manager for the initial tracker connection button. I intend to change this because I find it redundant, and there is a lot of unnecessary and disabled code for some reason. However, some of the fundamental logic within the activity is necessary for Bluetooth LE; therefore, only the fragment views will change or be eliminated entirely.  
## TS-240 Obtain Motion Status from BLE Connected Device
## TS-244 Obtain GPS Position from GPS Device via BLE
## TS-243 Obtain Engine Hours from ECM
## TS-245 Obtain the VIN from the ECM
## TS-242 Calculate traveled miles from ECM for each power cycle
The com.pt.devicemanager TrackerService class utilizes the broadcast receiver BaseRequest.Key.LIVE_EVENT flag within a TelemetryEventRequest service to monitor the devices real-time movement and change in status (TS-240). Within this request, I acquire the speed (velocity), Engine Age (how long the vehicle is off), and (TS-243) Engine Hours (how long the vehicle is running). Additionally, the TelemetryEventRequest provides the start/end time, heading, (TS-244) lat/long, (TS-245) VIN, and odometer, which allowed me to calculate trip data (TS-242) and convert kph to mph.  
## HoursOfServiceActivity and EventListFragment
I created the HoursOfServiceActivity for the main user interface. It contains the getters/setters for the driver database, the timer and chronometer logic, duty status, and buttons to the other necessary functions. However, the EVENTS button is the only one currently active. Pressing it inflates the EventListFragment where I display the last stored event data.     
## Libraries
fragments


implementation "androidx.fragment:fragment:1.5.2"

Firebase

Import the BoM for the Firebase platform


implementation platform('com.google.firebase:firebase-bom:28.4.2')

Declare the dependencies for the Crashlytics and Analytics libraries


implementation 'com.google.firebase:firebase-crashlytics'
implementation 'com.google.firebase:firebase-analytics'
implementation 'com.google.firebase:firebase-storage'
implementation 'com.google.firebase:firebase-auth'
implementation 'androidx.legacy:legacy-support-v4:1.0.0'
implementation 'androidx.appcompat:appcompat:1.5.0'
implementation 'com.google.android.material:material:1.7.0-alpha03'
implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
implementation 'androidx.preference:preference:1.1.1' 

pt-sdk


implementation project(':pt-sdk-release')
implementation 'com.google.gms.google-services:com.google.gms.google-services.gradle.plugin:4.3.13'
implementation 'com.google.android.gms:play-services-location:20.0.0'

Nordic


implementation 'no.nordicsemi.android.support.v18:scanner:1.6.0'
implementation 'no.nordicsemi.android:log:2.3.0'
implementation 'no.nordicsemi.android:ble-common:2.3.1'

implementation 'commons-net:commons-net:20030805.205232'
implementation 'com.google.code.gson:gson:2.8.9'

USB


implementation 'com.github.mik3y:usb-serial-for-android:3.4.6'
