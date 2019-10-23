package com.example.endlessquiz;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.preference.PreferenceManager;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import java.util.Set;
import java.util.prefs.PreferenceChangeEvent;

public class MainActivity extends AppCompatActivity {

    //Ключи для чтения данных из SharedPreference
    public static final String CHOICES = "pref_numberOfChoice";
    public static final String REGIONS = "pref_regionsToInclude";

    private boolean phoneDevice = true; // Включение портретного режима
    private boolean preferenceChanged = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Задание значений по умолчанию в файле SharedPreferences
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        // Регистрация слушателя для изменений SharedPreferences
        PreferenceManager.getDefaultSharedPreferences(this).
                registerOnSharedPreferenceChangeListener(preferencesChangeListener);

        // Определение размера экрана
        int screenSize = getResources().getConfiguration().screenLayout &
                Configuration.SCREENLAYOUT_SIZE_MASK;

        // Для планшета phoneDevice присваивается false
        if (screenSize == Configuration.SCREENLAYOUT_SIZE_LARGE ||
                screenSize == Configuration.SCREENLAYOUT_SIZE_XLARGE)
            phoneDevice = false; // Не соответсвует размерам экрана

        // На телефоне разрешена только портретная ориентация
        if (phoneDevice)
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    //Слушатель изменений в конфигурации SharedPreferences приложения
    private SharedPreferences.OnSharedPreferenceChangeListener preferencesChangeListener =
            new SharedPreferences.OnSharedPreferenceChangeListener() {
                // При изменении настроек приложения
                @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
                @Override
                public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                    preferenceChanged = true; // Пользователь изменил настройки

                    MainActivityFragment quizFragment = (MainActivityFragment) getSupportFragmentManager().
                            findFragmentById(R.id.quizFragment);

                    if (key.equals(CHOICES)) { // Изменилось число вариантов
                        quizFragment.updateGuessRows(sharedPreferences);
                        quizFragment.resetQuiz();
                    } else if (key.equals(REGIONS)) { // Изменились регионы
                        Set<String> regions = sharedPreferences.getStringSet(REGIONS, null);

                        if (regions != null && regions.size() > 0) {
                            quizFragment.updateRegions(sharedPreferences);
                            quizFragment.resetQuiz();
                        } else {
                            // Хотя бы один регион - по умолчанию Северная Америка
                            SharedPreferences.Editor editor = sharedPreferences.edit();

                            regions.add(getString(R.string.default_region));
                            editor.putStringSet(REGIONS, regions);
                            editor.apply();

                            Snackbar.make(quizFragment.getView(), R.string.restarting_quiz,
                                    Snackbar.LENGTH_SHORT).show();
                        }
                    }
                    Snackbar.make(quizFragment.getView(),R.string.restarting_quiz,
                            Snackbar.LENGTH_SHORT).show();
                }
            };

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onStart() {
        super.onStart();
        if (preferenceChanged) {
            // После задания настроек по умолчанию инициализировать
            // MainActivityFragment и запустить
            MainActivityFragment quizFragment = (MainActivityFragment) getSupportFragmentManager().findFragmentById(R.id.quizFragment);

            quizFragment.updateGuessRows(PreferenceManager.getDefaultSharedPreferences(this));
            quizFragment.updateRegions(PreferenceManager.getDefaultSharedPreferences(this));
            quizFragment.resetQuiz();

            preferenceChanged = false;
        }
    }

    // Меню отображается только в портретной ориентации
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Получение текущей ориентации устройства
        int orientation = getResources().getConfiguration().orientation;

        //Отображение меню приложения только в портретной ориентации
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            //Заполнение меню
            getMenuInflater().inflate(R.menu.menu_main, menu);
            return true;
        } else {
            return false;
        }

    }

    //Отображает SettingsActivity при запуске на телефоне
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent preferencesIntent = new Intent(this, SettingsActivity.class);
        startActivity(preferencesIntent);
        return super.onOptionsItemSelected(item);
    }
}
