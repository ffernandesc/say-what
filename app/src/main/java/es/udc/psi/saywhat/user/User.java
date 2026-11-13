package es.udc.psi.saywhat.user;

import com.google.firebase.database.IgnoreExtraProperties;

import java.util.HashMap;
import java.util.Map;

@IgnoreExtraProperties
public class User {
    private String uid;
    private String name;
    private String email;
    private int highScore;
    private Map<String, Integer> difficultyScores = new HashMap<>();

    public User(){}

    public User(String name, String email, String uid){
        this.name = name;
        this.email = email;
        this.uid = uid;
        this.highScore = 0;
    }

    public String getUid(){return uid;}
    public String getName(){return name;}
    public String getEmail(){return email;}
    public int getHighScore() { return highScore; }
    public Map<String, Integer> getDifficultyScores() {return difficultyScores;}

    public void setUid(String uid){ this.uid = uid;}
    public void setName(String name){this.name = name;}
//    public void setEmail(String email){this.email = email;}
    public void setHighScore(int highScore) {this.highScore = highScore;}
//    public void setDifficultyScores(Map<String, Integer> difficultyScores) {this.difficultyScores = difficultyScores;}
}
