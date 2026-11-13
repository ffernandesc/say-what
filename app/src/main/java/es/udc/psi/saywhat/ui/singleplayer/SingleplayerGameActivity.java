package es.udc.psi.saywhat.ui.singleplayer;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import es.udc.psi.saywhat.R;
import es.udc.psi.saywhat.databinding.ActivitySingleplayerGameBinding;
import es.udc.psi.saywhat.model.User;
import es.udc.psi.saywhat.ui.settings.SettingsActivity;

public class SingleplayerGameActivity extends AppCompatActivity {

    private ActivitySingleplayerGameBinding binding;

    private ArrayList<Integer> sequence = new ArrayList<>();
    private static final String KEY_HIGH_SCORE = "high_score_singleplayer";
    private Random random = new Random();
    private int currentRound = 0;
    private int currentStep = 0;
    private boolean isPlayerTurn = false;
    private int numberOfButtons = 4;

    private int numberOfRounds;
    private final Handler gameHandler = new Handler(Looper.getMainLooper());
    private Map<Integer, Button> gameButtons; //access buttons by their number

    private SoundPool soundPool;
    private int sound1, sound2, sound3, sound4, soundfail;
    private int sound5, sound6, sound7, sound8; //WIP
    private int score = 0;

    private static final long FLASH_DURATION = 400; // how much the buttons lights up
    private static final long GAP_BETWEEN_FLASHES = 600; // pause between buttons lighting up
    private static final long STATUS_TEXT_DURATION = 1500; // pause between whole
    private final Runnable hideStatusRunnable =
            () -> binding.textGameStatus.setVisibility(View.GONE);


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySingleplayerGameBinding.inflate(getLayoutInflater());
        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        numberOfButtons = getIntent().getIntExtra(getString(R.string.button_count), 4);

        initializeButtonMap();
        setupLayoutBasedOnDifficulty();
        setupSoundPool();
        setupClickListeners();
        gameHandler.postDelayed(this::startGame, 500);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        binding.btnRetry.setOnClickListener(v -> {

            //hide game over screen
            binding.layoutGameOver.setVisibility(View.GONE);

            //restart game
            startGame();
        });

        binding.btnLeaderboard.setOnClickListener(v -> {
            Intent intent = new Intent(SingleplayerGameActivity.this, LeaderboardActivity.class);
            intent.putExtra("DIFFICULTY_KEY", String.valueOf(numberOfButtons));
            startActivity(intent);

        });

        binding.btnExit.setOnClickListener(v -> {
            finish(); //exit to main menu
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void initializeButtonMap() {

        gameButtons = new HashMap<>();
        gameButtons.put(1, binding.button1);
        gameButtons.put(2, binding.button2);
        gameButtons.put(3, binding.button3);
        gameButtons.put(4, binding.button4);
        gameButtons.put(5, binding.button5);
        gameButtons.put(6, binding.button6);
        gameButtons.put(7, binding.button7);
        gameButtons.put(8, binding.button8);
    }

    private void setupLayoutBasedOnDifficulty() {
        binding.row3.setVisibility(View.GONE);
        binding.row4.setVisibility(View.GONE);
        binding.button5.setVisibility(View.GONE);
        binding.button6.setVisibility(View.GONE);
        binding.button7.setVisibility(View.GONE);
        binding.button8.setVisibility(View.GONE);

        if (numberOfButtons >= 5) {
            binding.row3.setVisibility(View.VISIBLE);
            binding.button5.setVisibility(View.VISIBLE);
        }

        if (numberOfButtons >= 6) {
            binding.button6.setVisibility(View.VISIBLE);
        }

        if (numberOfButtons >= 7) {
            binding.row4.setVisibility(View.VISIBLE);
            binding.button7.setVisibility(View.VISIBLE);
        }
        if (numberOfButtons >= 8) {
            binding.button8.setVisibility(View.VISIBLE);
        }
    }

    private void setupSoundPool() {
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();

        soundPool = new SoundPool.Builder()
                .setMaxStreams(9) // max 9 sounds at once
                .setAudioAttributes(audioAttributes)
                .build();

        sound1 = soundPool.load(this, R.raw.b1, 1); //sounds for the buttons
        sound2 = soundPool.load(this, R.raw.b2, 1);
        sound3 = soundPool.load(this, R.raw.b3, 1);
        sound4 = soundPool.load(this, R.raw.b4, 1);
        sound5 = soundPool.load(this, R.raw.b5, 1);
        sound6 = soundPool.load(this, R.raw.b6, 1);
        sound7 = soundPool.load(this, R.raw.b7, 1);
        sound8 = soundPool.load(this, R.raw.b8, 1);
        soundfail = soundPool.load(this, R.raw.fail, 1); //fail sound
    }

    private void setupClickListeners() {
        View.OnClickListener buttonClickListener = v -> {

            //if not the players turn, do nothing
            if (!isPlayerTurn) return;


            int buttonPressed = 0;

            if (v == binding.button1) buttonPressed = 1;
            else if (v == binding.button2) buttonPressed = 2;
            else if (v == binding.button3) buttonPressed = 3;
            else if (v == binding.button4) buttonPressed = 4;
            else if (v == binding.button5) buttonPressed = 5;
            else if (v == binding.button6) buttonPressed = 6;
            else if (v == binding.button7) buttonPressed = 7;
            else if (v == binding.button8) buttonPressed = 8;

            if (buttonPressed > 0) {
                flashButtonAndPlaySound(buttonPressed);
                handlePlayerInput(buttonPressed);
            }
        };

        for (int i = 1; i <= numberOfButtons; i++) {
            Button btn = gameButtons.get(i);
            if(btn != null) btn.setOnClickListener(buttonClickListener);
        }

    }

    private void startGame() {

        sequence.clear();
        currentRound = 0;
        score = 0; //reset score
        binding.tvScore.setText(getString(R.string.score_activity) + score);
        binding.layoutGameOver.setVisibility(View.GONE);
        showTemporaryGameStatus(R.string.get_ready);
        gameHandler.postDelayed(this::nextRound, 1500); // a lil bit of delay
    }

    private void nextRound() {

        isPlayerTurn = false;
        setButtonsEnabled(false); //disable buttons while computer playing the sequence
        currentStep = 0;
        currentRound++;


        sequence.add(random.nextInt(numberOfButtons) + 1); //add a number up to numberOfButtons

        showPermanentGameStatus(getString(R.string.round1) + currentRound);

        // start playing the sequence, beginning from the first item (index 0)
        gameHandler.postDelayed(() -> playSequenceStep(0), 1000);
    }

    //plays a single step in the sequence. method calls itself with postdelay to create a loop
    private void playSequenceStep(int index) {

        if(index >= sequence.size()) {
            //finished the sequence
            startPlayerTurn();
            return;
        }

        int buttonId = sequence.get(index);
        flashButtonAndPlaySound(buttonId);

        //next step in the sequence
        gameHandler.postDelayed(() -> playSequenceStep(index + 1), GAP_BETWEEN_FLASHES);

    }


    private void startPlayerTurn() {
        isPlayerTurn = true;
        setButtonsEnabled(true); // now we enable the buttons, cuz its the players turn
        showPermanentGameStatus(R.string.your_turn);
    }

    //checks players button press against the sequence
    private void handlePlayerInput(int buttonPressed) {

        if (buttonPressed == sequence.get(currentStep)) {
            //correct button
            currentStep++;

            if (currentStep == sequence.size()) {
                //completed the whole sequence

                score += 1;
                binding.tvScore.setText(getString(R.string.score_activity) + score);
                showTemporaryGameStatus(R.string.correct_sequence);
                //start next round
                gameHandler.postDelayed(this::nextRound, 1000);
            }
            // if not at the end, we wait for the next button press
        } else {
            //wrong button
            if (soundPool != null) {
                soundPool.play(soundfail, 1, 1, 0, 0, 1);
            }

            setButtonsEnabled(false);

            gameHandler.postDelayed(() -> {
                showGameOver();
            }, 1000);

        }
    }

    private void showGameOver() {
        //disable the buttons so they cant be pressed
        setButtonsEnabled(false);

        SharedPreferences prefs = getSharedPreferences(SettingsActivity.PREFS_NAME, Context.MODE_PRIVATE);

        int currentHighScore = prefs.getInt(KEY_HIGH_SCORE, 0);

        if (score > currentHighScore) {
            currentHighScore = score;
            prefs.edit().putInt(KEY_HIGH_SCORE, currentHighScore).apply();
        }

        //save highscore to firestore if the user is logged in
        String localUserId = prefs.getString("local_user_id", null);

        if (localUserId != null) {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            DocumentReference userRef = db.collection("users").document(localUserId);

            userRef.get().addOnSuccessListener(documentSnapshot -> {
                if(documentSnapshot.exists()) {
                    User user = documentSnapshot.toObject(User.class);

                    if(user != null) {
                        Map<String, Integer> scoresMap = user.getDifficultyScores();
                        if (scoresMap == null) {
                            scoresMap = new HashMap<>();
                        }

                        String difficultyKey = String.valueOf(numberOfButtons);

                        int currentDiffHighScore = 0;
                        if (scoresMap.containsKey(difficultyKey) && scoresMap.get(difficultyKey) != null) {
                            currentDiffHighScore = scoresMap.get(difficultyKey);
                        }

                        if (score > currentDiffHighScore) {
                            scoresMap.put(difficultyKey, score);// update the map locally
                            userRef.update("difficultyScores", scoresMap);// update field in Firestore
                        }

                        // legacy 'highScore' field
                        if (score > user.getHighScore()) {
                            userRef.update("highScore", score);
                        }
                    }
                }
            });
        }
        binding.tvFinalScore.setText(getString(R.string.score_activity) + score);
        binding.tvHighScore.setText(getString(R.string.personal_best_activity) + " " + currentHighScore);
        binding.layoutGameOver.setVisibility(View.VISIBLE);

        binding.layoutGameOver.setAlpha(0f);
        binding.layoutGameOver.setScaleX(0.5f);
        binding.layoutGameOver.setScaleY(0.5f);

        binding.layoutGameOver.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(400)
                .setInterpolator(new android.view.animation.OvershootInterpolator())
                .start();
    }

    //method to light up the button and play its corresponding sound
    private void flashButtonAndPlaySound(int buttonId) {

        Button button = gameButtons.get(buttonId);
        if (button == null) return;

        int soundId = 0;

        switch (buttonId) {
            case 1: soundId = sound1; break;
            case 2: soundId = sound2; break;
            case 3: soundId = sound3; break;
            case 4: soundId = sound4; break;
            case 5: soundId = sound5; break;
            case 6: soundId = sound6; break;
            case 7: soundId = sound7; break;
            case 8: soundId = sound8; break;
        }

        //play sound
        soundPool.play(soundId, 1, 1, 0, 0, 1);

        //button light up
        button.setPressed(true);
        gameHandler.postDelayed(() -> button.setPressed(false), FLASH_DURATION);
    }

    private void setButtonsEnabled(boolean enabled) {
        if (gameButtons == null) return;
        for (int i = 1; i <= numberOfButtons; i++) {
            Button btn = gameButtons.get(i);
            if (btn != null) btn.setEnabled(enabled);
        }
    }

    private void showPermanentGameStatus(String text) {
        gameHandler.removeCallbacks(hideStatusRunnable);
        binding.textGameStatus.setText(text);
        binding.textGameStatus.setVisibility(View.VISIBLE);
    }

    private void showPermanentGameStatus(int stringId) {
        showPermanentGameStatus(getString(stringId));
    }

    private void showTemporaryGameStatus(String text) {
        gameHandler.removeCallbacks(hideStatusRunnable);
        binding.textGameStatus.setText(text);
        binding.textGameStatus.setVisibility(View.VISIBLE);
        gameHandler.postDelayed(hideStatusRunnable, STATUS_TEXT_DURATION);
    }

    private void showTemporaryGameStatus(int stringId) {
        showTemporaryGameStatus(getString(stringId));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }

        gameHandler.removeCallbacksAndMessages(null);
        gameHandler.removeCallbacksAndMessages(null);
    }

}