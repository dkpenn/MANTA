package com.mymanet.manta;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by dk on 12/3/16.
 * http://www.vogella.com/tutorials/AndroidSQLite/article.html#more-on-listviews
 * http://hmkcode.com/android-simple-sqlite-database-tutorial/
 */

public class MySQLLiteHelper extends SQLiteOpenHelper {

    public static final String TABLE_FILES = "files";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_FILENAME = "filename";

    public static final String DATABASE_NAME = "filenames.db";
    private static final int DATABASE_VERSION = 1;

    //Database creation sql statement
    private static final String DATABASE_CREATE = "create table " +
            TABLE_FILES + "( " + COLUMN_ID + " integer primary key autoincrement, " + COLUMN_FILENAME + " text not null);";

    public MySQLLiteHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(DATABASE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        Log.w(MySQLLiteHelper.class.getName(),
                "Upgrading database from version " + oldVersion + " to "
                        + newVersion + ", which will destroy all old data");
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TABLE_FILES);
        onCreate(sqLiteDatabase);

    }

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

    public List<MantaFile> getFilesWithName(String name) {
        List<MantaFile> files = new LinkedList<MantaFile>();

        // 1. build the query
        String query = "SELECT * FROM "  + TABLE_FILES + " WHERE " + COLUMN_FILENAME + "= " + "\"" + name + "\"";
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
}
