package com.laink.filmFrame;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.widget.AppCompatButton;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;


public class MainActivityFragment extends Fragment {

    private static final String TAG = "FilmQuiz Activity";
    private final String extension = ".jpg";

    private FragmentManager fragmentManager;

    private List<String> fileNameList; // Имена файлов с фильмами
    private List<String> quizFilmsList; // Фильмы текущей викторины
    private Set<String> yearsSet; // Года текущей викторины
    private int correctAnswers;
    private String correctAnswer;
    private int totalGuess; // Количество попыток
    private int guessRows; // Количество строк с кнопками вариантов
    private SecureRandom random; // Генератор случайных чисел
    private Handler handler; // Для задержки заргузки следующего фильма
    private Animation shakeAnimation; // Анимация неправильного ответа

    private ConstraintLayout quizLinearLayout; // Макет викторины
    private TextView questionNumberTextView; // Номер текущего вопроса
    private ImageView filmImageView;
    private LinearLayout[] guessLinearLayouts; // Строки с кнопками
    private Locale deviceLocale; // Язык устройства

    int getTotalGuess() {
        return totalGuess;
    }

    int getCorrectAnswers() {
        return correctAnswers;
    }

    // Настройка MainActivityFragment при создании представления
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_main_activity, container, false);

        fragmentManager = getFragmentManager();

        setHasOptionsMenu(true);

        MobileAds.initialize(getContext());// Init AdMob sdk

        AdView adView = view.findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);

        deviceLocale = Resources.getSystem().getConfiguration().locale;
        fileNameList = new ArrayList<>();
        quizFilmsList = new ArrayList<>();
        random = new SecureRandom();
        handler = new Handler();

        //Загрузка для анимации для неправильных ответов
        shakeAnimation = AnimationUtils.loadAnimation(getActivity(),
                R.anim.incorrect_shake);
        shakeAnimation.setRepeatCount(3); // Повторение анимации 3 раза

        // Получение ссылок на компоненты графического интерфейса
        quizLinearLayout = view.findViewById(R.id.quizLinearLayout);
        questionNumberTextView = view.findViewById(R.id.questionNumberTextView);
        filmImageView = view.findViewById(R.id.filmImageView);
        guessLinearLayouts = new LinearLayout[]{
                view.findViewById(R.id.row1LinearLayout),
                view.findViewById(R.id.row2LinearLayout),
                view.findViewById(R.id.row3LinearLayout),
                view.findViewById(R.id.row4LinearLayout)
        };

        // Настройка слушателей для кнопок ответов
        for (LinearLayout row : guessLinearLayouts) {
            for (int column = 0; column < row.getChildCount(); column++) {
                Button button = (Button) row.getChildAt(column);
                button.setOnClickListener(guessButtonListener);
            }
        }

        //Назначени текста questionNumberTextView
        questionNumberTextView.setText(
                getString(R.string.question, 1));

        return view;
    }

    // Обновление guessRows на основании значения SharedPreferences
    void updateGuessRows(SharedPreferences sharedPreferences) {
        // Получение количества отображаемых вариантов ответа
        String choices = sharedPreferences.getString(
                MainActivity.CHOICES, null);

        if (choices != null)
            guessRows = Integer.parseInt(choices) / 2;

        // Все компненты LinearLayout скрываются
        for (LinearLayout layout : guessLinearLayouts)
            layout.setVisibility(View.GONE);

        //Отображение нужных компонентов LinearLayout
        for (int row = 0; row < guessRows; row++)
            guessLinearLayouts[row].setVisibility(View.VISIBLE);
    }

    // Обновление выбранных регионов по данным из SharedPreferences
    void updateYears(SharedPreferences sharedPreferences) {
        yearsSet = sharedPreferences.getStringSet(MainActivity.YEARS, null);
    }

    // Настройка и запуск следующей серии вопросов
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    void resetQuiz() {
        // Использование AssetManager для получения имён файлов изображений
        AssetManager assets = getActivity().getAssets();
        fileNameList.clear(); // Список имён файлов пуст
        Map<String, String> map = new HashMap<String, String>();

        try {
            for (String year : yearsSet) {
                // Список картинок всех фильмов в "годах"
                String[] paths = assets.list(year);

                for (String path : paths) {

                    fileNameList.add(path.replace(extension, ""));
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Error loading image file names", e);
        }

        correctAnswers = 0; // Сброс количества правильных ответов
        totalGuess = 0; // Сброс общего количества попыток
        updateFilmList();

        loadNextFilm(); // Запустить викторину загрузкой первого фильма
    }

    private void updateFilmList() {
        quizFilmsList.clear(); // Очистка предыдущего списка стран

        int filmCounter = 1;
        int numberOfFilms = fileNameList.size();

        // Добавление numberOfFilms случайных файлов в quizFilmsList
        while (filmCounter <= numberOfFilms) {
            int randomIndex = random.nextInt(numberOfFilms);
            // Получение случайного имени файла
            String filename = fileNameList.get(randomIndex);

            // Если регион включён, но ещё не был выбран
            if (!quizFilmsList.contains(filename)) {
                quizFilmsList.add(filename); // Добавить файл в список
                ++filmCounter;
            }
        }
    }

    // Загрузка следующего фильма после правильного ответа
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void loadNextFilm() {
        // Получение имени файла следующего фильма и удаление его из списка
        if (quizFilmsList.size() == 0)
            updateFilmList();

        String nextImage = quizFilmsList.remove(0);
        correctAnswer = nextImage; // Обновление правильного ответа

        // Отображение номера текущего вопроса
        questionNumberTextView.setText(getString(R.string.question,
                (totalGuess + 1)));

        // Извлечение годов из имени следующего изображения
        String year = nextImage.substring(0, nextImage.indexOf('-'));

        //Использование AssetManger для загрузки следующего изображения
        AssetManager assets = getActivity().getAssets();
        String str = "";

        //Получение объекта InputStream для ресурса следующего фильма
        InputStream inputStream = null;
        try {
            InputStream stream =
                    assets.open(year + "/" + nextImage + extension);
            //Загрузка графики в виде объекта Drawable и вывод на filmImageView
            Drawable film = Drawable.createFromStream(stream, nextImage);
            filmImageView.setImageDrawable(film);

            animate(false); // Анимация появления фильма на экране
        } catch (IOException exception) {
            Log.e(TAG, "Error loading " + nextImage, exception);
        }

        Collections.shuffle(fileNameList); // Перестановка имён файлов

        // Помещение правильного ответа в конце fileNameList
        int correct = fileNameList.indexOf(correctAnswer);
        fileNameList.add(fileNameList.remove(correct));

        //Добавление 2, 4, 6 или 8 кнопок в зависимости от значения guessRows
        for (int row = 0; row < guessRows; row++) {
            // Размещение кнопок в currentTableRow
            for (int column = 0;
                 column < guessLinearLayouts[row].getChildCount(); column++) {
                // Получение ссылки на Button
                Button newGuessButton =
                        (Button) guessLinearLayouts[row].getChildAt(column);
                newGuessButton.setEnabled(true);
                newGuessButton.setClickable(true);

                // Назначение названия фильма текстом newGuessButton
                String fileName = fileNameList.get((row * 2) + column);
                newGuessButton.setText(getFilmName(fileName));
            }
        }
        // Случайная замена одной кнопки правильным ответом
        int row = random.nextInt(guessRows); // Выбор случайной строки
        int column = random.nextInt(2); // Выбор случайного столбца
        LinearLayout randomRow = guessLinearLayouts[row]; // Получение строки
        String filmName = getFilmName(correctAnswer);
        ((Button) randomRow.getChildAt(column)).setText(filmName);

    }

    // Метод разбирает имя файла с фильмом и возвращает название страны
    private String getFilmName(String name) {
        String deleteStr;

        //Убирается ангийское назавние фильма, пометки языка и года выпуска
        if (deviceLocale != Locale.ENGLISH) {
            deleteStr = name.substring(name.indexOf("En"), name.lastIndexOf("En") + 2);
            return name.replace(deleteStr, "").substring(name.indexOf('-') + 1).
                    replace('_', ' ').replaceAll("Ru", "");
        } else {
            deleteStr = name.substring(name.indexOf("Ru"), name.lastIndexOf("Ru") + 2);
            return name.replace(deleteStr, "").substring(name.indexOf('-') + 1).
                    replace('_', ' ').replaceAll("Ru", "");
        }

    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void animate(boolean animateOut) {
        // Предотвращает анимации интерфейса для первого фильма
        if (totalGuess == 0)
            return;

        // Вычисление координат центра
        int centerX = (quizLinearLayout.getLeft() +
                quizLinearLayout.getRight()) / 2;
        int centerY = (quizLinearLayout.getTop() +
                quizLinearLayout.getBottom()) / 2;

        // Вычисление радиуса анимации
        int radius = Math.max(quizLinearLayout.getWidth(),
                quizLinearLayout.getHeight());

        Animator animator;

        // Если изображение должно исчезать с экрана
        if (animateOut) {
            // Создание круговой анимации
            animator = ViewAnimationUtils.createCircularReveal(
                    quizLinearLayout, centerX, centerY, radius, 0);
            animator.addListener(
                    new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            loadNextFilm();
                        }
                    }
            );
        } else { // Если макет quizLinearLayout должен появиться
            animator = ViewAnimationUtils.createCircularReveal(
                    quizLinearLayout, centerX, centerY, 0, radius);
        }

        animator.setDuration(500); // Продолжительность анимации 500 мс
        animator.start(); // Начало анимации
    }

    // Вызывается при нажатии кнопки ответа
    private final View.OnClickListener guessButtonListener = new View.OnClickListener() {
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void onClick(View v) {
            final Button guessButton = (Button) v;
            String guess = guessButton.getText().toString();
            String answer = getFilmName(correctAnswer);
            ++totalGuess; // Увеличение количества правильных ответов

            if (guess.equals(answer)) { // Если ответ правилен
                ++correctAnswers; // Увеличить количество правильных ответов

                disableButtons(answer, true); // Блокировка всех кнопок ответов

                // Загрузка следующего фильма после двухсекундной задержки
                handler.postDelayed(
                        new Runnable() {
                            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
                            @Override
                            public void run() {
                                animate(true);// Анимация исчезновения фильма
                            }
                        }, 2100); // 2000 мс для двухсекндной задержки
            } else { // Неправильный ответ
                filmImageView.startAnimation(shakeAnimation); //Встряхивание

                disableButtons(answer, false); // Блокировка всех кнопок ответов

                Animation animation = AnimationUtils.loadAnimation(getContext(), R.anim.incorrect_button);
                guessButton.getBackground().setColorFilter(Color.parseColor(getString(R.string.colorRed)), PorterDuff.Mode.MULTIPLY);
                guessButton.startAnimation(animation);

                handler.postDelayed(
                        new Runnable() {
                            @Override
                            public void run() {
                                loadNextFilm();   // Загрузка нового фильма
                                guessButton.getBackground().clearColorFilter();  // Очистка цвета кнопки для неверного ответа
                            }
                        }, 2100);
            }
        }
    };

    // Вспомогательный метод, блокирующий все кнопки
    private void disableButtons(String answer, boolean cor) {
        for (int row = 0; row < guessRows; row++) {
            LinearLayout guessRow = guessLinearLayouts[row];
            for (int i = 0; i < guessRow.getChildCount(); i++) {
                Button button = (Button) guessRow.getChildAt(i);

                if (button.getText().equals(answer)) {
                    Animation animation = AnimationUtils.loadAnimation(getContext(),
                            R.anim.sample);

                    button.setClickable(false);
                    button.startAnimation(animation);
                } else {
                    button.setEnabled(false);
                }
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case (R.id.action_settings):
                Intent preferencesIntent = new Intent(getActivity(), SettingsActivity.class);
                startActivity(preferencesIntent);
                break;
            case (R.id.action_result):
                // DialogFragment для вывода статистики и перезапуска
                MyDialogFragment quizResults = new MyDialogFragment();
                quizResults.setTargetFragment(MainActivityFragment.this, 10);
                //Использование FragmentManager для вывода DialogFragment
                quizResults.setCancelable(false);

                quizResults.show(getFragmentManager(), "quiz results");
                break;
        }
        return super.onOptionsItemSelected(item);
    }


}
