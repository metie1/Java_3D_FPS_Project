package com.game.fps.inputs;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.IntIntMap;
import com.game.fps.*;
import com.game.fps.bomb.BombManager;
import com.game.fps.gui.GUI;
import com.game.fps.net.NetworkClient;
import com.game.fps.net.NetworkWorld;
import com.game.fps.physics.PhysicsRayCaster;
import com.game.fps.animation.AnimationController;

public class PlayerController extends InputAdapter  {
    private boolean inputEnabled; // 추가
    public int forwardKey = Input.Keys.W;
    public int backwardKey = Input.Keys.S;
    public int strafeLeftKey = Input.Keys.A;
    public int strafeRightKey = Input.Keys.D;
    public int turnLeftKey = Input.Keys.Q;
    public int turnRightKey = Input.Keys.E;
    public int jumpKey = Input.Keys.SPACE;
    public int runShiftKey = Input.Keys.SHIFT_LEFT;
    public int switchWeaponKey = Input.Keys.TAB;
    public int toggleFireModeKey = Input.Keys.C;
    private boolean isCrouching = false;

    private final World world;
    private final IntIntMap keys = new IntIntMap();
    public final Vector3 linearForce;
    private final Vector3 forwardDirection;   // 플레이어가 바라보는 방향, 이동 방향, XZ 평면에서
    private final Vector3 viewingDirection;   // 시선 방향, forwardDirection에 Y 성분 추가
    private float mouseDeltaX;
    private float mouseDeltaY;
    private final Vector3 groundNormal = new Vector3();
    private final Vector3 tmp = new Vector3();
    private final Vector3 tmp2 = new Vector3();
    private final Vector3 tmp3 = new Vector3();
    private final PhysicsRayCaster.HitPoint hitPoint = new PhysicsRayCaster.HitPoint();
    private final Vector2 stickMove = new Vector2();
    private final Vector2 stickLook = new Vector2();
    private boolean isRunning;
    private float stickViewAngle; // 위아래 각도
    private final AnimationController animationController;

    private boolean isFiring; // 연사 상태를 저장
    private float fireTimer; // 다음 발사까지 남은 시간
    private final float fireRate = 0.1f; // 발사 간격 (초 단위, 예: 0.1초 = 10연사/초)
    private boolean isAutoFire = false; // 연발 모드 여부 저장 (true: 연발, false: 단발)

    private BombManager bombManager;
    // 폭탄 배치 충전 변수
    private float bombCharge = 0;
    private final float bombChargeTime = 2.0f; // 폭탄 완전 충전에 2초 소요
    private GUI gui;
    private NetworkClient networkClient;
    private NetworkWorld networkWorld;
    private GameScreen gameScreen;

    private boolean isBombPlanted = false;


    private boolean isCharging = false;

    public PlayerController(World world)  {
        animationController = new AnimationController();
        this.world = world;
        // BombManager 초기화 및 BombSite 추가
        bombManager = new BombManager();
        // 첫 번째 BombSite: 높이 0.21895f
        bombManager.addBombSite(
            new Vector3(-29.589f, 0, -297.25f),
            new Vector3(-56.786f, 0, -324.53f),
            0.21895f
        );
        // 두 번째 BombSite: 높이 -11.4853f
        bombManager.addBombSite(
            new Vector3(203.15f, 0, -172.26f),
            new Vector3(226.89f, 0, -196.23f),
            -10f
        );

        linearForce = new Vector3();
        forwardDirection = new Vector3();
        viewingDirection = new Vector3();
        reset();
        inputEnabled = true; // 초기화 추가
        this.isFiring = false;
        this.fireTimer = 0;
    }


    public void setInputEnabled(boolean enabled) {
        this.inputEnabled = enabled;

        if (!enabled) {
            keys.clear();  // 입력이 비활성화될 때 키 상태 초기화
        }
    }
    // setter
    public void setGameScreen(GameScreen gameScreen) {
        this.gameScreen = gameScreen;
    }

    public void setGUI(GUI gui) {
        this.gui = gui;
    }

    public void setNetworkWorld(NetworkWorld networkWorld) {
        this.networkWorld = networkWorld;
    }

    public void setNetworkClient(NetworkClient networkClient) {
        this.networkClient = networkClient;
    }


    public void reset() {
        forwardDirection.set(0,0,1);
        viewingDirection.set(forwardDirection);
    }

    public Vector3 getViewingDirection() {
        return viewingDirection;
    }

    public Vector3 getForwardDirection() {
        return forwardDirection;
    }

    @Override
    public boolean keyDown (int keycode) {
        if (!inputEnabled) {
            keys.clear();  // 입력이 비활성화될 때 키 상태 초기화
            return false;
        }
        keys.put(keycode, keycode);

        return true;
    }

    @Override
    public boolean keyUp (int keycode) {
        if (!inputEnabled)
            return false;
        keys.remove(keycode, 0);
        if (keycode == switchWeaponKey)
            world.weaponState.switchWeapon();
        if (keycode == toggleFireModeKey && !(networkWorld.weaponState.currentWeaponType == WeaponType.PISTOL||
            networkWorld.weaponState.currentWeaponType == WeaponType.KNIFE)) { // C 키로 연발/단발 모드 전환

            isAutoFire = !isAutoFire;
            if (gui != null) {
                gui.updateCrosshairMode(isAutoFire); // GUI에 연사 모드 전달
            }
            Gdx.app.log("PlayerController", isAutoFire ? "Auto-fire mode enabled" : "Single-fire mode enabled");
        }

        // 2번 또는 3번 키 입력 시 크로스헤어를 텍스트 모드로 전환
        if (keycode == Input.Keys.NUM_2 || keycode == Input.Keys.NUM_3) {
            if (gui != null) {
                gui.updateCrosshairMode(false); // 텍스트 크로스헤어 활성화
            }
            Gdx.app.log("PlayerController", "Switched to text crosshair mode");
        }
        return true;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        if (!inputEnabled) return false;


        if (button == Input.Buttons.LEFT) {
            if (!isAutoFire || networkWorld.weaponState.currentWeaponType == WeaponType.PISTOL
                || networkWorld.weaponState.currentWeaponType == WeaponType.KNIFE) {
                fireWeapon(); // 단발 모드에서는 즉시 발사
            } else {
                isFiring = true; // 연발 모드에서는 발사 상태 활성화
            }
            if (networkWorld.weaponState.currentWeaponType == WeaponType.BOMB && networkWorld.getPlayer() != null) {
                isCharging = true; // 충전 시작
            }

        }

        if(button == Input.Buttons.RIGHT)
            setScopeMode(true);
        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        if (!inputEnabled) return false;
        if (button == Input.Buttons.LEFT){
            isCharging = false; // 충전 중지
            isFiring = false;
            bombCharge = 0;
        }

        if(button == Input.Buttons.RIGHT)
            setScopeMode(false);
        return false;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        if (!inputEnabled) return false;
        mouseDeltaX = -Gdx.input.getDeltaX() * Settings.degreesPerPixel * 0.2f;
        mouseDeltaY = -Gdx.input.getDeltaY() * Settings.degreesPerPixel * 0.2f;
        return false;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        if (!inputEnabled) return false;
        if(Math.abs(Gdx.input.getDeltaX()) >= 100 && Math.abs(Gdx.input.getDeltaY()) >= 100)
            return true;
        mouseDeltaX = -Gdx.input.getDeltaX() * Settings.degreesPerPixel;
        mouseDeltaY = -Gdx.input.getDeltaY() * Settings.degreesPerPixel;
        return true;
    }

    public void setScopeMode(boolean mode){
        world.weaponState.scopeMode = mode;
    }

    public void fireWeapon() {
        if (!world.weaponState.canShoot()) {
            return;
        }

        float maxRange = 1f;
        float spread = 0.05f;
        /*
        if(world.weaponState.currentWeaponType == WeaponType.GUN)
            maxRange = 100f;
        */

        // 기본 발사 방향을 복사
        Vector3 randomizedDirection = new Vector3(viewingDirection);

        // 연발 모드일 경우 방향에 랜덤 요소 추가
        if (isAutoFire) {
            randomizedDirection.x += (float) (Math.random() * 2 - 1) * spread; // -spread ~ +spread
            randomizedDirection.y += (float) (Math.random() * 2 - 1) * spread;
            randomizedDirection.z += (float) (Math.random() * 2 - 1) * spread;
            randomizedDirection.nor(); // 방향 벡터를 정규화
        }
        if(world.weaponState.currentWeaponType == WeaponType.PISTOL ||
            world.weaponState.currentWeaponType == WeaponType.AK47 ||
            world.weaponState.currentWeaponType == WeaponType.SG553 ||
            world.weaponState.currentWeaponType == WeaponType.AR15) {
            maxRange = 100f;
            world.weaponState.shoot(); // 발사 후 탄약 감소
        }
        else if(world.weaponState.currentWeaponType == WeaponType.KNIFE)
            maxRange = 2.0f;

        world.rayCaster.findTarget(world.getPlayer().getPosition(), viewingDirection, hitPoint,maxRange);
        world.fireWeapon(viewingDirection, hitPoint );
    }

    public void setRunning( boolean mode ){
        isRunning = mode;
    }

    private void rotateView( float deltaX, float deltaY ) {
        viewingDirection.rotate(Vector3.Y, deltaX);

        if (!Settings.freeLook) {    // 카메라 움직임을 수평 평면에 유지
            viewingDirection.y = 0;
            return;
        }
        if (Settings.invertLook)
            deltaY = -deltaY;

        // 위나 아래를 볼 때 짐벌락을 피하기 위해
        Vector3 oldPitchAxis = tmp.set(viewingDirection).crs(Vector3.Y).nor();
        Vector3 newDirection = tmp2.set(viewingDirection).rotate(tmp, deltaY);
        Vector3 newPitchAxis = tmp3.set(tmp2).crs(Vector3.Y);
        if (!newPitchAxis.hasOppositeDirection(oldPitchAxis))
            viewingDirection.set(newDirection);
    }

    public void moveForward( float distance ){
        linearForce.set(forwardDirection).scl(distance);
    }

    private void strafe( float distance ){
        tmp.set(forwardDirection).crs(Vector3.Y);   // 벡터의 외적
        tmp.scl(distance);
        linearForce.add(tmp);
    }
    public BombManager getBombManager() {
        return bombManager;
    }

    public void update (GameObject player, float deltaTime ) {
        if (networkWorld.roundSystem.getRoundState() == RoundSystem.RoundState.WAITING) {
            setInputEnabled(false); // RoundSystem이 WAITING 상태일 때 입력 비활성화
            keys.clear();           // 키 상태 초기화
            return;
        } else {
            setInputEnabled(true); // 다른 상태일 때 입력 활성화
        }
        // 연발 모드 처리
        if (isAutoFire && isFiring) {
            fireTimer -= deltaTime;
            if (fireTimer <= 0) {
                fireWeapon();
                fireTimer = fireRate; // 타이머 초기화
            }
        }

        if (!inputEnabled || player.isDead()) {
            keys.clear();  // 확실히 키 상태 초기화
            return;
        }

        if (isCharging) {
            bombCharge += deltaTime; // 충전 진행
            gui.showBombProgressBar(true); // 게이지 표시
            gui.bombProgressBar.setValue(bombCharge / bombChargeTime); // 게이지 업데이트
        }

        if(!networkWorld.roundSystem.getBombPlanted()){
            // 충전 완료 여부 확인
            if (bombCharge >= bombChargeTime) {
                Vector3 playerPosition = networkWorld.getPlayer().getPosition(); // 플레이어 위치 가져오기
                boolean success = bombManager.placeBomb(playerPosition, networkWorld);

                if (success) {
                    Gdx.app.log("PlayerController", "Bomb successfully placed.");
                    networkWorld.roundSystem.plantBomb();

                    if (networkClient != null) {
                        try {
                            networkClient.sendBombPlanted(true);
                            networkClient.sendBombPosition(playerPosition);
                        }catch (Exception e) {
                            Gdx.app.error("PlayerController", "Error while placing the bomb", e);
                        }
                    }
                } else {
                    Gdx.app.log("PlayerController", "Failed to place bomb.");
                }
            }
        }


        if(player.isDead())
            return;


        // 플레이어의 회전 상태 업데이트
        Quaternion rotation = new Quaternion();
        // Z축과 viewingDirection 사이의 각도 계산
        float dotProduct = viewingDirection.dot(Vector3.Z);
        float angle = (float) Math.toDegrees(Math.acos(dotProduct / viewingDirection.len()));
        // 왼쪽/오른쪽 회전 방향 결정
        Vector3 cross = new Vector3();
        cross.set(Vector3.Z).crs(viewingDirection);
        if (cross.y < 0) {
            angle = 360 - angle;
        }
        rotation.setFromAxis(Vector3.Y, angle);
        player.body.setOrientation(rotation);


        // 시선 방향으로부터 전방 방향 벡터 유도
        forwardDirection.set(viewingDirection);
        forwardDirection.y = 0;
        forwardDirection.nor();

        // 속도 초기화
        linearForce.set(0,0,0);

        boolean isOnGround = world.rayCaster.isGrounded(player, player.getPosition(), Settings.groundRayLength, groundNormal);

        // 플레이어가 경사면에 있을 경우 중력 비활성화
        if(isOnGround) {
            float groundDot = groundNormal.dot(Vector3.Y); // 변수명을 groundDot으로 변경
            player.body.geom.getBody().setGravityMode(groundDot >= 0.99f);
            Settings.walkSpeed=100f;
        } else {
            player.body.geom.getBody().setGravityMode(true);
            linearForce.y =  -(deltaTime * 5000f);
            Settings.walkSpeed=50f;
            player.body.applyForce(linearForce);

        }

        float moveSpeed = Settings.walkSpeed;
        if(isRunning || keys.containsKey(runShiftKey))
            moveSpeed *= Settings.runFactor;

        // 마우스로 시선 방향 회전
        rotateView(mouseDeltaX*Settings.turnSpeed/60f, mouseDeltaY*Settings.turnSpeed/60f );
        mouseDeltaX = 0;
        mouseDeltaY = 0;

        // 컨트롤러 스틱 입력
        moveForward(stickMove.y*deltaTime * moveSpeed);
        strafe(stickMove.x * deltaTime * Settings.walkSpeed);
        float delta = 0;
        float speedFactor;
        if(world.weaponState.scopeMode) {
            speedFactor = 0.2f;
            delta = (stickLook.y * 30f );
        }
        else {
            speedFactor = 1f;
            delta = (stickLook.y * 90f - stickViewAngle);
        }
        delta *= deltaTime*Settings.verticalReadjustSpeed*speedFactor;
        stickViewAngle += delta;
        rotateView(stickLook.x * deltaTime * Settings.turnSpeed*speedFactor,  delta );

        // 팀에 따른 애니메이션 접미사 설정
        String animSuffix = networkWorld.returnTeamName().equals("Team_A") ? "" : ".agent";

        // 기본 애니메이션 설정
        String desiredAnimation = "p_idle" + animSuffix;
        boolean isLooping = true;

        if (keys.containsKey(forwardKey))
            moveForward(deltaTime * moveSpeed);
        if (keys.containsKey(backwardKey))
            moveForward(-deltaTime * moveSpeed);
        if (keys.containsKey(strafeLeftKey))
            strafe(-deltaTime * Settings.walkSpeed);
        if (keys.containsKey(strafeRightKey))
            strafe(deltaTime * Settings.walkSpeed);
        if (keys.containsKey(turnLeftKey))
            rotateView(deltaTime * Settings.turnSpeed, 0);
        if (keys.containsKey(turnRightKey))
            rotateView(-deltaTime * Settings.turnSpeed, 0);

        if (isOnGround && keys.containsKey(jumpKey) )
            linearForce.y =  Settings.jumpForce * deltaTime * 1000f;
        if(world.weaponState.currentWeaponType == WeaponType.AK47 ||
            world.weaponState.currentWeaponType == WeaponType.SG553 ||
            world.weaponState.currentWeaponType == WeaponType.AR15) {
            if (Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT)) {
                desiredAnimation = "r_crouch_idle" + animSuffix;
                if (Gdx.input.isKeyPressed(Input.Keys.W)) {
                    desiredAnimation = "r_crouch_front" + animSuffix;
                } else if (Gdx.input.isKeyPressed(Input.Keys.S)) {
                    desiredAnimation = "r_crouch_back" + animSuffix;
                } else if (Gdx.input.isKeyPressed(Input.Keys.A)) {
                    desiredAnimation = "r_crouch_left" + animSuffix;
                } else if (Gdx.input.isKeyPressed(Input.Keys.D)) {
                    desiredAnimation = "r_crouch_right" + animSuffix;
                }
                if (!isCrouching) { // 상태가 변경될 경우에만 전송
                    isCrouching = true;
                    networkWorld.getPlayer().isCrouch = isCrouching;
                }
            } else {
                if (Gdx.input.isKeyPressed(Input.Keys.W)) {
                    desiredAnimation = "r_walk_front" + animSuffix;
                } else if (Gdx.input.isKeyPressed(Input.Keys.S)) {
                    desiredAnimation = "r_walk_back" + animSuffix;
                } else if (Gdx.input.isKeyPressed(Input.Keys.A)) {
                    desiredAnimation = "r_walk_left" + animSuffix;
                } else if (Gdx.input.isKeyPressed(Input.Keys.D)) {
                    desiredAnimation = "r_walk_right" + animSuffix;
                }
                if (isCrouching) { // 상태가 변경될 경우에만 전송
                    isCrouching = false;
                    networkWorld.getPlayer().isCrouch = isCrouching;
                }
            }
        }
        else if (Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT)) {
            desiredAnimation = "p_crouch_idle" + animSuffix;
            if (Gdx.input.isKeyPressed(Input.Keys.W)) {
                desiredAnimation = "p_crouch_front" + animSuffix;
            } else if (Gdx.input.isKeyPressed(Input.Keys.S)) {
                desiredAnimation = "p_crouch_back" + animSuffix;
            } else if (Gdx.input.isKeyPressed(Input.Keys.A)) {
                desiredAnimation = "p_crouch_left" + animSuffix;
            } else if (Gdx.input.isKeyPressed(Input.Keys.D)) {
                desiredAnimation = "p_crouch_right" + animSuffix;
            }
            if (!isCrouching) { // 상태가 변경될 경우에만 전송
                isCrouching = true;
                networkWorld.getPlayer().isCrouch = isCrouching;
            }
        } else {
            if (Gdx.input.isKeyPressed(Input.Keys.W)) {
                desiredAnimation = "p_walk_front" + animSuffix;
            } else if (Gdx.input.isKeyPressed(Input.Keys.S)) {
                desiredAnimation = "p_walk_back" + animSuffix;
            } else if (Gdx.input.isKeyPressed(Input.Keys.A)) {
                desiredAnimation = "p_walk_left" + animSuffix;
            } else if (Gdx.input.isKeyPressed(Input.Keys.D)) {
                desiredAnimation = "p_walk_right" + animSuffix;
            }
            if (isCrouching) { // 상태가 변경될 경우에만 전송
                isCrouching = false;
                networkWorld.getPlayer().isCrouch = isCrouching;
            }
        }

        // 현재 애니메이션과 다를 경우에만 새로운 애니메이션 설정
        try {
            if (!animationController.getCurrentAnimation().equals(desiredAnimation)) {
                Gdx.app.log("PlayerController", "Setting animation: " + desiredAnimation);
                animationController.setAnimation(desiredAnimation, isLooping, player);
            }
        } catch (Exception e) {
            Gdx.app.error("PlayerController", "Error setting animation: " + desiredAnimation, e);
        }

        linearForce.scl(120);
        player.body.applyForce(linearForce);
    }



}
