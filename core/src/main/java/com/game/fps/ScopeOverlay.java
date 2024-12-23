package com.game.fps;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Disposable;

public class ScopeOverlay implements Disposable {
    private final SpriteBatch batch;
    private final Texture scopeTexture;
    private float recoilTimer;              // > 0이면 반동 효과 적용

    public ScopeOverlay() {
        this.batch = new SpriteBatch();
        scopeTexture = Main.assets.scopeImage;
        recoilTimer = 0;
    }

    public void startRecoilEffect() {
        recoilTimer = 0.5f; // 0.5초 동안 타이머 시작
    }

    public void render(float delta) {
        float effect = 0;

        recoilTimer -= delta;
        if (recoilTimer > 0)     // 발사 시 반동 효과 적용
            effect = recoilTimer * 50f;     // 발사 중 이미지 크기 조정

        batch.begin();
        batch.draw(scopeTexture, -effect, -effect, Gdx.graphics.getWidth() + 2 * effect, Gdx.graphics.getHeight() + 2 * effect);
        batch.end();
    }

    public void resize(int width, int height) {
        batch.getProjectionMatrix().setToOrtho2D(0, 0, width, height);
    }

    @Override
    public void dispose() {
        batch.dispose();
    }
}
