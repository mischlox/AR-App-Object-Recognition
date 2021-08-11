package hs.aalen.arora.persistence;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Class that handles object Database queries
 * to add and modify object meta data and model parameters
 *
 * @author Michael Schlosser
 */
public class SQLiteHelper extends SQLiteOpenHelper implements DatabaseHelper {
    private static final String TAG = SQLiteHelper.class.getSimpleName();



    public SQLiteHelper(@Nullable Context context) {
        super(context, DATABASE_NAME, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createModelTable = "CREATE TABLE " + MODEL_TABLE_NAME + " ("
                + MODEL_COL0 + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + MODEL_COL1 + " TEXT UNIQUE NOT NULL, "
                + MODEL_COL2 + " TEXT, "
                + MODEL_COL3 + " INTEGER DEFAULT 0"
                + ") ";


        String createObjectTable = "CREATE TABLE " + OBJECT_TABLE_NAME + " ("
                + OBJECT_COL0 + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + OBJECT_COL1 + " TEXT UNIQUE NOT NULL, "
                + OBJECT_COL2 + " TEXT, "
                + OBJECT_COL3 + " TEXT, "
                + OBJECT_COL4 + " DATETIME DEFAULT CURRENT_TIMESTAMP, "
                + OBJECT_COL5 + " BLOB, "
                + OBJECT_COL6 + " TEXT, "
                + OBJECT_COL7 + " INTEGER NOT NULL, "
                + OBJECT_COL8 + " INTEGER, "
                + " FOREIGN KEY(" + OBJECT_COL7 + ") REFERENCES " + MODEL_TABLE_NAME + "(" + MODEL_COL0 + "))";

        String createReplayBufferTable = "CREATE TABLE " + REPLAY_BUFFER_TABLE_NAME + " ( "
                + REPLAY_BUFFER_COL0 + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + REPLAY_BUFFER_COL1 +" TEXT NOT NULL, "
                + REPLAY_BUFFER_COL2 +" BLOB NOT NULL,"
                + REPLAY_BUFFER_COL3 + " INTEGER NOT NULL"
                +")";

        String createTrainingSamplesTable = "CREATE TABLE " + TRAINING_SAMPLES_TABLE_NAME + " ("
                + TRAINING_SAMPLES_COL0 + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + TRAINING_SAMPLES_COL1 +" TEXT NOT NULL, "
                + TRAINING_SAMPLES_COL2 +" BLOB NOT NULL, "
                + TRAINING_SAMPLES_COL3 + " TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
                + TRAINING_SAMPLES_COL4 + " INTEGER NOT NULL"
                + ")";

        db.execSQL(createObjectTable);
        db.execSQL(createModelTable);
        db.execSQL(createReplayBufferTable);
        db.execSQL(createTrainingSamplesTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + MODEL_TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + OBJECT_TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + REPLAY_BUFFER_TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + TRAINING_SAMPLES_TABLE_NAME);
        onCreate(db);
    }

    @Override
    public Cursor getReplayBuffer(String modelID) {
        if(modelID == null) modelID="";
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "SELECT * FROM "+ REPLAY_BUFFER_TABLE_NAME + " WHERE " + REPLAY_BUFFER_COL3+"=?";
        return db.rawQuery(query,new String[]{modelID});
    }

    @Override
    public void emptyReplayBuffer(String modelID) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(REPLAY_BUFFER_TABLE_NAME, REPLAY_BUFFER_COL3+"=?", new String[]{modelID});
    }

    @Override
    public Cursor getTrainingSamples(String modelID) {
        if (modelID == null) modelID = "";
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "SELECT * FROM " + TRAINING_SAMPLES_TABLE_NAME
                + " WHERE " + TRAINING_SAMPLES_COL4 + "=?"
                + " ORDER BY " + TRAINING_SAMPLES_COL3 + " ASC";
        return db.rawQuery(query,new String[]{modelID});
    }

    @Override
    public void insertReplaySampleBatch(HashMap<String, byte[]> activationsMap, String modelID) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        for(Map.Entry<String, byte[]> activation : activationsMap.entrySet()) {
            ContentValues contentValues = new ContentValues();
            contentValues.put(REPLAY_BUFFER_COL1, activation.getKey());
            contentValues.put(REPLAY_BUFFER_COL2, activation.getValue());
            contentValues.put(REPLAY_BUFFER_COL3, modelID);
            db.insert(REPLAY_BUFFER_TABLE_NAME, null, contentValues);
        }
        db.setTransactionSuccessful();
        db.endTransaction();
    }

    @Override
    public void insertTrainingSampleBatch(HashMap<String, byte[]> activationsMap, String modelID) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        for(Map.Entry<String, byte[]> activation : activationsMap.entrySet()) {
            ContentValues contentValues = new ContentValues();
            contentValues.put(TRAINING_SAMPLES_COL1, activation.getKey());
            contentValues.put(TRAINING_SAMPLES_COL2, activation.getValue());
            contentValues.put(TRAINING_SAMPLES_COL4, modelID);
            db.insert(TRAINING_SAMPLES_TABLE_NAME, null, contentValues);
        }
        db.setTransactionSuccessful();
        db.endTransaction();
    }

    @Override
    public boolean modelIsFrozen(String modelID) {
        if(modelID == null) modelID = "";
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "SELECT " + MODEL_COL3 + " FROM " + MODEL_TABLE_NAME
                + " WHERE " + MODEL_COL0 + "=?";
        Cursor cursor = db.rawQuery(query, new String[]{modelID});
        if(cursor.moveToFirst()) {
            int ret = cursor.getInt(0);
            cursor.close();
            return ret == 1;
        }
        return false;
    }

    @Override
    public void updateModelIsFrozen(String modelID, boolean isFrozen) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(MODEL_COL3, (isFrozen ? 1 : 0));

        db.update(MODEL_TABLE_NAME,
                contentValues,
                MODEL_COL0 + "=?",
                new String[]{modelID});

    }

    @Override
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

        return db.insert(OBJECT_TABLE_NAME, null, contentValues);
    }

    @Override
    public void updateImageBlob(String objectName, Bitmap bitmap) {
        SQLiteDatabase db = this.getWritableDatabase();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);
        byte[] image = bos.toByteArray();

        ContentValues contentValues = new ContentValues();
        contentValues.put(OBJECT_COL5, image);
        db.update(OBJECT_TABLE_NAME,
                contentValues,
                OBJECT_COL1 + "=?",
                new String[]{objectName});

    }

    @Override
    public boolean updateModelPos(String objectName, String modelPos) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(OBJECT_COL6, modelPos);

        long success = db.update(OBJECT_TABLE_NAME,
                contentValues,
                OBJECT_COL1 + "=?",
                new String[]{objectName});

        return success != 0;
    }

    @Override
    public Cursor getAllObjects() {
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "SELECT * FROM " + OBJECT_TABLE_NAME;
        return db.rawQuery(query, null);
    }

    @Override
    public Cursor getAllObjectsByModelID(String modelID) {
        if (modelID == null) modelID = "";
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "SELECT * FROM " + OBJECT_TABLE_NAME
                + " WHERE " + OBJECT_COL7 + "=?";
        return db.rawQuery(query, new String[]{modelID});
    }

    @Override
    public Cursor getAllModels() {
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "SELECT * FROM " + MODEL_TABLE_NAME;
        return db.rawQuery(query, null);
    }

    @Override
    public String getModelNameByID(String modelID) {
        if(modelID==null) modelID="";
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "SELECT " + MODEL_COL1 + " FROM " + MODEL_TABLE_NAME
                + " WHERE " + MODEL_COL0 + "=?";
        Cursor cursor = db.rawQuery(query, new String[]{modelID});

        if (cursor.moveToFirst()) {
            String modelName = cursor.getString(0);
            cursor.close();
            return modelName;
        }
        return null;
    }

    @Override
    public Cursor getObjectNamesByModelID(String modelID) {
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "SELECT " + OBJECT_COL1 + " FROM " + OBJECT_TABLE_NAME
                + " WHERE " + OBJECT_COL7 + "=?";
        return db.rawQuery(query, new String[]{modelID});
    }

    @Override
    public Cursor getObjectByModelPosAndModelID(String modelPos, String modelID) {
        if(modelPos == null) modelPos="";
        if(modelID == null) modelID="";

        SQLiteDatabase db = this.getWritableDatabase();
        String query = "SELECT * FROM " + OBJECT_TABLE_NAME
                + " WHERE " + OBJECT_COL6 + "=? AND " + OBJECT_COL7 + "=?";
        return db.rawQuery(query, new String[]{modelPos, modelID});
    }

    @Override
    public boolean deleteObjectById(String id) {
        Log.d(TAG, "deletebyId: Delete item with ID " + id);
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(OBJECT_TABLE_NAME, OBJECT_COL0 + "=?", new String[]{id}) > 0;
    }

    @Override
    public void deleteObjectByNameAndModelID(String name, String modelID) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(OBJECT_TABLE_NAME, OBJECT_COL1 + "=? AND " + OBJECT_COL7 + "=?", new String[]{name, modelID});
    }

    @Override
    public String getObjectModelPosByNameAndModelID(String name, String modelID) {
        if(name == null || modelID == null) return "";
        SQLiteDatabase db = this.getWritableDatabase();
        String modelPos = "";
        String query = "SELECT " + OBJECT_COL6 + " FROM " + OBJECT_TABLE_NAME
                + " WHERE " + OBJECT_COL1 + "=? AND " + OBJECT_COL7 + "=?";
        Cursor cursor = db.rawQuery(query, new String[]{name, modelID});
        if (cursor.moveToFirst()) {
            modelPos = cursor.getString(0);
        }
        cursor.close();
        return modelPos != null ? modelPos : "";
    }

    @Override
    public boolean editObject(String id,
                              String objectName,
                              String objectType,
                              String objectAdditionalData) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();

        if(objectName != null && !objectName.isEmpty())  {
            contentValues.put(OBJECT_COL1, objectName);
        }
        contentValues.put(OBJECT_COL2, objectType);
        contentValues.put(OBJECT_COL3, objectAdditionalData);

        Log.d(TAG, "editData: Editing " + objectName);

        long success = db.update(OBJECT_TABLE_NAME,
                contentValues,
                OBJECT_COL0 + "=?",
                new String[]{id});

        return success != -1;
    }

    @Override
    public String getModelPathByID(String modelID) {
        SQLiteDatabase db = this.getWritableDatabase();
        String modelPath = null;
        String query = "SELECT " + MODEL_COL2 + " FROM " + MODEL_TABLE_NAME
                + " WHERE " + MODEL_COL0 + "=?";
        Cursor cursor = db.rawQuery(query, new String[]{modelID});
        if (cursor.moveToFirst()) {
            modelPath = cursor.getString(0);
        }
        if (modelPath == null) {
            Log.d(TAG, "getModelPathByID: backup1:  model is null!");
        }
        cursor.close();
        return modelPath;
    }

    @Override
    public String getObjectModelPosByName(String objectName) {
        if(objectName == null) return null;
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "SELECT " + OBJECT_COL6 + " FROM " + OBJECT_TABLE_NAME
                + " WHERE " + OBJECT_COL1 + "=?";
        Cursor cursor = db.rawQuery(query, new String[]{objectName});
        String pos = null;
        if(cursor.moveToFirst()) {
            pos = cursor.getString(0);
        }
        cursor.close();
        return pos;
    }

    @Override
    public boolean insertOrUpdateModel(String name, Path path) {
        if(path == null || name == null || name.isEmpty()) return false;

        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues contentValues = new ContentValues();
        contentValues.put(MODEL_COL1, name);
        contentValues.put(MODEL_COL2, path.toString());
        long success;
        if (modelWithNameExists(name)) {
            success = db.update(MODEL_TABLE_NAME,
                    contentValues,
                    MODEL_COL1 + "=?",
                    new String[]{name});
        } else {
            success = db.insert(MODEL_TABLE_NAME, null, contentValues);
        }
        return success != -1;
    }

    @Override
    public boolean insertModel(String name) {
        if(name == null || name.equals("")) return false;
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(MODEL_COL1, name);
        long success;
        if (modelWithNameExists(name)) {
            success = -1;
        } else {
            success = db.insert(MODEL_TABLE_NAME, null, contentValues);
        }
        return success != -1;
    }

    @Override
    public boolean modelHasPath(String modelID) {
        if(modelID == null) return false;
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor;
        String query = "SELECT " + MODEL_COL0 + " FROM " + MODEL_TABLE_NAME
                + " WHERE " + MODEL_COL0 + "=? AND " + MODEL_COL2 + " IS NOT NULL";
        cursor = db.rawQuery(query, new String[]{modelID});
        int count = cursor.getCount();
        cursor.close();
        return count > 0;
    }

    @Override
    public boolean modelsExists() {
        return tableExists(MODEL_TABLE_NAME);
    }

    @Override
    public String getModelIdByName(String modelName) {
        Cursor data = selectModel(modelName);
        if (data.moveToFirst()) return data.getString(0);
        else return "";
    }

    @Override
    public void addSamplesToObject(String objectName, int amount) {
        SQLiteDatabase db = this.getWritableDatabase();
        if(amount < 0) amount = 0;
        ContentValues contentValues = new ContentValues();
        int updatedAmount = getAmountSamplesByObjectName(objectName) + amount;
        contentValues.put(OBJECT_COL8, updatedAmount);
        db.update(OBJECT_TABLE_NAME, contentValues, OBJECT_COL1 + "=?", new String[]{objectName});
    }

    @Override
    public int getAmountSamplesByObjectName(String objectName) {
        if(objectName==null) objectName="";
        SQLiteDatabase db = this.getWritableDatabase();
        int amountSamples = 0;
        String query = "SELECT " + OBJECT_COL8 + " FROM " + OBJECT_TABLE_NAME
                + " WHERE " + OBJECT_COL1 + "=?";
        Cursor cursor = db.rawQuery(query, new String[]{objectName});
        if (cursor.moveToFirst()) {
            amountSamples = cursor.getInt(0);
        }
        cursor.close();
        return amountSamples;
    }

    @Override
    public boolean objectsExist() {
        return tableExists(OBJECT_TABLE_NAME);
    }

    @Override
    public void deleteAll() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DROP TABLE IF EXISTS " + MODEL_TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + OBJECT_TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + REPLAY_BUFFER_TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + TRAINING_SAMPLES_TABLE_NAME);
        onCreate(db);
    }

    /**
     * Check if a model with the given name exists
     *
     * @param name name of model to be checked
     * @return true if it exists, false otherwise
     */
    private boolean modelWithNameExists(String name) {
        if (name == null) name = "";
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor;
        String query = "SELECT " + MODEL_COL1 + " FROM " + MODEL_TABLE_NAME
                + " WHERE " + MODEL_COL1 + "=?";
        cursor = db.rawQuery(query, new String[]{name});
        int count = cursor.getCount();
        cursor.close();
        return count > 0;
    }

    /**
     * Check if there are elements in table
     *
     * @param table table to check
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

    /**
     * Query for getting model parameters by name
     *
     * @param modelName name of model
     * @return Cursor with queried model ids
     */
    private Cursor selectModel(String modelName) {
        SQLiteDatabase db = this.getWritableDatabase();

        if (modelName.equals("")) modelName = "test";
        String query = "SELECT " + MODEL_COL0 + " FROM " + MODEL_TABLE_NAME
                + " WHERE " + MODEL_COL1 + " = " + "'" + modelName + "'";
        return db.rawQuery(query, null);
    }

    // For debug purposes because Android Studio DB Inspector is buggy sometimes
    @SuppressWarnings("unused")
    public String tableToString(String tableName) {
        SQLiteDatabase db = this.getWritableDatabase();
        Log.d("", "tableToString called");
        String tableString = String.format("Table %s:\n", tableName);
        Cursor allRows = db.rawQuery("SELECT * FROM " + tableName, null);
        tableString += cursorToString(allRows);
        return tableString;
    }

    public String cursorToString(Cursor cursor) {
        StringBuilder cursorString = new StringBuilder();
        if (cursor.moveToFirst()) {
            String[] columnNames = cursor.getColumnNames();
            for (String name : columnNames)
                cursorString.append(String.format("%s ][ ", name));
            cursorString.append("\n");
            do {
                for (String name : columnNames) {
                    if (name.equals("object_image") || name.equals("parameters") || name.equals("sample_blob")) {
                        cursorString.append(String.format("%s ][ ",
                                (cursor.getBlob(cursor.getColumnIndex(name)) != null)));
                    } else {
                        cursorString.append(String.format("%s ][ ",
                                cursor.getString(cursor.getColumnIndex(name))));
                    }
                }
                cursorString.append("\n");
            } while (cursor.moveToNext());
        }
        return cursorString.toString();
    }
}
