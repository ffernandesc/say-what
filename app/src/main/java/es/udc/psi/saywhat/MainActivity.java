package es.udc.psi.saywhat;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.*;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.*;

import es.udc.psi.saywhat.Multiplayer.ThirdActivity;
import es.udc.psi.saywhat.Singleplayer.SecondActivity;
import es.udc.psi.saywhat.databinding.ActivityMainBinding;
import es.udc.psi.saywhat.user.User;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private ActivityMainBinding binding;

    //for sounds
    private MediaPlayer clickPlayer;
    private SharedPreferences sharedPrefs;

    private String currentLanguageCode;

    private static final String KEY_GUEST_ID = "stored_guest_id";
    private static final String KEY_GUEST_NAME = "stored_guest_name";
    private static final String KEY_GUEST_EXPIRY = "guest_expiry_time";
    private static final long GUEST_SESSION_DURATION = 10 * 60 * 1000;

    @Override
    protected void onResume() {
        super.onResume();

        if (sharedPrefs == null) {
            sharedPrefs = getSharedPreferences(FourthActivity.PREFS_NAME, Context.MODE_PRIVATE);
        }
        applyVolumeSettings();
        checkLocaleChange();

//        startService(new Intent(this, MusicService.class));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        //release MediaPlayer resources
        if (clickPlayer != null) {
            clickPlayer.release();
            clickPlayer = null;
        }
    }

    @SuppressLint("RestrictedApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedPrefs = getSharedPreferences(FourthActivity.PREFS_NAME, Context.MODE_PRIVATE);
        loadLocale();

        EdgeToEdge.enable(this);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sharedPrefs = getSharedPreferences(FourthActivity.PREFS_NAME, Context.MODE_PRIVATE);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        //sounds
        initializeSoundPlayer();
        applyVolumeSettings();

        //animation for stars
        List<ImageView> starViews = Arrays.asList(
                binding.star1,
                binding.star2,
                binding.star3,
                binding.star4,
                binding.star5,
                binding.star6,
                binding.star7,
                binding.star8
        );
        startStarRotations(starViews);

        //buttons
        setupButtonListeners();
        setupDifficultyCard();

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void loadLocale() {
        //get saved language (default to english)
        String langCode = sharedPrefs.getString(FourthActivity.KEY_LANGUAGE, "en");
        currentLanguageCode = langCode;
        setLocale(langCode);
    }

    private void setLocale(String langCode) {
        Locale locale = new Locale(langCode);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.setLocale(locale);
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());
    }

    private void checkLocaleChange() {

        String savedLang = sharedPrefs.getString(FourthActivity.KEY_LANGUAGE, "en");

        if (!savedLang.equals(currentLanguageCode)) {
            setLocale(savedLang);
            recreate();
        }
    }

    //star animation
    private void startStarRotations(List<ImageView> stars) {
        long duration = 45000;

        for (int i = 0; i < stars.size(); i++) {
            ImageView star = stars.get(i);
            long starDuration = duration + (i * 500);
            float rotationDegrees = 3600f;

            star.animate()
                    .rotationBy(rotationDegrees)
                    .setDuration(starDuration)
                    .setStartDelay(i * 100)
                    .withEndAction(() -> {
                        star.setRotation(0f);
                        startRotation(star, starDuration);
                    })
                    .start();
        }
    }
    private void startRotation(ImageView star, long duration) {
        star.animate()
                .rotationBy(3600f)
                .setDuration(duration)
                .withEndAction(() -> {
                    star.setRotation(0f);
                    startRotation(star, duration);
                })
                .start();
    }

    //sounds player
    private void initializeSoundPlayer(){
        try {
            clickPlayer = MediaPlayer.create(this, R.raw.duck);
            if (clickPlayer == null) {
                Log.e(TAG, "Failed to create clickPlayer.");
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception creating clickPlayer: " + e.getMessage());
            clickPlayer = null;
        }
    }
    private void applyVolumeSettings() {
        if (sharedPrefs == null) return;

        int soundVolumeProgress = sharedPrefs.getInt(FourthActivity.KEY_SOUND_VOLUME, 100);
        float soundVolume = soundVolumeProgress / 100f;

        if (clickPlayer != null) {
            clickPlayer.setVolume(soundVolume, soundVolume);
        }
    }

    private void setupDifficultyCard() {

        String[] diffNames = getResources().getStringArray(R.array.difficulty_levels);

        if (diffNames.length >= 5) {
            binding.btnDiff4.setText(diffNames[0]);
            binding.btnDiff5.setText(diffNames[1]);
            binding.btnDiff6.setText(diffNames[2]);
            binding.btnDiff7.setText(diffNames[3]);
            binding.btnDiff8.setText(diffNames[4]);
        }

        View.OnClickListener diffClickListener = v -> {

            if (clickPlayer != null) clickPlayer.start();


            int count = 4;
            if (v == binding.btnDiff5) count = 5;
            else if (v == binding.btnDiff6) count = 6;
            else if (v == binding.btnDiff7) count = 7;
            else if (v == binding.btnDiff8) count = 8;

            int finalCount = count;

            animateButton(v, () -> startGameWithDifficulty(finalCount));
        };


        binding.btnDiff4.setOnClickListener(diffClickListener);
        binding.btnDiff5.setOnClickListener(diffClickListener);
        binding.btnDiff6.setOnClickListener(diffClickListener);
        binding.btnDiff7.setOnClickListener(diffClickListener);
        binding.btnDiff8.setOnClickListener(diffClickListener);

        binding.btnCloseDiff.setOnClickListener(v -> {
            if (clickPlayer != null) clickPlayer.start();

            animateButton(v, () -> binding.overlayDifficulty.setVisibility(View.GONE));
        });

        binding.overlayDifficulty.setOnClickListener(v -> {
            binding.overlayDifficulty.setVisibility(View.GONE);
        });
    }

    private void startGameWithDifficulty(int buttonCount) {

        Intent intent = new Intent(MainActivity.this, SecondActivity.class);
        intent.putExtra(getString(R.string.button_count), buttonCount);

        startActivity(intent);

        binding.overlayDifficulty.setVisibility(View.GONE);
    }

    //buttons
    private void setupButtonListeners() {
        binding.buttonSingleplayer.setOnClickListener(v -> {

            //play sound
            if (clickPlayer != null) {
                clickPlayer.start();
            }

            animateButton(v, () -> {
                binding.overlayDifficulty.setVisibility(View.VISIBLE);

                binding.overlayDifficulty.setAlpha(0f);
                binding.overlayDifficulty.animate().alpha(1f).setDuration(200).start();
            });

        });

        binding.buttonMultiplayer.setOnClickListener(v -> {

            //play sound
            if (clickPlayer != null) {
                clickPlayer.start();
            }

            animateButton(v, () -> {

                String localUserId = sharedPrefs.getString("local_user_id", null);

                if (localUserId == null) {
                    showGuestNamePopUp();
                    return;
                }

                String storedGuestId = sharedPrefs.getString(KEY_GUEST_ID, null);
                long expiryTime = sharedPrefs.getLong(KEY_GUEST_EXPIRY, 0);

                // if the current local_user_id matches the Guest ID and time hasnt run out
                if (localUserId.equals(storedGuestId) && System.currentTimeMillis() < expiryTime) {
                    // valid guest, go directly to multiplayer without popup
                    String guestName = sharedPrefs.getString(KEY_GUEST_NAME, "Guest");
                    goToMultiplayer(guestName);
                    return;
                }


                FirebaseFirestore db = FirebaseFirestore.getInstance();
                db.collection("users")
                        .document(localUserId)
                        .get()
                        .addOnSuccessListener(doc -> {
                            if (doc.exists()) {
                                User user = doc.toObject(User.class);
                                if (user != null && user.getName() != null && !user.getName().isEmpty()) {
                                    goToMultiplayer(user.getName());
                                } else {
                                    showGuestNamePopUp();
                                }
                            } else {
                                showGuestNamePopUp();
                            }
                        })
                        .addOnFailureListener(e -> showGuestNamePopUp());
            });
        });

        binding.buttonOptions.setOnClickListener(v -> {

            //play sound
            if (clickPlayer != null) {
                clickPlayer.start();
            }

            animateButton(v, () -> {

                Intent intent = new Intent(MainActivity.this, FourthActivity.class);
                startActivity(intent);

            });
        });

    }

    public void goToMultiplayer(String playerName){
        Intent intent = new Intent(MainActivity.this, ThirdActivity.class);
        intent.putExtra("player_name", playerName);
        startActivity(intent);
    }

    private void showGuestNamePopUp(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.multiplayer_access_title);
        builder.setMessage(R.string.multiplayer_access_message);

        //Create Account
        builder.setPositiveButton(R.string.create_account, (dialog, which) -> {
            Intent intent = new Intent(MainActivity.this, FourthActivity.class);
            startActivity(intent);
        });

        //Guest
        builder.setNegativeButton(R.string.play_as_guest, (dialog, which) -> {

            String guestId;
            String guestName;
            long currentTime = System.currentTimeMillis();
            long expiryTime = sharedPrefs.getLong(KEY_GUEST_EXPIRY, 0);
            String storedId = sharedPrefs.getString(KEY_GUEST_ID, null);

            if (storedId != null && currentTime < expiryTime) {
                // REUSE EXISTING GUEST
                guestId = storedId;
                guestName = sharedPrefs.getString(KEY_GUEST_NAME, "Guest");
            } else {
                guestId = java.util.UUID.randomUUID().toString();
                int randomNum = new Random().nextInt(9999);
                guestName = "Guest_" + randomNum;

                sharedPrefs.edit()
                        .putString(KEY_GUEST_ID, guestId)
                        .putString(KEY_GUEST_NAME, guestName)
                        .putLong(KEY_GUEST_EXPIRY, currentTime + GUEST_SESSION_DURATION)
                        .apply();
            }
            sharedPrefs.edit()
                    .putString("local_user_id", guestId)
                    .apply();

            goToMultiplayer(guestName);
        });

        //Cancel
        builder.setNeutralButton(R.string.cancel, (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void animateButton(android.view.View view, Runnable action) {

        view.animate()
                .scaleX(0.9f)
                .scaleY(0.9f)
                .setDuration(100)
                .withEndAction(() -> {

                    view.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setInterpolator(new android.view.animation.OvershootInterpolator())
                            .setDuration(300)
                            .start();
                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(action, 200);
                })
                .start();

    }
}