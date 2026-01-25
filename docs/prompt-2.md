As Web Bluetooth isn't supported in Android's WebView, and also in iOS, from now on we desided to try a new approach from scrath. Please write this new approach following the requirements below:

. from now on, the code present in folder @common/src/main/java/com/example/omronapp/omron and @common/src/main/java/com/example/omronapp/omromwebbluetooth should be used only to view how the 2 old approaches that didn't work were done and for reuse some ideas or structures that can │
 be useful in these new approach from scratch. │

. This new approach should implement a new native code in /android folder that should be used instead the Bluetooth cn1lib. Important, don't use any code present in the cn1lib Bluetooth. You should code from scratch a new native android code (following all the code name one good practices for that).

. after, you should create  a new @common/src/main/java/com/example/omronapp/mynativebluetooth and implement a new codenameone (in java) that does the samething the old approaches were trying to do. Define a good architecture using clean code principles.

. after, change OmronApp.java to use the new UI you wrote in mynativebluetooth folder. Allowing deploy the new app in a real device for testing.

. the new UI should have the same visual elements present in the old approaches, allowing capture all the measures stored in the OMRON device as a JSON string and also the log messages exposed during the Bluetooth communication.