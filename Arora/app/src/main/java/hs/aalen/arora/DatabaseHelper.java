package hs.aalen.arora;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import androidx.annotation.Nullable;

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

    public DatabaseHelper(@Nullable Context context) {
        super(context, TABLE_NAME, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_NAME + " (" +
                                "ID INTEGER PRIMARY KEY AUTOINCREMENT, "
                                + COL1 + " TEXT, "
                                + COL2 + " TEXT, "
                                + COL3 + " TEXT)";
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
    public boolean addData(String objectName, String objectType, String objectAdditionalData ) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COL1, objectName);
        contentValues.put(COL2, objectType);
        contentValues.put(COL3, objectAdditionalData);

        Log.d(TAG, "addData: Adding " + objectName + "to " + TABLE_NAME);

        long success = db.insert(TABLE_NAME, null, contentValues);

        return success != -1;
    }

    /**
     * Returns all data from Database
     *
     * @return All data from Table
     */
    public Cursor getData() {
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "SELECT * FROM " + TABLE_NAME;
        Cursor data = db.rawQuery(query, null);
        return data;
    }

    /**
     * Get object names
     * This is helpful for model initialization
     * @return
     */
    public Cursor getObjectNames() {
        SQLiteDatabase db = this.getWritableDatabase();
        String query ="SELECT " + COL1 + " FROM " + TABLE_NAME;
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
}
