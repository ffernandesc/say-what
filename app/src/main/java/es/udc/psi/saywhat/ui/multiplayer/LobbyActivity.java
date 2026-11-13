package es.udc.psi.saywhat.ui.multiplayer;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.FieldValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import es.udc.psi.saywhat.R;
import es.udc.psi.saywhat.databinding.ActivityLobbyBinding;
import es.udc.psi.saywhat.model.Game;
import es.udc.psi.saywhat.ui.settings.SettingsActivity;

public class LobbyActivity extends AppCompatActivity {

    private ActivityLobbyBinding binding;
    private FirebaseFirestore db;
    private ListenerRegistration gameListener;
    private String gameId;
    private String myUid;

    private static final String TAG = "LobbyActivity";

    private String DatabaseUid;
    private Game currentGame;
    private boolean isHost = false;
    private boolean isFirstLoad = true;
    private boolean isMovingOutOfLobby = false;
    private boolean isGameStarting = false;
    private boolean isMovingToGame = false;
    private ListenerRegistration lobbyListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityLobbyBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();

        gameId = getIntent().getStringExtra("game_id");
        isHost = getIntent().getBooleanExtra("is_host", false);

        SharedPreferences prefs = getSharedPreferences(SettingsActivity.PREFS_NAME, Context.MODE_PRIVATE);
        myUid = prefs.getString("local_user_id", "");

        binding.tvGameCode.setText(gameId);

        if (isHost) {
            binding.btnStart.setVisibility(View.VISIBLE);
            binding.btnStart.setOnClickListener(v -> startGame());

            binding.btnStart.setEnabled(false);
            binding.btnStart.setAlpha(0.5f);
        }

        setupStartButton();
        reactivateIfRejoining();
        listenForUpdates();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        getOnBackPressedDispatcher().addCallback(this,
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        showLeaveConfirmationDialog();
                    }
                });
    }

    public void showLeaveConfirmationDialog(){
        new AlertDialog.Builder(this).setTitle(R.string.leaving_lobby_title).
                setMessage(R.string.leaving_lobby_message).
                setPositiveButton(R.string.yes_text, (dialog, which) -> {
                    dialog.dismiss();
                    leaveLobbyHandle();
                }).setNegativeButton(R.string.no_text, (dialog, which) -> {
                    dialog.dismiss();
                }).setCancelable(true).show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        showLeaveConfirmationDialog();
        return true;
    }

    private void leaveLobbyHandle(){
        if(isMovingOutOfLobby || isMovingToGame || isGameStarting){
            finish();
            return;
        }

        isMovingOutOfLobby = true;

        if(gameId == null || myUid == null){
            finish();
            return;
        }

        db.runTransaction(transaction -> {
            DocumentReference reference = db.collection("games")
                    .document(gameId);
            DocumentSnapshot snapshot = transaction.get(reference);

            if(!snapshot.exists()) return null;

            String hostId = snapshot.getString("hostId");
            boolean isThisHost = (hostId != null && hostId.equals(myUid));

            if(isThisHost){
                transaction.delete(reference);
                return null;
            }

            @SuppressWarnings("unchecked")
            java.util.List<String> turnOrder = (java.util.List<String>) snapshot.get("turnOrder");
            String currentTurnUid = snapshot.getString("currentTurnUid");

            if(turnOrder == null) turnOrder = new ArrayList<>();
            turnOrder.remove(myUid);

            Map<String, Object> updates = new HashMap<>();
            updates.put("players." + myUid + ".state", "ELIMINATED");
            updates.put("turnOrder", turnOrder);

            if(myUid.equals(currentTurnUid)){
                String next = turnOrder.isEmpty() ? null : turnOrder.get(0);
                updates.put("currentTurnUid", next);
            }

            String status = snapshot.getString("status");
            if("PLAYING".equals(status) && turnOrder.size() <= 1){
                updates.put("status", "ENDED");
                updates.put("endedAt", com.google.firebase.firestore.FieldValue.serverTimestamp()
                );
            }

            transaction.update(reference, updates);
            return null;
        }).addOnCompleteListener(task -> finish());
    }

    private void setupStartButton() {
        if (isHost) {
            binding.btnStart.setText(R.string.start);
            binding.btnStart.setOnClickListener(v -> startGame());

            binding.btnStart.setEnabled(false);
            binding.btnStart.setAlpha(0.5f);
        } else {
            binding.btnStart.setText(R.string.waiting_for_host);
            binding.btnStart.setEnabled(false);
            binding.btnStart.setAlpha(1.0f);
            binding.btnStart.setBackgroundTintList(ColorStateList.valueOf(Color.GRAY));
        }
    }

    private void listenForUpdates() {
        lobbyListener = db.collection("games").document(gameId)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) {
                        Toast.makeText(this, R.string.error_loading_game, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (snapshot != null && snapshot.exists()) {
                        Game game = snapshot.toObject(Game.class);
                        if (game != null) {
                            updateUI(game);

                            if (!isHost && "PLAYING".equals(game.getStatus())) {
                                navigateToGame();
                            }
                        }
                    } else {
                        if(!isHost && !isMovingOutOfLobby && !isMovingToGame) {
                            Toast.makeText(LobbyActivity.this, R.string.lobby_closed_by_host, Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    }
                });
    }

    private void updateUI(Game game) {
        this.currentGame = game;

        Map<String, Game.PlayerInfo> players = game.getPlayers();
        List<String> turnOrder = game.getTurnOrder();

        StringBuilder sb = new StringBuilder();

        if(turnOrder != null && players != null){
            for (String uid : turnOrder) {
                Game.PlayerInfo info = players.get(uid);
                if(info != null && info.getName() != null){
                    sb.append(info.getName()).append("\n");
                } else {
                    sb.append("Player");
                }
            }
        }

        binding.tvPlayerList.setText(sb.toString());

        int playerCount = (turnOrder != null) ? turnOrder.size() : 0;

        if (isHost) {
            boolean canStart = playerCount >=2;
            binding.btnStart.setEnabled(canStart);
            binding.btnStart.setAlpha(canStart ? 1.0f : 0.5f);

        }
    }

    private void startGame() {

        if(currentGame == null || currentGame.getTurnOrder() == null || currentGame.getTurnOrder().size() < 2){
            Toast.makeText(this, R.string.wait_more_players, Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isHost) {
            return;
        }

        isGameStarting = true;
        binding.btnStart.setEnabled(false);

        String firstUid = currentGame.getTurnOrder().get(0);

        ArrayList<Integer> sequence = new ArrayList<>();
        sequence.add(new Random().nextInt(4));

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "PLAYING");
        updates.put("currentTurnUid", firstUid);
        updates.put("sequence", sequence);
        updates.put("buttonsUnlocked", 4);

        db.collection("games").document(gameId)
                .update(updates)
                .addOnSuccessListener(v -> {
                    Toast.makeText(this, R.string.starting, Toast.LENGTH_SHORT).show();
                    navigateToGame();
                })
                .addOnFailureListener(e -> {
                    isGameStarting = false;
                    binding.btnStart.setEnabled(true);
                    Toast.makeText(this, R.string.error_creating_game, Toast.LENGTH_SHORT).show();
                });
    }

    private void navigateToGame() {
        if (isFinishing()) return;

        isMovingToGame = true;

        Intent intent = new Intent(LobbyActivity.this, GameActivity.class);
        intent.putExtra("game_id", gameId);
        startActivity(intent);
        isMovingToGame = true;
        finish();
    }

    private void reactivateIfRejoining(){
        if(gameId == null || gameId.isEmpty() || myUid == null || myUid.isEmpty()) return;

        db.runTransaction(transaction -> {
           DocumentReference reference = db.collection("games")
                   .document(gameId);
           DocumentSnapshot snapshot = transaction.get(reference);

           if(!snapshot.exists()) return null;

           String status = snapshot.getString("status");
           if("ENDED".equals(status) || "PLAYING".equals(status)) return null;

           Object stateObj = snapshot.get("players." + myUid + ".state");

           String state = (stateObj instanceof String) ? (String) stateObj : null;

           Map<String, Object> updates = new HashMap<>();

           if("ELIMINATED".equals(state)){
               updates.put("players." + myUid + ".state", "ACTIVE");
           }

           @SuppressWarnings("unchecked")
           List<String> turnOrder = (List<String>) snapshot.get("turnOrder");

           if(turnOrder == null) turnOrder = new ArrayList<>();

           if(!turnOrder.contains(myUid)){
               turnOrder.add(myUid);
               updates.put("turnOrder", turnOrder);
           }

           if(!updates.isEmpty()){
               transaction.update(reference, updates);
           }

           return null;
        }).addOnSuccessListener(v ->
                android.util.Log.d(TAG,"reactivateIfRejoining: OK")
        ).addOnFailureListener(e ->
                android.util.Log.e(TAG,"reactivateIfRejoining: FAILED", e)
        );
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (lobbyListener != null) {
            lobbyListener.remove();
            lobbyListener = null;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (lobbyListener != null) {
            lobbyListener.remove();
            lobbyListener = null;
        }
    }
}