package hs.aalen.arora;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.ListFragment;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;

import hs.aalen.arora.dialogues.Dialog;
import hs.aalen.arora.dialogues.DialogFactory;
import hs.aalen.arora.dialogues.DialogType;

/**
 * Fragment that is responsible for showing the different help dialogues by clicking
 * an item in the ListView
 *
 * @author Michael Schlosser
 */
public class HelpFragment extends ListFragment {
    private final ArrayList<String> helpTitles = new ArrayList<>();
    private final ArrayList<Integer> helpIcons = new ArrayList<>();

    @Nullable
    @org.jetbrains.annotations.Nullable
    @Override
    public View onCreateView(@NonNull @NotNull LayoutInflater inflater, @Nullable @org.jetbrains.annotations.Nullable ViewGroup container, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_help, container, false);
        ListView helpListView = view.findViewById(android.R.id.list);

        // Initialize Titles with Resource Strings
        helpTitles.clear();
        helpTitles.addAll(Arrays.asList(
                getString(R.string.help_add_object),
                getString(R.string.help_add_further_samples),
                getString(R.string.help_configure_app),
                getString(R.string.help_object_overview),
                getString(R.string.help_model_overview)));

        // Initialize Icons with ResourceIDs
        helpIcons.clear();
        helpIcons.addAll(Arrays.asList(
                R.drawable.ic_eye,
                R.drawable.ic_add,
                R.drawable.ic_gear_wheel,
                R.drawable.ic_library,
                R.drawable.ic_model_overview));

        ListAdapter adapter = new HelpAdapter(getActivity(), helpTitles, helpIcons);
        helpListView.setAdapter(adapter);
        return view;
    }


    @Override
    public void onViewCreated(@NonNull @NotNull View view, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    /**
     * Custom Array Adapter in order to make a more fitting list for the help overview
     *
     * @author Michael Schlosser
     */
    class HelpAdapter extends ArrayAdapter<String> {
        Context context;
        ArrayList<String> helpTitles;
        ArrayList<Integer> helpIcons;

        HelpAdapter(Context c, ArrayList<String> titles, ArrayList<Integer> icons) {
            super(c, R.layout.help_item, R.id.list_help_name, titles);
            this.context = c;
            this.helpTitles = titles;
            this.helpIcons = icons;
        }

        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            LayoutInflater layoutInflater = (LayoutInflater) requireActivity()
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View item = layoutInflater.inflate(R.layout.help_item, parent, false);

            TextView helpTitleTextView = item.findViewById(R.id.list_help_name);
            helpTitleTextView.setText(helpTitles.get(position));

            ImageView helpTitleImageView = item.findViewById(R.id.list_help_icon);
            helpTitleImageView.setImageResource(helpIcons.get(position));

            item.setOnClickListener(v -> {
                if(helpTitles.get(position).equals(getString(R.string.help_add_object))) {
                    DialogFactory.getDialog(DialogType.HELP_ADD_OBJ).createDialog(context);
                }
                else if(helpTitles.get(position).equals(getString(R.string.help_add_further_samples))) {
                    DialogFactory.getDialog(DialogType.HELP_ADD_MORE_SAMPLES).createDialog(context);
                }
                else if(helpTitles.get(position).equals(getString(R.string.help_configure_app))) {
                    DialogFactory.getDialog(DialogType.HELP_SETTINGS).createDialog(context);
                }
                else if(helpTitles.get(position).equals(getString(R.string.help_object_overview))) {
                    DialogFactory.getDialog(DialogType.HELP_OBJ_OVERVIEW).createDialog(context);
                }
                else if(helpTitles.get(position).equals(getString(R.string.help_model_overview))) {
                    DialogFactory.getDialog(DialogType.HELP_MODEL_OVERVIEW).createDialog(context);
                }
            });
            return item;
        }
    }
}
