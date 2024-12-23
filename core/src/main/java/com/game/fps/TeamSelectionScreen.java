package com.game.fps;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

public class TeamSelectionScreen extends ScreenAdapter {
    private final Stage stage;
    private final Main game;
    private final Window window;
    private Team selectedTeam = null;

    public enum Team {
        TEAM_A("Terrorist"),
        TEAM_B("Counter-Terrorist");

        private final String displayName;

        Team(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public TeamSelectionScreen(Main game) {
        this.game = game;
        stage = new Stage(new ScreenViewport());

        window = new Window("Select Team", Main.assets.skin);
        window.setMovable(false);

        Table content = new Table();
        content.defaults().pad(20).width(300);

        Label titleLabel = new Label("Choose Your Team", Main.assets.skin);
        titleLabel.setFontScale(1.5f);

        TextButton teamAButton = new TextButton("Join " + Team.TEAM_A.getDisplayName(), Main.assets.skin);
        teamAButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                selectTeam(Team.TEAM_A);
            }
        });

        TextButton teamBButton = new TextButton("Join " + Team.TEAM_B.getDisplayName(), Main.assets.skin);
        teamBButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                selectTeam(Team.TEAM_B);
            }
        });

        TextButton backButton = new TextButton("Back", Main.assets.skin);
        backButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.setScreen(new TitleScreen(game));
            }
        });

        content.add(titleLabel).padBottom(40).row();
        content.add(teamAButton).row();
        content.add(teamBButton).row();
        content.add(backButton).padTop(40).row();

        window.add(content).pad(20);
        window.pack();

        // 창을 화면 중앙에 위치
        window.setPosition(
            (Gdx.graphics.getWidth() - window.getWidth()) * 0.5f,
            (Gdx.graphics.getHeight() - window.getHeight()) * 0.5f
        );

        stage.addActor(window);
    }

    private void selectTeam(Team team) {
        selectedTeam = team;
        GameScreen gameScreen = new GameScreen();
        gameScreen.setPlayerTeam(team);  // GameScreen에 선택된 팀 정보 전달
        game.setScreen(gameScreen);
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
        window.setPosition(
            (width - window.getWidth()) * 0.5f,
            (height - window.getHeight()) * 0.5f
        );
    }

    @Override
    public void dispose() {
        stage.dispose();
    }
}
