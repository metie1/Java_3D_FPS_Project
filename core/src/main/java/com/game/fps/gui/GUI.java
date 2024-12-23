package com.game.fps.gui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.game.fps.GameScreen;
import com.game.fps.Main;
import com.game.fps.World;
import com.game.fps.RoundSystem;

import com.game.fps.WeaponState;
import com.game.fps.WeaponType;
import com.game.fps.AmmoInfo;
import com.game.fps.net.NetworkWorld;
import com.game.fps.SpectatorSystem;

public class GUI implements Disposable {
    // UI 컴포넌트들
    public Stage stage;
    private final Skin skin;
    private World world;
    private final GameScreen screen;

    // 라벨 컴포넌트들
    private Label healthLabel;
    private Label timeLabel;

    private Label gameOverLabel;
    private Label crossHairLabel;
    private Label fpsLabel;
    private Label roundLabel;
    private Label timerLabel;
    private Label scoreLabel;
    private Label countdownLabel;
    public ProgressBar bombProgressBar;
    private TextButton restartButton;
    private final StringBuffer sb;
    private Label ammoLabel;
    private Label spectatorLabel;
    private Label spectatorTargetLabel;
    private Label NextRoundcountdownLabel; // 카운트다운을 표시할 레이블


    private Image crossHairImage;
    private boolean isUsingImageCrossHair = false; // 현재 크로스헤어 상태
    private final Texture crossHairTexture; // 연사 크로스헤어 이미지



    private String formatTime(float timeInSeconds) {
        int minutes = (int)(timeInSeconds / 60);
        int seconds = (int)(timeInSeconds % 60);
        return String.format("%d:%02d", minutes, seconds);
    }

    public GUI(World world, GameScreen screen) {
        this.world = world;
        this.screen = screen;
        stage = new Stage(new ScreenViewport());
        skin = Main.assets.skin;
        sb = new StringBuffer();
        crossHairTexture = Main.assets.crosshairAutoFire;
        rebuild();
    }
    public void showGameOverMessage(String message) {
        gameOverLabel.setText(message);
        gameOverLabel.setVisible(true);
        restartButton.setVisible(false); // 재시작 버튼 비활성화
        Gdx.input.setCursorCatched(false); // 커서 활성화
        // 5초 후에 nextRound() 실행

        startCountdown(5);
    }
    // 카운트다운을 시작하는 메서드
    // 카운트다운을 시작하는 메서드
    private void startCountdown(int startNumber) {
        NextRoundcountdownLabel.setVisible(true); // 카운트다운 표시
        NextRoundcountdownLabel.setText(String.valueOf(startNumber));

        Timer.schedule(new Timer.Task() {
            int currentCount = startNumber;

            @Override
            public void run() {
                currentCount--;

                if (currentCount > 0) {
                    NextRoundcountdownLabel.setText(String.valueOf(currentCount));
                } else {
                    // 카운트다운 완료 시 작업 수행
                    NextRoundcountdownLabel.setVisible(false);
                    screen.nextRound();
                    gameOverLabel.setVisible(false);
                    Gdx.input.setCursorCatched(true);
                    cancel();
                }
            }
        }, 0, 1, startNumber - 1); // 1초 간격으로 실행
    }

    public void updateBombDefuseProgress(float progress) {
        if (progress > 0 && progress <= 1) {
            bombProgressBar.setVisible(true);
            bombProgressBar.setValue(progress);
        } else {
            bombProgressBar.setVisible(false);
        }
    }


    public void setWorld(World world) {
        this.world = world;
        rebuild();
    }

    public void showDialog(String title, String message, Runnable onClose) {
        Dialog dialog = new Dialog(title, skin) {
            @Override
            protected void result(Object obj) {
                if (onClose != null) {
                    onClose.run();
                }
            }
        };

        dialog.text(message);
        dialog.button("OK", true);
        dialog.show(stage);
    }

    public Skin getSkin() {
        return skin;
    }

    public Stage getStage() {
        return stage;
    }

    private void rebuild() {
        stage.clear();

        if (world == null) return;

        BitmapFont bitmapFont= Main.assets.uiFont;
        float scale = Gdx.graphics.getHeight() / 720f; // 기준 화면 높이: 720
        bitmapFont.getData().setScale(scale);
        Label.LabelStyle labelStyle = new Label.LabelStyle(bitmapFont, Color.BLUE);



        // 메인 UI를 위한 테이블
        Table screenTable = new Table();
        screenTable.setFillParent(true);
        screenTable.top();

        // 기존 UI 요소들
        healthLabel = new Label("100%", labelStyle);
        timeLabel = new Label("00:00", labelStyle);
        // enemiesLabel = new Label("2", labelStyle);
        // coinsLabel = new Label("0", labelStyle);
        fpsLabel = new Label("00", labelStyle);
        countdownLabel = new Label("00:00", labelStyle);
        gameOverLabel = new Label("GAME OVER", labelStyle);
        restartButton = new TextButton("RESTART", skin);
        crossHairLabel = new Label("+", skin);
        ammoLabel = new Label("0 / 0", labelStyle); // 탄약

        // 라운드 정보
        roundLabel = new Label("Round 1", labelStyle);
        timerLabel = new Label("2:00", labelStyle);
        scoreLabel = new Label("0 - 0", labelStyle);

        // 관전 정보
        spectatorLabel = new Label("SPECTATING", skin);
        spectatorLabel.setColor(Color.YELLOW);
        spectatorTargetLabel = new Label("", skin);
        spectatorTargetLabel.setColor(Color.YELLOW);

        //screenTable.debug();

        // 상단 정보 배치
        screenTable.add(new Label("Health: ", labelStyle)).padLeft(5);
        screenTable.add(healthLabel).left().expandX();
        screenTable.add(new Label("Time: ", labelStyle));
        screenTable.add(timeLabel).padRight(5).row();

        //screenTable.add(new Label("Enemies: ", labelStyle)).colspan(3).right();
        //screenTable.add(enemiesLabel).padRight(5).row();

        //screenTable.add(new Label("Coins: ", labelStyle)).colspan(3).right();
        //screenTable.add(coinsLabel).padRight(5).row();

        screenTable.add(new Label("Countdown: ", labelStyle)).colspan(3).right();
        screenTable.add(countdownLabel).padRight(5).row();

        // 탄약 정보 추가
        screenTable.add(new Label("Ammo: ", labelStyle)).colspan(3).right();
        screenTable.add(ammoLabel).padRight(5).row();

        // row 4
        screenTable.add(gameOverLabel).colspan(4).row();
        gameOverLabel.setVisible(false);            // hide until needed
        // row 5
        screenTable.add(restartButton).colspan(4).pad(20);
        restartButton.setVisible(false);            // hide until needed
        screenTable.row();

        // FPS 테이블 (좌측 하단)
        Table fpsTable = new Table();
        fpsTable.setFillParent(true);
        fpsTable.bottom().left();
        fpsTable.add(new Label("FPS: ", labelStyle)).padLeft(5);
        fpsTable.add(fpsLabel).padBottom(5);

        // 라운드 정보 테이블 (하단 중앙)
        Table roundTable = new Table();
        roundTable.setFillParent(true);
        roundTable.bottom();  // 하단 중앙 정렬
        roundTable.padBottom(5);  // 하단 여백

        // 라운드 정보를 하나의 레이블로 합침
        Table roundInfoTable = new Table();
        roundInfoTable.add(roundLabel).padRight(10);
        roundInfoTable.add(timerLabel).padRight(10);
        roundInfoTable.add(scoreLabel);

        roundTable.add(roundInfoTable);

        // 크로스헤어 테이블 (화면 중앙)
        Table crossHairTable = new Table();
        crossHairTable.setFillParent(true);
        crossHairTable.add(crossHairLabel);
        // 크로스 헤어 연사 이미지
        crossHairImage = new Image(crossHairTexture);
        crossHairImage.setSize(50, 50); // 크로스헤어 이미지 크기
        crossHairImage.setPosition(
            Gdx.graphics.getWidth() / 2f - crossHairImage.getWidth() / 2f,
            Gdx.graphics.getHeight() / 2f - crossHairImage.getHeight() / 2f
        );
        crossHairImage.setVisible(false); // 기본적으로 숨김


        // 관전 테이블
        Table spectatorTable = new Table();
        spectatorTable.add(spectatorLabel).padRight(10);
        spectatorTable.add(spectatorTargetLabel);

        screenTable.add(spectatorTable).colspan(4).padTop(20).row();
        spectatorLabel.setVisible(false);
        spectatorTargetLabel.setVisible(false);

        // 모든 테이블을 스테이지에 추가
        stage.addActor(screenTable);    // 상단 정보
        stage.addActor(fpsTable);       // FPS
        stage.addActor(roundTable);     // 라운드 정보
        stage.addActor(crossHairTable); // 크로스헤어
        stage.addActor(crossHairImage);

        // GameOver 관련 요소들은 기본적으로 숨김
        gameOverLabel.setVisible(false);
        restartButton.setVisible(false);

        ProgressBar.ProgressBarStyle progressBarStyle = new ProgressBar.ProgressBarStyle();
        progressBarStyle.background = skin.newDrawable("white", Color.DARK_GRAY);
        progressBarStyle.knobBefore = skin.newDrawable("white", Color.GREEN);

        progressBarStyle.knobBefore.setMinHeight(10); // 기존보다 더 두껍게 설정 (30px)
        progressBarStyle.background.setMinHeight(10); // 배경도 같은 두께로 맞춤
        bombProgressBar = new ProgressBar(0, 1, 0.01f, false, progressBarStyle);
        bombProgressBar.setValue(0); // Initial value
        bombProgressBar.setSize(200, 20);
        bombProgressBar.setPosition(Gdx.graphics.getWidth() / 2 - 100, 50);
        stage.addActor(bombProgressBar);

        bombProgressBar.setVisible(false);

        restartButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                super.clicked(event, x, y);
                screen.nextRound();

                restartButton.setVisible(false);
                gameOverLabel.setVisible(false);
                Gdx.input.setCursorCatched(true);
            }
        });

        NextRoundcountdownLabel = new Label("", new Label.LabelStyle(bitmapFont, Color.RED));
        NextRoundcountdownLabel.setFontScale(3f); // 텍스트 크기 조정
        NextRoundcountdownLabel.setVisible(false); // 기본적으로 숨김
        NextRoundcountdownLabel.setPosition(
            Gdx.graphics.getWidth() / 2f - NextRoundcountdownLabel.getPrefWidth() / 2f,
            Gdx.graphics.getHeight() / 2f - NextRoundcountdownLabel.getPrefHeight() / 2f
        );


    }
    public void showBombProgressBar(boolean visible) {
        bombProgressBar.setVisible(visible);
    }

    private void updateLabels() {
        if (world == null || world.roundSystem == null) return;

        sb.setLength(0);
        sb.append((int)(world.getPlayer().health*100));
        sb.append("%");
        healthLabel.setText(sb.toString());

        // 탄약 정보 업데이트
        WeaponState weaponState = world.weaponState;
        if (weaponState.currentWeaponType != WeaponType.KNIFE &&
            weaponState.currentWeaponType != WeaponType.BOMB) {
            AmmoInfo ammoInfo = weaponState.getCurrentAmmoInfo();
            if (ammoInfo != null) {
                ammoLabel.setText(String.format("%d / %d",
                    ammoInfo.getCurrentAmmo(),
                    ammoInfo.getReserveAmmo()));
            }
        } else {
            ammoLabel.setText("-");
        }

        // 관전 정보 업데이트
        if (world instanceof NetworkWorld) {
            NetworkWorld netWorld = (NetworkWorld) world;
            SpectatorSystem spectatorSystem = netWorld.getSpectatorSystem();

            boolean isSpectating = spectatorSystem.isSpectating();
            spectatorLabel.setVisible(isSpectating);
            spectatorTargetLabel.setVisible(isSpectating);

            if (isSpectating && spectatorSystem.getCurrentTarget() != null) {
                spectatorTargetLabel.setText(spectatorSystem.getCurrentTarget().getPlayerName());
                Gdx.app.log("GUI", "Spectating: " + spectatorSystem.getCurrentTarget().getPlayerName());
            }
        }

        sb.setLength(0);
        sb.append(Gdx.graphics.getFramesPerSecond());
        fpsLabel.setText(sb.toString());

        sb.setLength(0);
        int mm = (int) (world.stats.gameTime/60);
        int ss = (int)( world.stats.gameTime - 60*mm);
        if(mm <10)
            sb.append("0");
        sb.append(mm);
        sb.append(":");
        if(ss <10)
            sb.append("0");
        sb.append(ss);
        timeLabel.setText(sb.toString());


        roundLabel.setText(world.roundSystem.getRoundStatusText());
        timerLabel.setText(formatTime(world.roundSystem.getRoundTimeRemaining()));
        scoreLabel.setText(world.roundSystem.getTeamAScore() + " - " + world.roundSystem.getTeamBScore());

        countdownLabel.setText(formatTime(world.roundSystem.getBombTimeRemaining()));
        // 시간이 30초 이하일 때 빨간색으로 변경
        if (world.roundSystem.getRoundState() == RoundSystem.RoundState.IN_PROGRESS) {
            if (world.roundSystem.getRoundTimeRemaining() <= 30f) {
                timerLabel.setColor(Color.RED);
            } else {
                timerLabel.setColor(Color.BLUE);
            }
        } else {
            timerLabel.setColor(Color.BLUE);
        }

        scoreLabel.setText(world.roundSystem.getTeamAScore() + " - " +
            world.roundSystem.getTeamBScore());
    }

    public void render(float deltaTime) {
        updateLabels();

        stage.act(deltaTime);
        stage.draw();
    }

    public void resize(int width, int height) {

        stage.getViewport().update(width, height, true);
        rebuild();

        // bombProgressBar 크기와 위치를 윈도우 크기에 맞게 조정
        float progressBarWidth = 200 * ((float) width / 1920); // 1920 기준으로 스케일링
        float progressBarHeight = 20 * ((float) height / 1080); // 1080 기준으로 스케일링

        bombProgressBar.setSize(progressBarWidth, progressBarHeight);
        bombProgressBar.setPosition(
            (width - progressBarWidth) / 2, // 화면 중앙에 위치
            50 * ((float) height / 1080) // 화면 하단에서 비율에 따라 위로 올림
        );
    }


    @Override
    public void dispose() {
        if (stage != null) {
            stage.dispose();
            stage = null;
        }
        skin.dispose();
    }

    public void updateCrosshairMode(boolean isAutoFire) {
        if (isAutoFire) {
            switchToImageCrossHair();
        } else {
            switchToTextCrossHair();
        }
    }

    private void switchToImageCrossHair() {
        isUsingImageCrossHair = true;
        crossHairLabel.setVisible(false);
        crossHairImage.setVisible(true);
    }

    private void switchToTextCrossHair() {
        isUsingImageCrossHair = false;
        crossHairLabel.setVisible(true);
        crossHairImage.setVisible(false);
    }
}
