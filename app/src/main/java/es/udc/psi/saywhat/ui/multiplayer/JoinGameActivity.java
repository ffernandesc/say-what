package es.udc.psi.saywhat.ui.multiplayer;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import es.udc.psi.saywhat.R;
import es.udc.psi.saywhat.databinding.ActivityJoinGameBinding;
import es.udc.psi.saywhat.model.Game;
import es.udc.psi.saywhat.ui.settings.SettingsActivity;

public class JoinGameActivity extends AppCompatActivity {

    private ActivityJoinGameBinding binding;
    private FirebaseFirestore db;
    private String playerName;
    private String localUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityJoinGameBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();

        playerName = getIntent().getStringExtra("player_name");
        SharedPreferences prefs = getSharedPreferences(SettingsActivity.PREFS_NAME, Context.MODE_PRIVATE);
        localUserId = prefs.getString("local_user_id", "");

        binding.btnJoin.setOnClickListener(v -> {
            String code = binding.etGameCode.getText().toString().toUpperCase().trim();
            if(code.isEmpty()) {
                binding.etGameCode.setError(getString(R.string.enter_game_code));
                return;
            }
            joinGame(code);
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void joinGame(String gameId) {
        db.collection("games").document(gameId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        Toast.makeText(this, R.string.game_not_found, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Game game = documentSnapshot.toObject(Game.class);

                    if(game == null){
                        Toast.makeText(this, R.string.network_error, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // If they're already in game, just go lobby
                    if(game.getPlayers()!=null && game.getPlayers().containsKey(localUserId)){
                        Game.PlayerInfo existingInfo = game.getPlayers().get(localUserId);

                        if(existingInfo != null && "ELIMINATED".equals(existingInfo.getState())) {
                            Intent intent = new Intent(this, GameOverActivity.class);
                            intent.putExtra("game_id", gameId);
                            intent.putExtra("my_uid", localUserId);
                            startActivity(intent);
                            finish();
                            return;
                        }

                        Intent intent = new Intent(this, LobbyActivity.class);
                        intent.putExtra("game_id", gameId);
                        intent.putExtra("is_host", false);
                        startActivity(intent);
                        finish();
                        return;
                    }

                    if("PLAYING".equals(game.getStatus())){
                        Toast.makeText(this, R.string.failed_to_join, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if("HOST_LEFT".equals(game.getStatus()) || "ENDED".equals(game.getStatus())) {
                        Toast.makeText(this, R.string.match_ended, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Game.PlayerInfo info = new Game.PlayerInfo(playerName, "ACTIVE", System.currentTimeMillis());

                    db.collection("games").document(gameId)
                            .update(
                                    "players." + localUserId, info,
                                    "turnOrder", FieldValue.arrayUnion(localUserId)
                            )
                            .addOnSuccessListener(aVoid -> {
                                Intent intent = new Intent(this, LobbyActivity.class);
                                intent.putExtra("game_id", gameId);
                                intent.putExtra("is_host", false);
                                startActivity(intent);
                                finish();
                            }).addOnFailureListener(e ->
                                    Toast.makeText(this, R.string.failed_to_join, Toast.LENGTH_SHORT).show()
                            );
            }).addOnFailureListener(e ->
            Toast.makeText(this, R.string.network_error, Toast.LENGTH_SHORT).show()
        );
    }
}
