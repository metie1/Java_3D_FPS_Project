package com.game.fps.gui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.game.fps.Main;
import com.game.fps.Settings;
import com.game.fps.TitleScreen;

public class SettingsOverlay {

    public interface SettingsCallback {
        void onResume();
    }

    private final Stage stage;
    private final Window window;
    private final Main game;
    private final SettingsCallback callback;
    private boolean visible;

    public SettingsOverlay(Main game, SettingsCallback callback) {
        this.game = game;
        this.callback = callback;
        stage = new Stage(new ScreenViewport());

        window = new Window("Settings", Main.assets.skin);
        window.setMovable(false);
        window.setModal(false);
        window.setFillParent(false);

        Table content = new Table();
        content.defaults().pad(10).width(300);

        Label sensitivityLabel = new Label("Mouse Sensitivity", Main.assets.skin);
        final Label sensitivityValue = new Label(String.format("%.2f", 0.1f), Main.assets.skin);
        Slider sensitivitySlider = new Slider(0.01f, 0.5f, 0.01f, false, Main.assets.skin);
        sensitivitySlider.setValue(Settings.degreesPerPixel);

        Table sliderTable = new Table();
        sliderTable.add(sensitivitySlider).width(200).padRight(10);
        sliderTable.add(sensitivityValue).width(50);

        sensitivitySlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                float value = ((Slider) actor).getValue();
                sensitivityValue.setText(String.format("%.2f", value));
                Settings.degreesPerPixel = value; // 감도 값을 Settings에 저장
            }
        });

        TextButton resumeButton = new TextButton("Resume", Main.assets.skin);
        resumeButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                hide();
                if (callback != null) {
                    callback.onResume(); // "Resume" 버튼의 콜백 실행
                }
            }
        });

        TextButton exitButton = new TextButton("Exit to Title", Main.assets.skin);
        exitButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.create();
                game.setScreen(new TitleScreen(game)); // TitleScreen으로 전환
            }
        });

        content.add(sensitivityLabel).left().row();
        content.add(sliderTable).row();
        content.add(resumeButton).padTop(10).row();
        content.add(exitButton).padTop(10).row(); // "Exit to Title" 버튼 추가

        window.add(content).pad(20);
        window.pack();

        window.setPosition(
            (Gdx.graphics.getWidth() - window.getWidth()) * 0.5f,
            (Gdx.graphics.getHeight() - window.getHeight()) * 0.5f
        );

        stage.addActor(window);
        hide();
    }

    public void show() {
        visible = true;
        window.setVisible(true);
    }

    public void hide() {
        visible = false;
        window.setVisible(false);
    }

    public boolean isVisible() {
        return visible;
    }

    public void render() {
        if (visible) {
            stage.act(Gdx.graphics.getDeltaTime());
            stage.draw();
        }
    }

    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
        window.setPosition(
            (width - window.getWidth()) * 0.5f,
            (height - window.getHeight()) * 0.5f
        );
    }

    public Stage getStage() {
        return stage;
    }

    public void dispose() {
        stage.dispose();
    }
}
