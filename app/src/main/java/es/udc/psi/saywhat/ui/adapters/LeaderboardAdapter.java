package es.udc.psi.saywhat.ui.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Map;

import es.udc.psi.saywhat.R;
import es.udc.psi.saywhat.databinding.ItemLeaderboardBinding;
import es.udc.psi.saywhat.model.User;

public class LeaderboardAdapter extends RecyclerView.Adapter<LeaderboardAdapter.ViewHolder> {

    private final List<User> users;
    private static boolean showScores = true;
    private String currentUserId; // the person playing
    private String currentDifficulty;

//    public LeaderboardAdapter(List<User> users) {
//        this(users, true);
//    }

    public LeaderboardAdapter(List<User> users, String currentUserId, String difficultyKey) {
        this.users = users;
        this.currentUserId = currentUserId;
        this.currentDifficulty = difficultyKey;
        this.showScores = true;
    } // constructor for singleplayer

    public LeaderboardAdapter(List<User> users, boolean showScores) {
        this.users = users;
        this.showScores = showScores;
    } // constructor for multiplayer

    public void setDifficulty(String difficulty) {
        this.currentDifficulty = difficulty;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate using the generated Binding class
        ItemLeaderboardBinding binding = ItemLeaderboardBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User user = users.get(position);

        // Check if this row is the current user to highlight it
        boolean isMe = user.getUid() != null && user.getUid().equals(currentUserId);
        holder.bind(user, position, currentDifficulty, isMe);
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        // Hold a reference to the binding
        private final ItemLeaderboardBinding binding;

        public ViewHolder(ItemLeaderboardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(User user, int position, String difficulty, boolean isMe) {
            // Access views directly through the binding object
            binding.tvRank.setText(String.valueOf(position + 1));
            binding.tvName.setText(user.getName());

            // Get score specific to the selected tab
            int score = 0;
            Map<String, Integer> scores = user.getDifficultyScores();
            if (scores != null && scores.containsKey(difficulty) && scores.get(difficulty) != null) {
                score = scores.get(difficulty);
            }
            binding.tvScoreVal.setText(String.valueOf(score));

            if (showScores) {
                // Singleplayer - show the score
                binding.tvScoreVal.setVisibility(View.VISIBLE);
                binding.tvScoreVal.setText(String.valueOf(score));

                Context context = binding.getRoot().getContext();// context to access resources
                if (isMe) {
                    // highlight background
                    binding.getRoot().setBackgroundColor(ContextCompat.getColor(context, R.color.yellow_pressed));

                    // change text color
                    int pink = ContextCompat.getColor(context, R.color.pink_pressed);
                    binding.tvName.setTextColor(pink);
                    binding.tvRank.setTextColor(pink);
                    binding.tvScoreVal.setTextColor(pink);
                } else {
                    // reset to default
                    binding.getRoot().setBackgroundColor(Color.TRANSPARENT);
                    binding.tvName.setTextColor(Color.WHITE);
                    binding.tvRank.setTextColor(ContextCompat.getColor(context, R.color.yellow));
                    binding.tvScoreVal.setTextColor(Color.WHITE);
                }
            } else {
                // Multiplayer - hide the score
                binding.tvScoreVal.setVisibility(View.GONE);
                // reset styles for multiplayer (just in case)
                binding.getRoot().setBackgroundColor(Color.TRANSPARENT);
                binding.tvName.setTextColor(Color.WHITE);
                binding.tvRank.setTextColor(ContextCompat.getColor(binding.getRoot().getContext(), R.color.yellow));
            }
        }
    }
}