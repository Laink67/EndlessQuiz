package com.example.endlessquiz;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;


public class MainActivityFragment extends Fragment {

    private static final String TAG = "FlagQuiz Activity";

    private static final int FLAGS_IN_QUIZ = 2;

    private List<String> fileNameList; // Имена файлов с флагами
    private List<String> quizCountriesList; // Страны текущей викторины
    private Set<String> regionSet; // Регионы текущей викторины
    private int correctAnswers;
    private String correctAnswer;
    private int totalGuess; // Количество попыток
    private int guessRows; // Количество строк с кнопками вариантов
    private SecureRandom random; // Генератор случайных чисел
    private Handler handler; // Для задержки заргузки следующего флага
    private Animation shakeAnimation; // Анимация неправильного ответа

    private LinearLayout quizLinearLayout; // Макет викторины
    private TextView questionNumberTextView; // Номер текущего вопроса
    private ImageView flagImageView;
    private LinearLayout[] guessLinearLayouts; // Строки с кнопками
    private TextView answerTextView; // Для правильного ответа


    // Настройка MainActivityFragment при создании представления
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_main_activity, container, false);

        fileNameList = new ArrayList<>();
        quizCountriesList = new ArrayList<>();
        random = new SecureRandom();
        handler = new Handler();

        //Загрузка для анимации для неправильных ответов
        shakeAnimation = AnimationUtils.loadAnimation(getActivity(),
                R.anim.incorrect_shake);
        shakeAnimation.setRepeatCount(3); // Повторение анимации 3 раза

        // Получение ссылок на компоненты графического интерфейса
        quizLinearLayout = view.findViewById(R.id.quizLinearLayout);
        questionNumberTextView = view.findViewById(R.id.questionNumberTextView);
        flagImageView = view.findViewById(R.id.flagImageView);
        guessLinearLayouts = new LinearLayout[]{
                view.findViewById(R.id.row1LinearLayout),
                view.findViewById(R.id.row2LinearLayout),
                view.findViewById(R.id.row3LinearLayout),
                view.findViewById(R.id.row4LinearLayout)
        };

        answerTextView = view.findViewById(R.id.answerTextView);

        // Настройка слушателей для кнопок ответов
        for (LinearLayout row : guessLinearLayouts) {
            for (int column = 0; column < row.getChildCount(); column++) {
                Button button = (Button) row.getChildAt(column);
                button.setOnClickListener(guessButtonListener);
            }
        }

        //Назначени текста questionNumberTextView
        questionNumberTextView.setText(
                getString(R.string.question, 1, FLAGS_IN_QUIZ));

        return view;
    }

    // Обновление guessRows на основании значения SharedPreferences
    public void updateGuessRows(SharedPreferences sharedPreferences) {
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
    public void updateRegions(SharedPreferences sharedPreferences) {
        regionSet = sharedPreferences.getStringSet(MainActivity.REGIONS, null);
    }

    // Настройка и запуск следующей серии вопросов
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void resetQuiz() {
        // Использование AssetManager для получения имён файлов изображений
        AssetManager assets = getActivity().getAssets();
        fileNameList.clear(); // Список имён файлов пуст

        try {
            for (String region : regionSet) {
                // Список картинок всех флагов в регионе
                String[] paths = assets.list(region);

                for (String path : paths)
                    fileNameList.add(path.replace(".png", ""));
            }
        } catch (IOException e) {
            Log.e(TAG, "Error loading image file names", e);
        }

        correctAnswers = 0; // Сброс количества правильных ответов
        totalGuess = 0; // Сброс общего количества попыток
        quizCountriesList.clear(); // Очистка предыдущего списка стран

        int flagCounter = 1;
        int numberOfFlags = fileNameList.size();

        // Добавление FLAGS_IN_QUIZ случайных файлов в quizCountriesList
        while (flagCounter <= FLAGS_IN_QUIZ) {
            int randomIndex = random.nextInt(numberOfFlags);

            // Получение случайного имени файла
            String filename = fileNameList.get(randomIndex);

            // Если регион включён, но ещё не был выбран
            if (!quizCountriesList.contains(filename)) {
                quizCountriesList.add(filename); // Добавить файл в список
                ++flagCounter;
            }
        }

        loadNextFlag(); // Запустить викторину загрузкой первого флага
    }

    // Загрузка следующего флага после правильного ответа
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void loadNextFlag() {
        // Получение имени файла следующего флага и удаление его из списка
        String nextImage = quizCountriesList.remove(0);
        correctAnswer = nextImage; // Обновление правильного ответа
        answerTextView.setText(""); // Очистка answerTextView

        // Отображение номера текущего вопроса
        questionNumberTextView.setText(getString(R.string.question,
                (correctAnswers + 1), FLAGS_IN_QUIZ));

        // Извлечение региона из имени следующего изображения
        String region = nextImage.substring(0, nextImage.indexOf('-'));

        //Использование AssetManger для загрузки следующего изображения
        AssetManager assets = getActivity().getAssets();

        //Получение объекта InputStream для ресурса следующего флага
        InputStream inputStream = null;
        try {
            InputStream stream =
                    assets.open(region + "/" + nextImage + ".png");
            //Загрузка графики в виде объекта Drawable и вывод на flagImageView
            Drawable flag = Drawable.createFromStream(stream, nextImage);
            flagImageView.setImageDrawable(flag);

            animate(false); // Анимация появления флага на экране
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

                // Назначение названия страны текстом newGuessButton
                String fileName = fileNameList.get((row * 2) + column);
                newGuessButton.setText(getCountryName(fileName));
            }
        }
        // Случайная замена одной кнопки правильным ответом
        int row = random.nextInt(guessRows); // Выбор случайной строки
        int column = random.nextInt(2); // Выбор случайного столбца
        LinearLayout randomRow = guessLinearLayouts[row]; // Получение строки
        String countryName = getCountryName(correctAnswer);
        ((Button) randomRow.getChildAt(column)).setText(countryName);

    }

    // Метод разбирает имя файла с флагом и возвращает название страны
    private String getCountryName(String name) {
        return name.substring(name.indexOf('-') + 1).
                replace('_', ' ');
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void animate(boolean animateOut) {
        // Предотвращает анимации интерфейса для первого флага
        if (correctAnswers == 0)
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
                            loadNextFlag();
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
    private View.OnClickListener guessButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Button guessButton = (Button) v;
            String guess = guessButton.getText().toString();
            final String answer = getCountryName(correctAnswer);
            ++totalGuess; // Увеличение количества правильных ответов

            if (guess.equals(answer)) { // Если ответ правилен
                ++correctAnswers; // Увеличить количество правильных ответов

                // Правильный ответ выводится зелёным цветом
                answerTextView.setText(answer + "!");
                answerTextView.setTextColor(getResources().
                        getColor(R.color.correct_answer));

                disableButtons(); // Блокировка всех кнопок ответов

                // Если пользователь правильно угадал FLAGS_IN_QUIZ флагов
                if (correctAnswers == FLAGS_IN_QUIZ) {
                    // DialogFragment для вывода статистики и перезапуска
                    DialogFragment quizResults = new DialogFragment() {
                        // Создание окна AlertDialog
                        @NonNull
                        @Override
                        public Dialog onCreateDialog(@Nullable Bundle bundle) {
                            AlertDialog.Builder builder =
                                    new AlertDialog.Builder(getActivity());
                            builder.setMessage(getString(R.string.results,
                                    totalGuess,
                                    (1000 / (double) totalGuess)));
                            //Кнопка сброса "Reset Quiz"
                            builder.setPositiveButton(R.string.reset_quiz,
                                    new DialogInterface.OnClickListener() {
                                        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
                                        @Override
                                        public void onClick(DialogInterface dialog, int id) {
                                            resetQuiz();
                                        }
                                    });
                            return builder.create(); // Вернуть AlertDialog
                        }
                    };

                    //Использование FragmentManager для вывода DialogFragment
                    quizResults.setCancelable(false);
                    quizResults.show(getFragmentManager(), "quiz results");
                } else { // Ответ правильный, но викторина не закончена
                    // Загрузка следующего флага после двухсекундной задержки
                    handler.postDelayed(
                            new Runnable() {
                                @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
                                @Override
                                public void run() {
                                    animate(true); // Анимация исчезновения флага
                                }
                            }, 2000); // 2000 мс для двухсекндной задержки
                }
            } else { // Неправильный ответ
                flagImageView.startAnimation(shakeAnimation); //Встряхивание

                // Сообщение "Incorrect!" выводится на экран красным шрифтом
                answerTextView.setText(R.string.incorrect_answer);
                answerTextView.setTextColor(getResources().getColor(R.color.incorrect_answer));
                guessButton.setEnabled(false); // Блокировка неправильного ответа
            }
        }
    };

    // Вспомогательный метод, блокирующий все кнопки
    private void disableButtons() {
        for (int row = 0; row < guessRows; row++) {
            LinearLayout guessRow = guessLinearLayouts[row];
            for (int i = 0; i < guessRow.getChildCount(); i++)
                guessRow.getChildAt(i).setEnabled(false);
        }
    }

}
