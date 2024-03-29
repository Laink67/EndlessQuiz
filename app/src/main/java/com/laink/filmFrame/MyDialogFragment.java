package com.laink.filmFrame;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

public class MyDialogFragment extends DialogFragment {
    private MainActivityFragment mainActivityFragment;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        mainActivityFragment = (MainActivityFragment) getTargetFragment();
        int totalGuesses = 0, correctAnswers = 0;
        if (mainActivityFragment != null) {
            correctAnswers = mainActivityFragment.getCorrectAnswers();
            totalGuesses = mainActivityFragment.getTotalGuess();
        }

        return new AlertDialog.Builder(getContext())
                .setTitle(R.string.alert_title)
                .setPositiveButton(getString(R.string.alert_positive),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                    dismiss();
                                }
                            }
                        }
                ).setMessage(getString(R.string.results, totalGuesses, correctAnswers))
                .create();
    }
}