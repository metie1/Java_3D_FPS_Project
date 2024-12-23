// NetworkPlayer.java
package com.game.fps.net;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.game.fps.GameObject;
import com.game.fps.WeaponState;
import com.game.fps.WeaponType;
import com.game.fps.net.message.PlayerStateMessage;

public class NetworkPlayer {
    // 보간 속도를 더 낮게 조정
    private static final float INTERPOLATION_SPEED = 5f;  // 10f에서 5f로 변경
    private static final float POSITION_THRESHOLD = 0.05f; // 더 작은 임계값 사용

    private final int playerId;
    private final String playerName;
    private final GameObject gameObject;
    private final Vector3 targetPosition;
    private final Vector3 targetDirection;
    private float lastUpdateTime;
    private final String playerTeam;
    private boolean isCrouch;
    private boolean isFiring;
    private final Quaternion targetRotation;
    private final WeaponState weaponState;
    private WeaponType currentWeaponType;
    private WeaponType previousWeaponType;


    public NetworkPlayer(int playerId, String playerName, GameObject gameObject, String playerTeam,WeaponState weaponState) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.gameObject = gameObject;
        this.playerTeam = playerTeam;
        this.targetPosition = new Vector3();
        this.targetDirection = new Vector3();
        this.lastUpdateTime = 0;
        this.targetRotation = new Quaternion();
        this.isCrouch = false;
        this.isFiring = false;
        this.weaponState = weaponState;
        this.currentWeaponType = null;
        this.previousWeaponType = null;

        Gdx.app.log("NetworkPlayer", "Created player: " + playerName);
    }

    public String getTeam() {
        return playerTeam;
    }

    public boolean isCrouch() {
        return isCrouch;
    }

    public void setCrouch(boolean crouch){
        this.isCrouch = crouch;
    }

    public boolean isFiring() {
        return isFiring;
    }

    public void setFiring(boolean firing){
        this.isFiring = firing;
    }

    public void updateState(PlayerStateMessage msg, float currentTime) {
        targetPosition.set(msg.position);
        targetDirection.set(msg.direction);
        targetRotation.set(msg.rotation);
        gameObject.health = msg.health;

        if (!msg.currentAnimation.equals(gameObject.getCurrentAnimation())) {
            gameObject.setAnimation(msg.currentAnimation, -1);
        }
        lastUpdateTime = currentTime;
    }


    public void interpolate(float deltaTime) {
        // 부드러운 보간
        float alpha = Math.min(1.0f, deltaTime * INTERPOLATION_SPEED);


        // 회전 보간 (1)
        Quaternion currentRotation = gameObject.body.getBodyOrientation();
        Quaternion targetRotation;
        if (targetDirection.isZero()) {
            targetRotation = currentRotation;
        } else {
            // Z축과 targetDirection 사이의 각도 계산
            float dot = targetDirection.dot(Vector3.Z);
            float angle = (float) Math.toDegrees(Math.acos(dot / targetDirection.len()));
            // 왼쪽/오른쪽 회전 방향 결정
            Vector3 cross = new Vector3();
            cross.set(Vector3.Z).crs(targetDirection);
            if (cross.y < 0) {
                angle = 360 - angle;
            }
            targetRotation = new Quaternion().setFromAxis(Vector3.Y, angle);
        }
        currentRotation.slerp(targetRotation, alpha);
        gameObject.body.setOrientation(currentRotation);
    }

    public int getPlayerId() { return playerId; }
    public String getPlayerName() { return playerName; }
    public GameObject getGameObject() { return gameObject; }
    public Vector3 getPlayerPosition() {return gameObject.getPosition();}

    public void setCurrentWeaponType(WeaponType weaponType) {
        if(currentWeaponType != null) {
            this.previousWeaponType = this.currentWeaponType; // 이전 무
        }
        this.currentWeaponType = weaponType; // 새로운 무기로 설정
    }
    public WeaponType getCurrentWeaponType() {
        return currentWeaponType;
    }
    public WeaponType getPreviousWeaponType() {
        return previousWeaponType;
    }


}
