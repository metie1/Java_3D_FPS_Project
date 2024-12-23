package com.game.fps;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Disposable;
import net.mgsx.gltf.loaders.gltf.GLTFAssetLoader;
import net.mgsx.gltf.scene3d.scene.SceneAsset;

import com.badlogic.gdx.assets.loaders.TextureLoader;

public class Assets implements Disposable {

    public class AssetSounds {

        public final Sound GAME_OVER;
        public final Sound HIT;
        public final Sound JUMP;
        public final Sound GAME_COMPLETED;
        public final Sound UPGRADE;
        public final Sound GUN_SHOT;
        public final Sound KNIFE_STAB;
        public final Sound BOMB_SOUND;
        public final Sound RELOAD;
        public final Sound PISTOL_RELOAD;

        public AssetSounds() {

            GAME_OVER = assets.get ("sound/gameover1.ogg");
            HIT  = assets.get("sound/hit1.ogg");
            JUMP  = assets.get("sound/jump1.ogg");
            GAME_COMPLETED = assets.get("sound/secret1.ogg");
            UPGRADE = assets.get ("sound/upgrade1.ogg");
            GUN_SHOT = assets.get ("sound/9mm-pistol-shoot-short-reverb-7152.mp3");
            KNIFE_STAB = assets.get("sound/knife-stab.ogg");
            BOMB_SOUND = assets.get("sound/counter_strike_bomb.mp3");
            RELOAD = assets.get("sound/ak-47-reload-sound-effect.mp3");
            PISTOL_RELOAD = assets.get("sound/gun-reload-sound-fx_C_minor.wav");
        }
    }

    private static final Object LOCK = new Object(); // 동기화를 위한 lock 객체
    private static volatile Assets instance; // 싱글톤 인스턴스
    private final AssetManager assets;
    public AssetSounds sounds;
    public Skin skin;
    public BitmapFont uiFont;
    public SceneAsset sceneAsset;
    public Texture scopeImage;
    public Texture crosshairAutoFire;

    public static Assets getInstance() {
        Assets result = instance;
        if (result == null) {
            synchronized (LOCK) {
                result = instance;
                if (result == null) {
                    instance = result = new Assets();
                }
            }
        }
        return result;
    }

    public AssetManager getManager() {
        return assets;
    }

    public Assets() {
        Gdx.app.log("Assets constructor", "");
        assets = new AssetManager();


        try {
            // 텍스처 로더 설정
            TextureLoader.TextureParameter param = new TextureLoader.TextureParameter();
            param.minFilter = Texture.TextureFilter.Linear;
            param.magFilter = Texture.TextureFilter.Linear;
            param.genMipMaps = false;  // 밉맵 비활성화로 메모리 절약

            synchronized(LOCK) {
                // 에셋 로더들 설정
                //assets.setLoader(Texture.class, new TextureLoader(new InternalFileHandleResolver()));

                // UI 관련 에셋 로드
                assets.load("ui/uiskin.json", Skin.class);
                assets.load("font/Amble-Regular-26.fnt", BitmapFont.class);

                // GLTF 파일 로드
                assets.setLoader(SceneAsset.class, ".gltf", new GLTFAssetLoader());
                assets.load(Settings.GLTF_FILE, SceneAsset.class);

                // 사운드 파일들 로드
                loadSounds();

                // 이미지 파일 로드
                assets.load("images/scope.png", Texture.class);
                assets.load("images/crosshair_autofire.png", Texture.class);
            }
        } catch (Exception e) {
            Gdx.app.error("Assets", "Error loading assets", e);
        }
        // sounds = new AssetSounds(assets);
    }

    private void loadSounds() {
        String[] soundFiles = {
            "sound/gameover1.ogg",
            "sound/hit1.ogg",
            "sound/jump1.ogg",
            "sound/secret1.ogg",
            "sound/upgrade1.ogg",
            "sound/9mm-pistol-shoot-short-reverb-7152.mp3",
            "sound/knife-stab.ogg",
            "sound/counter_strike_bomb.mp3",
            "sound/ak-47-reload-sound-effect.mp3",
            "sound/gun-reload-sound-fx_C_minor.wav"
        };

        for (String soundFile : soundFiles) {
            assets.load(soundFile, Sound.class);
        }
    }

    public void finishLoading() {

        try {
            float progress = 0;
            while (!assets.update(32)) {  // 32ms 타임아웃
                float newProgress = assets.getProgress() * 100;
                if (newProgress != progress) {
                    progress = newProgress;
                    Gdx.app.log("Assets", String.format("Loading progress: %.2f%%", progress));
                }
                Thread.sleep(5); // 로딩 간격을 더 짧게 조정
            }

            synchronized(LOCK) {
                initializeAssets();
            }

            // 에셋 로드 실패시에도 계속 진행할 수 있도록 처리
            try {
                skin = assets.get("ui/uiskin.json");
            } catch (Exception e) {
                Gdx.app.error("Assets", "Error loading skin", e);
            }

            try {
                uiFont = assets.get("font/Amble-Regular-26.fnt");
            } catch (Exception e) {
                Gdx.app.error("Assets", "Error loading font", e);
            }

            try {
                sceneAsset = assets.get(Settings.GLTF_FILE);
                if (sceneAsset != null) {
                    sceneAsset.maxBones = 50;
                }
            } catch (Exception e) {
                Gdx.app.error("Assets", "Error loading scene", e);
            }

            try {
                scopeImage = assets.get("images/scope.png");
            } catch (Exception e) {
                Gdx.app.error("Assets", "Error loading scope image", e);
            }
            try{
                crosshairAutoFire = assets.get("images/crosshair_autofire.png");
            } catch (Exception e) {
            }

            try {
                sounds = new AssetSounds();
            } catch (Exception e) {
                Gdx.app.error("Assets", "Error loading sounds", e);
            }

        } catch (Exception e) {
            Gdx.app.error("Assets", "Error in finishLoading", e);
            handleLoadingError();
        }
    }

    private void initializeAssets() {
        try {
            // 에셋 로드 실패시에도 계속 진행할 수 있도록 처리
            try {
                skin = assets.get("ui/uiskin.json");
            } catch (Exception e) {
                Gdx.app.error("Assets", "Error loading skin", e);
            }

            try {
                uiFont = assets.get("font/Amble-Regular-26.fnt");
            } catch (Exception e) {
                Gdx.app.error("Assets", "Error loading font", e);
            }

            try{
                crosshairAutoFire = assets.get("images/crosshair_autofire.png");
            } catch (Exception e) {
            }

            try {
                sceneAsset = assets.get(Settings.GLTF_FILE);
                if (sceneAsset != null) {
                    sceneAsset.maxBones = 50;
                }
            } catch (Exception e) {
                Gdx.app.error("Assets", "Error loading scene", e);
            }

            try {
                scopeImage = assets.get("images/scope.png");
            } catch (Exception e) {
                Gdx.app.error("Assets", "Error loading scope image", e);
            }

            try {
                sounds = new AssetSounds();
            } catch (Exception e) {
                Gdx.app.error("Assets", "Error loading sounds", e);
            }
        } catch (Exception e) {
            Gdx.app.error("Assets", "Error initializing assets", e);
            throw e;  // Main에서 처리할 수 있도록 예외를 다시 던짐
        }
    }

    private void handleLoadingError() {
        // 에러 발생시 기본 에셋으로 대체하거나 에러 처리
        if (skin == null) {
            skin = new Skin();  // 기본 스킨 생성
        }
        if (uiFont == null) {
            uiFont = new BitmapFont();  // 기본 폰트 생성
        }
        // 다른 필수 에셋들에 대한 기본값 설정
    }

    // NullPointerException 방지를 위한 메서드 추가
    public boolean isLoaded() {
        return skin != null && uiFont != null && sceneAsset != null;
    }

    private void initConstants() {
        try {
            skin = assets.get("ui/uiskin.json");
            uiFont = assets.get("font/Amble-Regular-26.fnt");
            sceneAsset = assets.get(Settings.GLTF_FILE);
            sceneAsset.maxBones = 50; // 100에서 50으로 줄임
            scopeImage = assets.get("images/scope.png");
            sounds = new AssetSounds();
            Gdx.app.log("Assets", "maxBones set to: " + sceneAsset.maxBones);
        } catch (Exception e) {
            Gdx.app.error("Assets", "Error initializing constants", e);
        }
    }

    public <T> T get(String name ) {
        return assets.get(name);
    }

    @Override
    public void dispose() {
        Gdx.app.log("Assets dispose()", "");
        assets.dispose();
        // assets = null;
    }

    public static void disposeInstance() {
        synchronized(LOCK) {
            if (instance != null) {
                instance.dispose();
                instance = null;
            }
        }
    }
}
