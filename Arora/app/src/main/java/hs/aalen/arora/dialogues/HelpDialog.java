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
 * Base Dialog that gives instructions for the usage of this app.
 * Extending classes only have to define elements the textList LinkedList
 *
 * @author Michael Schlosser
 */
public abstract class HelpDialog implements Dialog {
    private ProgressBar helpProgress;
    private TextView helpProgressText;
    private Context context;

    private TextView helpTextView;
    private TextView helpTextTitle;
    private int helpCardCount = 1;
    protected AlertDialog helpDialog;
    protected LinkedList<Pair<Integer, String>> textList = new LinkedList<>();
    protected View helpDialogView;

    @Override
    public void createDialog(Context context) {
        this.context = context;

        AlertDialog.Builder helpDialogBuilder = new AlertDialog.Builder(context);

        helpDialogView = ((LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                .inflate(R.layout.help_popup, null);
        FloatingActionButton backwardButton = helpDialogView.findViewById(R.id.help_backward_button);
        FloatingActionButton forwardButton = helpDialogView.findViewById(R.id.help_forward_button);

        helpTextView = helpDialogView.findViewById(R.id.help_dialog_text);
        helpProgress = helpDialogView.findViewById(R.id.help_progress);
        helpProgressText = helpDialogView.findViewById(R.id.help_progress_text);
        helpTextTitle = helpDialogView.findViewById(R.id.help_dialog_title);

        helpDialogBuilder.setView(helpDialogView);
        helpDialog = helpDialogBuilder.create();
        helpDialog.show();


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
        Button trainingButton = helpDialogView.findViewById(R.id.help_training_button);
        trainingButton.setOnClickListener(v -> {
            helpDialog.dismiss();
        });
    }

    /**
     * Method to initialize the Card View with any amount of texts and therefore make
     * it very generic in showing cards
     *
     * @param title title of card view has to be the first argument
     * @param cardTexts text that will appear on the slide
     */
    protected void initCards(String title, String... cardTexts) {
        helpTextTitle.setText(title);
        for(String text : cardTexts) {
            if(helpCardCount == 1) {
                helpTextView.setText(text);
            }
            else {
                textList.add(Pair.create(helpCardCount, text));
            }
            helpCardCount++;
        }
        helpProgress.setMax(helpCardCount-1);
        // Init progress text
        String progressText = 1 + " / " + helpProgress.getMax();
        helpProgressText.setText(progressText);
    }

    private void startTextAnimation(TextView textView, int animationResource) {
        Animation animation = AnimationUtils.loadAnimation(context, animationResource);
        textView.startAnimation(animation);
    }
}
