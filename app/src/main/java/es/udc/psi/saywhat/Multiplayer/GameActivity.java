package es.udc.psi.saywhat.Multiplayer;

import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import es.udc.psi.saywhat.FourthActivity;
import es.udc.psi.saywhat.R;
import es.udc.psi.saywhat.databinding.ActivityGameBinding;
import es.udc.psi.saywhat.game.Game;

public class GameActivity extends AppCompatActivity {

    private ActivityGameBinding binding;
    private Button[] gameButtons = new Button[8];

    // online data
    private FirebaseFirestore db;
    private ListenerRegistration gameListener;
    private String gameId;
    private String myUid;
    private String status;
    private String lastTurnUid = null;

    // local GameState (from db)
    private Map<String, Game.PlayerInfo> playersMap;
    private List<String> activePlayers;
    private String currentTurnUid;

    private List<Integer> sequence = new ArrayList<>();
    private int currentInputIndex = 0;
    private int buttonsUnlocked = 4;
    private boolean isUserTurn = false;
    private boolean isMovingOutOfGame = false;
    private boolean leaveHandled = false;
    private boolean hasNavigated = false;

    private Handler handler = new Handler();
    private Random random = new Random();

    private List<String> previousActivePlayers = null;
    private boolean isShowingEliminationMessage = false;

    private SoundPool soundPool;
    private int sound1, sound2, sound3, sound4, sound5, sound6, sound7, sound8, soundFail;
    private float soundVolume = 1.0f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityGameBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();

        gameId = getIntent().getStringExtra("game_id");

        android.content.SharedPreferences prefs = getSharedPreferences(FourthActivity.PREFS_NAME, android.content.Context.MODE_PRIVATE);
        myUid = prefs.getString("local_user_id", null);

        int volProgress = prefs.getInt(FourthActivity.KEY_SOUND_VOLUME, 100);
        soundVolume = volProgress / 100f;

        if (gameId == null) {
            Toast.makeText(this, "Error: missing game_id", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        if (myUid == null) {
            Toast.makeText(this, "Error: missing user_id", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        binding.tvPlayerTurn.setVisibility(View.GONE);
        binding.tvPlayerCount.setText("");

        setupSoundPool();

        initViews();

        listenForGameUpdates();

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        getOnBackPressedDispatcher().addCallback(this,
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        showLeaveConfirmationDialog();
                    }
                });
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

        sound1 = soundPool.load(this, R.raw.b1, 1);
        sound2 = soundPool.load(this, R.raw.b2, 1);
        sound3 = soundPool.load(this, R.raw.b3, 1);
        sound4 = soundPool.load(this, R.raw.b4, 1);
        sound5 = soundPool.load(this, R.raw.b5, 1);
        sound6 = soundPool.load(this, R.raw.b6, 1);
        sound7 = soundPool.load(this, R.raw.b7, 1);
        sound8 = soundPool.load(this, R.raw.b8, 1);
        soundFail = soundPool.load(this, R.raw.fail, 1);
    }

    public void showLeaveConfirmationDialog(){
        new AlertDialog.Builder(this).setTitle(R.string.leaving_game_title).
                setMessage(R.string.leaving_game_message).
                setPositiveButton(R.string.yes_text, (dialog, which) -> {
                    dialog.dismiss();
                    leaveGameHandle();
                }).setNegativeButton(R.string.no_text, (dialog, which) -> {
                    dialog.dismiss();
                }).setCancelable(true).show();
    }


    private void updateTurnFlow(Game game) {
        if (currentTurnUid == null) return;

        if (!currentTurnUid.equals(lastTurnUid)) {
            lastTurnUid = currentTurnUid;
            startTurnVideo();
            return;
        }

        updateTurnUI();
    }

    @Override
    public boolean onSupportNavigateUp() {
        showLeaveConfirmationDialog();
        return true;
    }

    private void leaveGameHandle(){
        if (leaveHandled) {
            finish();
            return;
        }

        leaveHandled = true;
        isMovingOutOfGame = true;

        db.collection("games").document(gameId)
                .get()
                .addOnSuccessListener(snapshot -> {

                    if(!snapshot.exists()) {
                        finish();
                        return;
                    }

                    String hostId = snapshot.getString("hostId");

                    if (myUid.equals(hostId)) {
                        db.collection("games").document(gameId)
                                .delete()
                                .addOnCompleteListener(task -> finish());
                    } else {
                        eliminateMyself(true);
                        finish();
                    }
                });
    }

    private void initViews() {
        gameButtons[0] = binding.button1;
        gameButtons[1] = binding.button2;
        gameButtons[2] = binding.button3;
        gameButtons[3] = binding.button4;
        gameButtons[4] = binding.button5;
        gameButtons[5] = binding.button6;
        gameButtons[6] = binding.button7;
        gameButtons[7] = binding.button8;

        for (int i = 0; i < gameButtons.length; i++) {
            final int buttonIndex = i;
            gameButtons[i].setOnClickListener(v -> handlePlayerInput(buttonIndex));
        }
    }

    private void listenForGameUpdates() {
        gameListener = db.collection("games").document(gameId)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) {
                        Toast.makeText(this, R.string.error_loading_game, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (snapshot != null && snapshot.exists()) {
                        Game game = snapshot.toObject(Game.class);
                        if (game != null) {

                            if("HOST_LEFT".equals(game.getStatus())) {
                                if(!leaveHandled) {
                                    showHostLeftDialog();
                                }
                                return;
                            }

                            syncGameState(game);
                        }
                    } else {
                        if (!leaveHandled && !isFinishing()) {
                            showHostLeftDialog();
                        }
                    }
                });
    }

    private void showHostLeftDialog() {
        if (isFinishing() || isDestroyed()) return;

        if (gameListener != null) {
            gameListener.remove();
            gameListener = null;
        }

        String myName = "Player";
        if(playersMap != null && playersMap.containsKey(myUid)) {
            Game.PlayerInfo info = playersMap.get(myUid);
            if (info != null && info.getName() != null) {
                myName = info.getName();
            }
        }
        final String playerNameFinal = myName;

        new AlertDialog.Builder(this)
                .setTitle(R.string.game_over_)
                .setMessage(R.string.host_left)
                .setPositiveButton("OK", (dialog, which) -> {
                    // navigate to ThirdActivity (Join/Create screen)
                    Intent intent = new Intent(GameActivity.this, ThirdActivity.class);
                    intent.putExtra("player_name", playerNameFinal);
                    // clear flags to avoid back stack issues
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                })
                .setCancelable(false)
                .show();
    }
    private void syncGameState(Game game) {
        this.sequence = new ArrayList<>();

        if (game.getSequence() != null) {
            for (Object num : game.getSequence()) {
                if (num instanceof Number) {
                    this.sequence.add(((Number) num).intValue());
                }
            }
        } // converting from LONG (Firestore default) to INTEGER

        this.buttonsUnlocked = game.getButtonsUnlocked();
        this.currentTurnUid = game.getCurrentTurnUid();
        this.status = game.getStatus();

        this.playersMap = game.getPlayers();
        this.activePlayers = game.getTurnOrder();

        if (activePlayers == null) {
            return;
        }


        //Notify what player was just eliminated
        if (previousActivePlayers != null && activePlayers.size() < previousActivePlayers.size()) {

            String eliminatedName = "Player";

            for (String oldId : previousActivePlayers) {
                if (!activePlayers.contains(oldId)) {
                    eliminatedName = resolveName(oldId);
                    break;
                }
            }

            binding.tvPlayerCount.setText(eliminatedName + getString(R.string.eliminated_player));
            isShowingEliminationMessage = true;

            // Revert back after 2 seconds
            new Handler().postDelayed(() -> {
                if (!isFinishing() && !isDestroyed()) {
                    binding.tvPlayerCount.setText(getString(R.string.players_remaining) + activePlayers.size());
                    isShowingEliminationMessage = false;
                }
            }, 2000);

        }
        else {
            if (!isShowingEliminationMessage) {
                binding.tvPlayerCount.setText(getString(R.string.players_remaining) + activePlayers.size());
            }
        }
        previousActivePlayers = new ArrayList<>(activePlayers);


        //show the player that (s)he is eliminated
        if (playersMap != null && playersMap.containsKey(myUid)) {
            Game.PlayerInfo myInfo = playersMap.get(myUid);
            if (myInfo != null && "ELIMINATED".equals(myInfo.getState())) {
                binding.tvEliminatedMsg.setVisibility(View.VISIBLE);
            }
        }


        if ("ENDED".equals(status)
                || activePlayers == null
                || activePlayers.isEmpty()
                || currentTurnUid == null) {
            goToLeaderboard();
            return;
        }

        updateButtonVisibility();
        updateTurnFlow(game);
    }

    private void updateButtonVisibility() {
        for (int i = 0; i < 8; i++) {
            if (i < buttonsUnlocked) {
                gameButtons[i].setVisibility(View.VISIBLE);
                gameButtons[i].setAlpha(1.0f);
                gameButtons[i].setEnabled(true);
            } else {
                gameButtons[i].setVisibility(View.GONE);
            }
        }
    }

    private void goToLeaderboard() {
        if (hasNavigated) return;
        hasNavigated = true;

        Intent intent = new Intent(this, GameOverActivity.class);
        intent.putExtra("game_id", gameId);
        startActivity(intent);
        finish();
    }

    private void startTurnVideo() {
        isUserTurn = false;
        currentInputIndex = 0;

        if (currentTurnUid != null) {
            binding.tvPlayerTurn.setText(
                    getString(R.string.game_player_watch, resolveName(currentTurnUid))
            );
            binding.tvPlayerTurn.setVisibility(View.VISIBLE);
        }

        handler.postDelayed(() -> {
            binding.tvPlayerTurn.setVisibility(View.GONE);
            playSequence();
        }, 1000);
    }

    private void playSequence() {
        long delay = 0;
        long flash = 850;

        for (int colorIndex : sequence) {
            handler.postDelayed(() -> flashButton(colorIndex, flash), delay);
            delay += 1100;
        }

        handler.postDelayed(this::updateTurnUI, delay);
    }

    private void updateTurnUI() {
        if (currentTurnUid == null) return;

        String name = resolveName(currentTurnUid);
        binding.tvPlayerTurn.setText(
                getString(R.string.game_player_playing, name)
        );
        binding.tvPlayerTurn.setVisibility(View.VISIBLE);

        handler.postDelayed(() -> {
            binding.tvPlayerTurn.setVisibility(View.GONE);
        }, 1000);

        isUserTurn = myUid.equals(currentTurnUid);
        currentInputIndex = 0;
    }

    private String resolveName(String uId){
        Game.PlayerInfo info = playersMap != null ? playersMap.get(uId) : null;
        return info != null && info.getName() != null ? info.getName() : "Player";
    }

    private void flashButton(int index, long flashMs) {
        Button btn = gameButtons[index];
        btn.setAlpha(0.5f);

        int soundId = 0;
        switch (index) {
            case 0: soundId = sound1; break;
            case 1: soundId = sound2; break;
            case 2: soundId = sound3; break;
            case 3: soundId = sound4; break;
            case 4: soundId = sound5; break;
            case 5: soundId = sound6; break;
            case 6: soundId = sound7; break;
            case 7: soundId = sound8; break;
        }

        if (soundId != 0 && soundPool != null) {
            soundPool.play(soundId, soundVolume, soundVolume, 0, 0, 1);
        }

        handler.postDelayed(() -> btn.setAlpha(1.0f), flashMs);
    }

    private void handlePlayerInput(int index) {

        if (!isUserTurn) {
            Toast.makeText(this, R.string.not_your_turn, Toast.LENGTH_SHORT).show();
            return;
        }

        flashButton(index, 700);
        if (sequence == null || sequence.isEmpty()) return;

        if (index == sequence.get(currentInputIndex)) {
            currentInputIndex++;
            if (currentInputIndex == sequence.size()) {
                finishTurn(true);
            }
        } else {
            finishTurn(false);
        }
    }

    private void finishTurn(boolean success) {
        isUserTurn = false;

        if (!success) {
            if (soundFail != 0 && soundPool != null) {
                soundPool.play(soundFail, soundVolume, soundVolume, 0, 0, 1);
            }

            eliminateMyself();
            return;
        }

        int currentIndex = activePlayers.indexOf(currentTurnUid);
        int nextIndex = (currentIndex + 1) % activePlayers.size();
        boolean newRound = nextIndex == 0;

        String nextUid = activePlayers.get(nextIndex);

        updateFirestoreTurn(nextUid, newRound);
    }

    private void updateFirestoreTurn(String nextUid, boolean newRound) {
        List<Integer> newSequence = new ArrayList<>(sequence);

        if (buttonsUnlocked > 0) {
            newSequence.add(random.nextInt(buttonsUnlocked));
        }

        int newUnlocked = buttonsUnlocked;
        if (newRound && buttonsUnlocked < 8) {
            newUnlocked++;
        }

        db.collection("games").document(gameId)
                .update(
                        "currentTurnUid", newRound ? activePlayers.get(0) : nextUid,
                        "sequence", newSequence,
                        "buttonsUnlocked", newUnlocked
                );
    }

    private void eliminateMyself() {
        eliminateMyself(false);
    }

    private void eliminateMyself(boolean force) {

        if (!force && leaveHandled) return;

        leaveHandled = true;
        if (activePlayers == null) return;

        int myIndex = activePlayers.indexOf(myUid);
        activePlayers.remove(myUid);

        Map<String, Object> updates = new java.util.HashMap<>();
        updates.put("players." + myUid + ".state", "ELIMINATED");
        updates.put("players." + myUid + ".eliminationTime", System.currentTimeMillis());
        updates.put("turnOrder", activePlayers);

        if (activePlayers.size() <= 1) {
            updates.put("status", "ENDED");
        } else {
            String nextUid;
            boolean newRound = false;

            if (myIndex >= activePlayers.size()) {
                // if the last player is eliminated, wrap around to the first player
                nextUid = activePlayers.get(0);
                newRound = true;
            } else {
                nextUid = activePlayers.get(myIndex);
                newRound = false;
            }

            updates.put("currentTurnUid", nextUid);

            // if the last player was eliminated => a new round starts
            if (newRound) {
                List<Integer> newSequence = new ArrayList<>(sequence);

                if (buttonsUnlocked > 0) {
                    newSequence.add(random.nextInt(buttonsUnlocked));
                }
                updates.put("sequence", newSequence);

                if (buttonsUnlocked < 8) {
                    updates.put("buttonsUnlocked", buttonsUnlocked + 1);
                }
            }
        }

        db.collection("games").document(gameId).update(updates);
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (gameId != null && status != null && !status.equals("ENDED") && !status.equals("HOST_LEFT")) {
            getSharedPreferences(FourthActivity.PREFS_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .putString("pending_game_id", gameId)
                    .apply();
        } else {
            getSharedPreferences(FourthActivity.PREFS_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .remove("pending_game_id")
                    .apply();
        }
    }

    @Override
    protected void onDestroy() {

        super.onDestroy();

        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }

        if (gameListener != null) {
            gameListener.remove();
            gameListener = null;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (gameListener != null) {
            gameListener.remove();
            gameListener = null;
        }
    }
}