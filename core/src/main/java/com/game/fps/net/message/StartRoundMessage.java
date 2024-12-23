package com.game.fps.net.message;

public class StartRoundMessage extends NetworkMessage {
    public int roundNumber;

    public StartRoundMessage() {
    }

    public StartRoundMessage(int roundNumber) {
        this.roundNumber = roundNumber;
    }
}
