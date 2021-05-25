package hs.aalen.arora;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.ListFragment;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Array;
import java.util.ArrayList;

public class ObjectOverviewFragment extends ListFragment {
    private static final String TAG = ObjectOverviewFragment.class.getSimpleName();

    private DatabaseHelper databaseHelper;
    private ListView objectListView;

    private ArrayList<String> objectIds = new ArrayList<>();
    private ArrayList<String> objectNames = new ArrayList<>();
    private ArrayList<String> objectTypes = new ArrayList<>();
    private ArrayList<String> objectAdditionalDatas = new ArrayList<>();
    private ArrayList<String> objectCreationDates = new ArrayList<>();

    @Override
    public void onCreate(@Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @org.jetbrains.annotations.Nullable
    @Override
    public View onCreateView(@NonNull @NotNull LayoutInflater inflater, @Nullable @org.jetbrains.annotations.Nullable ViewGroup container, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_object_overview, container, false);
        databaseHelper = new DatabaseHelper(getActivity());
        objectListView = view.findViewById(android.R.id.list);
        populateView();
        return view;
    }

    private void populateView() {
        Cursor data = databaseHelper.getData();
        ArrayList<String> listData = new ArrayList<>();
        while(data.moveToNext()) {
            objectIds.add(data.getString(0));
            objectNames.add(data.getString(1));
            objectTypes.add(data.getString(2));
            objectAdditionalDatas.add(data.getString(3));
        }
        ListAdapter adapter = new ObjectOverviewAdapter(getActivity().getApplicationContext(), objectNames, objectTypes, objectAdditionalDatas, null);
        objectListView.setAdapter(adapter);
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
        ArrayList<Integer> images;

        ObjectOverviewAdapter(Context c, ArrayList<String> title, ArrayList<String> type, ArrayList<String> additionalInfo, ArrayList<Integer> images) {
            super(c, R.layout.object_item, R.id.list_object_name, title);
            this.context = c;
            this.title = title;
            this.type = type;
            this.additionalInfo = additionalInfo;
            this.images = images;
        }

        @Override
        public View getView(int position, @Nullable View convertView, ViewGroup parent) {
            LayoutInflater layoutInflater = (LayoutInflater)getActivity().getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View item = layoutInflater.inflate(R.layout.object_item, parent, false);
            ImageButton editButton = item.findViewById(R.id.list_edit_button);
            editButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // TODO edit button
                }
            });
            ImageButton deleteButton = item.findViewById(R.id.list_delete_button);
            deleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(databaseHelper.deletebyId(objectIds.get(position))) {
                        deleteItem(position);
                        notifyDataSetChanged();
                    }
                }
            });
            ImageView images = item.findViewById(R.id.list_image_preview);
            TextView title = item.findViewById(R.id.list_object_name);
            TextView type = item.findViewById(R.id.list_object_type);
            TextView additionalInfo = item.findViewById(R.id.list_object_additional_data);

            images.setImageResource(R.drawable.method_draw_image_1_);
            title.setText(this.title.get(position));
            type.setText(this.title.get(position));
            additionalInfo.setText(this.additionalInfo.get(position));
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
    }

}
