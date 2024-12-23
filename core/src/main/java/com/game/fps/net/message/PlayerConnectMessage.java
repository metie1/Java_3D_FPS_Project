package com.game.fps.net.message;

public class PlayerConnectMessage extends NetworkMessage {
    public String playerName;
    public String playerTeam;
    public PlayerConnectMessage() {
        playerName = "" ;playerTeam = "";
    }
}
