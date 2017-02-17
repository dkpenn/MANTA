package com.mymanet.manta;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by dk on 12/3/16.
 * http://www.vogella.com/tutorials/AndroidSQLite/article.html#more-on-listviews
 * http://hmkcode.com/android-simple-sqlite-database-tutorial/
 */

class MySQLLiteHelper extends SQLiteOpenHelper {

    // database constants
    private static final String DATABASE_NAME = "filenames.db";
    private static final int DATABASE_VERSION = 2; // UPDATE every time db is updated

    // tables

    // keeps track of files available to the application
    private static final String TABLE_FILES = "files";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_FILENAME = "filename";
    private static final String FILES_CREATE = "create table " + TABLE_FILES + " (" + COLUMN_ID +
            " integer primary key autoincrement, " + COLUMN_FILENAME + " text not null);";

    // used by source node to keep track of status of a request
    private static final String TABLE_REQUEST = "request";
    private static final String COLUMN_STATUS = "status";
    private static final String REQUEST_CREATE = "create table " + TABLE_REQUEST + " (" +
            COLUMN_FILENAME + " text primary key, " + COLUMN_STATUS + " integer);";

    // used by file owner to keep track of what responses it's sent
    private static final String TABLE_RESPONSE = "response";
    private static final String COLUMN_SRC = "src";
    private static final String RESPONSE_CREATE = "create table " + TABLE_RESPONSE + " (" +
            COLUMN_FILENAME + " text, " + COLUMN_SRC + " text, " + COLUMN_STATUS +
            " integer, primary key (" + COLUMN_FILENAME + ", " + COLUMN_SRC + "));";

    // stores device names trusted by this device
    private static final String TABLE_TRUSTED = "trusted";
    private static final String COLUMN_DEVICE = "device";
    private static final String TRUSTED_CREATE = "create table " + TABLE_TRUSTED + " (" +
            COLUMN_DEVICE + " text primary key);";

    // used by transit nodes to keep track of requests that have been seen
    private static final String TABLE_FILTER = "filter";
    private static final String FILTER_CREATE = "create table " + TABLE_FILTER + " (" +
            COLUMN_FILENAME + " text, " + COLUMN_SRC + " text, primary key (" + COLUMN_FILENAME +
            ", " + COLUMN_SRC + "));";

    // keeps track of packets and which recipients they need to be sent to
    private static final String TABLE_SEND = "send";
    private static final String COLUMN_PACKET = "packet";
    private static final String COLUMN_TARGET = "target";
    private static final String SEND_CREATE = "create table " + TABLE_SEND + " (" +
            COLUMN_PACKET + " text, " + COLUMN_TARGET + " text, primary key (" + COLUMN_PACKET +
            ", " + COLUMN_TARGET + "));";

    //Database creation sql statement
    private static final String DATABASE_CREATE =
            FILES_CREATE + REQUEST_CREATE + RESPONSE_CREATE + TRUSTED_CREATE + FILTER_CREATE +
            SEND_CREATE;

    private static MySQLLiteHelper instance;

    MySQLLiteHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public static synchronized MySQLLiteHelper getHelper(Context context) {
        if (instance == null) {
            instance = new MySQLLiteHelper(context);
        }

        return instance;
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(FILES_CREATE);
        sqLiteDatabase.execSQL(REQUEST_CREATE);
        sqLiteDatabase.execSQL(RESPONSE_CREATE);
        sqLiteDatabase.execSQL(TRUSTED_CREATE);
        sqLiteDatabase.execSQL(FILTER_CREATE);
        sqLiteDatabase.execSQL(SEND_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        Log.w(MySQLLiteHelper.class.getName(),
                "Upgrading database from version " + oldVersion + " to "
                        + newVersion + ", which will destroy all old data");
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TABLE_FILES + ";");
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TABLE_REQUEST + ";");
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TABLE_RESPONSE + ";");
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TABLE_FILTER + ";");
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TABLE_TRUSTED + ";");
        onCreate(sqLiteDatabase);

    }

    // **** FILES TABLE FUNCTIONS ****

    //CRUD for File names
    public void addFile(MantaFile file) {
        //for logging

        Log.d("addFile", file.toString());

        // 1. get reference to writable DB
        SQLiteDatabase db = this.getWritableDatabase();

        List<MantaFile> files = getFilesWithName(file.getFilename());
        if(files.isEmpty()) {
            // 2. create ContentValues to add key "column"/value
            ContentValues values = new ContentValues();
            values.put(COLUMN_FILENAME, file.getFilename()); // get title

            // 3. insert
            db.insert(TABLE_FILES, // table
                    null, //nullColumnHack
                    values); // key/value -> keys = column names/ values = column values
        }
        // 4. close
        db.close();
    }

    // Deleting single book
    public void deleteFile(MantaFile file) {

        // 1. get reference to writable DB
        SQLiteDatabase db = this.getWritableDatabase();

        // 2. delete
        db.delete(TABLE_FILES,
                COLUMN_FILENAME + " = ?",
                new String[]{file.getFilename()});

        // 3. close
        db.close();

        Log.d("deleteFile", file.toString());

    }

    // Deleting single book
    public void deleteAllFiles() {

        // 1. get reference to writable DB
        SQLiteDatabase db = this.getWritableDatabase();

        // 2. delete
        db.delete(TABLE_FILES,
                "1",
                null);

        // 3. close
        db.close();

        Log.d("deleteFiles", "deleted everything");

    }

    public List<MantaFile> getAllFiles() {
        List<MantaFile> files = new LinkedList<MantaFile>();

        // 1. build the query
        String query = "SELECT  * FROM " + TABLE_FILES;
        // 2. get reference to writable DB
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(query, null);
        // 3. go over each row, build book and add it to list
        MantaFile file = null;
        if (cursor.moveToFirst()) {
            do {
                file = new MantaFile();
                file.setId(Integer.parseInt(cursor.getString(0)));
                file.setFilename(cursor.getString(1));
                files.add(file);
            } while (cursor.moveToNext());
        }
        Log.d("getAllBooks()", files.toString());
        return files;
    }

    public List<String> getAllFileNames() {
        List<String> files = new LinkedList<String>();

        // 1. build the query
        String query = "SELECT * FROM " + TABLE_FILES;
        // 2. get reference to writable DB
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(query, null);
        // 3. go over each row, build book and add it to list
        if (cursor.moveToFirst()) {
            do {
                files.add(cursor.getString(1));
            } while (cursor.moveToNext());
        }
        Log.d("getAllBooks()", files.toString());
        return files;
    }

    boolean containsFile(String filename) {
        List<String> files = getAllFileNames();
        for (String file : files) {
            if (file.equals(filename)) {
                return true;
            }
        }
        return false;
    }

    public List<MantaFile> getFilesWithName(String name) {
        List<MantaFile> files = new LinkedList<MantaFile>();

        // 1. build the query
        String query = "SELECT * FROM "  + TABLE_FILES + " WHERE " + COLUMN_FILENAME +
                "= " + "\"" + name + "\";";
        // 2. get reference to writable DB
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(query, null);
        // 3. go over each row, build book and add it to list
        MantaFile file = null;
        if (cursor.moveToFirst()) {
            do {
                file = new MantaFile();
                file.setId(Integer.parseInt(cursor.getString(0)));
                file.setFilename(cursor.getString(1));
                files.add(file);
            } while (cursor.moveToNext());
        }
        Log.d("getAllBooks()", files.toString());
        cursor.close();
        return files;
    }


    // **** REQUEST TABLE FUNCTIONS ****

    /**
     * Checks if request has previously been made by this device
     * @param filename requested file
     * @return success
     */
    boolean requestMade(String filename) {
        String query = "SELECT * FROM " + TABLE_REQUEST + " WHERE " + COLUMN_FILENAME + "= " +
                "\"" + filename + "\";";
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(query, null);
        boolean exists = cursor.moveToFirst();
        cursor.close();
        return exists;
    }

    /**
     * Update status of request
     *      0 = REQ
     *      1 = ACK
     *      2 = FILE
     */
    void updateRequest(String filename, int status) {
        String query = "UPDATE " + TABLE_REQUEST + " SET " + COLUMN_STATUS + "= " +
                Integer.toString(status) + " WHERE " + COLUMN_FILENAME + "= \"" + filename + "\";";
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL(query);
    }

    /**
     * Record new request made
     */
    void addRequest(String filename, int status) {
        String query = "INSERT INTO " + TABLE_REQUEST + " (" + COLUMN_FILENAME + ", " +
                COLUMN_STATUS + ") VALUES " + "(\"" + filename + "\", " + Integer.toString(status) +
                ");";
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL(query);
    }
    // **** RESPONSE TABLE FUNCTIONS ****

    /**
     * Record new request seen
     * Assume the status is 0 since this is the first time it's been seen
     */
    void addResponse(String filename, String src) {
        String query = "INSERT INTO " + TABLE_REQUEST + " (" + COLUMN_FILENAME + ", " +
                COLUMN_STATUS + ", " + COLUMN_SRC + ") VALUES " + "(\"" + filename + "\", " +
                Integer.toString(0) + ", \"" + src + "\");";
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL(query);
    }

    /**
     * Update request status
     *      0 = ACK
     *      1 = SEND
     */
    void updateResponse(String filename, String src, int status) {
        String query = "UPDATE " + TABLE_RESPONSE + " SET " + COLUMN_STATUS + "= " +
                Integer.toString(status) + " WHERE " + COLUMN_FILENAME + "= \"" + filename + "\" " +
                "AND " + COLUMN_SRC + "= \"" + src + "\";";
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL(query);
    }

    // **** FILTER TABLE FUNCTIONS ****

    /**
     * checks if a request has already passed through this node
     * @return result
     */
    boolean requestSeen(String filename, String src) {
        String query = "SELECT * FROM " + TABLE_FILTER + " WHERE " + COLUMN_FILENAME + "= " +
                "\"" + filename + "\" AND " + COLUMN_SRC + "= \"" + src + "\";";
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(query, null);
        boolean exists = cursor.moveToFirst();
        cursor.close();
        return exists;
    }

    void addFilterRequest(String filename, String src) {
        String query = "INSERT INTO " + TABLE_FILTER + " ( " + COLUMN_FILENAME + ", " + COLUMN_SRC +
                ") VALUES (\"" + filename + "\", \"" + src + "\");";
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL(query);
    }

    // **** TRUSTED TABLE FUNCTIONS ****

    /**
     * checks if a device is trusted by this node
     * @return result
     */
    boolean isTrusted(String device) {
        String query = "SELECT * FROM " + TABLE_TRUSTED + " WHERE " + COLUMN_DEVICE + "= " +
                "\"" + device + "\";";
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(query, null);
        boolean exists = cursor.moveToFirst();
        cursor.close();
        return exists;
    }

    List<String> getTrustedPeers() {
        List<String> devices = new ArrayList<>();
        String query = "SELECT * FROM " + TABLE_TRUSTED + ";";
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(query, null);
        if (cursor.moveToFirst()) {
            do {
                devices.add(cursor.getString(0));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return devices;
    }

    // **** SEND TABLE FUNCTIONS ****

    // TODO void removePacket(String packet, String target)
    // TODO String getNextPacket()
    // TODO void addPacket(String packet, String target)

}
