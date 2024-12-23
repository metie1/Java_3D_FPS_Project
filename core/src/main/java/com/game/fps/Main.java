package com.game.fps;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
// import com.badlogic.gdx.graphics.g3d.utils.AnimationController;
// import net.mgsx.gltf.scene3d.scene.Scene;

import static com.badlogic.gdx.Application.ApplicationType.Desktop;

public class Main extends Game {
    // private Screen currentScreen;
    public static Assets assets;
    public static Main instance; // 추가된 부분
    public World world;

    @Override
    public void create() {
        try {
            instance = this; // 추가된 부분
            Settings.supportControllers = (Gdx.app.getType() == Desktop);

            assets = new Assets();
            assets.finishLoading();
            assets.sceneAsset.maxBones = 50;

            // 에셋 로딩 확인
            if (!assets.isLoaded()) {
                Gdx.app.error("Main", "Failed to load essential assets");
                // 기본 화면으로 진행
            }

            setScreen( new TitleScreen(this) );
        } catch (Exception e) {
            Gdx.app.error("Main", "Error in create", e);
            // 오류 발생시에도 기본 화면으로 진행
            setScreen(new TitleScreen(this));
        }
    }

    @Override
    public void setScreen(Screen screen) {
        try {
            Screen oldScreen = getScreen();
            if (oldScreen != null) {
                oldScreen.dispose();
            }

            super.setScreen(screen);
        } catch (Exception e) {
            Gdx.app.error("Main", "Error setting screen", e);
        }
    }


    @Override
    public void dispose() {
        super.dispose();
        if (assets != null) {
            assets.dispose();
        }
        if (getScreen() != null) {
            getScreen().dispose();
        }
    }
}
