package com.example.endlessquiz;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

public class MyDialogFragment extends DialogFragment {
    private MainActivityFragment mainActivityFragment;

    public static MyDialogFragment newInstance(int title) {
        MyDialogFragment frag = new MyDialogFragment();
        Bundle args = new Bundle();
        args.putInt("title", title);
        frag.setArguments(args);
        return frag;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        String title = "Alert";
        mainActivityFragment = (MainActivityFragment) getTargetFragment();
        int totalGuesses = 0, correctAnswers = 0;
        if (mainActivityFragment != null) {
            correctAnswers = mainActivityFragment.getCorrectAnswers();
            totalGuesses = mainActivityFragment.getTotalGuess();
        }

        return new AlertDialog.Builder(getActivity())
                .setTitle(title)
                .setPositiveButton(getString(R.string.alert_positive),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                    mainActivityFragment.resetQuiz();
                                }
                            }
                        }
                ).setMessage(getString(R.string.results, totalGuesses, correctAnswers))
                .create();
    }
}