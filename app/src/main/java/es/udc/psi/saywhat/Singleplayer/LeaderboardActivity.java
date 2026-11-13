package es.udc.psi.saywhat.Singleplayer;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.tabs.TabLayout;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

import es.udc.psi.saywhat.Adapters.LeaderboardAdapter;
import es.udc.psi.saywhat.FourthActivity;
import es.udc.psi.saywhat.R;
import es.udc.psi.saywhat.databinding.ActivityLeaderboardBinding;
import es.udc.psi.saywhat.user.User;

public class LeaderboardActivity extends AppCompatActivity {

    private ActivityLeaderboardBinding binding;
    private FirebaseFirestore db;
    private LeaderboardAdapter adapter;
    private List<User> userList = new ArrayList<>();
    private String currentDifficulty = "4";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLeaderboardBinding.inflate(getLayoutInflater());
        EdgeToEdge.enable(this);
        setContentView(binding.getRoot());

        // current User ID for highlighting
        SharedPreferences prefs = getSharedPreferences(FourthActivity.PREFS_NAME, Context.MODE_PRIVATE);
        String currentUserId = prefs.getString("local_user_id", "");

        // current difficulty for leaderboard tab
        if (getIntent().hasExtra("DIFFICULTY_KEY")) {
            currentDifficulty = getIntent().getStringExtra("DIFFICULTY_KEY");
        } else {
            currentDifficulty = "4"; // default
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // setup Recycler
        binding.recyclerViewLeaderboard.setLayoutManager(new LinearLayoutManager(this));
        adapter = new LeaderboardAdapter(userList, currentUserId, currentDifficulty);
        binding.recyclerViewLeaderboard.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();

        setupTabs();
        loadLeaderboard(currentDifficulty);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Leaderboard");
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void setupTabs() {
        // tabs for 4, 5, 6, 7, 8 buttons
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(R.string._4_buttons).setTag("4"));
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(R.string._5_buttons).setTag("5"));
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(R.string._6_buttons).setTag("6"));
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(R.string._7_buttons).setTag("7"));
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(R.string._8_buttons).setTag("8"));

        // loop through all tabs to find the one matching currentDifficulty
        for (int i = 0; i < binding.tabLayout.getTabCount(); i++) {
            TabLayout.Tab tab = binding.tabLayout.getTabAt(i);
            if (tab != null && tab.getTag() != null && tab.getTag().toString().equals(currentDifficulty)) {
                tab.select();
                break;
            }
        }

        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getTag() != null) {
                    currentDifficulty = tab.getTag().toString();
                    adapter.setDifficulty(currentDifficulty); // update adapter so it shows right scores
                    loadLeaderboard(currentDifficulty); // fetch new data
                }
            }
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}
            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void loadLeaderboard(String difficultyKey) {
        db.collection("users")
                .orderBy("difficultyScores." + difficultyKey, Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    userList.clear();
//                    List<User> users = queryDocumentSnapshots.toObjects(User.class);
//                    userList.addAll(users);

                    for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots) {
                        User user = doc.toObject(User.class);
                        if (user != null) {
                            user.setUid(doc.getId());
                            userList.add(user);
                        }
                    }

                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, getString(R.string.failed_to_load_leaderboard), Toast.LENGTH_SHORT).show();
                });


    }
}