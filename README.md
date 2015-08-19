# BroadLinkHTTPProxy

#####API: HTTP GET request, default port 8080. Response is application/json directly from Broadlink RMPro (some keys may be added).

http://android.device:8080/findDevices 
- Search for available RM2 Pro devices in the network, add them to internal database, existing devices are updated.


http://android.device:8080/listDevices 
- List devices, saved in internal database.


http://android.device:8080/removeDevice?mac=11:22:33:44:55:66 
- Remove device from DB by MAC.


http://android.device:8080/scanCode?mac=11:22:33:44:55:66&name=Samsung 
- Switch RM2 to Study-mode. If studying successful, saves code to DB with specified name (i.e. Samsung).
Returns studied code and unique internal ID.


http://android.device:8080/sendCode?mac=11:22:33:44:55:66&name=Samsung 
- Emits code named 'name' to the device with specified MAC.


http://android.device:8080/sendCode?name=Samsung 
- Emits code named 'name' on all devices, containing this code.


http://android.device:8080/sendCode?id=1 
- Emits code with ID = 'id' on device, where it belongs to.


http://android.device:8080/sendCode?mac=11:22:33:44:55:66&data=26004e00080008320900059696931337133813361311141114111411151113361338123713131212131212121411131312381311131114121311141312111437121214361336143615361336133812000d0500000000000000000000 
- Emits code from 'data' on device 'mac'.


http://android.device:8080/removeCode?mac=11:22:33:44:55:66&name=Samsung 
- Removes code named 'name' from device 'mac' from internal DB.


http://android.device:8080/removeCode?name=Samsung 
- Removes code named 'name' from all devices in DB.


http://android.device:8080/removeCode?id=1 
- Removes code with ID = 'id' from DB.


http://android.device:8080/study?mac=11:22:33:44:55:66 
- Switches device 'mac' to study-mode.


http://android.device:8080/getCode?mac=11:22:33:44:55:66 
- Gets studied code from device 'mac'.


http://android.device:8080/saveCode?mac=11:22:33:44:55:66&name=Fan&id=3&data=26004e00080008320900059696931337133813361311141114111411151113361338123713131212131212121411131312381311131114121311141312111437121214361336143615361336133812000d0500000000000000000000 
- Inserts or updates code with ID = 'id'.
name and id can be ommited.


http://android.device:8080/listCodes 
- Lists saved codes.


http://android.device:8080/refresh?mac=11:22:33:44:55:66 
- Retrieves current temperature from device 'mac'.
