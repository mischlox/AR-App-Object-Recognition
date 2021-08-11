package hs.aalen.arora.persistence;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

import static com.google.common.truth.Truth.assertThat;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class SQLiteHelperTest {
    SQLiteDatabase db;
    SQLiteHelper dbHelper;
    @Before
    public void setUp() {
        dbHelper = new SQLiteHelper(ApplicationProvider.getApplicationContext());
        db = dbHelper.getWritableDatabase();
        seedDB();
    }

    @After
    public void tearDown() {
        dbHelper.deleteAll();
        dbHelper.close();
    }

    @Test
    public void getTrainingSamples_modelIDisValid_returnsCursorWithElements() {
        Cursor result = dbHelper.getTrainingSamples("1");
        assertThat(result.getCount()).isGreaterThan(0);
    }

    @Test
    public void getTrainingSamples_modelIDisInvalid_returnsEmptyCursor() {
        Cursor result = dbHelper.getTrainingSamples("-999");
        assertThat(result.getCount()).isEqualTo(0);
    }

    @Test
    public void getTrainingSamples_modelIDisEmpty_returnsEmptyCursor() {
        Cursor result = dbHelper.getTrainingSamples("");
        assertThat(result.getCount()).isEqualTo(0);
    }

    @Test
    public void getTrainingSamples_modelIDisNull_returnsEmptyCursor() {
        Cursor result = dbHelper.getTrainingSamples(null);
        assertThat(result.getCount()).isEqualTo(0);
    }

    @Test
    public void getReplayBuffer_modelIDisValid_returnsCursorWithElements() {
        Cursor result = dbHelper.getReplayBuffer("1");
        assertThat(result.getCount()).isGreaterThan(0);
    }

    @Test
    public void getReplayBuffer_modelIDisInvalid_returnsEmptyCursor() {
        Cursor result = dbHelper.getReplayBuffer("-999");
        assertThat(result.getCount()).isEqualTo(0);
    }

    @Test
    public void getReplayBuffer_modelIDisNull_returnsEmptyCursor() {
        Cursor result = dbHelper.getReplayBuffer(null);
        assertThat(result.getCount()).isEqualTo(0);
    }

    @Test
    public void emptyReplayBuffer_deletesBuffer() {
        String before = dbHelper.cursorToString(dbHelper.getReplayBuffer("1"));
        dbHelper.emptyReplayBuffer("1");
        String after = dbHelper.cursorToString(dbHelper.getReplayBuffer("1"));

        assertThat(before).isNotEqualTo(after);
    }

    @Test
    public void modelIsFrozen_modelIDisValidAndModelFrozen_returnsTrue() {
        dbHelper.updateModelIsFrozen("1", true);
        assertThat(dbHelper.modelIsFrozen("1")).isEqualTo(true);
    }

    @Test
    public void modelIsFrozen_modelIDisValidAndModelNotFrozen_returnsFalse() {
        dbHelper.updateModelIsFrozen("1", false);
        assertThat(dbHelper.modelIsFrozen("1")).isEqualTo(false);
    }

    @Test
    public void modelIsFrozen_modelIDisInvalid_returnsFalse() {
        assertThat(dbHelper.modelIsFrozen("-999")).isEqualTo(false);
    }

    @Test
    public void modelIsFrozen_modelIDisNull_returnsFalse() {
        assertThat(dbHelper.modelIsFrozen(null)).isEqualTo(false);
    }

    @Test
    public void insertObject_returnsPositiveLong() {
        long result = dbHelper.insertObject("name","type","add", "1");
        assertThat(result).isGreaterThan(0);
    }

    @Test
    public void insertObject_modelIDisNull_returnsNegativeLong() {
        long result = dbHelper.insertObject("name","type","add", null);
        assertThat(result).isLessThan(0);
    }

    @Test
    public void insertObject_NameIsNull_returnsNegativeLong() {
        long result = dbHelper.insertObject(null,"type","add", "1");
        assertThat(result).isLessThan(0);
    }

    @Test
    public void insertObject_NameAlreadyExists_returnsNegativeLong() {
        long result = dbHelper.insertObject("object 1","type","add", "1");
        assertThat(result).isLessThan(0);
    }

    @Test
    public void updateImageBlob_updatesRow() {
        String before = dbHelper.cursorToString(dbHelper.getAllObjects());
        dbHelper.updateImageBlob("object 1", Bitmap.createBitmap(10,10,Bitmap.Config.ARGB_8888));
        String after = dbHelper.cursorToString(dbHelper.getAllObjects());

        assertThat(before).isNotEqualTo(after);
    }

    @Test
    public void updateImageBlob_objectNameIsInvalidOrNull_doesNothing() {
        String before = dbHelper.cursorToString(dbHelper.getAllObjects());
        dbHelper.updateImageBlob("objekt 1", Bitmap.createBitmap(10,10,Bitmap.Config.ARGB_8888));
        String afterInvalid = dbHelper.cursorToString(dbHelper.getAllObjects());
        dbHelper.updateImageBlob(null, Bitmap.createBitmap(10,10, Bitmap.Config.ARGB_8888));
        String afterNull = dbHelper.cursorToString(dbHelper.getAllObjects());

        assertThat(before).isEqualTo(afterInvalid);
        assertThat(before).isEqualTo(afterNull);
    }

    @Test
    public void updateModelPos_objectNameIsInvalidOrNull_returnsFalse() {
        boolean resultInvalid = dbHelper.updateModelPos("objekt 1","1");
        boolean resultNull = dbHelper.updateModelPos(null, "1");

        assertThat(resultInvalid).isFalse();
        assertThat(resultNull).isFalse();
    }

    @Test
    public void updateModelPos_modelPosIsNoDigit_returnsTrue() {
        // should also be possible, because the model position refers to the endpoints of CL-Model
        boolean result = dbHelper.updateModelPos("object 1","position 2" );
        assertThat(result).isTrue();
    }

    @Test
    public void getAllObjectsByModelID_returnsAllObjectsByModelID() {
        String[] TEST_NAMES = new String[]{"first obj", "second obj", "third obj"};
        String MODEL_ID = "100";
        String MODEL_ID2 = "101";

        dbHelper.insertObject(TEST_NAMES[0], null, null, MODEL_ID);
        dbHelper.insertObject(TEST_NAMES[1], null, null, MODEL_ID);
        dbHelper.insertObject(TEST_NAMES[2], null, null, MODEL_ID2);

        Cursor result = dbHelper.getAllObjectsByModelID(MODEL_ID);
        Cursor result2 = dbHelper.getAllObjectsByModelID(MODEL_ID2);

        assertThat(result.getCount()).isEqualTo(2);
        assertThat(result2.getCount()).isEqualTo(1);
    }

    @Test
    public void getObjectNamesByModelID_returnsObjectNamesByModelID() {
        String[] TEST_NAMES1 = new String[]{"first obj", "second obj", "third obj"};
        String[] TEST_NAMES2 = new String[]{"fourth obj", "fifth obj", "sixth obj"};

        String MODEL_ID = "100";
        String MODEL_ID2 = "101";

        dbHelper.insertObject(TEST_NAMES1[0], null, null, MODEL_ID);
        dbHelper.insertObject(TEST_NAMES1[1], null, null, MODEL_ID);
        dbHelper.insertObject(TEST_NAMES1[2], null, null, MODEL_ID);

        dbHelper.insertObject(TEST_NAMES2[0], null, null, MODEL_ID2);
        dbHelper.insertObject(TEST_NAMES2[1], null, null, MODEL_ID2);
        dbHelper.insertObject(TEST_NAMES2[2], null, null, MODEL_ID2);

        Cursor result = dbHelper.getObjectNamesByModelID(MODEL_ID);
        int i = 0;
        while(result.moveToNext()) {
            assertThat(result.getString(0)).isEqualTo(TEST_NAMES1[i++]);
        }

        Cursor result2 = dbHelper.getObjectNamesByModelID(MODEL_ID2);
        i = 0;
        while(result2.moveToNext()) {
            assertThat(result2.getString(0)).isEqualTo(TEST_NAMES2[i++]);
        }
    }

    @Test
    public void getObjectNamesByModelID_invalidModelID_returnsEmptyCursor() {
        String TEST_NAME = "test object";
        String MODEL_ID = "100";

        dbHelper.insertObject(TEST_NAME, null, null, MODEL_ID);
        Cursor result = dbHelper.getObjectNamesByModelID("-1");

        assertThat(result.getCount()).isEqualTo(0);
    }

    @Test
    public void getModelNameByID_returnsModelNameByID() {
        dbHelper.deleteAll();
        String TEST_NAME = "test name";
        dbHelper.insertModel(TEST_NAME);
        String result = dbHelper.getModelNameByID("1");

        assertThat(result).isEqualTo(TEST_NAME);
    }

    @Test
    public void getModelNameByID_modelIDisInvalid_returnsNull() {
        String result = dbHelper.getModelNameByID("invalidId999");
        assertThat(result).isNull();
    }

    @Test
    public void getModelNameByID_modelIDisNull_returnsNull() {
        String result = dbHelper.getModelNameByID(null);
        assertThat(result).isNull();
    }

    @Test
    public void getObjectByModelPosAndModelID_returnsObjects() {
        String OBJECT_NAME = "test object";
        dbHelper.insertObject(OBJECT_NAME, null, null, "1");
        dbHelper.updateModelPos(OBJECT_NAME, "1");
        Cursor result = dbHelper.getObjectByModelPosAndModelID("1", "1");

        assertThat(result.getCount()).isGreaterThan(0);
    }

    @Test
    public void deleteObjectById_IDisValid_returnsTrueAndDeletesObject() {
        dbHelper.deleteAll();
        dbHelper.insertObject("test object", null, null, "1");
        boolean result = dbHelper.deleteObjectById("1");
        String after = dbHelper.cursorToString(dbHelper.getAllObjects());

        assertThat(result).isTrue();
        assertThat(after).isEmpty();
    }

    @Test
    public void deleteObjectById_IDisInvalid_returnsFalse() {
        dbHelper.deleteAll();
        dbHelper.insertObject("test object", null, null, "1");
        boolean result = dbHelper.deleteObjectById("-999");

        assertThat(result).isFalse();
    }

    @Test
    public void deleteObjectByNameAndModelID_deletesObject() {
        dbHelper.deleteAll();
        String MODEL_ID = "1";
        String OBJECT_NAME = "test";
        dbHelper.insertObject(OBJECT_NAME, null, null, MODEL_ID);
        String before = dbHelper.cursorToString(dbHelper.getAllObjects());
        dbHelper.deleteObjectByNameAndModelID(OBJECT_NAME, MODEL_ID);
        String after = dbHelper.cursorToString(dbHelper.getAllObjects());

        assertThat(before).isNotEqualTo(after);
    }

    @Test
    public void deleteObjectByNameAndModelID_objectNameOrModelIDisInvalid_doesNothing() {
        dbHelper.deleteAll();
        String MODEL_ID = "1";
        String OBJECT_NAME = "test";
        dbHelper.insertObject(OBJECT_NAME, null, null, MODEL_ID);
        String before = dbHelper.cursorToString(dbHelper.getAllObjects());

        dbHelper.deleteObjectByNameAndModelID("invalid", MODEL_ID);
        String afterObjectNameInvalid = dbHelper.cursorToString(dbHelper.getAllObjects());
        assertThat(before).isEqualTo(afterObjectNameInvalid);

        dbHelper.deleteObjectByNameAndModelID(OBJECT_NAME, "invalid");
        String afterModelIDInvalid = dbHelper.cursorToString(dbHelper.getAllObjects());
        assertThat(before).isEqualTo(afterModelIDInvalid);

        dbHelper.deleteObjectByNameAndModelID("invalid", "invalid");
        String afterBothInvalid = dbHelper.cursorToString(dbHelper.getAllObjects());
        assertThat(before).isEqualTo(afterBothInvalid);
    }

    @Test
    public void getObjectByModelPosAndModelID_modelPosIsNull_returnsEmptyCursor() {
        dbHelper.insertObject("test object", null, null, "1");
        Cursor result = dbHelper.getObjectByModelPosAndModelID(null, "1");

        assertThat(result.getCount()).isEqualTo(0);
    }

    @Test
    public void getObjectByModelPosAndModelID_modelPosAndIDisNull_returnsEmptyCursor() {
        dbHelper.insertObject("test object", null, null, "1");
        Cursor result = dbHelper.getObjectByModelPosAndModelID(null, null);

        assertThat(result.getCount()).isEqualTo(0);
    }

    @Test
    public void getObjectModelPosByNameAndModelID_returnsPos() {
        String OBJECT_NAME = "test";
        String MODEL_ID = "test";
        String MODEL_POS = "1";
        dbHelper.insertObject(OBJECT_NAME, null, null, MODEL_ID);
        dbHelper.updateModelPos(OBJECT_NAME, MODEL_POS);
        String result = dbHelper.getObjectModelPosByNameAndModelID(OBJECT_NAME, MODEL_ID);

        assertThat(result).isEqualTo(MODEL_POS);
    }

    @Test
    public void getObjectModelPosByNameAndModelID_posIsNull_returnsEmptyString() {
        String OBJECT_NAME = "test";
        String MODEL_ID = "test";
        dbHelper.insertObject(OBJECT_NAME, null, null, MODEL_ID);
        String result = dbHelper.getObjectModelPosByNameAndModelID(OBJECT_NAME, MODEL_ID);

        assertThat(result).isEmpty();
    }

    @Test
    public void getObjectModelPosByNameAndModelID_nameOrIDisNullOrEmpty_returnsEmptyString() {
        String OBJECT_NAME = "test";
        String MODEL_ID = "test";
        dbHelper.insertObject(OBJECT_NAME, null, null, MODEL_ID);
        String resultNameNull = dbHelper.getObjectModelPosByNameAndModelID(null, MODEL_ID);
        String resultIDNull = dbHelper.getObjectModelPosByNameAndModelID(OBJECT_NAME, null);
        String resultBothNull = dbHelper.getObjectModelPosByNameAndModelID(null, null);

        assertThat(resultNameNull).isEmpty();
        assertThat(resultIDNull).isEmpty();
        assertThat(resultBothNull).isEmpty();

        String resultNameEmpty = dbHelper.getObjectModelPosByNameAndModelID("", MODEL_ID);
        String resultIDEmpty = dbHelper.getObjectModelPosByNameAndModelID(OBJECT_NAME, "");
        String resultBothEmpty = dbHelper.getObjectModelPosByNameAndModelID("", "");

        assertThat(resultNameEmpty).isEmpty();
        assertThat(resultIDEmpty).isEmpty();
        assertThat(resultBothEmpty).isEmpty();
    }

    @Test
    public void editObject_allValuesGiven_updatesObjectValues() {
        String TEST_MODEL_NAME = "model 1";
        String NEW_OBJECT_NAME = "new name";
        String NEW_OBJECT_TYPE = "new type";
        String NEW_OBJECT_ADD = "new add";
        dbHelper.deleteAll();

        dbHelper.insertModel(TEST_MODEL_NAME);
        dbHelper.insertObject("1", null, null, "1");
        dbHelper.editObject("1", NEW_OBJECT_NAME ,NEW_OBJECT_TYPE, NEW_OBJECT_ADD);

        Cursor result = dbHelper.getAllObjects();
        if(result.moveToFirst()) {
            assertThat(result.getString(1)).isEqualTo(NEW_OBJECT_NAME);
            assertThat(result.getString(2)).isEqualTo(NEW_OBJECT_TYPE);
            assertThat(result.getString(3)).isEqualTo(NEW_OBJECT_ADD);
        }
    }

    @Test
    public void editObject_ValuesNullOrEmpty_updatesObjectValuesExceptRequiredName() {
        String TEST_MODEL_NAME = "model 1";
        String OLD_OBJECT_NAME = "old name";
        String OLD_OBJECT_TYPE = "old type";
        String OLD_OBJECT_ADD = "old add";
        dbHelper.deleteAll();

        dbHelper.insertModel(TEST_MODEL_NAME);
        dbHelper.insertObject(OLD_OBJECT_NAME, OLD_OBJECT_TYPE, OLD_OBJECT_ADD, "1");
        dbHelper.editObject("1", "" ,null, "");

        Cursor result = dbHelper.getAllObjects();
        if(result.moveToFirst()) {
            assertThat(result.getString(1)).isEqualTo(OLD_OBJECT_NAME);
            assertThat(result.getString(2)).isEqualTo(null);
            assertThat(result.getString(3)).isEqualTo("");
        }
    }

    @Test
    public void getModelPathByID_pathIsStored_returnsPath() {
        String TEST_MODEL_NAME = "test model";
        Path TEST_PATH = Paths.get("/path/to/test/params");
        dbHelper.deleteAll();
        dbHelper.insertModel(TEST_MODEL_NAME);
        dbHelper.insertOrUpdateModel(TEST_MODEL_NAME, TEST_PATH);
        String result = dbHelper.getModelPathByID("1");

        assertThat(result).isEqualTo(TEST_PATH.toString());
    }

    @Test
    public void getModelPathByID_ModelIDInvalid_returnsNull() {
        String TEST_MODEL_NAME = "test model";
        Path TEST_PATH = Paths.get("/path/to/test/params");
        dbHelper.deleteAll();
        dbHelper.insertModel(TEST_MODEL_NAME);
        dbHelper.insertOrUpdateModel(TEST_MODEL_NAME, TEST_PATH);
        String result = dbHelper.getModelPathByID("-999");

        assertThat(result).isNull();
    }

    @Test
    public void getModelPathByID_pathIsEmpty_returnsNull() {
        dbHelper.deleteAll();
        dbHelper.insertModel("model");

        String result = dbHelper.getModelPathByID("1");
        assertThat(result).isNull();
    }

    @Test
    public void getObjectModelPosByName_returnsPos() {
        String TEST_OBJ_NAME = "test object";
        String TEST_POS = "1";
        dbHelper.insertObject(TEST_OBJ_NAME, null, null,  "1");
        dbHelper.updateModelPos(TEST_OBJ_NAME, TEST_POS);

        String result = dbHelper.getObjectModelPosByName(TEST_OBJ_NAME);
        assertThat(result).isEqualTo(TEST_POS);
    }

    @Test
    public void getObjectModelPosByName_modelPosIsEmpty_returnsNull() {
        String TEST_OBJ_NAME = "test object";
        dbHelper.insertObject(TEST_OBJ_NAME, null, null,  "1");

        String result = dbHelper.getObjectModelPosByName(TEST_OBJ_NAME);
        assertThat(result).isNull();
    }

    @Test
    public void getObjectModelPosByName_objectNameIsNullOrEmpty_returnsNull() {
        String TEST_OBJ_NAME = "test object";
        dbHelper.insertObject(TEST_OBJ_NAME, null, null,  "1");

        String resultEmpty = dbHelper.getObjectModelPosByName("");
        String resultNull = dbHelper.getObjectModelPosByName(null);

        assertThat(resultEmpty).isNull();
        assertThat(resultNull).isNull();
    }

    @Test
    public void insertOrUpdateModel_noModelSaved_returnsTrueAndInserts() {
        String TEST_MODEL_NAME= "test";
        Path TEST_PATH = Paths.get("/path/to/parameters");
        dbHelper.deleteAll();
        long before = dbHelper.getAllModels().getCount();
        boolean result = dbHelper.insertOrUpdateModel(TEST_MODEL_NAME, TEST_PATH);
        long after = dbHelper.getAllModels().getCount();

        assertThat(before).isLessThan(after);
        assertThat(result).isTrue();
    }

    @Test
    public void insertOrUpdateModel_modelSaved_returnsTrueAndUpdates() {
        String TEST_MODEL_NAME= "test";
        Path TEST_PATH = Paths.get("/path/to/parameters");
        dbHelper.deleteAll();
        dbHelper.insertModel(TEST_MODEL_NAME);

        long before = dbHelper.getAllModels().getCount();
        boolean result = dbHelper.insertOrUpdateModel(TEST_MODEL_NAME, TEST_PATH);
        long after = dbHelper.getAllModels().getCount();

        assertThat(before).isEqualTo(after);
        assertThat(result).isTrue();
    }

    @Test
    public void insertOrUpdateModel_nameOrPathEmptyOrNull_returnsFalse() {
        String TEST_MODEL_NAME= "test";
        Path TEST_PATH = Paths.get("/path/to/parameters");

        dbHelper.deleteAll();

        boolean resultNameEmpty = dbHelper.insertOrUpdateModel("", TEST_PATH);
        boolean resultNameNull = dbHelper.insertOrUpdateModel(null, TEST_PATH);
        boolean resultPathNull = dbHelper.insertOrUpdateModel(TEST_MODEL_NAME, null);

        assertThat(resultNameEmpty).isFalse();
        assertThat(resultNameNull).isFalse();
        assertThat(resultPathNull).isFalse();
    }

    @Test
    public void insertModel_NameIsValid_returnsTrue() {
        boolean result = dbHelper.insertModel("a valid name");
        assertThat(result).isTrue();
    }

    @Test
    public void insertModel_NameAlreadyExists_returnsFalse() {
        dbHelper.insertModel("test");
        boolean result = dbHelper.insertModel("test");

        assertThat(result).isFalse();
    }

    @Test
    public void insertModel_NameIsEmpty_returnsFalse() {
        boolean result = dbHelper.insertModel("");
        assertThat(result).isFalse();
    }

    @Test
    public void insertModel_NameIsNull_returnsFalse() {
        boolean result = dbHelper.insertModel(null);
        assertThat(result).isFalse();
    }

    @Test
    public void modelHasPath_hasPath_returnsTrue() {
        String TEST_MODEL_NAME = "test";
        dbHelper.deleteAll();
        dbHelper.insertOrUpdateModel(TEST_MODEL_NAME, Paths.get("/a/b/c"));
        boolean result = dbHelper.modelHasPath("1");

        assertThat(result).isTrue();
    }

    @Test
    public void modelHasPath_hasNoPath_returnsFalse() {
        String TEST_MODEL_NAME = "test";
        dbHelper.deleteAll();
        dbHelper.insertModel(TEST_MODEL_NAME);
        boolean result = dbHelper.modelHasPath("1");

        assertThat(result).isFalse();
    }

    @Test
    public void modelHasPath_IDisInvalid_returnsFalse() {
        boolean resultNull = dbHelper.modelHasPath(null);
        boolean resultEmpty = dbHelper.modelHasPath("");

        assertThat(resultNull).isFalse();
        assertThat(resultEmpty).isFalse();
    }

    @Test
    public void modelsExists_modelsAreSaved_returnsTrue() {
        assertThat(dbHelper.modelsExists()).isTrue();
    }

    @Test
    public void modelsExists_noModelsSaved_returnsFalse() {
        dbHelper.deleteAll();
        assertThat(dbHelper.modelsExists()).isFalse();
    }

    @Test
    public void objectsExists_ObjectsAreSaved_returnsTrue() {
        assertThat(dbHelper.objectsExist()).isTrue();
    }

    @Test
    public void objectsExists_noObjectsSaved_returnsFalse() {
        dbHelper.deleteAll();
        assertThat(dbHelper.objectsExist()).isFalse();
    }

    @Test
    public void getModelIdByName_returnsID() {
        String MODEL_NAME = "name";
        String MODEL_ID = "1";
        dbHelper.deleteAll();
        dbHelper.insertModel(MODEL_NAME);
        String result = dbHelper.getModelIdByName(MODEL_NAME);

        assertThat(result).isEqualTo(MODEL_ID);
    }

    @Test
    public void getModelIdByName_nameInvalid_returnsEmptyString() {
        String MODEL_NAME = "name";
        dbHelper.deleteAll();
        dbHelper.insertModel(MODEL_NAME);
        String result = dbHelper.getModelIdByName("invalid");

        assertThat(result).isEmpty();
    }

    @Test
    public void addSamplesToObject_addsSamples() {
        String OBJECT_NAME = "object 1";

        int before = dbHelper.getAmountSamplesByObjectName(OBJECT_NAME);
        dbHelper.addSamplesToObject(OBJECT_NAME, 10);
        int after = dbHelper.getAmountSamplesByObjectName(OBJECT_NAME);

        assertThat(before).isLessThan(after);
    }

    @Test
    public void addSamplesToObject_amountNegative_addsNothing() {
        String OBJECT_NAME = "object 1";

        int before = dbHelper.getAmountSamplesByObjectName(OBJECT_NAME);
        dbHelper.addSamplesToObject(OBJECT_NAME, -100);
        int after = dbHelper.getAmountSamplesByObjectName(OBJECT_NAME);

        assertThat(before).isEqualTo(after);
    }

    @Test
    public void addSamplesToObject_nameIsInvalid_doesNothing() {
        String OBJECT_NAME = "object 1";
        int before = dbHelper.getAmountSamplesByObjectName(OBJECT_NAME);
        dbHelper.addSamplesToObject(null, 30);
        int afterNull = dbHelper.getAmountSamplesByObjectName(OBJECT_NAME);
        dbHelper.addSamplesToObject("invalid", 10);
        int afterInvalid = dbHelper.getAmountSamplesByObjectName(OBJECT_NAME);
        assertThat(before).isEqualTo(afterNull);
        assertThat(before).isEqualTo(afterInvalid);
    }

    private void seedDB() {
        dbHelper.insertModel("model 1");
        dbHelper.insertModel("model 2");
        dbHelper.insertModel("model 3");

        dbHelper.insertObject("object 1", "type", "add","1");
        dbHelper.insertObject("object 2", "type", null, "2");
        dbHelper.insertObject("object 3", null, null, "3");

        HashMap<String, byte[]> TEST_ACTIVATIONS = new HashMap<String, byte[]>() {{
            put("1", new byte[]{1, 2, 4, 8, 64, 32, 11});
        }};
        dbHelper.insertTrainingSampleBatch(TEST_ACTIVATIONS, "1");
        dbHelper.insertTrainingSampleBatch(TEST_ACTIVATIONS, "2");
        dbHelper.insertTrainingSampleBatch(TEST_ACTIVATIONS, "3");
        dbHelper.insertTrainingSampleBatch(TEST_ACTIVATIONS, "4");

        dbHelper.insertReplaySampleBatch(TEST_ACTIVATIONS, "1");
        dbHelper.insertReplaySampleBatch(TEST_ACTIVATIONS, "2");
        dbHelper.insertReplaySampleBatch(TEST_ACTIVATIONS, "3");

    }
}