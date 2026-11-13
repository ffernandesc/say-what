package es.udc.psi.saywhat.ui.multiplayer;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.Random;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import es.udc.psi.saywhat.R;
import es.udc.psi.saywhat.databinding.ActivityMultiplayerMenuBinding;
import es.udc.psi.saywhat.databinding.LayoutNotificationBarBinding;
import es.udc.psi.saywhat.model.Game;
import es.udc.psi.saywhat.ui.settings.SettingsActivity;

public class MultiplayerMenuActivity extends AppCompatActivity {

    private ActivityMultiplayerMenuBinding binding;
    private LayoutNotificationBarBinding notificationBinding;
    private FirebaseFirestore db;
    private String localUserId;
    private String playerName;

    private ListenerRegistration pendingGameListener;

    private Handler notificationHandler = new Handler(Looper.getMainLooper());
    private Runnable hideNotificationRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityMultiplayerMenuBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        notificationBinding = binding.notificationOverlay;
        db = FirebaseFirestore.getInstance();

        playerName = getIntent().getStringExtra("player_name");
        if (playerName != null && !playerName.isEmpty()) {
            binding.tvWelcomePlayer.setText(String.format("%s%s", getString(R.string.welcome), playerName));
        }

        SharedPreferences prefs = getSharedPreferences(SettingsActivity.PREFS_NAME, Context.MODE_PRIVATE);
        localUserId = prefs.getString("local_user_id", "unknown_user");

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        if(getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        binding.btnCreateMatch.setOnClickListener(v -> createMatch());

        binding.btnJoinMatch.setOnClickListener(v -> {
            Intent intent = new Intent(MultiplayerMenuActivity.this, JoinGameActivity.class);
            intent.putExtra("player_name", playerName);
            startActivity(intent);
        });

        hideNotificationRunnable = this::hideNotificationWithAnimation;
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkPendingGame();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (pendingGameListener != null) {
            pendingGameListener.remove();
            pendingGameListener = null;
        }

        notificationHandler.removeCallbacks(hideNotificationRunnable);
    }

    private void checkPendingGame() {
        SharedPreferences prefs = getSharedPreferences(SettingsActivity.PREFS_NAME, Context.MODE_PRIVATE);
        String pendingGameId = prefs.getString("pending_game_id", null);

        if (pendingGameId == null) return;
        if (pendingGameListener != null) return;

        pendingGameListener = db.collection("games").document(pendingGameId)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null || snapshot == null || !snapshot.exists()) return;

                    String status = snapshot.getString("status");

                    if ("ENDED".equals(status)) {
                        showGameEndedNotification(pendingGameId);

                        prefs.edit().remove("pending_game_id").apply();
                        if (pendingGameListener != null) {
                            pendingGameListener.remove();
                            pendingGameListener = null;
                        }
                    }
                });
    }

    private void showGameEndedNotification(String gameId) {

        notificationHandler.removeCallbacks(hideNotificationRunnable);
        // Use notificationBinding to access views without findViewById
        notificationBinding.btnNotifSeeRanking.setOnClickListener(v -> {

            notificationHandler.removeCallbacks(hideNotificationRunnable);

            Intent intent = new Intent(MultiplayerMenuActivity.this, GameOverActivity.class);
            intent.putExtra("game_id", gameId);
            startActivity(intent);

            notificationBinding.getRoot().setVisibility(View.GONE);
        });

        notificationBinding.btnNotifClose.setOnClickListener(v -> {
            notificationHandler.removeCallbacks(hideNotificationRunnable);
            hideNotificationWithAnimation();
        });

        // Show and Animate
        notificationBinding.getRoot().setVisibility(View.VISIBLE);
        notificationBinding.getRoot().setTranslationY(-500f);
        ObjectAnimator.ofFloat(notificationBinding.getRoot(), "translationY", 0f)
                .setDuration(500)
                .start();

        notificationHandler.postDelayed(hideNotificationRunnable, 4000);
    }

    private void hideNotificationWithAnimation() {
        // Slide Up Animation
        ObjectAnimator animator = ObjectAnimator.ofFloat(notificationBinding.getRoot(), "translationY", -200f);
        animator.setDuration(500);
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                // actually hide the view after animation finishes
                notificationBinding.getRoot().setVisibility(View.GONE);
            }
        });
        animator.start();
    }

    private void createMatch() {
        binding.btnCreateMatch.setEnabled(false);
        Toast.makeText(this, R.string.creating, Toast.LENGTH_SHORT).show();

        // generate a random 4-digit code
        int randomCode = new Random().nextInt(9000) + 1000;
        String gameId = String.valueOf(randomCode);

        Game newGame = new Game(gameId, localUserId);
        newGame.setStatus("WAITING");

        Game.PlayerInfo hostInfo = new Game.PlayerInfo(playerName, "ACTIVE", System.currentTimeMillis());
        newGame.getPlayers().put(localUserId,hostInfo);

        newGame.getTurnOrder().add(localUserId);
        newGame.setCurrentTurnUid(localUserId);

        db.collection("games").document(gameId)
                .set(newGame)
                .addOnSuccessListener(aVoid -> {
                    Intent intent = new Intent(MultiplayerMenuActivity.this, LobbyActivity.class);
                    intent.putExtra("game_id", gameId);
                    intent.putExtra("is_host", true);
                    startActivity(intent);
                    binding.btnCreateMatch.setEnabled(true);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, R.string.error_creating_game, Toast.LENGTH_SHORT).show();
                    binding.btnCreateMatch.setEnabled(true);
                });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}