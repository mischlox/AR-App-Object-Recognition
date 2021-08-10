package hs.aalen.arora.persistence;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

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
    public void insertObject_returnPositiveLong() {
        long result = dbHelper.insertObject("name","type","add", "1");
        assertThat(result).isGreaterThan(0);
    }

    @Test
    public void insertObject_modelIDisNull_returnNegativeLong() {
        long result = dbHelper.insertObject("name","type","add", null);
        assertThat(result).isLessThan(0);
    }

    @Test
    public void insertObject_NameIsNull_returnNegativeLong() {
        long result = dbHelper.insertObject(null,"type","add", "1");
        assertThat(result).isLessThan(0);
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