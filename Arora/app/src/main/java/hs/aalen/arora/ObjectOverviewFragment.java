package hs.aalen.arora;

import android.app.AlertDialog;
import android.content.Context;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.ListFragment;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

/**
 * Class that contains a list of all saved Objects in the SQLite Database
 *
 * @author Michael Schlosser
 */
public class ObjectOverviewFragment extends ListFragment {
    private static final String TAG = ObjectOverviewFragment.class.getSimpleName();

    private DatabaseHelper objectDatabaseHelper;
    private ListView objectListView;

    private ArrayList<String> objectIds = new ArrayList<>();
    private ArrayList<String> objectNames = new ArrayList<>();
    private ArrayList<String> objectTypes = new ArrayList<>();
    private ArrayList<String> objectAdditionalDatas = new ArrayList<>();
    private ArrayList<String> objectCreationDates = new ArrayList<>();
    private ArrayList<byte[]> objectPreviewImages = new ArrayList<>();

    @Override
    public void onCreate(@Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @org.jetbrains.annotations.Nullable
    @Override
    public View onCreateView(@NonNull @NotNull LayoutInflater inflater, @Nullable @org.jetbrains.annotations.Nullable ViewGroup container, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_object_overview, container, false);
        objectDatabaseHelper = new DatabaseHelper(getActivity());
        objectListView = view.findViewById(android.R.id.list);
        TextView noObjectsText = view.findViewById(R.id.no_objects_info_text);
        if(populateView())
            noObjectsText.setVisibility(View.INVISIBLE);
        else
            noObjectsText.setVisibility(View.VISIBLE);
        return view;
    }

    /**
     * Fill List View with data from our Database
     *
     * @return true if list could be populated with records of DB, false otherwise
     */
    private boolean populateView() {
        clearArrayLists();
        Cursor data = objectDatabaseHelper.getAllObjects();
        ArrayList<String> listData = new ArrayList<>();
        while(data.moveToNext()) {
            objectIds.add(data.getString(0));
            objectNames.add(data.getString(1));
            objectTypes.add(data.getString(2));
            objectAdditionalDatas.add(data.getString(3));
            objectCreationDates.add(data.getString(4));
            objectPreviewImages.add(data.getBlob(5));
        }
        ListAdapter adapter = new ObjectOverviewAdapter(getActivity().getApplicationContext(),
                                                        objectNames,
                                                        objectTypes,
                                                        objectAdditionalDatas,
                                                        objectCreationDates,
                                                        objectPreviewImages);
        objectListView.setAdapter(adapter);

        TextView noInfoText = objectListView.findViewById(R.id.no_objects_info_text);

        return !adapter.isEmpty();
    }

    /**
     * Clear Array Lists
     */
    private void clearArrayLists() {
        objectIds.clear();
        objectNames.clear();
        objectTypes.clear();
        objectAdditionalDatas.clear();
    }

    /**
     * Custom Array Adapter in order to make a more fitting list
     *
     * @author Michael Schlosser
     */
    class ObjectOverviewAdapter extends ArrayAdapter<String> {
        Context context;
        ArrayList<String> title;
        ArrayList<String> type;
        ArrayList<String> additionalInfo;
        ArrayList<String> createdAt;
        ArrayList<byte[]> image;

        // Edit Object Dialog items
        private AlertDialog.Builder editObjectDialogBuilder;
        private AlertDialog editObjectDialog;
        private EditText dialogObjectName;
        private EditText dialogObjectType;
        private EditText dialogObjectAdditionalData;
        private ImageButton confirmButton;
        private ImageButton cancelButton;

        ObjectOverviewAdapter(Context c,
                              ArrayList<String> title,
                              ArrayList<String> type,
                              ArrayList<String> additionalInfo,
                              ArrayList<String> createdAt,
                              ArrayList<byte[]> previewImage) {
            super(c, R.layout.object_item, R.id.list_object_name, title);
            this.context = c;
            this.title = title;
            this.type = type;
            this.additionalInfo = additionalInfo;
            this.createdAt = createdAt;
            this.image = previewImage;
        }

        @Override
        public View getView(int position, @Nullable View convertView, ViewGroup parent) {
            LayoutInflater layoutInflater = (LayoutInflater)getActivity().getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View item = layoutInflater.inflate(R.layout.object_item, parent, false);
            ImageButton editButton = item.findViewById(R.id.list_edit_button);
            editButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    createEditObjectDialog(position);
                }
            });
            ImageButton deleteButton = item.findViewById(R.id.list_delete_button);
            deleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(objectDatabaseHelper.deletebyId(objectIds.get(position))) {
                        deleteItem(position);
                        notifyDataSetChanged();
                    }
                }
            });
            ImageView previewImageView = item.findViewById(R.id.list_image_preview);
            TextView title = item.findViewById(R.id.list_object_name);
            TextView type = item.findViewById(R.id.list_object_type);
            TextView additionalInfo = item.findViewById(R.id.list_object_additional_data);
            TextView date = item.findViewById(R.id.list_object_date);

            previewImageView.setImageResource(R.drawable.method_draw_image_1_);
            title.setText(this.title.get(position));
            type.setText(this.type.get(position));
            additionalInfo.setText(this.additionalInfo.get(position));
            date.setText(this.createdAt.get(position));
            byte[] imageBitmap = this.image.get(position);
            try {
                previewImageView.setImageBitmap(
                        BitmapFactory.decodeByteArray(imageBitmap, 0, imageBitmap.length));
            } catch (NullPointerException e) {
                e.printStackTrace();
            }
            return item;
        }

        /**
         * Remove all Attributes from list item in GUI
         *
         * @param position position of list item
         */
        private void deleteItem(int position) {
            objectIds.remove(position);
            objectNames.remove(position);
            objectTypes.remove(position);
            objectAdditionalDatas.remove(position);
        }

        /**
         * Edit all Attributes from list item in GUI
         *
         * @param position      Position of list item
         * @param name          Name of list item
         * @param type          Type of list item
         * @param additional    Additional Information of list item
         */
        private void editItem(int position, String name, String type, String additional) {
            objectNames.set(position, name);
            objectTypes.set(position, type);
            objectAdditionalDatas.set(position, additional);
        }

        /**
         * Edit Object popup
         *
         * @param position needed to recognize clicked item and show binded information
         */
        private void createEditObjectDialog(int position) {
            editObjectDialogBuilder = new AlertDialog.Builder(getActivity());
            final View editObjectDialogView = getLayoutInflater().inflate(R.layout.edit_object_dialog_popup, null);
            dialogObjectName = editObjectDialogView.findViewById(R.id.edit_dialog_object_name);
            dialogObjectName.setText(objectNames.get(position));

            dialogObjectType = editObjectDialogView.findViewById(R.id.edit_dialog_object_type);
            dialogObjectType.setText(objectTypes.get(position));

            dialogObjectAdditionalData = editObjectDialogView.findViewById(R.id.edit_dialog_object_additional_data);
            dialogObjectAdditionalData.setText(objectAdditionalDatas.get(position));

            confirmButton = editObjectDialogView.findViewById(R.id.edit_dialog_confirm);
            cancelButton = editObjectDialogView.findViewById(R.id.edit_dialog_cancel);

            editObjectDialogBuilder.setView(editObjectDialogView);
            editObjectDialog = editObjectDialogBuilder.create();
            editObjectDialog.show();

            confirmButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String name = dialogObjectName.getText().toString();
                    String type = dialogObjectType.getText().toString();
                    String additional = dialogObjectAdditionalData.getText().toString();

                    if(objectDatabaseHelper.editObject(objectIds.get(position), name, type, additional)) {
                        editItem(position, name, type, additional);
                        Toast.makeText(context, "Successfully edited Object!", Toast.LENGTH_SHORT).show();
                        notifyDataSetChanged();
                        editObjectDialog.dismiss();
                    }
                }
            });

            cancelButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    editObjectDialog.dismiss();
                }
            });

        }
    }

}
