package es.udc.psi.saywhat.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Game {
    private String gameId;
    private String hostId;
    private String status;
    private Map<String, PlayerInfo> players = new HashMap<>();
    private List<String> turnOrder = new ArrayList<>();
    private String currentTurnUId;
    private List<Integer> sequence = new ArrayList<>();
    private int buttonsUnlocked = 4;
    private long lastUpdateTimestamp = 0;
    private static long eliminationTime;

    public Game() {     }

    public Game(String gameId, String hostId) {
        this.gameId = gameId;
        this.hostId = hostId;
        this.status = "WAITING";
    }

    public static class PlayerInfo{
        private String name;
        private String state;
        private long joinedAt;
        private long eliminationTime;

        public PlayerInfo(){    }

        public PlayerInfo(String name, String state, long joinedAt){
            this.joinedAt = joinedAt;
            this.name = name;
            this.state = state;
        }

        public String getName(){
            return name;
        }
        public String getState(){
            return state;
        }
        public long getJoinedAt() {
            return joinedAt;
        }
        public void setName(String name){
            this.name = name;
        }
        public void setJoinedAt(long joinedAt) {
            this.joinedAt = joinedAt;
        }
        public void setState(String state) {
            this.state = state;
        }

        public long getEliminationTime() { return eliminationTime; }
        public void setEliminationTime(long eliminationTime) { this.eliminationTime = eliminationTime; }
    }

    public String getHostId(){ return hostId; }
    public void setHostId(String hostId){ this.hostId = hostId; }

    public String getStatus() { return status; }
    public void setStatus(String status){ this.status = status;}

    public Map<String, PlayerInfo> getPlayers(){ return players;}
    public void setPlayers(Map<String, PlayerInfo> players){ this.players = players; }

    public List<String> getTurnOrder() { return turnOrder; }
    public void setTurnOrder(List<String> turnOrder) { this.turnOrder = turnOrder; }



//  public List<String> getPlayerNames() { return playerNames; }
//  public List<String> getPlayerIds() { return playerIds; }

    public List<Integer> getSequence() { return sequence; }
    public void setSequence(List<Integer> sequence) { this.sequence = sequence; }

    public String getCurrentTurnUid() { return currentTurnUId; }
    public void setCurrentTurnUid(String currentTurnUid) { this.currentTurnUId = currentTurnUid; }


 //   public List<Integer> getActivePlayerIds() { return activePlayerIds; }
//    public void setActivePlayerIds(List<Integer> activePlayerIds) { this.activePlayerIds = activePlayerIds; }

//    public int getCurrentPlayerIndex() { return currentPlayerIndex; }
//    public void setCurrentPlayerIndex(int currentPlayerIndex) { this.currentPlayerIndex = currentPlayerIndex; }

    public int getButtonsUnlocked() { return buttonsUnlocked; }
//    public void setButtonsUnlocked(int buttonsUnlocked) { this.buttonsUnlocked = buttonsUnlocked; }
//
//    public long getLastUpdateTimestamp() { return lastUpdateTimestamp; }
//    public void setLastUpdateTimestamp(long lastUpdateTimestamp) { this.lastUpdateTimestamp = lastUpdateTimestamp; }

}