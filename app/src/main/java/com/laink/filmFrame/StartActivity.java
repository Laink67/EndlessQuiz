package com.laink.filmFrame;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.transition.TransitionManager;
import android.view.animation.LinearInterpolator;
import android.widget.ProgressBar;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;

import com.google.android.material.card.MaterialCardView;

public class StartActivity extends AppCompatActivity {

    Handler handler;
    ConstraintLayout cs;
    ConstraintSet constraintSet1 = new ConstraintSet();
    ConstraintSet constraintSet2 = new ConstraintSet();
    ConstraintSet constraintSet3 = new ConstraintSet();
    ConstraintSet constraintSet4 = new ConstraintSet();
    MaterialCardView card;
    private ProgressBar progressBar;
    ObjectAnimator progressAnimator;
    LinearInterpolator linearInterpolator;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.start_activity);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        cs = findViewById(R.id.start_activity);
        card = findViewById(R.id.start_activity_card1);
        progressBar = findViewById(R.id.progress_bar);

        linearInterpolator = new LinearInterpolator();
        constraintSet2.clone(this, R.layout.start_activity_after_animate);
        constraintSet1.clone(this, R.layout.start_activity3);
        constraintSet3.clone(this, R.layout.start_activity4);
        constraintSet4.clone(this, R.layout.start_activity5);

        handler = new Handler();

    }

    @Override
    protected void onStart() {
        super.onStart();

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    TransitionManager.beginDelayedTransition(cs);
                }
                constraintSet2.applyTo(cs);
                animateProgress(10,25);
            }
        }, 500);


        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    TransitionManager.beginDelayedTransition(cs);
                }
                constraintSet1.applyTo(cs);
                animateProgress(25,40);
            }
        }, 1200);


        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    TransitionManager.beginDelayedTransition(cs);
                }
                constraintSet3.applyTo(cs);
                animateProgress(40,65);
            }
        }, 1800);

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    TransitionManager.beginDelayedTransition(cs);
                }
                constraintSet4.applyTo(cs);
                animateProgress(65,100);
            }
        }, 2300);



        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(StartActivity.this, MainActivity.class);
                startActivity(intent);
            }
        }, 3000);
    }

    private void animateProgress(int begin, int end) {
        progressAnimator = ObjectAnimator.ofInt(progressBar, "progress", begin, end);
        progressAnimator.setInterpolator(linearInterpolator);
        progressAnimator.start();
    }
}
