package hs.aalen.arora.dialogues;

import android.app.AlertDialog;
import android.content.Context;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.LinkedList;

import hs.aalen.arora.persistence.GlobalConfig;
import hs.aalen.arora.R;
import hs.aalen.arora.persistence.SharedPrefsHelper;

/**
 * Dialog that gives instructions for the usage of this app
 */
public class HelpDialog implements Dialog {
    private AlertDialog helpDialog;
    private TextView helpTextView;
    private ProgressBar helpProgress;
    private TextView helpProgressText;
    private Context context;
    private GlobalConfig globalConfig;

    @Override
    public void createDialog(Context context) {
        this.context = context;
        this.globalConfig = new SharedPrefsHelper(context);

        AlertDialog.Builder helpDialogBuilder = new AlertDialog.Builder(context);

        final View helpDialogView = ((LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                .inflate(R.layout.help_popup, null);
        FloatingActionButton backwardButton = helpDialogView.findViewById(R.id.help_backward_button);
        FloatingActionButton forwardButton = helpDialogView.findViewById(R.id.help_forward_button);
        Button trainingButton = helpDialogView.findViewById(R.id.help_training_button);
        CheckBox notShowAgainCheckBox = helpDialogView.findViewById(R.id.help_checkbox);
        helpTextView = helpDialogView.findViewById(R.id.help_dialog_text);
        helpProgress = helpDialogView.findViewById(R.id.help_progress);
        helpProgressText = helpDialogView.findViewById(R.id.help_progress_text);

        helpDialogBuilder.setView(helpDialogView);
        helpDialog = helpDialogBuilder.create();
        helpDialog.show();

        // Linked list to browse through text views
        LinkedList<Pair<Integer, String>> textList = new LinkedList<>();
        textList.add(Pair.create(2, context.getString(R.string.help_text_tilt_camera)));
        textList.add(Pair.create(3, context.getString(R.string.help_text_counter_training)));
        textList.add(Pair.create(4, context.getString(R.string.help_text_havefun)));

        notShowAgainCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> globalConfig.setHelpShowing(!isChecked));
        // Next slide
        forwardButton.setOnClickListener(v -> {
            startTextAnimation(helpTextView, R.anim.anim_fade_out);

            String currentText = helpTextView.getText().toString();
            int currentProgress = helpProgress.getProgress();
            Pair<Integer, String> nextItem = textList.removeFirst();

            helpProgress.setProgress(nextItem.first);
            startTextAnimation(helpTextView, R.anim.anim_fade_in);
            helpTextView.setText(nextItem.second, TextView.BufferType.SPANNABLE);
            String progressText = nextItem.first.toString() + " / " + helpProgress.getMax();
            helpProgressText.setText(progressText);

            textList.addLast(Pair.create(currentProgress, currentText));
        });
        // Previous slide
        backwardButton.setOnClickListener(v -> {
            startTextAnimation(helpTextView, R.anim.anim_fade_out);

            String currentText = helpTextView.getText().toString();
            int currentProgress = helpProgress.getProgress();
            Pair<Integer, String> nextItem = textList.removeLast();

            helpProgress.setProgress(nextItem.first);
            startTextAnimation(helpTextView, R.anim.anim_fade_in);
            helpTextView.setText(nextItem.second, TextView.BufferType.SPANNABLE);
            String progressText = nextItem.first.toString() + " / " + helpProgress.getMax();
            helpProgressText.setText(progressText);

            textList.addFirst(Pair.create(currentProgress, currentText));
        });
        // Go to Add object dialog
        trainingButton.setOnClickListener(v -> {
            helpDialog.dismiss();
            new AddObjectDialog().createDialog(context);
        });
    }

    private void startTextAnimation(TextView textView, int animationResource) {
        Animation animation = AnimationUtils.loadAnimation(context, animationResource);
        textView.startAnimation(animation);
    }
}
