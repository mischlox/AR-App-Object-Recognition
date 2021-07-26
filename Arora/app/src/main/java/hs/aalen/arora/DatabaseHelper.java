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
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 *  Class that handles object Database queries
 *  to add and modify object meta data and model parameters
 *
 * @author Michael Schlosser
 */
public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String TAG =DatabaseHelper.class.getSimpleName();
    private static final String DATABASE_NAME = "arora_db";

    private static final String MODEL_TABLE_NAME = "model_table";
    private static final String MODEL_COL0 = "model_ID";
    private static final String MODEL_COL1 = "model_name";
    private static final String MODEL_COL2 = "model_path";

    private static final String OBJECT_TABLE_NAME = "object_table";
    private static final String OBJECT_COL0 = "object_ID";
    private static final String OBJECT_COL1 = "object_name";
    private static final String OBJECT_COL2 = "object_type";
    private static final String OBJECT_COL3 = "object_additional_data";
    private static final String OBJECT_COL4 = "object_created_at";
    private static final String OBJECT_COL5 = "object_image";
    private static final String OBJECT_COL6 = "model_pos";
    private static final String OBJECT_COL7 = "model_id";

    public DatabaseHelper(@Nullable Context context) {
        super(context, DATABASE_NAME, null, 1);
    }

    /**
     * Check if there are elements in table
     *
     * @param table table to check
     *
     * @return true if elements in table exists, false otherwise
     */
    private boolean tableExists(String table) {
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "SELECT * FROM " + table;
        Cursor data = db.rawQuery(query, null);
        int count = data.getCount();
        data.close();
        return count > 0;
    }
    @Override
    public void onCreate(SQLiteDatabase db) {
        String createModelTable = "CREATE TABLE " + MODEL_TABLE_NAME + " ("
                + MODEL_COL0 + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + MODEL_COL1 + " TEXT UNIQUE NOT NULL, "
                + MODEL_COL2 + " TEXT) ";


        String createObjectTable = "CREATE TABLE " + OBJECT_TABLE_NAME + " ("
                + OBJECT_COL0 + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + OBJECT_COL1 + " TEXT UNIQUE NOT NULL, "
                + OBJECT_COL2 + " TEXT, "
                + OBJECT_COL3 + " TEXT, "
                + OBJECT_COL4 + " DATETIME DEFAULT CURRENT_TIMESTAMP, "
                + OBJECT_COL5 + " BLOB, "
                + OBJECT_COL6 + " TEXT, "
                + OBJECT_COL7 + " INTEGER NOT NULL, "
                + " FOREIGN KEY("+OBJECT_COL7+") REFERENCES "+ MODEL_TABLE_NAME+"("+MODEL_COL0+"))";

        db.execSQL(createObjectTable);
        db.execSQL(createModelTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + MODEL_TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + OBJECT_TABLE_NAME);
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
    public long insertObject(String objectName,
                                String objectType,
                                String objectAdditionalData,
                                String modelID) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(OBJECT_COL1, objectName);
        contentValues.put(OBJECT_COL2, objectType);
        contentValues.put(OBJECT_COL3, objectAdditionalData);
        contentValues.put(OBJECT_COL7, modelID);

        Log.d(TAG, "addData: Adding " + objectName + "to " + OBJECT_TABLE_NAME);

        long success = db.insert(OBJECT_TABLE_NAME, null, contentValues);

        return success;
    }

    public void updateImageBlob(String objectID, Bitmap bitmap) {
        SQLiteDatabase db = this.getWritableDatabase();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);
        byte[] image = bos.toByteArray();

        ContentValues contentValues = new ContentValues();
        contentValues.put(OBJECT_COL5, image);
        db.update(OBJECT_TABLE_NAME,
                contentValues,
                OBJECT_COL1+"=?",
                new String[]{objectID});

    }

    public boolean updateModelPos(String objectName, String modelPos) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(OBJECT_COL6, modelPos);

        long success = db.update(OBJECT_TABLE_NAME,
                contentValues,
                OBJECT_COL1+"=?",
                new String[]{objectName});

        return success != -1;
    }


    /**
     * Returns all data from Object Table
     *
     * @return All data from table
     */
    public Cursor getAllObjects() {
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "SELECT * FROM " + OBJECT_TABLE_NAME;
        return db.rawQuery(query, null);
    }

    /**
     * Returns all data from Object Table that are saved in a specific model
     *
     * @return All data from table
     */
    public Cursor getAllObjectsByModelID(String modelID) {
        if(modelID == null) modelID="";
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "SELECT * FROM " + OBJECT_TABLE_NAME
                + " WHERE " + OBJECT_COL7 + "=?";
        return db.rawQuery(query, new String[]{modelID});
    }


    /**
     * Return all data from model table
     *
     * @return All data from table
     */
    public Cursor getAllModels() {
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "SELECT * FROM " + MODEL_TABLE_NAME;
        return db.rawQuery(query, null);
    }

    public Cursor getObjectByRowID(String rowid) {
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "SELECT * FROM " + OBJECT_TABLE_NAME
                + " WHERE rowid=?";
        return db.rawQuery(query, new String[]{rowid});
    }

    public String getModelNameByID(String modelID) {
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "SELECT "  + MODEL_COL1 + " FROM " + MODEL_TABLE_NAME
                + " WHERE " + MODEL_COL0 + "=?";
        Cursor cursor = db.rawQuery(query, new String[]{modelID});

        if(cursor.moveToFirst()) {
            String modelName = cursor.getString(0);
            cursor.close();
            return modelName;
        }
        return null;
    }

    public Cursor getObjectNamesByModelID(String modelID) {
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "SELECT " + OBJECT_COL1 + " FROM " + OBJECT_TABLE_NAME
                + " WHERE " + OBJECT_COL7 + "=?";
        return db.rawQuery(query, new String[]{modelID});
    }

    public Cursor getObjectByModelPos(String pos) {
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "SELECT * FROM " + OBJECT_TABLE_NAME
                + " WHERE " + OBJECT_COL6 + "=?";
        return db.rawQuery(query, new String[]{pos});
    }

    public Cursor getObjectByModelPosAndModelID(String modelPos, String modelID ) {
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "SELECT * FROM " + OBJECT_TABLE_NAME
                + " WHERE " + OBJECT_COL6 + "=? AND "+ OBJECT_COL7 + "=?";
        return db.rawQuery(query, new String[]{modelPos, modelID});
    }

    public boolean deleteObjectById(String id) {
        Log.d(TAG, "deletebyId: Delete item with ID " + id);
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(OBJECT_TABLE_NAME, OBJECT_COL0+"=?", new String[]{id}) > 0;
    }

    public boolean deleteObjectByModelPosAndModelID(String modelpos, String modelID) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(OBJECT_TABLE_NAME, OBJECT_COL6 + "=? AND " + OBJECT_COL7 + "=?", new String[]{modelpos, modelID}) > 0;
    }

    public boolean deleteObjectByNameAndModelID(String name, String modelID) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(OBJECT_TABLE_NAME, OBJECT_COL1+"=? AND "+ OBJECT_COL7 + "=?", new String[]{name, modelID}) > 0;
    }

    public String getObjectModelPosByNameAndModelID(String name, String modelID) {
        SQLiteDatabase db = this.getWritableDatabase();
        String modelPos="";
        String query = "SELECT " + OBJECT_COL6 +" FROM " + OBJECT_TABLE_NAME
                + " WHERE " + OBJECT_COL1 + "=? AND " + OBJECT_COL7 + "=?";
        Cursor cursor = db.rawQuery(query, new String[]{name, modelID});
        if(cursor.moveToFirst()) {
            modelPos = cursor.getString(0);
        }
        return modelPos;
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
    public boolean editObject(String id,
                              String objectName,
                              String objectType,
                              String objectAdditionalData) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(OBJECT_COL1, objectName);
        contentValues.put(OBJECT_COL2, objectType);
        contentValues.put(OBJECT_COL3, objectAdditionalData);

        Log.d(TAG, "editData: Editing " + objectName);

        long success = db.update(OBJECT_TABLE_NAME,
                contentValues,
                OBJECT_COL0+"=?",
                new String[]{id});

        return success != -1;
    }

    public String getModelPathByID(String modelID) {
        SQLiteDatabase db = this.getWritableDatabase();
        String modelPath = null;
        String query = "SELECT " + MODEL_COL2 + " FROM " + MODEL_TABLE_NAME
                + " WHERE " + MODEL_COL0 + "=?";
        Cursor cursor = db.rawQuery(query, new String[]{modelID});
        if(cursor.moveToFirst()) {
            modelPath = cursor.getString(0);
        }
        if(modelPath == null) {
            Log.d(TAG, "getModelPathByID: backup1:  model is null!");
        }
        cursor.close();
        return modelPath;
    }

    /**
     * Inserts a model to DB. If it already exists the record gets updated with a new path
     *
     * @param name name of the model
     * @param path path of the model parameters binary file
     *
     * @return true if successful, false otherwise
     */
    public boolean insertOrUpdateModel(String name, Path path) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues contentValues = new ContentValues();
        contentValues.put(MODEL_COL1, name);
        contentValues.put(MODEL_COL2, path.toString());
        long success;
        if(modelWithNameExists(name)) {
            success = db.update(MODEL_TABLE_NAME,
                    contentValues,
                    MODEL_COL1+"=?",
                    new String[]{name});
        }
        else {
            success = db.insert(MODEL_TABLE_NAME, null, contentValues);
        }
        return success != -1;
    }

    /**
     * Insert a model with name only.
     * The path should be updated when the application is closed
     *
     * @param name of the model
     * @return true if successful, false otherwise
     */
    public boolean insertModel(String name) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues contentValues = new ContentValues();
        contentValues.put(MODEL_COL1, name);
        long success;
        if(modelWithNameExists(name)) {
            success = -1;
        }
        else {
            success = db.insert(MODEL_TABLE_NAME, null, contentValues);
        }
        return success != -1;
    }

    public boolean modelWithNameExists(String name) {
        if(name == null) name = "";
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = null;
        String query = "SELECT " + MODEL_COL1 + " FROM " + MODEL_TABLE_NAME
                + " WHERE " + MODEL_COL1 + "=?";
        cursor = db.rawQuery(query, new String[]{name});
        int count = cursor.getCount();
        cursor.close();
        return count > 0;
    }

    public boolean modelHasPath(String modelID) {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor;
        String query = "SELECT " + MODEL_COL0 + " FROM " + MODEL_TABLE_NAME
                + " WHERE " + MODEL_COL0 + "=? AND " + MODEL_COL2 + " IS NOT NULL";
        cursor = db.rawQuery(query, new String[]{modelID});
        int count = cursor.getCount();
        cursor.close();
        return count > 0;
    }

    /**
     * Check if there are models in DB
     *
     * @return true if a model exists in table, false otherwise
     */
    public boolean modelsExists() {
        return tableExists(MODEL_TABLE_NAME);
    }

    /**
     * Query for getting model parameters by name
     *
     * @param modelName name of model
     * @return Cursor with queried model ids
     */
    private Cursor selectModel(String modelName) {
        SQLiteDatabase db = this.getWritableDatabase();

        if(modelName.equals("")) modelName = "test";
        String query = "SELECT " + MODEL_COL0 + " FROM " + MODEL_TABLE_NAME
                + " WHERE " + MODEL_COL1 + " = " + "'"+modelName+"'";
        return db.rawQuery(query, null);
    }

    /**
     * Converts ByteBuffer to Byte Array
     *
     * @param byteBuffer buffer that will be converted
     * @return converted byte array
     */
    private byte[] convertByteBuffer(ByteBuffer byteBuffer) {
        return byteBuffer.array();
    }

    /**
     * Converts Byte Array to ByteBuffer
     *
     * @param bytes byte array that will be converted
     * @return converted ByteBuffer
     */
    private ByteBuffer convertBytes(byte[] bytes) {
        return ByteBuffer.wrap(bytes);
    }

    /**
     * Get model ID by model name
     *
     * @param modelName name of model
     * @return id of model
     */
    public String getModelIdByName(String modelName) {
        Cursor data = selectModel(modelName);
        if(data.moveToFirst()) return data.getString(0);
        else return "";
    }

    public String tableToString(String tableName) {
        SQLiteDatabase db = this.getWritableDatabase();
        Log.d("","tableToString called");
        String tableString = String.format("Table %s:\n", tableName);
        Cursor allRows  = db.rawQuery("SELECT * FROM " + tableName, null);
        tableString += cursorToString(allRows);
        return tableString;
    }


    public String cursorToString(Cursor cursor){
        StringBuilder cursorString = new StringBuilder();
        if (cursor.moveToFirst() ){
            String[] columnNames = cursor.getColumnNames();
            for (String name: columnNames)
                cursorString.append(String.format("%s ][ ", name));
            cursorString.append("\n");
            do {
                for (String name: columnNames) {
                    if(name.equals("object_image") || name.equals("parameters")) {
                        cursorString.append(String.format("%s ][ ",
                                (cursor.getBlob(cursor.getColumnIndex(name)) != null)));
                    }
                    else {
                        cursorString.append(String.format("%s ][ ",
                                cursor.getString(cursor.getColumnIndex(name))));
                    }
                }
                cursorString.append("\n");
            } while (cursor.moveToNext());
        }
        return cursorString.toString();
    }
    /**
     * Check if there are objects in DB
     *
     * @return true if an object exists in table, false otherwise
     */
    public boolean objectsExist() {
        return tableExists(OBJECT_TABLE_NAME);
    }

    /**
     * Drop all tables
     *
     * @return
     */
    public void deleteAll() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DROP TABLE IF EXISTS " + OBJECT_TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + MODEL_TABLE_NAME);
        onCreate(db);
    }
}
