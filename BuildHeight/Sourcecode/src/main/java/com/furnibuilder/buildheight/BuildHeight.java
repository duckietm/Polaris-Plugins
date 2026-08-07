package com.furnibuilder.buildheight;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.plugin.EventHandler;
import com.eu.habbo.plugin.EventListener;
import com.eu.habbo.plugin.HabboPlugin;
import com.eu.habbo.plugin.events.emulator.EmulatorLoadedEvent;
import com.eu.habbo.plugin.events.furniture.FurnitureBuildheightEvent;
import com.eu.habbo.plugin.events.users.UserEnterRoomEvent;
import com.eu.habbo.plugin.events.users.UserExitRoomEvent;
import com.eu.habbo.plugin.events.users.UserRightsGivenEvent;
import com.eu.habbo.plugin.events.users.UserRightsTakenEvent;
import com.furnibuilder.buildheight.network.BuildHeightAvailableComposer;
import com.furnibuilder.buildheight.network.BuildHeightProtocol;
import com.furnibuilder.buildheight.network.SetBuildHeightEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BuildHeight extends HabboPlugin implements EventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(BuildHeight.class);

    public static BuildHeight INSTANCE;

    private volatile boolean registered;

    @Override
    public void onEnable() {
        INSTANCE = this;
        Emulator.getPluginManager().registerEvents(this, this);

        if (Emulator.isReady) {
            this.register();
        }

        LOGGER.info("[BuildHeight] Enabled.");
    }

    @Override
    public void onDisable() {
        LOGGER.info("[BuildHeight] Disabled.");
    }

    @Override
    public boolean hasPermission(Habbo habbo, String key) {
        return false;
    }

    @EventHandler
    public static void onEmulatorLoaded(EmulatorLoadedEvent event) {
        if (INSTANCE != null) {
            INSTANCE.register();
        }
    }

    private synchronized void register() {
        if (this.registered) {
            return;
        }
        this.registered = true;

        Emulator.getConfig().register("building.cmd_buildheight.min_height", "-40");
        Emulator.getConfig().register("building.cmd_buildheight.max_height", "40");

        try {
            Emulator.getGameServer().getPacketManager()
                    .registerHandler(BuildHeightProtocol.SET_BUILD_HEIGHT, SetBuildHeightEvent.class);
        } catch (Exception e) {
            LOGGER.debug("[BuildHeight] SetBuildHeight handler already registered: {}", e.getMessage());
        }
    }

    @EventHandler
    public static void onUserEnterRoom(UserEnterRoomEvent event) {
        sendAvailability(event.habbo, event.room);
    }

    @EventHandler
    public static void onUserRightsGiven(UserRightsGivenEvent event) {
        Habbo receiver = event.receiver;
        if (receiver != null) {
            sendAvailability(receiver, receiver.getHabboInfo().getCurrentRoom());
        }
    }

    @EventHandler
    public static void onUserRightsTaken(UserRightsTakenEvent event) {
        Habbo victim = event.victim;
        if (victim != null) {
            victim.getHabboStats().cache.remove(BuildHeightProtocol.BUILD_HEIGHT_KEY);
            sendAvailability(victim, victim.getHabboInfo().getCurrentRoom());
        }
    }

    private static void sendAvailability(Habbo habbo, Room room) {
        if (habbo == null || habbo.getClient() == null) {
            return;
        }

        boolean available = BuildHeightProtocol.canUse(habbo, room);

        int min = Emulator.getConfig().getInt("building.cmd_buildheight.min_height");
        int max = Emulator.getConfig().getInt("building.cmd_buildheight.max_height");

        habbo.getClient().sendResponse(new BuildHeightAvailableComposer(available, min, max));
    }

    @EventHandler
    public static void onFurnitureBuildheightEvent(FurnitureBuildheightEvent event) {
        if (event.habbo == null) {
            return;
        }

        if (event.habbo.getHabboStats().cache.get(BuildHeightProtocol.BUILD_HEIGHT_KEY) instanceof Double height) {
            event.setNewHeight(height);
        }
    }

    @EventHandler
    public static void onUserExitRoomEvent(UserExitRoomEvent event) {
        if (event.habbo != null) {
            event.habbo.getHabboStats().cache.remove(BuildHeightProtocol.BUILD_HEIGHT_KEY);
        }
    }
}
