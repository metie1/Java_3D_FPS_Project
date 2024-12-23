package com.game.fps;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.math.Vector3;

import com.badlogic.gdx.math.MathUtils;

import com.game.fps.bomb.BombManager;
import com.game.fps.gui.GUI;
import com.game.fps.gui.SettingsOverlay;
import com.game.fps.gui.ShopOverlay;
import com.game.fps.inputs.PlayerController;
import com.game.fps.physics.CollisionShapeType;
import com.game.fps.views.GameView;
import com.game.fps.views.GridView;
import com.game.fps.physics.PhysicsView;


import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.game.fps.net.NetworkClient;
import com.game.fps.net.NetworkWorld;

import static com.game.fps.net.NetworkConstants.DEFAULT_HOST;


public class GameScreen extends ScreenAdapter {
    private SettingsOverlay settingsOverlay;
    private ShopOverlay shopOverlay;
    private GameView gameView;
    private GridView gridView;
    private GameView gunView;
    private PhysicsView physicsView;
    private ScopeOverlay scopeOverlay;


    // private World world;
    private NetworkWorld world;  // World 대신 NetworkWorld 사용
    private NetworkClient networkClient;


    private World gunWorld;
    private GameObject gun;
    private GUI gui;
    private boolean debugRender = false;
    private boolean thirdPersonView = false;
    private boolean navScreen = false;
    private boolean lookThroughScope = false;
    private int windowedWidth, windowedHeight;
    private String gunname="KnifeArmature";
    private String currentfire = "stab";
    private PlayerController playerController;

    private TeamSelectionScreen.Team playerTeam;
    private boolean teamSent = false;
    private boolean weaponSwitchedAfterBombPlanted = false;

    public BombManager bombManager;
    // 폭탄 관련 요소
    private float bombCharge = 0;
    private final float bombChargeTime = 2.0f; // 폭탄 완전 충전에 2초 소요
    public void setPlayerTeam(TeamSelectionScreen.Team team) {
        this.playerTeam = team;
    }
    private final float bombDefuseRange = 5.0f; // 폭탄 해체 범위
    private final float bombDefuseTime = 2.0f; // 폭탄 해체 시간
    private float defuseProgress = 0;
    private boolean isDefusing = false;
    private String teamString;
    // 팀에 따라 다른 스폰 위치 설정
    private void spawnPlayer() {
        Vector3 spawnPosition;
        String playerModel;     // 플레이어 모델 선택용 변수 추가
        String animationName;   // 애니메이션 이름도 모델별로 다르게 설정

        if (playerTeam == TeamSelectionScreen.Team.TEAM_A) {
            spawnPosition = world.spawnSystem.getRandomSpawnPoint(true);
            teamString = "Team_A";
            playerModel = "player01";  // 테러리스트 모델
            animationName = "p_idle";  // player01용 애니메이션
            Gdx.app.log("GameScreen", "Spawning Terrorist with model: " + playerModel);
        } else {
            spawnPosition = world.spawnSystem.getRandomSpawnPoint(false);
            teamString = "Team_B";
            playerModel = "player02";        // 대테러리스트 모델
            animationName = "p_idle.agent";  // player02용 애니메이션
            Gdx.app.log("GameScreen", "Spawning Counter-Terrorist with model: " + playerModel);
        }

        try {
            // 선택된 모델로 플레이어 스폰
            GameObject go = world.spawnObject(
                GameObjectType.TYPE_PLAYER,
                playerModel,  // 선택된 모델 사용
                "playerProxy",
                CollisionShapeType.CAPSULE,
                true,
                spawnPosition
            );

            if (go != null && go.scene != null) {
                world.setPlayer(go);

                // go.setAnimation(animationName, -1);  // 해당 모델에 맞는 애니메이션 설정

                // 애니메이션 컨트롤러가 있는지 확인
                if (go.animationController != null) {
                    try {
                        go.setAnimation(animationName, -1);
                        Gdx.app.log("GameScreen", "Animation set: " + animationName);
                    } catch (Exception e) {
                        Gdx.app.error("GameScreen", "Failed to set animation: " + e.getMessage());
                    }
                } else {
                    Gdx.app.error("GameScreen", "Animation controller is null");
                }

                // 팀 리스트에 추가
                if (playerTeam == TeamSelectionScreen.Team.TEAM_A) {
                    world.roundSystem.getTeamA().add(go); // teamA 리스트에 추가
                    Gdx.app.log("GameScreen", "Added to Team A (Terrorist)");
                } else {
                    world.roundSystem.getTeamB().add(go); // teamB 리스트에 추가
                    Gdx.app.log("GameScreen", "Added to Team B (Counter-Terrorist)");
                }

                Gdx.app.log("GameScreen", "Player spawned successfully at: " + spawnPosition);
            } else {
                Gdx.app.error("GameScreen", "Failed to create player object");
            }
        } catch (Exception e) {
            Gdx.app.error("GameScreen", "Error spawning player: " + e.getMessage(), e);
        }
    }

    @Override
    public void show() {
        try {

            Gdx.app.log("GameScreen", "Starting initialization...");

            // GUI 초기화를 먼저 수행
            gui = new GUI(null, this);  // world는 나중에 설정

            // NetworkClient 초기화
            String playerName = "Player" + MathUtils.random(1000);
            Gdx.app.log("GameScreen", "Creating NetworkClient with name: " + playerName);
            networkClient = new NetworkClient(playerName);

            // world = new World();

            // NetworkWorld 초기화
            world = new NetworkWorld();
            Gdx.app.log("GameScreen", "NetworkWorld created");

            // 게임 객체 초기화
            Gdx.app.log("GameScreen", "Populating world");
            Populator.populate(world);

            // GUI에 world 설정
            gui.setWorld(world);

            // 상호 참조 설정
            world.setNetworkClient(networkClient);
            networkClient.setNetworkWorld((NetworkWorld)world);
            playerController = world.getPlayerController();
            playerController.setNetworkClient(networkClient);
            world.roundSystem.setNetworkClient(networkClient);
            playerController.setNetworkWorld(world);
            // 게임 화면 구성 요소 초기화
            Gdx.app.log("GameScreen", "Initializing UI components");
            gui = new GUI(world, this);
            playerController.setGUI(gui);
            world.roundSystem.setGUI(gui);
            // 게임 객체 초기화
            Gdx.app.log("GameScreen", "Populating world");
            Populator.populate(world);

            // 이제 플레이어 스폰
            spawnPlayer();

            // 뷰 초기화
            Gdx.app.log("GameScreen", "Initializing views");
            gameView = new GameView(world,false, 0.1f, 300f, 1f);
            gameView.getCameraController().setThirdPersonMode(thirdPersonView);
            if (world.getPlayer() != null) {
                world.getPlayer().visible = thirdPersonView;
            }
            //world.getPlayer().visible = thirdPersonView;            // 1인칭에서 플레이어 메쉬 숨기기

            gridView = new GridView();
            physicsView = new PhysicsView(world);
            scopeOverlay = new ScopeOverlay();

            // Settings 오버레이 초기화
            settingsOverlay = new SettingsOverlay(Main.instance, new SettingsOverlay.SettingsCallback() {
                @Override
                public void onResume() {
                    Gdx.input.setCursorCatched(true);
                    if (world != null && world.getPlayerController() != null) {
                        world.getPlayerController().setInputEnabled(true);
                    }
                }
            });

            // 상점 오버레이 초기화
            shopOverlay = new ShopOverlay(world.weaponState, new ShopOverlay.ShopCallback() {
                @Override
                public void onResume() {
                    Gdx.input.setCursorCatched(true);
                    if (world != null && world.getPlayerController() != null) {
                        world.getPlayerController().setInputEnabled(true);
                    }
                }
            });
            world.weaponState.setWorld(world); // World 참조 설정

            // InputMultiplexer 초기화 - 한 번만 설정하고 유지
            InputMultiplexer im = new InputMultiplexer();
            // 입력 처리기 설정 - 우선순위 순서대로 추가
            im.addProcessor(settingsOverlay.getStage());
            im.addProcessor(shopOverlay.getStage());
            im.addProcessor(gui.stage);
            im.addProcessor(gameView.getCameraController());
            im.addProcessor(world.getPlayerController());
            Gdx.input.setInputProcessor(im);

            // 마우스 커서 설정
            Gdx.input.setCursorCatched(true);
            Gdx.input.setCursorPosition(Gdx.graphics.getWidth() / 2, Gdx.graphics.getHeight() / 2);

            // 키 설정
            Gdx.input.setCatchKey(Input.Keys.F1, true);
            Gdx.input.setCatchKey(Input.Keys.F2, true);
            // Gdx.input.setCatchKey(Input.Keys.F3, true);
            Gdx.input.setCatchKey(Input.Keys.F11, true);

            // 건 모델 로드
            initGunWorld();

            // 서버 연결 시도
            startConnectionThread();

            world.getTeamName(teamString); //d

        } catch (Exception e) {
            Gdx.app.error("GameScreen", "Error in show()", e);
            if (gui != null) {
                showError("Initialization Error", "Failed to initialize game: " + e.getMessage());
            }
        }

    }


    private void showError(final String title, final String message) {
        Gdx.app.postRunnable(() -> {
            if (gui != null) {
                gui.showDialog(title, message, () -> {
                    Main.instance.setScreen(new TitleScreen(Main.instance));
                });
            } else {
                Gdx.app.error("GameScreen", "Cannot show error dialog: GUI is null");
                Main.instance.setScreen(new TitleScreen(Main.instance));
            }
        });
    }

    private void startConnectionThread() {
        if (networkClient == null) {
            showError("Connection Error", "NetworkClient is not initialized");
            return;
        }

        Thread connectionThread = new Thread(() -> {
            try {
                Gdx.app.log("GameScreen", "Connection thread started");
                Thread.sleep(1000);

                try {
                    networkClient.connect(DEFAULT_HOST);
                    Gdx.app.log("GameScreen", "Successfully connected to server");
                } catch (Exception e) {
                    Gdx.app.error("GameScreen", "Failed to connect to server: " + e.getMessage(), e);
                    showError("Connection Error", "Failed to connect to server. Please make sure the server is running.");
                }

            } catch (Exception e) {
                Gdx.app.error("GameScreen", "Error in connection thread", e);
                showError("Connection Error", "Connection thread error: " + e.getMessage());
            }
        }, "NetworkConnection-Thread");

        connectionThread.setDaemon(true);
        connectionThread.start();
    }

    private void showConnectionErrorDialog() {
        Dialog dialog = new Dialog("Connection Error", Main.assets.skin);
        dialog.text("Failed to connect to server.\nPlease check if the server is running.");
        dialog.button("OK", new Runnable() {
            @Override
            public void run() {
                Main.instance.setScreen(new TitleScreen(Main.instance));
            }
        });
        dialog.show(gui.stage);
    }

    private void initGunWorld() {
        try {
            gunWorld = new World();
            gunWorld.clear();
            gun = gunWorld.spawnObject(
                GameObjectType.TYPE_STATIC,
                "KnifeArmature",
                null,
                CollisionShapeType.BOX,
                true,
                Vector3.Zero
            );

            if (gun != null && gun.scene != null && gun.scene.animationController != null) {
                gun.scene.animationController.allowSameAnimation = true;

                // 초기 무기 변환 설정
                gun.scene.modelInstance.transform.idt(); // 변환 행렬 초기화
                gun.scene.modelInstance.transform.setToScaling(Settings.gunScale, Settings.gunScale, Settings.gunScale);
                gun.scene.modelInstance.transform.setTranslation(Settings.knifePosition);

                // 초기 애니메이션 설정
                gun.scene.animationController.setAnimation("stab", 1);
                gun.scene.animationController.current.time = 147 / 60.0f;

                gunname = "KnifeArmature";
                currentfire = "stab";
            }

            // 무기 뷰 초기화
            gunView = new GameView(gunWorld, true, 0.01f, 5f, 0.2f);
            gunView.getCamera().position.set(0, Settings.eyeHeight, 0);
            gunView.getCamera().lookAt(0, Settings.eyeHeight, 1);
            gunView.getCamera().up.set(Vector3.Y);
            gunView.getCamera().update();

            Gdx.app.log("GameScreen", "Gun world initialized successfully");
        } catch (Exception e) {
            Gdx.app.error("GameScreen", "Error initializing gun world", e);
        }
    }

    public void restart() {
        Populator.populate(world);
        world.getPlayer().visible = thirdPersonView;            // 1인칭에서 플레이어 메쉬 숨기기
    }

    public void nextRound(){ // 다음 라운드
        Vector3 spawnPosition;
        world.roundSystem.setRoundState(RoundSystem.RoundState.WAITING);
        if (playerTeam == TeamSelectionScreen.Team.TEAM_A) {
            spawnPosition = world.spawnSystem.getRandomSpawnPoint(true);
        } else {
            spawnPosition = world.spawnSystem.getRandomSpawnPoint(false);
        }
        world.getPlayer().body.setPosition(spawnPosition);
//        world.getPlayer().health = 1f;
//        world.getPlayer().setAlive();
        for (GameObject player : world.roundSystem.getTeamA()){
            player.health = 1f;
            player.setAlive();

        }
        for (GameObject player : world.roundSystem.getTeamB()){
            player.health = 1f;
            player.setAlive();
        }
        world.roundSystem.isPlayed = false;
    }

    private void setScopeMode( boolean scopeView ){
        boolean sv = scopeView && !thirdPersonView &&
            (world.weaponState.currentWeaponType == WeaponType.PISTOL ||
                world.weaponState.currentWeaponType == WeaponType.AK47);

        if(sv == this.lookThroughScope) // 변경 없음
            return;
        this.lookThroughScope = sv;
        if(sv)  // 스코프 뷰로 전환
            gameView.setFieldOfView(20f);        // 매우 좁은 시야각
        else   // 스코프 뷰에서 정상 뷰로 복귀
            gameView.setFieldOfView(67f);
    }

    public void toggleViewMode() {
        thirdPersonView = !gameView.getCameraController().getThirdPersonMode();
        gameView.getCameraController().setThirdPersonMode(thirdPersonView);
        world.getPlayer().visible = thirdPersonView;            // 1인칭에서 플레이어 메쉬 숨기기
        gameView.refresh();
    }

    private void toggleFullScreen() {        // 전체 화면 / 창 모드 전환
        if (!Gdx.graphics.isFullscreen()) {
            windowedWidth = Gdx.graphics.getWidth();        // 현재 너비 및 높이 기억
            windowedHeight = Gdx.graphics.getHeight();
            Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
            resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        } else {
            Gdx.graphics.setWindowedMode(windowedWidth, windowedHeight);
            resize(windowedWidth, windowedHeight);
        }
    }

    private void switchToWeapon(String armatureName, String fireAnimation) {
        try {
            gunWorld.clear();
            gunname = armatureName;
            currentfire = fireAnimation;

            gun = gunWorld.spawnObject(
                GameObjectType.TYPE_STATIC,
                gunname,
                null,
                CollisionShapeType.BOX,
                true,
                Vector3.Zero
            );

            if (gun != null && gun.scene != null && gun.scene.animationController != null) {
                gun.scene.animationController.allowSameAnimation = true;

                // 무기 위치 및 크기 설정
                gun.scene.modelInstance.transform.idt();
                gun.scene.modelInstance.transform.setToScaling(Settings.gunScale, Settings.gunScale, Settings.gunScale);

                // 무기 종류에 따른 위치 설정
                Vector3 position = armatureName.equals("KnifeArmature") ? Settings.knifePosition : Settings.gunPosition;
                gun.scene.modelInstance.transform.setTranslation(position);

                // 애니메이션 설정
                gun.scene.animationController.setAnimation(fireAnimation, 1);
                gun.scene.animationController.current.time = 20 / 60.0f;

                Gdx.app.log("GameScreen", "Switched to weapon: " + armatureName);
            }
        } catch (Exception e) {
            Gdx.app.error("GameScreen", "Error switching weapon", e);
        }
    }

    @Override
    public void render(float delta) {
        try {
            if (networkClient != null && !networkClient.isConnected()) {
                return;  // 이미 에러 처리됨
            }

            gui.bombProgressBar.setValue(bombCharge / bombChargeTime);
            setScopeMode(world.weaponState.scopeMode);

            // ESC키로 설정창 열기/닫기
            if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
                if (settingsOverlay.isVisible()) {
                    settingsOverlay.hide();
                    Gdx.input.setCursorCatched(true);
                    world.getPlayerController().setInputEnabled(true);  // 플레이어 입력 활성화
                } else if (!shopOverlay.isVisible()) { // 상점이 열려있지 않을 때만
                    settingsOverlay.show();
                    Gdx.input.setCursorCatched(false);
                    world.getPlayerController().setInputEnabled(false); // 플레이어 입력 비활성화
                }
            }

            // B키로 상점 열기/닫기
            if (Gdx.input.isKeyJustPressed(Input.Keys.B)) {
                if (shopOverlay.isVisible()) {
                    shopOverlay.hide();
                } else if (!settingsOverlay.isVisible()) {
                    shopOverlay.show();
                    Gdx.input.setCursorCatched(false);
                    world.getPlayerController().setInputEnabled(false);
                }
            }

            if (Gdx.input.isKeyJustPressed(Input.Keys.F1))
                debugRender = !debugRender;
            if (Gdx.input.isKeyJustPressed(Input.Keys.F2))
                toggleViewMode();
            if (Gdx.input.isKeyJustPressed(Input.Keys.F3))
                navScreen = !navScreen;

            if (Gdx.input.isKeyJustPressed(Input.Keys.F5)) {
                // F5: 라운드 시작 (대기 -> 준비)
                if (world.roundSystem.getRoundState() == RoundSystem.RoundState.WAITING) {
                    world.roundSystem.startRound();
                    Gdx.app.log("GameScreen", "Starting round preparation phase");
                }
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.F6)) {
                // F6: 준비 시간 스킵 (준비 -> 진행 중)
                world.roundSystem.skipPreparationPhase();
            }

            if (Gdx.input.isKeyJustPressed(Input.Keys.F7)) {
                // F7: 현재 라운드 강제 종료
                if (world.roundSystem.getRoundState() == RoundSystem.RoundState.IN_PROGRESS) {
                    world.roundSystem.forceEndRound();  // 이 메소드는 아래에서 추가
                    Gdx.app.log("GameScreen", "Forcing round end");
                }
            }

            if (Gdx.input.isKeyJustPressed(Input.Keys.F8)) {
                // F8: 다음 라운드로 진행
                if (world.roundSystem.getRoundState() == RoundSystem.RoundState.ROUND_END) {
                    world.roundSystem.startNextRound();  // 이 메소드는 아래에서 추가
                    Gdx.app.log("GameScreen", "Starting next round");
                }
            }


            if (Gdx.input.isKeyJustPressed(Input.Keys.F11))
                toggleFullScreen();

            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) {
                // 주무기 선택
                if (world.weaponState.haveAK47 && !gunname.equals("AkArmature")) {
                    world.weaponState.currentWeaponType = WeaponType.AK47;
                    switchToWeapon("AkArmature", "fire");
                } else if (world.weaponState.haveSG553 && !gunname.equals("SgArmature")) {
                    world.weaponState.currentWeaponType = WeaponType.SG553;
                    switchToWeapon("SgArmature", "sg553_shoot");
                } else if (world.weaponState.haveAR15 && !gunname.equals("ArArmature")) {
                    world.weaponState.currentWeaponType = WeaponType.AR15;
                    switchToWeapon("ArArmature", "ar15_shoot");
                }
            }

            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2) &&
                world.weaponState.havePistol &&
                !gunname.equals("GunArmature")) {
                world.weaponState.currentWeaponType = WeaponType.PISTOL;
                gunWorld.clear();
                gunname = "GunArmature";
                currentfire = "Fire";
                gun = gunWorld.spawnObject(GameObjectType.TYPE_STATIC, gunname, null,
                    CollisionShapeType.BOX, true, new Vector3(0, 0, 0));
                gun.scene.animationController.allowSameAnimation = true;
                gun.scene.modelInstance.transform.setToScaling(Settings.gunScale, Settings.gunScale, Settings.gunScale);
                gun.scene.modelInstance.transform.setTranslation(Settings.gunPosition);
            }

            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_3) &&
                !gunname.equals("KnifeArmature")) {
                world.weaponState.currentWeaponType = WeaponType.KNIFE;
                gunWorld.clear();
                gunname = "KnifeArmature";
                currentfire = "stab";
                gun = gunWorld.spawnObject(GameObjectType.TYPE_STATIC, gunname, null,
                    CollisionShapeType.BOX, true, new Vector3(0, 0, 0));
                gun.scene.animationController.allowSameAnimation = true;
                gun.scene.modelInstance.transform.setToScaling(Settings.gunScale, Settings.gunScale, Settings.gunScale);
                gun.scene.modelInstance.transform.setTranslation(Settings.knifePosition);
                gun.scene.animationController.setAnimation(currentfire, 1);
                gun.scene.animationController.current.time = 147 / 60.0f;
            }

            if (Gdx.input.isKeyPressed(Input.Keys.NUM_4)) {
                if (playerTeam == TeamSelectionScreen.Team.TEAM_A) {
                    world.weaponState.currentWeaponType = WeaponType.BOMB;
                    gunWorld.clear();
                    gunname = "bomb";
                    gun = gunWorld.spawnObject(GameObjectType.TYPE_STATIC, gunname, null,
                        CollisionShapeType.BOX, true, new Vector3(0, 0, 0));
                    gun.scene.animationController.allowSameAnimation = true;
                    gun.scene.modelInstance.transform.setToScaling(Settings.bombScale, Settings.bombScale, Settings.bombScale);
                    gun.scene.modelInstance.transform.setTranslation(Settings.gunPosition);
                }
                else if (playerTeam == TeamSelectionScreen.Team.TEAM_B) {
                    Vector3 bombPosition = world.getBombPosition(); // 폭탄 위치 가져오기
                    Vector3 playerPosition = world.getPlayer().getPosition();

                    gunWorld.clear();
                    gunname ="empty";
                    // 폭탄과 플레이어의 거리를 확인
                    if (playerPosition.dst(bombPosition) < bombDefuseRange) {
                        if (!isDefusing) {
                            isDefusing = true; // 해체 작업 시작
                            defuseProgress = 0; // 초기화
                            Gdx.app.log("GameScreen", "Defusing bomb started...");
                        }

                        // 게이지 증가
                        defuseProgress += Gdx.graphics.getDeltaTime();
                        gui.updateBombDefuseProgress(defuseProgress / bombDefuseTime); // GUI 업데이트

                        // 게이지가 다 차면 폭탄 해체 완료
                        if (defuseProgress >= bombDefuseTime) {
                            isDefusing = false;
                            defuseProgress = 0;
                            Gdx.app.log("GameScreen", "Bomb defused!");
                            try{
                                networkClient.sendBombDefusedMessage();
                                world.removeBomb();
                                world.roundSystem.setBombPlanted(false);
                            } catch (Exception e) {
                                Gdx.app.error("PlayerController", "Error while placing the bomb", e);
                            }


                        }
                    } else {
                        // 폭탄에서 멀어지면 해체 작업 중단
                        if (isDefusing) {
                            isDefusing = false;
                            defuseProgress = 0;
                            gui.updateBombDefuseProgress(0); // GUI 초기화
                            Gdx.app.log("GameScreen", "Defusing bomb cancelled (too far).");
                        }
                    }
                }
            } else {
                // NUM_4를 놓으면 해체 작업 중단
                if (isDefusing) {
                    isDefusing = false;
                    defuseProgress = 0;
                    gui.updateBombDefuseProgress(0); // GUI 초기화
                    Gdx.app.log("GameScreen", "Defusing bomb cancelled.");
                }
            }

            if( world.weaponState.currentWeaponType == WeaponType.BOMB && world.roundSystem.getBombPlanted() && !weaponSwitchedAfterBombPlanted){
                world.weaponState.currentWeaponType = WeaponType.KNIFE;
                gunWorld.clear();
                gunname = "KnifeArmature";
                currentfire = "stab";
                gun = gunWorld.spawnObject(GameObjectType.TYPE_STATIC, gunname, null,
                    CollisionShapeType.BOX, true, new Vector3(0,0,0));
                gun.scene.animationController.allowSameAnimation = true;
                gun.scene.modelInstance.transform.setToScaling(Settings.gunScale, Settings.gunScale, Settings.gunScale);
                gun.scene.modelInstance.transform.setTranslation(Settings.knifePosition);
                gun.scene.animationController.setAnimation(currentfire, 1);
                gun.scene.animationController.current.time = 147 / 60.0f;

                // 플래그를 true로 설정하여 로직이 다시 실행되지 않도록 함
                weaponSwitchedAfterBombPlanted = true;
            }

            if(Gdx.input.isKeyJustPressed(Input.Keys.R)) {
                WeaponState weaponState = world.weaponState;
                if (weaponState.reload()) {
                    // 재장전 애니메이션과 사운드 재생
                    if(gunname.equals("SgArmature")) {
                        gun.scene.animationController.setAnimation("sg553_reload", 1);
                        Main.assets.sounds.RELOAD.play();
                    }
                    else if(gunname.equals("ArArmature")) {
                        gun.scene.animationController.setAnimation("ar15_reload", 1);
                        Main.assets.sounds.RELOAD.play();
                    }
                    else if(gunname.equals("AkArmature")) {
                        gun.scene.animationController.setAnimation("reload", 1);
                        Main.assets.sounds.RELOAD.play();
                    }
                    else if(gunname.equals("GunArmature")) {
                        gun.scene.animationController.setAnimation("Reload_Fast",1);
                        Main.assets.sounds.PISTOL_RELOAD.play();
                    }
                }
            }


            if(world.weaponState.firing){
                world.weaponState.firing = false;

                if((world.weaponState.currentWeaponType == WeaponType.PISTOL ||
                    world.weaponState.currentWeaponType == WeaponType.AK47 ||
                    world.weaponState.currentWeaponType == WeaponType.KNIFE ||
                    world.weaponState.currentWeaponType == WeaponType.AR15 ||
                    world.weaponState.currentWeaponType == WeaponType.SG553) &&
                    !thirdPersonView && !lookThroughScope)
                    gun.scene.animationController.setAnimation(currentfire, 1);

                scopeOverlay.startRecoilEffect();
            }

            // 게임 월드 업데이트는 항상 실행
            try{
                world.update(delta);
            } catch (Exception e) {}

            float moveSpeed = world.getPlayer().body.getVelocity().len();
            gameView.render(delta, moveSpeed);

            if(debugRender) {
                gridView.render(gameView.getCamera());
                physicsView.render(gameView.getCamera());
            }

            // 무기 뷰 렌더링
            if(!thirdPersonView && (world.weaponState.currentWeaponType == WeaponType.PISTOL ||
                world.weaponState.currentWeaponType == WeaponType.AK47 ||
                world.weaponState.currentWeaponType == WeaponType.SG553 ||
                world.weaponState.currentWeaponType == WeaponType.AR15 ||
                world.weaponState.currentWeaponType == WeaponType.KNIFE ||
                world.weaponState.currentWeaponType == WeaponType.BOMB) &&
                !lookThroughScope) {
                gunView.render(delta, moveSpeed);
            }

            // UI 렌더링
            if(lookThroughScope)
                scopeOverlay.render(delta);

            gui.render(delta);

            // 오버레이 렌더링
            if (settingsOverlay.isVisible()) {
                settingsOverlay.render();
            }
            if (shopOverlay.isVisible()) {
                shopOverlay.render();
            }

            // GUI 업데이트
            if (gui != null) {
                gui.render(delta);
            }

        } catch (Exception e) {
            Gdx.app.error("GameScreen", "Error in render", e);
            showError("Runtime Error", "An error occurred: " + e.getMessage());
        }
    }


    @Override
    public void resize(int width, int height) {
        if (gui != null) {
            gui.resize(width, height);
        }

        if (shopOverlay != null) {
            shopOverlay.resize(width, height);
        }
        if (settingsOverlay != null) {
            settingsOverlay.resize(width, height);
        }

        gameView.resize(width, height);
        gui.resize(width, height);
        scopeOverlay.resize(width, height);
    }


    @Override
    public void hide() {
        // 화면이 숨겨질 때 (다른 화면으로 전환될 때) 모든 리소스를 정리

        if (networkClient != null) {
            networkClient.dispose();
            networkClient = null;
        }

        dispose();
    }

    @Override
    public void dispose() {
        try {
            if (gui != null) {
                gui.dispose();
                gui = null;
            }
            if (networkClient != null) {
                networkClient.dispose();
                networkClient = null;
            }
            if (gameView != null) {
                gameView.dispose();
                gameView = null;
            }
            if (gridView != null) {
                gridView.dispose();
                gridView = null;
            }
            if (physicsView != null) {
                physicsView.dispose();
                physicsView = null;
            }
            if (world != null) {
                world.dispose();
                world = null;
            }
            if (scopeOverlay != null) {
                scopeOverlay.dispose();
                scopeOverlay = null;
            }
            if (settingsOverlay != null) {
                settingsOverlay.dispose();
                settingsOverlay = null;
            }
            if (shopOverlay != null) {
                shopOverlay.dispose();
                shopOverlay = null;
            }
        } catch (Exception e) {
            Gdx.app.error("GameScreen", "Error during disposal", e);
        }
    }
}
