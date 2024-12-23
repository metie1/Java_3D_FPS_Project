package com.game.fps.gui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.game.fps.Main;
import com.game.fps.WeaponState;
import com.game.fps.WeaponType;

public class ShopOverlay {
    private final Stage stage;
    private final Window window;
    private boolean visible;
    private final WeaponState weaponState;
    private final ShopCallback callback;

    private static final int PISTOL_PRICE = 500;
    private static final int AK47_PRICE = 2000;
    private static final int SG553_PRICE = 2500;
    private static final int AR15_PRICE = 2300;
    private int playerMoney = 100000; // 초기 돈
    private Label moneyLabel;

    public interface ShopCallback {
        void onResume();
    }

    public ShopOverlay(WeaponState weaponState, ShopCallback callback) {
        this.weaponState = weaponState;
        this.callback = callback;
        stage = new Stage(new ScreenViewport());

        window = new Window("Shop", Main.assets.skin);
        window.setMovable(false);

        Table content = new Table();
        content.defaults().pad(10);

        moneyLabel = new Label("Money: $" + playerMoney, Main.assets.skin);
        content.add(moneyLabel).left().row();

        TextButton ak47Button = new TextButton("Buy AK-47 ($" + AK47_PRICE + ")", Main.assets.skin);
        ak47Button.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (playerMoney >= AK47_PRICE) {
                    purchaseWeapon(WeaponType.AK47, AK47_PRICE);
                }
            }
        });

        TextButton sg553Button = new TextButton("Buy SG553 ($" + SG553_PRICE + ")", Main.assets.skin);
        sg553Button.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (playerMoney >= SG553_PRICE) {
                    purchaseWeapon(WeaponType.SG553, SG553_PRICE);
                }
            }
        });

        TextButton ar15Button = new TextButton("Buy AR15 ($" + AR15_PRICE + ")", Main.assets.skin);
        ar15Button.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (playerMoney >= AR15_PRICE) {
                    purchaseWeapon(WeaponType.AR15, AR15_PRICE);
                }
            }
        });

        TextButton pistolButton = new TextButton("Buy Pistol ($" + PISTOL_PRICE + ")", Main.assets.skin);
        pistolButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (playerMoney >= PISTOL_PRICE && !weaponState.havePistol) {
                    playerMoney -= PISTOL_PRICE;
                    weaponState.havePistol = true;
                    moneyLabel.setText("Money: $" + playerMoney);
                    Main.assets.sounds.UPGRADE.play();
                }
            }
        });

        TextButton resumeButton = new TextButton("Resume", Main.assets.skin);
        resumeButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                hide();
                if (callback != null) {
                    callback.onResume();
                }
            }
        });

        content.add(pistolButton).width(200).padBottom(10).row();
        content.add(ak47Button).width(200).padBottom(10).row();
        content.add(sg553Button).width(200).padBottom(10).row();
        content.add(ar15Button).width(200).padBottom(20).row();
        content.add(resumeButton).width(200).row();

        window.add(content).pad(20);
        window.pack();

        window.setPosition(
            (Gdx.graphics.getWidth() - window.getWidth()) * 0.5f,
            (Gdx.graphics.getHeight() - window.getHeight()) * 0.5f
        );

        stage.addActor(window);
        hide();
    }

    private void purchaseWeapon(WeaponType weaponType, int price) {
        if (playerMoney >= price) {
            playerMoney -= price;
            switch (weaponType) {
                case AK47:
                    weaponState.removeMainWeapons(); // 기존 무기 제거
                    weaponState.haveAK47 = true;
                    break;
                case SG553:
                    weaponState.removeMainWeapons();
                    weaponState.haveSG553 = true;
                    break;
                case AR15:
                    weaponState.removeMainWeapons();
                    weaponState.haveAR15 = true;
                    break;
            }
            moneyLabel.setText("Money: $" + playerMoney);
            Main.assets.sounds.UPGRADE.play();
        }
    }

    public void show() {
        visible = true;
        window.setVisible(true);
    }

    public void hide() {
        visible = false;
        window.setVisible(false);
        if (callback != null) {
            callback.onResume();
        }
    }

    public boolean isVisible() {
        return visible;
    }

    public Stage getStage() {
        return stage;
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

    public void dispose() {
        if (stage != null) {
            stage.dispose();
        }
    }
}
