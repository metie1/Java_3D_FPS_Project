package com.game.fps;

import com.badlogic.gdx.Gdx;
import com.game.fps.gui.GUI;

import java.util.ArrayList;
import java.util.List;
import com.game.fps.net.NetworkClient;
import com.game.fps.net.message.StartRoundMessage;

public class RoundSystem {
    public static final int MAX_NORMAL_ROUNDS = 12;  // 전체 라운드 수 12로 변경
    public static final float PREP_TIME = 15f;      // 라운드 시작 전 준비 시간 (15초)
    public static final float ROUND_TIME = 120f; // 2분 = 120초
    public static final int PLAYERS_PER_TEAM = 5;

    public static final float BOMB_TIME = 25f;

    private int currentRound;
    private float roundTimer;
    private float displayTimer; // 표시용 타이머 추가
    private GameMode gameMode;
    private int teamAScore;
    private int teamBScore;
    private RoundState roundState;
    private float prepTimer;
    private final List<GameObject> teamA;
    private final List<GameObject> teamB;
    private final SpawnSystem spawnSystem;

    private GUI gui;
    public void setGUI(GUI gui) {
        this.gui = gui;
    }
    //폭탄
    private boolean bombPlanted = false;
    private float bombTimer;
    public boolean isPlayed = false;
    private GameObject player;
    private NetworkClient networkClient;

    int i= 0;
    public void setNetworkClient(NetworkClient networkClient) {
        this.networkClient = networkClient;
    }

    public void setBombPlanted(boolean b){
        this.bombPlanted = b;
    }

    public boolean getBombPlanted() {
        return bombPlanted;
    }
    public enum RoundState {
        WAITING,        // 라운드 시작 전 대기
        PREPARING,      // 준비 시간
        IN_PROGRESS, // 라운드 진행 중
        BOMB_PLANTED,
        ROUND_END,      // 라운드 종료
        GAME_END        // 게임 종료
    }

    public RoundSystem() {
        currentRound = 1;
        roundTimer = ROUND_TIME;
        prepTimer = PREP_TIME;
        bombTimer = BOMB_TIME;
        gameMode = GameMode.NORMAL;
        teamAScore = 0;
        teamBScore = 0;
        roundState = RoundState.WAITING;
        teamA = new ArrayList<>(PLAYERS_PER_TEAM);
        teamB = new ArrayList<>(PLAYERS_PER_TEAM);
        spawnSystem = new SpawnSystem();
    }

    // RoundSystem 클래스 내부
    public List<GameObject> getTeamA() {
        return teamA;
    }

    public List<GameObject> getTeamB() {
        return teamB;
    }

    public void setRoundState(RoundState state) {
        roundState = state;
    }

    public void plantBomb() {
        if (!bombPlanted) {
            bombPlanted = true;
            Gdx.app.log("RoundSystem", "Bomb planted! Timer reduction started.");
        }
    }

    public void defuseBomb() {
        bombPlanted = false;
    }

    public void startRound() {
        if (roundState != RoundState.WAITING) return;

        roundState = RoundState.PREPARING;
        prepTimer = PREP_TIME;
        roundTimer = ROUND_TIME;

        // StartRoundMessage 전송
        if (networkClient != null) {
            StartRoundMessage msg = new StartRoundMessage(currentRound);
            try {
                networkClient.sendStartRoundMessage(msg);
            }catch (Exception e) {   Gdx.app.error("PlayerController", "Error while placing the bomb", e);}

        }
        Gdx.app.log("RoundSystem", "Round " + currentRound + " preparation started");

        // 플레이어들 초기 위치로 리스폰
        respawnPlayers();
    }

    // 준비 시간을 스킵하고 바로 라운드 시작
    public void skipPreparationPhase() {
        if (roundState != RoundState.PREPARING) return;


        roundState = RoundState.IN_PROGRESS;
        roundTimer = ROUND_TIME;


        Gdx.app.log("RoundSystem", "Round " + currentRound + " started (preparation skipped)");
    }


    // 현재 라운드를 강제로 종료
    public void forceEndRound() {
        if (roundState != RoundState.IN_PROGRESS) return;

        roundTimer = 0;
        bombTimer = 0;
        endRound();


        Gdx.app.log("RoundSystem", "Round " + currentRound + " forcefully ended");
    }


    // 다음 라운드 시작
    public void startNextRound() {
        if (roundState == RoundState.ROUND_END) {

            startRound();
            Gdx.app.log("RoundSystem", "Starting round " + currentRound);
        }
    }

    // GUI에 표시할 디버그 정보 추가
    public String getDebugInfo() {
        return String.format("State: %s, Round: %d, Score: %d-%d",
            roundState.toString(), currentRound, teamAScore, teamBScore);
    }

    public void update(float deltaTime) {
        switch (roundState) {

            case WAITING:
                roundTimer = ROUND_TIME;
                bombTimer = BOMB_TIME;
                bombPlanted = false;
                break;

            case PREPARING:
                prepTimer -= deltaTime;
                displayTimer = ROUND_TIME; // 준비 시간에도 최대 시간 표시

                if (prepTimer <= 0) {
                    roundState = RoundState.IN_PROGRESS;
                    roundTimer = ROUND_TIME;
                    Gdx.app.log("RoundSystem", "Round " + currentRound + " started");
                }
                break;

            case IN_PROGRESS:

                if (roundTimer <= 0 || isTeamEliminated()) { //isTeamEliminated()
                    endRound();
                }

                if (!bombPlanted){
                    roundTimer -= deltaTime;
                    Gdx.app.log("RoundSystem", "B: "+countAlivePlayers(teamB));
                    Gdx.app.log("RoundSystem", "A: "+countAlivePlayers(teamA));
                }
                else{
                    roundTimer = bombTimer;
                    if(roundTimer <= 30){
                        if(!isPlayed){
                            Main.assets.sounds.BOMB_SOUND.play();
                            isPlayed = true;
                        }
                    }
//                    Gdx.app.log("RoundSystem", "count players: "+countAlivePlayers(teamB));
                }

                break;

            case ROUND_END:
                // 다음 라운드 준비 또는 게임 종료
                displayTimer = 0; // 라운드 종료시 0으로 표시

                break;

            case GAME_END:
                // 게임 종료 처리
                displayTimer = 0;
                break;
        }

        if(bombPlanted){
            bombTimer -= deltaTime;
        }
    }

    // 시간 형식을 "2:00" 형식으로 반환하는 메서드
    public String getFormattedTime() {
        int minutes = (int)(displayTimer / 60f);
        int seconds = (int)(displayTimer % 60f);
        return String.format("%d:%02d", minutes, seconds);
    }

    // 현재 라운드 상태와 시간을 문자열로 반환
    public String getRoundStatusText() {
        String baseText = "Round " + currentRound;
        switch (roundState) {
            case WAITING:
                return baseText + " - Waiting";
            case PREPARING:
                return baseText + " - Prepare (" + String.format("%.0f", prepTimer) + ")";
            case IN_PROGRESS:
                if(bombPlanted)
                    return baseText + "BOMBPLANTED!";
                else{
                    return baseText;
                }
            case ROUND_END:
                return baseText + " - End";
            case GAME_END:
                return "Game Over";
            default:
                return baseText;
        }
    }

    public void BombDefuseEnd(){
        roundTimer =0;
        bombTimer = 0;
        bombPlanted = false;
    }

    private void endRound() {
        roundState = RoundState.ROUND_END;

        // 라운드 승자 결정
        determineRoundWinner();

        // 다음 라운드로 진행 또는 게임 종료
        if (shouldEndGame()) {
            roundState = RoundState.GAME_END;
            endGame();
        }
    }

    private void determineRoundWinner() {
        int aliveTeamA = countAlivePlayers(teamA);
        int aliveTeamB = countAlivePlayers(teamB);

        if (aliveTeamA > aliveTeamB) {
            teamAScore++;
            if (teamAScore >= 7) {
                gui.showGameOverMessage("Game Over - Team A wins!");
                roundState = RoundState.GAME_END;
                return;
            }
            gui.showGameOverMessage("ALL KILL - Team A wins!");
        } else if (aliveTeamB > aliveTeamA) {
            teamBScore++;
            if (teamBScore >= 7) {
                gui.showGameOverMessage("Game Over - Team B wins!");
                roundState = RoundState.GAME_END;
                return;
            }
            gui.showGameOverMessage("ALL KILL - Team B wins!");
        } else if (roundTimer <= 0) {
            // 시간 초과시 수비팀(B팀) 승리
            if(bombPlanted){
                try {
                    networkClient.sendBombDefusedMessage();
                    networkClient.getNetworkWorld().removeBomb();
                } catch (Exception e) {
                    Gdx.app.error("PlayerController", "Error while placing the bomb", e);
                }
                teamAScore++;
                if (teamAScore >= 7) {
                    gui.showGameOverMessage("Game Over - Team A wins!");
                    roundState = RoundState.GAME_END;
                    return;
                }
                gui.showGameOverMessage("Time out - Team A wins (Bomb detonated)!");
            }
            else{
                teamBScore++;
                if (teamBScore >= 7) {
                    gui.showGameOverMessage("Game Over - Team B wins!");
                    roundState = RoundState.GAME_END;
                    return;
                }
                gui.showGameOverMessage("Time out - Team B wins (Bomb defused)!");
            }

            Gdx.app.log("RoundSystem", "Time out - Team B wins round " + currentRound);
        }
        currentRound++;
    }



    private boolean shouldEndGame() {
        // 한 팀이 7라운드 승리하면 게임 종료
        if (teamAScore >= 7 || teamBScore >= 7) return true;

        // 최대 라운드에 도달하면 게임 종료
        if (currentRound >= MAX_NORMAL_ROUNDS) return true;

        return false;
    }

    public RoundState getRoundState() {
        return roundState;
    }

    public float getPrepTimeRemaining() {
        return prepTimer;
    }

    public boolean isPreparationPhase() {
        return roundState == RoundState.PREPARING;
    }

    private boolean isGameDecided() {
        return Math.abs(teamAScore - teamBScore) > 0;
    }

    private void endGame() {
        Gdx.app.log("RoundSystem", "Game ended - Final score: Team A " + teamAScore + " - Team B " + teamBScore);
        // TODO: 게임 종료 처리 (결과 화면 표시 등)
    }

    private boolean isTeamEliminated() {
        return countAlivePlayers(teamA) == 0 || countAlivePlayers(teamB) == 0;
    }

    private int countAlivePlayers(List<GameObject> team) {
        int count = 0;
        for (GameObject player : team) {
            if (!player.isDead()) {
                count++;
            }
        }
        Gdx.app.log("RoundSystem",""+i);
        return count;
    }

    private void respawnPlayers() {
        // TODO: 각 팀 플레이어들을 미리 지정된 스폰 포인트로 이동
        // 팀별 스폰 포인트 필요
    }

    // Getters
    public int getCurrentRound() { return currentRound; }
    public float getRoundTimeRemaining() {return roundTimer;}
    public int getTeamAScore() { return teamAScore; }
    public int getTeamBScore() { return teamBScore; }
    public float getBombTimeRemaining() {return bombTimer;}
}
