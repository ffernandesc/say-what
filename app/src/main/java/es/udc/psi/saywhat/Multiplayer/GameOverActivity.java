package es.udc.psi.saywhat.Multiplayer;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import es.udc.psi.saywhat.Adapters.LeaderboardAdapter;
import es.udc.psi.saywhat.FourthActivity;
import es.udc.psi.saywhat.MainActivity;
import es.udc.psi.saywhat.R;
import es.udc.psi.saywhat.databinding.ActivityGameOverBinding;
import es.udc.psi.saywhat.game.Game;
import es.udc.psi.saywhat.user.User;

public class GameOverActivity extends AppCompatActivity {

    private ActivityGameOverBinding binding;
    private FirebaseFirestore db;
    private String gameId;
    private String myUid;
    private LeaderboardAdapter adapter;
    private List<User> leaderboardList = new ArrayList<>();
    private ListenerRegistration statusListener;
    private boolean isHost = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityGameOverBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();

        gameId = getIntent().getStringExtra("game_id");
        SharedPreferences prefs = getSharedPreferences(FourthActivity.PREFS_NAME, Context.MODE_PRIVATE);
        myUid = prefs.getString("local_user_id", null);

        if (gameId == null || myUid == null) {
            Toast.makeText(this, R.string.error_missing_game_data, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupRecyclerView();

        checkIfHostAndLoadResults();
        listenForGameRestart();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                handleExit();
            }
        });

        binding.btnReturn.setOnClickListener(v -> handleExit());
        binding.btnReplay.setOnClickListener(v -> resetGameToLobby());
    }
    private void handleExit() {
        if (isHost && gameId != null) {
            // host leaves -> delete the game room so code is invalid
            db.collection("games").document(gameId).delete()
                    .addOnCompleteListener(task -> returnToMain());
        } else {
            // guest leaves -> just exit
            returnToMain();
        }
    }
    private void returnToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
    private void setupRecyclerView() {
        binding.rvLeaderboard.setLayoutManager(new LinearLayoutManager(this));

        adapter = new LeaderboardAdapter(leaderboardList, false);
        binding.rvLeaderboard.setAdapter(adapter);
    }

    private void checkIfHostAndLoadResults() {
        db.collection("games").document(gameId).get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        Game game = snapshot.toObject(Game.class);
                        if (game != null) {
                            isHost = myUid.equals(game.getHostId());
                            if (isHost) {
                                binding.btnReplay.setVisibility(View.VISIBLE);
                                binding.btnReplay.setEnabled(true);
                            } else {
                                binding.btnReplay.setVisibility(View.GONE);
                            }
                            processGameResults(game);
                        }
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, R.string.error_loading_results, Toast.LENGTH_SHORT).show()
                );
    }

    private void resetGameToLobby() {
        binding.btnReplay.setEnabled(false); // Prevent double taps

        db.collection("games").document(gameId).get()
                .addOnSuccessListener(snapshot -> {
                    Game game = snapshot.toObject(Game.class);
                    if (game == null) return;

                    Map<String, Object> updates = new HashMap<>();

                    updates.put("status", "WAITING");

                    // reset difficulty
                    updates.put("buttonsUnlocked", 4);
                    updates.put("sequence", null);
                    updates.put("currentTurnUid", null);

                    // reset all players to ACTIVE
                    if (game.getPlayers() != null) {
                        for (String uid : game.getPlayers().keySet()) {
                            updates.put("players." + uid + ".state", "ACTIVE");
                            updates.put("players." + uid + ".eliminationTime", 0);
                        }
                    }

                    db.collection("games").document(gameId).update(updates)
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, R.string.failed_to_restart, Toast.LENGTH_SHORT).show();
                                binding.btnReplay.setEnabled(true);
                            });
                });
    }

    private void listenForGameRestart() {
        statusListener = db.collection("games").document(gameId)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null || snapshot == null || !snapshot.exists()) return;

                    String status = snapshot.getString("status");

                    // If Host reset the game, we go back to LobbyActivity
                    if ("WAITING".equals(status)) {
                        Intent intent = new Intent(GameOverActivity.this, LobbyActivity.class);
                        intent.putExtra("game_id", gameId);
                        intent.putExtra("is_host", isHost); // Pass correct host status

                        // Clear back stack so they can't go back to Game Over
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

                        startActivity(intent);
                        finish();
                    }
                });
    }

    private void processGameResults(Game game) {
        Map<String, Game.PlayerInfo> players = game.getPlayers();
        List<String> survivors = game.getTurnOrder();

        boolean isSurvivor = survivors != null && survivors.contains(myUid);
        if (isSurvivor) {
            binding.tvWin.setVisibility(View.VISIBLE);
            binding.tvLose.setVisibility(View.GONE);
        } else {
            binding.tvWin.setVisibility(View.GONE);
            binding.tvLose.setVisibility(View.VISIBLE);
        }

        leaderboardList.clear();
        List<Game.PlayerInfo> eliminatedPlayers = new ArrayList<>();

        if (players != null) {
            if (survivors != null) {
                for (String uid : survivors) {
                    Game.PlayerInfo p = players.get(uid);
                    if (p != null) {
                        leaderboardList.add(createDummyUser(p));
                    }
                }
            }

            for (Map.Entry<String, Game.PlayerInfo> entry : players.entrySet()) {
                String uid = entry.getKey();
                if (survivors == null || !survivors.contains(uid)) {
                    String name = (entry.getValue().getName() != null) ? entry.getValue().getName() : "Player";

                    User user = new User();
                    user.setName(name);
                    user.setHighScore(0);
                    leaderboardList.add(user);
                }
            }
        }

        Collections.sort(eliminatedPlayers, (p1, p2) ->
                Long.compare(p2.getEliminationTime(), p1.getEliminationTime())
        ); // sort players descending from last to "die" to first

        for (Game.PlayerInfo p : eliminatedPlayers) {
            leaderboardList.add(createDummyUser(p));
        } // add sorted player to the list

        adapter.notifyDataSetChanged();
    }

    private User createDummyUser(Game.PlayerInfo p) {
        String name = (p.getName() != null) ? p.getName() : "Player";
        User user = new User();
        user.setName(name);
        user.setHighScore(0);
        return user;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (statusListener != null) {
            statusListener.remove();
        }
    }
}