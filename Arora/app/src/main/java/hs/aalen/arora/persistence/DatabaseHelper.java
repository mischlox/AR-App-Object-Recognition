package hs.aalen.arora.persistence;

import android.database.Cursor;
import android.graphics.Bitmap;

import java.nio.file.Path;
import java.util.HashMap;

public interface DatabaseHelper {
    /**
     * Name of the Database
     */
    String DATABASE_NAME = "arora_db";

    /**
     * Model Table
     */
    String MODEL_TABLE_NAME = "model_table";
    String MODEL_COL0 = "model_ID";
    String MODEL_COL1 = "model_name";
    String MODEL_COL2 = "model_path";
    String MODEL_COL3 = "is_frozen";

    /**
     * Object Table
     */
    String OBJECT_TABLE_NAME = "object_table";
    String OBJECT_COL0 = "object_ID";
    String OBJECT_COL1 = "object_name";
    String OBJECT_COL2 = "object_type";
    String OBJECT_COL3 = "object_additional_data";
    String OBJECT_COL4 = "object_created_at";
    String OBJECT_COL5 = "object_image";
    String OBJECT_COL6 = "model_pos";
    String OBJECT_COL7 = "model_id";
    String OBJECT_COL8 = "object_amount_samples";

    /**
     * Replay Buffer Table for Latent-Replay Algorithm
     */
    String REPLAY_BUFFER_TABLE_NAME = "replay_buffer_images";
    String REPLAY_BUFFER_COL0 = "buffer_id";
    String REPLAY_BUFFER_COL1 = "class";
    String REPLAY_BUFFER_COL2 = "sample_blob";
    String REPLAY_BUFFER_COL3 = "model_id";

    /**
     * Training Samples Table for updating Replay Buffer
     */
    String TRAINING_SAMPLES_TABLE_NAME = "training_samples";
    String TRAINING_SAMPLES_COL0 = "sample_id";
    String TRAINING_SAMPLES_COL1 = "class";
    String TRAINING_SAMPLES_COL2 = "sample";
    String TRAINING_SAMPLES_COL3 = "sample_timestamp";
    String TRAINING_SAMPLES_COL4 = "model_id";


    /* Replay Buffer related methods */

    /**
     * Get all data from replay buffer for a specific model
     *
     * @param modelID of model that the buffer is related to
     * @return queried data
     */
    Cursor getReplayBuffer(String modelID);

    /**
     * Clear the replay buffer in order to update it correctly
     * @param modelID of model that the buffer is related tp
     */
    void emptyReplayBuffer(String modelID);

    /**
     * Get all training samples that are stored to the
     * specified model
     *
     * @param modelID of model that the samples are stored for
     * @return all sample data of the specified model
     */
    Cursor getTrainingSamples(String modelID);

    /**
     * Insert Replay Buffer data as a batch in a transaction to increase performance
     *
     * @param activationsMap map of model position and its activation in the CL Model
     * @param modelID that the Replay Buffer is related to
     */
    void insertReplaySampleBatch(HashMap<String, byte[]> activationsMap, String modelID);


    /* Training samples related methods */

    /**
     * Insert Training Samples as a batch in a transaction to increase performance
     *
     * @param activationsMap activations and their belonging model position
     * @param modelID the activations belong to
     */
    void insertTrainingSampleBatch(HashMap<String, byte[]> activationsMap, String modelID);


    /*  Model related methods */

    /**
     * Checks if the model os frozen and therefore overwritable by new objects
     * because no replay algorithm was applied in the last training session
     *
     * @param modelID of model that will be checked
     * @return true if model is frozen, false otherwise
     */
    boolean modelIsFrozen(String modelID);

    /**
     * Change the isFrozen status
     *
     * @param modelID id of model that will be changed
     * @param isFrozen the new status
     */
    void updateModelIsFrozen(String modelID, boolean isFrozen);

    /**
     * Update the model position of the specified object
     *
     * @param objectName of the object to be updated
     * @param modelPos new model position for the object
     *
     * @return true if update was successful, false otherwise
     */
    boolean updateModelPos(String objectName, String modelPos);

    /**
     * Return all data from model table
     *
     * @return All data from table
     */
    Cursor getAllModels();

    /**
     * get the name of the model by specified ID
     *
     * @param modelID of model
     * @return name of model
     */
    String getModelNameByID(String modelID);

    /**
     * get the model parameters path of a model
     *
     * @param modelID of model that the path will be from
     * @return the path of the specified model
     */
    String getModelPathByID(String modelID);

    /**
     * Check if a model with the given name exists
     *
     * @param name name of model to be checked
     * @return true if it exists, false otherwise
     */
    boolean modelWithNameExists(String name);

    /**
     * Insert a model with name only.
     * The path should be updated when the application is closed
     *
     * @param name of the model
     * @return true if successful, false otherwise
     */
    boolean insertModel(String name);

    /**
     * Checks if a path to a model parameters file exists
     *
     * @param modelID ID of model that will be checked
     * @return true if path exists, false otherwise
     */
    boolean modelHasPath(String modelID);

    /**
     * Check if there are models in DB
     *
     * @return true if a model exists in table, false otherwise
     */
    boolean modelsExists();

    /**
     * Get model ID by model name
     *
     * @param modelName name of model
     * @return id of model
     */
    String getModelIdByName(String modelName);

    /**
     * Inserts a model to DB. If it already exists the record gets updated with a new path
     *
     * @param name name of the model
     * @param path path of the model parameters binary file
     *
     * @return true if successful, false otherwise
     */
    boolean insertOrUpdateModel(String name, Path path);


    /* Object related methods */

    /**
     * Insert object to DB
     *
     * @param objectName           name of object
     * @param objectType           type of object
     * @param objectAdditionalData additional data to object
     *
     * @return true if successful, false otherwise
     */
    long insertObject(String objectName,
                             String objectType,
                             String objectAdditionalData,
                             String modelID);

    /**
     * Update the image bitmap that is stored in the object
     *
     * @param objectID of object that will be updated
     * @param bitmap that will be stored in the specified object row
     */
    void updateImageBlob(String objectID, Bitmap bitmap);

    /**
     * Returns all data from Object Table
     *
     * @return All data from object table
     */
    Cursor getAllObjects();

    /**
     * Returns all data from Object Table that are saved in a specific model
     *
     * @return All data from table
     */
    Cursor getAllObjectsByModelID(String modelID);

    /**
     * Get all object names that are saved for the specified model
     *
     * @param modelID of the model that the objects are related to
     * @return all object names
     */
    Cursor getObjectNamesByModelID(String modelID);

    /**
     * Get the object that is saved at a specific model position in a
     * specified model
     *
     * @param modelPos position of the object in the model
     * @param modelID of the model that the object is stored in
     *
     * @return All rows of the object
     */
    Cursor getObjectByModelPosAndModelID(String modelPos, String modelID);

    /**
     * Delete an object
     *
     * @param id of the object that will be deleted
     * @return true if successful, false otherwise
     */
    boolean deleteObjectById(String id);

    /**
     * Delete an object by is name and its model ID that the object is
     * related to
     *
     * @param name of the specified object
     * @param modelID of the model the object is related to
     */
    void deleteObjectByNameAndModelID(String name, String modelID);

    /**
     * Get the model position of an object
     *
     * @param name of the specified object
     * @param modelID of the model that the object is related to
     *
     * @return the model position of the object
     */
    String getObjectModelPosByNameAndModelID(String name, String modelID);

    /**
     * Edit object by ID
     *
     * @param id                   ID of Object
     * @param objectName           Name of Object
     * @param objectType           Type of Object
     * @param objectAdditionalData Additional Data of Object
     * @return True if successful, false otherwise
     */
    boolean editObject(String id,
                              String objectName,
                              String objectType,
                              String objectAdditionalData);

    /**
     * Get the model position where the object is mapped to
     *
     * @param objectName name of object that will be queried
     * @return model position of the object
     */
    String getObjectModelPosByName(String objectName);

    /**
     * Add to total number of samples
     *
     * @param objectName name of object
     * @param amount of samples to be added to the total
     */
    void addSamplesToObject(String objectName, int amount);

    /**
     * Get the saved amount of Samples by Object name
     *
     * @param objectName name of the object
     *
     * @return amount of samples saved for this object
     */
    int getAmountSamplesByObjectName(String objectName);

    /**
     * Check if there are objects in DB
     *
     * @return true if an object exists in table, false otherwise
     */
    boolean objectsExist();

    /**
     * Drop all tables
     */
    void deleteAll();
}
