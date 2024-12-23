package com.game.fps;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

public class TitleScreen extends ScreenAdapter {
    private Stage stage;
    private final Main game;
    private Table table;

    public TitleScreen(Main game) {
        this.game = game;
    }

    @Override
    public void show() {
        try {
            stage = new Stage(new ScreenViewport());
            Skin skin = Main.assets.skin;

            table = new Table(skin);
            table.setFillParent(true);

            // UI 요소들 생성
            Label label = new Label("FPS GAME", skin);
            TextButton startButton = new TextButton("Start Game", skin);
            TextButton exitButton = new TextButton("Exit", skin);

            // 버튼 리스너 추가
            startButton.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    game.setScreen(new TeamSelectionScreen(game));  // GameScreen 대신 TeamSelectionScreen으로 변경
                }
            });
            /*
            startButton.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    game.setScreen(new GameScreen());
                }
            });
            */

            exitButton.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    Gdx.app.exit();
                }
            });

            // 테이블에 UI 요소 추가
            table.add(label).padBottom(50).row();
            table.add(startButton).width(200).padBottom(20).row();
            table.add(exitButton).width(200);

            // 스테이지에 테이블 추가
            stage.addActor(table);

            // 입력 프로세서 설정
            Gdx.input.setInputProcessor(stage);
            Gdx.input.setCursorCatched(false);

        } catch (Exception e) {
            Gdx.app.error("TitleScreen", "Error in show()", e);
        }
    }

    @Override
    public void render(float delta) {
        try {
            Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1);
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

            if (stage != null) {
                stage.act(delta);
                stage.draw();
            }
        } catch (Exception e) {
            Gdx.app.error("TitleScreen", "Error in render()", e);
        }
    }

    @Override
    public void resize(int width, int height) {
        if (stage != null) {
            stage.getViewport().update(width, height, true);
        }
    }

    @Override
    public void dispose() {
        if (stage != null) {
            stage.dispose();
            stage = null;
        }
    }
}
