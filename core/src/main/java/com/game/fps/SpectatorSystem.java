package com.game.fps;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.Vector3;
import com.game.fps.net.NetworkPlayer;
import com.game.fps.net.NetworkWorld;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class SpectatorSystem {
    private final WeakReference<NetworkWorld> worldRef; // 메모리 leak 방지
    private final NetworkWorld world;
    private NetworkPlayer currentTarget;
    private List<NetworkPlayer> spectateTargets;
    private int currentTargetIndex;
    private boolean isSpectating;
    private final Vector3 spectatorPosition;
    private final Vector3 spectatorDirection;
    private String teamName;  // 팀 이름 저장용 필드 추가

    public SpectatorSystem(NetworkWorld world) {
        this.worldRef = new WeakReference<>(world);
        this.world = world;
        this.spectateTargets = new ArrayList<>();
        this.spectatorPosition = new Vector3();
        this.spectatorDirection = new Vector3();
        this.isSpectating = false;
        this.teamName = world.returnTeamName(); // 팀 이름 초기화
    }

    public void startSpectating() {
        if (world.getPlayer().isDead()) {
            Gdx.app.log("SpectatorSystem", "Attempting to start spectating...");
            updateSpectateTargets();

            if (!spectateTargets.isEmpty()) {
                isSpectating = true;
                currentTargetIndex = 0;
                currentTarget = spectateTargets.get(0);
                Gdx.app.log("SpectatorSystem", "Started spectating: " + currentTarget.getPlayerName());
            }
            else {
                Gdx.app.log("SpectatorSystem", "No valid spectate targets found");
            }
        }
        else {
            Gdx.app.log("SpectatorSystem", "Cannot start spectating - player is not dead or null");
        }
    }

    public void updateSpectateTargets() {
        NetworkWorld world = worldRef.get();
        if (world == null) return;

        spectateTargets.clear();

        // 임시 리스트 사용하여 가비지 생성 최소화
        List<NetworkPlayer> tempTargets = new ArrayList<>();
        for (NetworkPlayer player : world.getNetworkPlayers()) {
            if (!player.getGameObject().isDead() && player.getTeam().equals(teamName)) {
                tempTargets.add(player);
            }
        }

        // 타겟이 변경된 경우에만 리스트 업데이트
        if (!spectateTargets.equals(tempTargets)) {
            spectateTargets.clear();
            spectateTargets.addAll(tempTargets);

            // 현재 타겟이 리스트에 없으면 첫 번째 타겟으로 변경
            if (!spectateTargets.contains(currentTarget) && !spectateTargets.isEmpty()) {
                currentTargetIndex = 0;
                currentTarget = spectateTargets.get(0);
            }
        }

    }

    public void switchTarget(boolean next) {
        if (!isSpectating || spectateTargets.isEmpty()) return;

        if (next) {
            currentTargetIndex = (currentTargetIndex + 1) % spectateTargets.size();
        } else {
            currentTargetIndex = (currentTargetIndex - 1 + spectateTargets.size()) % spectateTargets.size();
        }
        currentTarget = spectateTargets.get(currentTargetIndex);
        Gdx.app.log("SpectatorSystem", "Switched to spectating: " + currentTarget.getPlayerName());
    }

    public void update(float deltaTime) {
        NetworkWorld world = worldRef.get();

        if (world == null) return;

        if (!isSpectating) return;

        // 키 입력 처리를 한 번만 체크
        boolean leftPressed = Gdx.input.isKeyJustPressed(Input.Keys.LEFT);
        boolean rightPressed = Gdx.input.isKeyJustPressed(Input.Keys.RIGHT);

        if (leftPressed || rightPressed) {
            switchTarget(rightPressed);
        }

        // 관전 대상의 위치와 방향 업데이트
        if (currentTarget != null) {
            GameObject targetObj = currentTarget.getGameObject();
            if (targetObj != null) {
                spectatorPosition.set(targetObj.getPosition()).add(0, Settings.eyeHeight, 0);
                spectatorDirection.set(targetObj.getDirection());
            }
        }

        // 메모리 최적화를 위해 타겟 목록 업데이트 주기 조절
        if ((int)(deltaTime * 1000) % 500 == 0) { // 0.5초마다 업데이트
            updateSpectateTargets();
        }
    }



    public void stopSpectating() {
        isSpectating = false;
        currentTarget = null;
        spectateTargets.clear();
    }

    public boolean isSpectating() {
        return isSpectating;
    }

    public Vector3 getSpectatorPosition() {
        return spectatorPosition;
    }

    public Vector3 getSpectatorDirection() {
        return spectatorDirection;
    }

    public NetworkPlayer getCurrentTarget() {
        return currentTarget;
    }
}
