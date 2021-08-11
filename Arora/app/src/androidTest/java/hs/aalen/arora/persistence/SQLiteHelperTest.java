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