package com.dreams.androidquizapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import com.dreams.androidquizapp.fragments.QuestionFragment;
import com.dreams.androidquizapp.repository.Tc2rGithubRepository;
import com.dreams.androidquizapp.repository.models.Answer;
import com.dreams.androidquizapp.repository.models.Question;
import com.dreams.androidquizapp.repository.reponseModels.AnswersResponse;
import com.dreams.androidquizapp.repository.reponseModels.QuestionsResponse;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Random;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity implements OnFragmentInteractionListener {
    // Static Variables
    private final static int QUIZ_SIZE = 10;
    private Tc2rGithubRepository tc2rGithubRepository;

    private TextView titleTv, scoreTv;

    private ArrayList<Question> quizList;
    private ArrayList<Question> testList;
    private ArrayList<Answer> answersList;

    // Variables
    private Random random;
    private int currentQuestion = 0;
    private int numOfCorrect = 0;
    private int scorePer = 0;
    private double pointPerQ, score;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        tc2rGithubRepository = new Tc2rGithubRepository();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initalize and assignments
        titleTv = (TextView) findViewById(R.id.title_tv);
        scoreTv = (TextView) findViewById(R.id.score_tv);
        testList = new ArrayList<>();
        quizList = new ArrayList<>();
        answersList = new ArrayList<>();
        random = new Random();

        // get Wrong answers from server
        getAnswers();
    }

    public void getAnswers() {
        tc2rGithubRepository.getAnswersList.enqueue(new Callback<AnswersResponse>() {
            @Override
            public void onResponse(@NonNull Call<AnswersResponse> call,
                                   @NonNull Response<AnswersResponse> response) {
                int statusCode = response.code();
                AnswersResponse answers = response.body();
                if((answers != null ? answers.getAnswersList() : null) != null) {
                    answersList.addAll(answers.getAnswersList());
                    getQuestions();
                }
            }

            @Override
            public void onFailure(@NonNull Call<AnswersResponse> call, @NonNull Throwable t) {
                Log.e("Answers Error", t.toString());
            }
        });
    }

    public void getQuestions() {
        tc2rGithubRepository.getQuestionsList.enqueue(new Callback<QuestionsResponse>() {
            @Override
            public void onResponse(@NonNull Call<QuestionsResponse> call,
                                   @NonNull Response<QuestionsResponse> response) {
                QuestionsResponse questionsResponse = response.body();
                if((questionsResponse != null ? questionsResponse.getQuestionsList() : null) != null) {
                    quizList.addAll(questionsResponse.getQuestionsList());
                    createQuiz();
                }
            }

            @Override
            public void onFailure(@NonNull Call<QuestionsResponse> call, @NonNull Throwable t) {
                Log.e("Questions API Failure", t.toString());
            }
        });
    }

    private void createQuiz() {
        Log.wtf(" Size: ", "QuestionList is: " + quizList.size());
        Log.wtf(" Size: ", "AnswerList is: " + answersList.size());

        // set booleanArray to be same size as quizList
        boolean[] selectedQuestion = new boolean[quizList.size()];

        // set an int to a random number in the quizList
        int randNum = random.nextInt(quizList.size());
        int i = 0;

        // sets the score system for quiz.
        // TODO: 5/31/2017 maybe remove this and simply divide correct answers by total questions
        pointPerQ = 1.0 / QUIZ_SIZE;

        // prevent from having repeated questions in a single quiz
        while(i < QUIZ_SIZE) {
            // if boolean at randNum in selectedQuestion is false
            if(!selectedQuestion[randNum] && quizList
                                                     .get(randNum)
                                                     .getQuestionType()
                                                     .equals("multi")) {
                // Add position randNum to test list;
                testList.add(quizList.get(randNum));
                // set this question selected to true.
                selectedQuestion[randNum] = true;
                i++;
            } else {
                // if question already selected, change randNum;
                randNum = random.nextInt(quizList.size());
            }
        }

        // On first run, start quiz without updating score
        nextQuestion(false);
    }

    public void nextQuestion(boolean correctAnswer) {
        // if previous question was answered correctly
        // update variables accordingly.
        if(correctAnswer) {
            numOfCorrect++;
            score += pointPerQ;
            scorePer = (int) (score * 100);
            scoreTv.setText(String.format(Locale.US, "%s%d", getString(R.string.score_display_text),
                                          scorePer));
        }

        // if quiz is not complete, continue quiz with new QuestionFragment
        if(currentQuestion < QUIZ_SIZE) {
            // Fragments and Model Variables
            QuestionFragment newFragment = QuestionFragment.newInstance(
                    testList.get(currentQuestion), answersList);
            currentQuestion++;
            titleTv.setText(String.format(Locale.US, "%s%d of %d",
                                          getString(R.string.question_display_text),
                                          currentQuestion, QUIZ_SIZE));
            // UI Variables
            LinearLayout fragContainer = (LinearLayout) findViewById(R.id.fragment_container);
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.replace(fragContainer.getId(), newFragment);
            ft.commit();
        } else {
            // Quiz is over, go to final page!
            // create intent
            Intent intent = new Intent(this, ScoreActivity.class);

            // add variables to send.
            intent.putExtra("scorePercentage", scorePer);
            intent.putExtra("quizSize", QUIZ_SIZE);
            intent.putExtra("numCorrect", numOfCorrect);

            // use intent.
            startActivity(intent);
        }
    }

    @Override
    public void fragmentInitialized() {

    }
}
