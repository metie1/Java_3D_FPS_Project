package com.game.fps;

public class AmmoInfo {
    private int currentAmmo;
    private final int maxAmmo;
    private int reserveAmmo;
    private final int maxReserveAmmo;

    public AmmoInfo(int maxAmmo, int maxReserveAmmo) {
        this.maxAmmo = maxAmmo;
        this.maxReserveAmmo = maxReserveAmmo;
        this.currentAmmo = maxAmmo;
        this.reserveAmmo = maxReserveAmmo;
    }

    public boolean canShoot() {
        return currentAmmo > 0;
    }

    public void shoot() {
        if (currentAmmo > 0) {
            currentAmmo--;
        }
    }

    public boolean reload() {
        if (reserveAmmo <= 0 || currentAmmo == maxAmmo) {
            return false;
        }

        int neededAmmo = maxAmmo - currentAmmo;
        int reloadAmount = Math.min(neededAmmo, reserveAmmo);

        currentAmmo += reloadAmount;
        reserveAmmo -= reloadAmount;
        return true;
    }

    public void refillAmmo() {
        reserveAmmo = maxReserveAmmo;
    }

    public int getCurrentAmmo() { return currentAmmo; }
    public int getReserveAmmo() { return reserveAmmo; }
    public int getMaxAmmo() { return maxAmmo; }
    public int getMaxReserveAmmo() { return maxReserveAmmo; }
}
