package hs.aalen.arora;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.ByteArrayOutputStream;

/**
 * Class that handles Database queries to add and modify object meta data
 *
 * @author Michael Schlosser
 */
public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String TAG = DatabaseHelper.class.getSimpleName();

    private static final String TABLE_NAME = "object_metadata";
    private static final String COL0 = "ID";
    private static final String COL1 = "object_name";
    private static final String COL2 = "object_type";
    private static final String COL3 = "object_additional_data";
    private static final String COL4 = "object_created_at";
    private static final String COL5 = "object_image";
    private static final String COL6 = "model_pos";

    public DatabaseHelper(@Nullable Context context) {
        super(context, TABLE_NAME, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_NAME + " (" +
                                "ID INTEGER PRIMARY KEY AUTOINCREMENT, "
                                + COL1 + " TEXT, "
                                + COL2 + " TEXT, "
                                + COL3 + " TEXT, "
                                + COL4 + " DATETIME DEFAULT CURRENT_TIMESTAMP, "
                                + COL5 + " BLOB, "
                                + COL6 + " TEXT)";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    /**
     * Insert object to DB
     *
     * @param objectName            name of object
     * @param objectType            type of object
     * @param objectAdditionalData  additional data to object
     *
     * @return true if successful, false otherwise
     */
    public boolean addData(String objectName, String objectType, String objectAdditionalData) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COL1, objectName);
        contentValues.put(COL2, objectType);
        contentValues.put(COL3, objectAdditionalData);

        Log.d(TAG, "addData: Adding " + objectName + "to " + TABLE_NAME);

        long success = db.insert(TABLE_NAME, null, contentValues);

        return success != -1;
    }

    public boolean updateImageBlob(String objectID, Bitmap bitmap) {
        SQLiteDatabase db = this.getWritableDatabase();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);
        byte[] image = bos.toByteArray();

        ContentValues contentValues = new ContentValues();
        contentValues.put(COL5, image);

        long success = db.update(TABLE_NAME, contentValues, "object_name=?", new String[]{objectID});

        return success != -1;
    }

    public boolean updateModelPos(String objectName, String modelPos) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COL6, modelPos);

        long success = db.update(TABLE_NAME, contentValues, "object_name=?", new String[]{objectName});

        return success != -1;
    }


    /**
     * Returns all data from Object Table
     *
     * @return All data from Table
     */
    public Cursor getAllObjects() {
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "SELECT * FROM " + TABLE_NAME;
        Cursor data = db.rawQuery(query, null);
        return data;
    }

    public Cursor getByID(String id) {
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "SELECT * FROM " + TABLE_NAME
                + " WHERE " + COL0 + " = " + id;
        Cursor data = db.rawQuery(query, null);
        return data;
    }

    public Cursor getByModelPos(String pos) {
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "SELECT * FROM " + TABLE_NAME
                + " WHERE " + COL6 + " = " + pos;
        Cursor data = db.rawQuery(query, null);
        return data;
    }

    public boolean deletebyId(String id) {
        Log.d(TAG, "deletebyId: Delete item with ID " + id);
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(TABLE_NAME, "ID=?", new String[]{id}) > 0;
    }

    /**
     * Edit object by ID
     *
     * @param id                    ID of Object
     * @param objectName            Name of Object
     * @param objectType            Type of Object
     * @param objectAdditionalData  Additional Data of Object
     *
     * @return True if successful, false otherwise
     */
    public boolean editData(String id, String objectName, String objectType, String objectAdditionalData) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COL1, objectName);
        contentValues.put(COL2, objectType);
        contentValues.put(COL3, objectAdditionalData);

        Log.d(TAG, "editData: Editing " + objectName);

        long success = db.update(TABLE_NAME, contentValues, "ID=?", new String[]{id});

        return success != -1;
    }

    public String tableToString() {
        SQLiteDatabase db = this.getWritableDatabase();
        String tableName = TABLE_NAME;
        Log.d("","tableToString called");
        String tableString = String.format("Table %s:\n", tableName);
        Cursor allRows  = db.rawQuery("SELECT * FROM " + tableName, null);
        tableString += cursorToString(allRows);
        return tableString;
    }


    public String cursorToString(Cursor cursor){
        String cursorString = "";
        if (cursor.moveToFirst() ){
            String[] columnNames = cursor.getColumnNames();
            for (String name: columnNames)
                cursorString += String.format("%s ][ ", name);
            cursorString += "\n";
            do {
                for (String name: columnNames) {
                    if(name.equals("object_image")) {
                        cursorString += String.format("%s ][ ",
                                (cursor.getBlob(cursor.getColumnIndex(name)) != null));
                    }
                    else {
                        cursorString += String.format("%s ][ ",
                                cursor.getString(cursor.getColumnIndex(name)));
                    }
                }
                cursorString += "\n";
            } while (cursor.moveToNext());
        }
        return cursorString;
    }
}
