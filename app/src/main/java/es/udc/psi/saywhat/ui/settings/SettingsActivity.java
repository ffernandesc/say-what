package es.udc.psi.saywhat.ui.settings;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult;
import com.google.android.gms.auth.api.Auth;
import com.google.firebase.Firebase;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import es.udc.psi.saywhat.R;
import es.udc.psi.saywhat.databinding.ActivitySettingsBinding;
import es.udc.psi.saywhat.model.User;
import es.udc.psi.saywhat.ui.adapters.LanguageAdapter;

public class SettingsActivity extends AppCompatActivity {

    private ActivitySettingsBinding binding;
    private SharedPreferences sharedPrefs;
    private FirebaseFirestore db;
    private String localUserId;

    public static final String PREFS_NAME = "GameSettings";
    private static final String KEY_LOCAL_USER_ID = "local_user_id";
    public static final String KEY_SOUND_VOLUME = "sound_volume";
    public static final String KEY_LANGUAGE = "app_language";
    public static final String ACTION_SETTINGS_CHANGED = "es.udc.psi.saywhat.SETTINGS_CHANGED";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        db = FirebaseFirestore.getInstance();
        localUserId = sharedPrefs.getString(KEY_LOCAL_USER_ID, null);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        loadSettings();
        setupLanguageSpinner();
        setupListeners();

        setupAccountUi();
        setupAccountButton();

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

    }

    private void startGoogleSignIn(){
        List<AuthUI.IdpConfig> providers = Arrays.asList(
                new AuthUI.IdpConfig.GoogleBuilder().build()
        );

        Intent intent = AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .build();

        googleSignInLauncher.launch(intent);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void loadSettings() {
        int soundVolume = sharedPrefs.getInt(KEY_SOUND_VOLUME, 100);
        binding.soundVolumeSlider.setProgress(soundVolume);
        binding.soundVolumeSlider.setEnabled(true);
    }

    private void setupLanguageSpinner() {
        String[] languages = {"English", "Español", "Română", "Português"};
        String[] langCodes = {"en", "es", "ro", "pt"};
        int[] flags = {
                R.drawable.uk_flag,
                R.drawable.spain_flag,
                R.drawable.romania_flag,
                R.drawable.portugal_flag
        };

        LanguageAdapter adapter = new LanguageAdapter(this, languages, flags);
        binding.languageSpinner.setAdapter(adapter);

        String currentLang = sharedPrefs.getString(KEY_LANGUAGE, "en");
        for (int i = 0; i < langCodes.length; i++) {
            if (langCodes[i].equals(currentLang)) {
                binding.languageSpinner.setSelection(i);
                break;
            }
        }


        binding.languageSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedCode = langCodes[position];
                String savedCode = sharedPrefs.getString(KEY_LANGUAGE, "en");

                // Only reload if the language actually changed
                if (!selectedCode.equals(savedCode)) {
                    setLocale(selectedCode);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void setLocale(String langCode) {
        Locale locale = new Locale(langCode);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.setLocale(locale);

        // Update Configuration
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());

        // Save preference
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putString(KEY_LANGUAGE, langCode);
        editor.apply();

        // Restart Activity to apply changes
        Intent intent = getIntent();
        finish();
        startActivity(intent);
    }

    private void setupListeners() {
        SharedPreferences.Editor editor = sharedPrefs.edit();

        binding.soundVolumeSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    editor.putInt(KEY_SOUND_VOLUME, progress);
                    editor.apply();
                    sendSettingsChangeBroadcast();
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }
    private void sendSettingsChangeBroadcast() {
        Intent intent = new Intent(ACTION_SETTINGS_CHANGED);
        sendBroadcast(intent);
    }

    private void setupAccountUi(){
        if(localUserId == null){
            binding.createAccountTv.setText(R.string.create_account);
            binding.createButton.setText(R.string.create_account);
            binding.btnGoogleSignIn.setVisibility(View.VISIBLE);
        } else {
            binding.createAccountTv.setText(R.string.account_details_text);
            binding.createButton.setText(R.string.change_details_text);
            binding.btnGoogleSignIn.setVisibility(View.GONE);

            db.collection("users")
                    .document(localUserId)
                    .get()
                    .addOnSuccessListener(doc ->{
                        if(doc.exists()){
                            User user = doc.toObject(User.class);
                            if(user!=null){
                                binding.usernameInput.setText(user.getName());
                                binding.emailInput.setText(user.getEmail());
                            }
                        }
                    })
                    .addOnFailureListener( e ->
                        Toast.makeText(this, getText(R.string.account_error).toString(), Toast.LENGTH_SHORT)
                                .show());
        }
    }

    private void setupAccountButton(){
        binding.createButton.setOnClickListener(v->{
            String username = binding.usernameInput.getText().toString().trim();
            String email = binding.emailInput.getText().toString().trim();

            if(username.isEmpty()){
                binding.usernameInput.setError(getText(R.string.name_required).toString());
            }

            if(email.isEmpty()){
                binding.emailInput.setError(getText(R.string.email_required).toString());
            }

            if (localUserId != null) {
                db.collection("users")
                        .document(localUserId)
                        .update("name", username, "email", email)
                        .addOnSuccessListener(s -> {
                            Toast.makeText(this, getText(R.string.details_saved), Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e ->
                                Toast.makeText(this, getText(R.string.account_saving_error), Toast.LENGTH_SHORT).show());
            }

            else {

                db.collection("users")
                        .whereEqualTo("email", email)
                        .get()
                        .addOnSuccessListener(queryDocumentSnapshots -> {
                            if (!queryDocumentSnapshots.isEmpty()) {

                                User existingUser = queryDocumentSnapshots.getDocuments().get(0).toObject(User.class);

                                if (existingUser != null) {

                                    localUserId = existingUser.getUid();
                                    sharedPrefs.edit().putString(KEY_LOCAL_USER_ID, localUserId).apply();
                                    db.collection("users").document(localUserId).update("name", username);

                                    binding.createAccountTv.setText(R.string.account_details_text);
                                    binding.createButton.setText(R.string.change_details_text);

                                    Toast.makeText(this, getString(R.string.welcome_back_options) + username, Toast.LENGTH_SHORT).show();
                                }
                            } else {

                                localUserId = db.collection("users").document().getId();
                                sharedPrefs.edit().putString(KEY_LOCAL_USER_ID, localUserId).apply();

                                User newUser = new User(username, email, localUserId);

                                db.collection("users")
                                        .document(localUserId)
                                        .set(newUser)
                                        .addOnSuccessListener(s -> {
                                            Toast.makeText(this, getText(R.string.details_saved), Toast.LENGTH_SHORT).show();

                                            binding.createAccountTv.setText(R.string.account_details_text);
                                            binding.createButton.setText(R.string.change_details_text);
                                        })
                                        .addOnFailureListener(e ->
                                                Toast.makeText(this, getText(R.string.account_saving_error), Toast.LENGTH_SHORT).show());
                            }
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(this, getString(R.string.error_checking_account) + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            }
        });

        binding.btnLogout.setOnClickListener(v -> {

            FirebaseAuth.getInstance().signOut();

            sharedPrefs.edit().remove(KEY_LOCAL_USER_ID).apply();
            localUserId = null;


            binding.usernameInput.setText("");
            binding.emailInput.setText("");


            binding.createAccountTv.setText(R.string.create_account);
            binding.createButton.setText(R.string.create_account);
            binding.btnGoogleSignIn.setVisibility(View.VISIBLE);

            Toast.makeText(this, R.string.logged_out, Toast.LENGTH_SHORT).show();
        });

        binding.btnGoogleSignIn.setOnClickListener(v -> startGoogleSignIn());
    }

    private final ActivityResultLauncher<Intent> googleSignInLauncher =
            registerForActivityResult(
                    new FirebaseAuthUIActivityResultContract(),
                    this::onGoogleSignInResult
            );

    private void onGoogleSignInResult(FirebaseAuthUIAuthenticationResult result){

        if(result.getResultCode() != RESULT_OK){
                Toast.makeText(this, R.string.account_error, Toast.LENGTH_SHORT).show();
                return;
            }

            FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
            if(firebaseUser == null) return;

            final String firebaseUid = firebaseUser.getUid();
            final String email = firebaseUser.getEmail();
            final String name = firebaseUser.getDisplayName()!= null ? firebaseUser.getDisplayName().trim() : "";

            if(email == null){
                Toast.makeText(this, R.string.account_error, Toast.LENGTH_SHORT).show();
                return;
            }

            db.collection("users")
                    .whereEqualTo("email", email.trim())
                    .limit(1)
                    .get()
                    .addOnSuccessListener(query ->{
                        if(!query.isEmpty()){
                            String oldDocId = query.getDocuments().get(0).getId();
                            User oldUser = query.getDocuments().get(0).toObject(User.class);

                            if(oldUser == null){
                                Toast.makeText(this, R.string.account_error, Toast.LENGTH_SHORT).show();
                                return;
                            }

                            oldUser.setUid(firebaseUid);

                            if (!name.isEmpty()){
                                oldUser.setName(name);
                            }

                            db.collection("users")
                                .document(firebaseUid)
                                .set(oldUser)
                                .addOnSuccessListener(s->{
                                    if(!oldDocId.equals(firebaseUid)){
                                        db.collection("users")
                                            .document(oldDocId)
                                            .delete();
                                    }

                                    localUserId = firebaseUid;

                                    sharedPrefs.edit().putString(KEY_LOCAL_USER_ID, localUserId).apply();

                                    setupAccountUi();

                                    Toast.makeText(this,
                                            getString(R.string.welcome_back_options)
                                    + " " + name, Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e->
                                    Toast.makeText(this, R.string.account_saving_error, Toast.LENGTH_SHORT).show());

                        } else {

                            User newUser = new User(
                                name,
                                email.trim(),
                                firebaseUid
                            );
                            db.collection("users")
                                .document(firebaseUid)
                                .set(newUser)
                                .addOnSuccessListener( s ->{
                                    localUserId = firebaseUid;
                                    sharedPrefs.edit().putString(KEY_LOCAL_USER_ID,localUserId).apply();
                                    setupAccountUi();
                                    Toast.makeText(this,
                                            R.string.details_saved, Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e->
                                    Toast.makeText(this, R.string.account_saving_error, Toast.LENGTH_SHORT).show());
                            }
                        })
                            .addOnFailureListener(e->
                                Toast.makeText(this, R.string.account_error, Toast.LENGTH_SHORT).show());
    }
}

