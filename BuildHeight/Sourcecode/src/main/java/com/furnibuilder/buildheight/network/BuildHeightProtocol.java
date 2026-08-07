package com.furnibuilder.buildheight.network;

import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.users.Habbo;

public final class BuildHeightProtocol {

    public static final int BUILD_HEIGHT_AVAILABLE = 9350;

    public static final int SET_BUILD_HEIGHT = 9351;

    public static final double HEIGHT_SCALE = 100.0D;

    public static final String BUILD_HEIGHT_KEY = "building.build_height";

    public static final String PERMISSION = "cmd_buildheight";

    public static boolean canUse(Habbo habbo, Room room) {
        if (habbo == null || room == null) {
            return false;
        }
        boolean hasRights = room.hasRights(habbo);
        return hasRights && habbo.hasPermission(PERMISSION, hasRights);
    }

    private BuildHeightProtocol() {
    }
}
