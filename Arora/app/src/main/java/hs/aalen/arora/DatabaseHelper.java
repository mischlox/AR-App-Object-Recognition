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
import java.util.ArrayList;

/**
 *  Class that handles object Database queries to add and modify object meta data and model parameters
 *
 * @author Michael Schlosser
 */
public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String TAG =DatabaseHelper.class.getSimpleName();
    private static final String DATABASE_NAME = "arora_db";

    private static final String MODEL_TABLE_NAME = "model_table";
    private static final String MODEL_COL0 = "ID";
    private static final String MODEL_COL1 = "model_name";

    private static final String PARAM_TABLE_NAME = "param_table";
    private static final String PARAM_COL0 = "ID";
    private static final String PARAM_COL1 = "model_id";
    private static final String PARAM_COL2 = "parameters";

    private static final String OBJECT_TABLE_NAME = "object_table";
    private static final String OBJECT_COL0 = "ID";
    private static final String OBJECT_COL1 = "object_name";
    private static final String OBJECT_COL2 = "object_type";
    private static final String OBJECT_COL3 = "object_additional_data";
    private static final String OBJECT_COL4 = "object_created_at";
    private static final String OBJECT_COL5 = "object_image";
    private static final String OBJECT_COL6 = "model_pos";

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
        Log.d(TAG, "tableExists: backup: count of " + table + ": " + 64);
        data.close();
        return count > 0;
    }
    @Override
    public void onCreate(SQLiteDatabase db) {
        String createModelTable = "CREATE TABLE " + MODEL_TABLE_NAME + " (" +
                "ID INTEGER PRIMARY KEY AUTOINCREMENT, "
                + MODEL_COL1 + " TEXT NOT NULL) ";

        String createParamTable = "CREATE TABLE " + PARAM_TABLE_NAME + " (" +
                "ID INTEGER PRIMARY KEY AUTOINCREMENT, "
                + PARAM_COL1 + " INTEGER NOT NULL, "
                + PARAM_COL2 + " BLOB NOT NULL, "
                + "FOREIGN KEY(" + PARAM_COL1
                + ") REFERENCES " + MODEL_TABLE_NAME + "(" + MODEL_COL0 + "))";

        String createObjectTable = "CREATE TABLE " + OBJECT_TABLE_NAME + " (" +
                "ID INTEGER PRIMARY KEY AUTOINCREMENT, "
                + OBJECT_COL1 + " TEXT, "
                + OBJECT_COL2 + " TEXT, "
                + OBJECT_COL3 + " TEXT, "
                + OBJECT_COL4 + " DATETIME DEFAULT CURRENT_TIMESTAMP, "
                + OBJECT_COL5 + " BLOB, "
                + OBJECT_COL6 + " TEXT)";

        db.execSQL(createObjectTable);
        db.execSQL(createModelTable);
        db.execSQL(createParamTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + MODEL_TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + PARAM_TABLE_NAME);
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
    public boolean insertObject(String objectName, String objectType, String objectAdditionalData) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(OBJECT_COL1, objectName);
        contentValues.put(OBJECT_COL2, objectType);
        contentValues.put(OBJECT_COL3, objectAdditionalData);

        Log.d(TAG, "addData: Adding " + objectName + "to " + OBJECT_TABLE_NAME);

        long success = db.insert(OBJECT_TABLE_NAME, null, contentValues);

        return success != -1;
    }

    public boolean updateImageBlob(String objectID, Bitmap bitmap) {
        SQLiteDatabase db = this.getWritableDatabase();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);
        byte[] image = bos.toByteArray();

        ContentValues contentValues = new ContentValues();
        contentValues.put(OBJECT_COL5, image);

        long success = db.update(OBJECT_TABLE_NAME, contentValues, "object_name=?", new String[]{objectID});

        return success != -1;
    }

    public boolean updateModelPos(String objectName, String modelPos) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(OBJECT_COL6, modelPos);

        long success = db.update(OBJECT_TABLE_NAME, contentValues, "object_name=?", new String[]{objectName});

        return success != -1;
    }


    /**
     * Returns all data from Object Table
     *
     * @return All data from Table
     */
    public Cursor getAllObjects() {
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "SELECT * FROM " + OBJECT_TABLE_NAME;
        Cursor data = db.rawQuery(query, null);
        return data;
    }

    public Cursor getByID(String id) {
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "SELECT * FROM " + OBJECT_TABLE_NAME
                + " WHERE " + OBJECT_COL0 + " = " + id;
        Cursor data = db.rawQuery(query, null);
        return data;
    }

    public Cursor getByModelPos(String pos) {
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "SELECT * FROM " + OBJECT_TABLE_NAME
                + " WHERE " + OBJECT_COL6 + " = " + pos;
        Cursor data = db.rawQuery(query, null);
        return data;
    }

    public boolean deletebyId(String id) {
        Log.d(TAG, "deletebyId: Delete item with ID " + id);
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(OBJECT_TABLE_NAME, "ID=?", new String[]{id}) > 0;
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
    public boolean editObject(String id, String objectName, String objectType, String objectAdditionalData) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(OBJECT_COL1, objectName);
        contentValues.put(OBJECT_COL2, objectType);
        contentValues.put(OBJECT_COL3, objectAdditionalData);

        Log.d(TAG, "editData: Editing " + objectName);

        long success = db.update(OBJECT_TABLE_NAME, contentValues, "ID=?", new String[]{id});

        return success != -1;
    }

    /**
     * Check if there are models in DB
     *
     * @return true if a model exists in table, false otherwise
     */
    public boolean modelExists() {
        return tableExists(MODEL_TABLE_NAME);
    }

    public boolean saveModel(String modelName, ByteBuffer[] parameters) {
        SQLiteDatabase db = this.getWritableDatabase();
        boolean successModel = insertModel(modelName);
        int modelID = getModelID(modelName);
        boolean success = false;
        for(ByteBuffer param : parameters) {
            Log.d(TAG, "saveModel: backup: trying to write buffer");
            success = insertParameters(modelID, param);
        }
        Log.d(TAG, "saveModel: backup: success get bytebuffers: "
        + success + " success get model id: " + successModel);
        return success && successModel;
    }

    /**
     * Inserts model to DB
     * @param modelName name of model (if available)
     *
     * @return true if successful, false otherwise
     */
    private boolean insertModel(String modelName) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();

        if(modelName.equals("")) modelName = "test";
        contentValues.put(MODEL_COL1, modelName);

        long success = db.insert(MODEL_TABLE_NAME, null, contentValues);
        Log.d(TAG, "insertModel: backup:");
        return success != -1;
    }


    private boolean insertParameters(int modelID, ByteBuffer parameter) {
        Log.d(TAG, "insertParameters: backup: trying to save bytebuffers to model with id: "+ modelID);
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        byte[] parameterBlob = convertByteBuffer(parameter);

        contentValues.put(PARAM_COL1, modelID);
        contentValues.put(PARAM_COL2, parameterBlob);

        long success = db.insert(PARAM_TABLE_NAME, null, contentValues);

        return success != -1;
    }

    /**
     * Get model parameters by name
     *
     * @param modelName name of model
     * @return Parameters buffer
     */
    public ByteBuffer[] getParameters(String modelName) {
        Cursor selectedModel = selectModel(modelName);
        ArrayList<ByteBuffer> allParameters = new ArrayList<>();
        int modelID = -1;
        if(selectedModel.moveToFirst())
            modelID = selectedModel.getInt(0);

        Cursor selectedParameters = selectParameters(modelID);

        while(selectedParameters.moveToNext()) {
            ByteBuffer parameter = convertBytes(selectedParameters.getBlob(0));
            allParameters.add(parameter);
        }
        return allParameters.toArray(new ByteBuffer[0]);
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
     * Query for getting all buffers from model parameter
     * @param modelID id of model (foreign key of parameters table)
     *
     * @return Cursor with queried parameters
     */
    private Cursor selectParameters(int modelID) {
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "SELECT " + PARAM_COL2 + " FROM " + PARAM_TABLE_NAME
                + " WHERE " + PARAM_COL1 + " = " + modelID;
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
    private int getModelID(String modelName) {
        Cursor data = selectModel(modelName);
        if(data.moveToFirst()) return data.getInt(0);
        else return -1;
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
        String cursorString = "";
        if (cursor.moveToFirst() ){
            String[] columnNames = cursor.getColumnNames();
            for (String name: columnNames)
                cursorString += String.format("%s ][ ", name);
            cursorString += "\n";
            do {
                for (String name: columnNames) {
                    if(name.equals("object_image") || name.equals("parameters")) {
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
    /**
     * Check if there are objects in DB
     *
     * @return true if an object exists in table, false otherwise
     */
    public boolean objectExists() {
        return tableExists(OBJECT_TABLE_NAME);
    }
}
