package com.furnibuilder.buildheight.network;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.messages.incoming.MessageHandler;

public class SetBuildHeightEvent extends MessageHandler {

    private static final int RATELIMIT_MS = 100;

    @Override
    public int getRatelimit() {
        return RATELIMIT_MS;
    }

    @Override
    public void handle() throws Exception {
        boolean enabled = this.packet.readBoolean();
        int heightHundredths = this.packet.readInt();

        Habbo habbo = this.client.getHabbo();
        if (habbo == null) {
            return;
        }

        Room room = this.currentRoom();
        if (room == null) {
            return;
        }

        if (!BuildHeightProtocol.canUse(habbo, room)) {
            return;
        }

        if (!enabled) {
            habbo.getHabboStats().cache.remove(BuildHeightProtocol.BUILD_HEIGHT_KEY);
            return;
        }

        double height = heightHundredths / BuildHeightProtocol.HEIGHT_SCALE;

        int min = Emulator.getConfig().getInt("building.cmd_buildheight.min_height");
        int max = Emulator.getConfig().getInt("building.cmd_buildheight.max_height");

        if (height < min || height > max) {
            return;
        }

        habbo.getHabboStats().cache.put(BuildHeightProtocol.BUILD_HEIGHT_KEY, height);
    }
}
