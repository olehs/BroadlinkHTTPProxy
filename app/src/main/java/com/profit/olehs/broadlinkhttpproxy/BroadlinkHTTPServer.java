package com.profit.olehs.broadlinkhttpproxy;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.Map;
import java.util.logging.Logger;

import cn.com.broadlink.blnetwork.BLNetwork;

class BroadlinkHTTPServer extends NanoHTTPD {
    /**
     * logger to log to.
     */
    private static final Logger LOG = Logger.getLogger(BroadlinkHTTPServer.class.getName());

    private BLNetwork mBlNetwork;
    private String api_id = "api_id";
    private String command = "command";

    private DBHelper dbHelper;

    public BroadlinkHTTPServer() {
        super(8080);
        mBlNetwork = BLNetwork.getInstanceBLNetwork(MainActivity.getAppContext());
        dbHelper = new DBHelper(MainActivity.getAppContext());
        addAllDevices();
    }

    @Override
    public Response serve(IHTTPSession session) {
        Method method = session.getMethod();
        String uri = session.getUri();
        BroadlinkHTTPServer.LOG.info(method + " '" + uri + "' ");
        Map<String, String> parms = session.getParms();

        switch (uri) {
            case "/findDevices":
                String s = probeList();
                JsonObject devs = new JsonParser().parse(s).getAsJsonObject();
                updateDevices(devs);
                return newFixedLengthResponse(Response.Status.OK, "application/json", s);

            case "/listDevices":
                return newFixedLengthResponse(Response.Status.OK, "application/json", listDevices());

            case "/removeDevice":
                if (parms.get("mac") != null)
                    return newFixedLengthResponse(Response.Status.OK, "application/json", removeDevice(parms.get("mac")));

            case "/study":
                if (parms.get("mac") != null)
                    return newFixedLengthResponse(Response.Status.OK, "application/json", RM2Study(parms.get("mac")));

                break;
            case "/getCode":
                if (parms.get("mac") != null)
                    return newFixedLengthResponse(Response.Status.OK, "application/json", RM2GetCode(parms.get("mac")));

                break;
            case "/saveCode":
                if (parms.get("mac") != null)
                    return newFixedLengthResponse(Response.Status.OK, "application/json", saveCode(parms));

                break;
            case "/sendCode":
                if (parms.get("mac") != null && parms.get("data") != null)
                    return newFixedLengthResponse(Response.Status.OK, "application/json", RM2SendCode(parms.get("mac"), parms.get("data")));
                else if (parms.get("id") != null)
                    return newFixedLengthResponse(Response.Status.OK, "application/json", RM2SendCodeById(parms.get("id")));
                else if (parms.get("name") != null)
                    return newFixedLengthResponse(Response.Status.OK, "application/json", RM2SendCodeByName(parms.get("name"), parms.get("mac")));

                break;
            case "/removeCode":
                if (parms.get("id") != null)
                    return newFixedLengthResponse(Response.Status.OK, "application/json", removeCodeById(parms.get("id")));
                else if (parms.get("name") != null)
                    return newFixedLengthResponse(Response.Status.OK, "application/json", removeCodeByName(parms.get("name"), parms.get("mac")));

                break;
            case "/refresh":
                if (parms.get("mac") != null)
                    return newFixedLengthResponse(Response.Status.OK, "application/json", RM2Refresh(parms.get("mac")));

                break;
            case "/scanCode":
                if (parms.get("mac") != null)
                    return newFixedLengthResponse(Response.Status.OK, "application/json", RM2ScanCode(parms.get("mac"), parms.get("name")));
                break;
            case "/listCodes":
                return newFixedLengthResponse(Response.Status.OK, "application/json", listCodes());
        }

        return newFixedLengthResponse(Response.Status.NOT_FOUND, NanoHTTPD.MIME_PLAINTEXT, "Not Found");
    }

    public void updateDevices(JsonObject devs) {
        if (!devs.has("code") || devs.get("code").getAsInt() != 0)
            return;

        JsonArray list = devs.get("list").getAsJsonArray();
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        for (int i = 0; i < list.size(); i++) {
            JsonObject dev = list.get(i).getAsJsonObject();
            String mac = dev.get("mac").getAsString();
            if(!dev.get("type").getAsString().equals("RM2"))
                continue;

            ContentValues cv = new ContentValues();
            cv.put("mac", mac);
            cv.put("type", dev.get("type").getAsString());
            cv.put("name", dev.get("name").getAsString());
            cv.put("lock", dev.get("lock").getAsInt());
            cv.put("password", dev.get("password").getAsString());
            cv.put("id", dev.get("id").getAsInt());
            cv.put("subdevice", dev.get("subdevice").getAsInt());
            cv.put("key", dev.get("key").getAsString());

            db.replace("devices", null, cv);
            addDevice(cv);
        }
    }

    public void addAllDevices() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        Cursor c = db.query("devices", null, null, null, null, null, null);
        if (c.moveToFirst()) {
            do {
                ContentValues cv = new ContentValues();
                DatabaseUtils.cursorRowToContentValues(c, cv);
                addDevice(cv);
            } while (c.moveToNext());
        }
        c.close();
    }

    // Probe List
    public String probeList() {
        JsonObject in = new JsonObject();
        in.addProperty(api_id, 11);
        in.addProperty(command, "probe_list");
        String string = in.toString();
        return mBlNetwork.requestDispatch(string);
    }

    private String RM2Study(String mac) {
        JsonObject in = new JsonObject();
        in.addProperty(api_id, 132);
        in.addProperty(command, "rm2_study");
        in.addProperty("mac", mac);
        String inString = in.toString();
        return mBlNetwork.requestDispatch(inString);
    }

    private String RM2GetCode(String mac) {
        JsonObject in = new JsonObject();
        in.addProperty(api_id, 133);
        in.addProperty(command, "rm2_code");
        in.addProperty("mac", mac);
        String inString = in.toString();

        return mBlNetwork.requestDispatch(inString);
    }

    private String RM2ScanCode(String mac, String name) {
        String resp = RM2Study(mac);
        JsonObject json = new JsonParser().parse(resp).getAsJsonObject();
        if (!json.has("code") || json.get("code").getAsInt() != 0)
            return resp;

        int code;
        do {
            try {
                Thread.sleep(500);                 //1000 milliseconds is one second.
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
            resp = RM2GetCode(mac);
            json = new JsonParser().parse(resp).getAsJsonObject();
            if (!json.has("code"))
                return resp;
            code = json.get("code").getAsInt();
        } while (code == -10);

        if (code == 0) {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            ContentValues cv = new ContentValues();
            cv.put("mac", mac);
            cv.put("name", name);
            cv.put("data", json.get("data").getAsString());
            json.addProperty("id", db.insert("codes", null, cv));
        }
        return json.toString();
    }

    private String RM2SendCode(String mac, String code) {
        JsonObject in = new JsonObject();
        in.addProperty(api_id, 134);
        in.addProperty(command, "rm2_send");
        in.addProperty("mac", mac);
        in.addProperty("data", code);
        String inString = in.toString();

        return mBlNetwork.requestDispatch(inString);
    }

    private String RM2SendCodeById(String id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        Cursor c = db.query("codes", null, "id=?", new String[]{id}, null, null, null);
        try {
            if (!c.moveToFirst()) {
                return "{\"code\": -1, \"msg\": \"Not found\"}";
            }

            return RM2SendCode(c.getString(c.getColumnIndex("mac")), c.getString(c.getColumnIndex("data")));
        } finally {
            c.close();
        }
    }

    private String removeCodeById(String id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        int count = db.delete("codes", "id=?", new String[]{id});
        if (count != 1)
            return "{\"code\": 2, \"msg\": \"Not found\"}";
        else
            return "{\"code\": 0, \"msg\": \"Removed\"}";
    }

    private String removeCodeByName(String name, @Nullable String mac) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        String where = "name=?";
        String pars[];
        if (mac != null) {
            where += " and mac=?";
            pars = new String[]{mac, name};
        } else {
            pars = new String[]{name};
        }

        int count = db.delete("codes", where, pars);
        if (count <= 0)
            return "{\"code\": 2, \"msg\": \"Not found\"}";
        else
            return "{\"code\": 0, \"msg\": \"Removed " + count + " code(s)\"}";
    }

    private String RM2SendCodeByName(String name, @Nullable String mac) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        String where = "name=?";
        String pars[];
        if (mac != null) {
            where += " and mac=?";
            pars = new String[]{name, mac};
        } else {
            pars = new String[]{name};
        }

        String resp = "{\"code\": 0, \"list\": [";
        int i = 0;
        Cursor c = db.query("codes", null, where, pars, null, null, null);
        try {
            if (!c.moveToFirst())
                return "{\"code\": -1, \"msg\": \"Not found\"}";

            do {
                if (i++ > 0)
                    resp += ", ";
                resp += RM2SendCode(c.getString(c.getColumnIndex("mac")), c.getString(c.getColumnIndex("data")));
            } while (c.moveToNext());

            resp += "]}";
            return resp;
        } finally {
            c.close();
        }
    }

    private String RM2Refresh(String mac) {
        JsonObject in = new JsonObject();
        in.addProperty(api_id, 131);
        in.addProperty(command, "rm2_refresh");
        in.addProperty("mac", mac);
        String inString = in.toString();

        return mBlNetwork.requestDispatch(inString);
    }

    public JsonObject device2Json(ContentValues device) {
        JsonObject in = new JsonObject();
        in.addProperty("mac", device.getAsString("mac"));
        in.addProperty("type", device.getAsString("type"));
        in.addProperty("name", device.getAsString("name"));
        in.addProperty("lock", device.getAsInteger("lock"));
        in.addProperty("password", device.getAsString("password"));
        in.addProperty("id", device.getAsInteger("id"));
        in.addProperty("subdevice", device.getAsInteger("subdevice"));
        in.addProperty("key", device.getAsString("key"));
        return in;
    }

    public String addDevice(ContentValues device) {
        JsonObject in = device2Json(device);
        in.addProperty(api_id, 12);
        in.addProperty(command, "device_add");
        String string = in.toString();
        return mBlNetwork.requestDispatch(string);
    }

    public String listDevices() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        JsonObject json = new JsonObject();
        json.addProperty("code", 0);
        JsonArray list = new JsonArray();

        Cursor c = db.query("devices", null, null, null, null, null, "mac,id");
        if (c.moveToFirst()) {
            do {
                DatabaseUtils.cursorRowToContentValues(c, cv);
                list.add(device2Json(cv));
            } while (c.moveToNext());
        }
        c.close();

        json.add("list", list);
        return json.toString();
    }

    public String removeDevice(String mac) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int count = db.delete("devices", "mac=?", new String[] {mac});
        if (count <= 0)
            return "{\"code\": 2, \"msg\": \"Not found\"}";
        else
            return "{\"code\": 0, \"msg\": \"Removed " + count + " device\"}";
    }

    public String listCodes() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        JsonObject json = new JsonObject();
        json.addProperty("code", 0);
        JsonArray list = new JsonArray();

        Cursor c = db.query("codes", null, null, null, null, null, "id");
        if (c.moveToFirst()) {
            do {
                JsonObject code = new JsonObject();
                code.addProperty("id", c.getInt(c.getColumnIndex("id")));
                code.addProperty("mac", c.getString(c.getColumnIndex("mac")));
                code.addProperty("name", c.getString(c.getColumnIndex("name")));
                code.addProperty("data", c.getString(c.getColumnIndex("data")));
                list.add(code);
            } while (c.moveToNext());
        }
        c.close();

        json.add("list", list);
        return json.toString();
    }

    public String saveCode(Map<String, String> parms) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("mac", parms.get("mac"));
        if (parms.get("name") != null)
            cv.put("name", parms.get("name"));
        if (parms.get("id") != null)
            cv.put("id", parms.get("id"));
        if (parms.get("data") != null)
            cv.put("data", parms.get("data"));

        long id = db.replace("codes", null, cv);
        return "{\"code\": 0, \"id\": " + id + ", \"msg\": \"Saved\"}";
    }

    class DBHelper extends SQLiteOpenHelper {

        public DBHelper(Context context) {
            super(context, "BroadLinkDB", null, 1);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("create table devices ("
                    + "mac text primary key,"
                    + "type text,"
                    + "name text,"
                    + "lock integer,"
                    + "password text,"
                    + "id integer,"
                    + "subdevice integer,"
                    + "key text"
                    + ");");

            db.execSQL("create table codes ("
                    + "id integer primary key autoincrement,"
                    + "mac text,"
                    + "name text,"
                    + "data text"
                    + ");");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        }
    }
}