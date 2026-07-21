package com.furnibuilder.wordguesser;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.commands.CommandHandler;
import com.eu.habbo.habbohotel.rooms.RoomChatMessageBubbles;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.plugin.EventHandler;
import com.eu.habbo.plugin.EventListener;
import com.eu.habbo.plugin.HabboPlugin;
import com.eu.habbo.plugin.events.emulator.EmulatorLoadedEvent;
import com.eu.habbo.plugin.events.users.UserEnterRoomEvent;
import com.eu.habbo.plugin.events.users.UserLoginEvent;
import com.furnibuilder.wordguesser.commands.RandomWordCommand;
import com.furnibuilder.wordguesser.commands.UpdateWordsCommand;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WordGuesser extends HabboPlugin implements EventListener {
    private static final Logger log = LoggerFactory.getLogger(WordGuesser.class);
    public static final String HAS_LOGGED_IN_KEY = "wordguesser.has.logged.in";
    private static final AtomicBoolean INITIALIZED = new AtomicBoolean(false);
    private static volatile WordManager wordManager = null;
    private static final Object WORD_LOCK = new Object();
    private static boolean hasBeenGuessed = true;
    private static String word = "";
    private static String scrambledWord = "";

    public enum GuessResult { NO_WORD, WRONG, WINNER }

    @Override
    public void onEnable() {
        Emulator.getPluginManager().registerEvents(this, this);
        if (Emulator.isReady) {
            initialize();
        }
        log.info("[WordGuesser] Started Word Guesser Plugin!");
    }

    @Override
    public void onDisable() {
        log.info("[WordGuesser] Stopped Word Guesser Plugin!");
    }

    @EventHandler
    public static void onEmulatorLoaded(EmulatorLoadedEvent event) {
        initialize();
    }

    @Override
    public boolean hasPermission(Habbo habbo, String key) {
        return false;
    }

    private static void initialize() {
        if (!INITIALIZED.compareAndSet(false, true)) {
            return;
        }

        wordManager = new WordManager();
        CommandHandler.addCommand(new RandomWordCommand("cmd_randomword",
                text("commands.keys.cmd_randomword", "rw").split(";")));
        CommandHandler.addCommand(new UpdateWordsCommand("cmd_update_words",
                text("commands.keys.cmd_update_words", "update_words").split(";")));
        new WordScheduler();
    }

    public static String text(String key, String defaultValue) {
        String value = Emulator.getTexts().getValue(key, defaultValue);
        return (value == null || value.isEmpty()) ? defaultValue : value;
    }

    @EventHandler
    public static void onUserLoginEvent(UserLoginEvent event) {
        if (event.habbo == null) {
            return;
        }
        event.habbo.getHabboStats().cache.put(HAS_LOGGED_IN_KEY, Boolean.TRUE);
    }

    @EventHandler
    public static void onUserEnterRoomEvent(UserEnterRoomEvent event) {
        if (event.habbo == null || event.room == null) {
            return;
        }
        if (!event.habbo.getHabboStats().cache.containsKey(HAS_LOGGED_IN_KEY)) {
            return;
        }
        event.habbo.getHabboStats().cache.remove(HAS_LOGGED_IN_KEY);

        final String scrambled = getScrambledWord();
        if (getGuessed() || scrambled.isEmpty()) {
            return;
        }
        Emulator.getThreading().run(() -> {
            if (event.habbo.getRoomUnit() == null || getGuessed()) {
                return;
            }
            event.habbo.whisper(text("randomword.new_word",
                    "The word is scrambled. Beat it! \"%word%\" use :rw (word)")
                    .replace("%word%", scrambled), RoomChatMessageBubbles.WIRED);
        }, 1000L);
    }

    public static WordManager getWordManager() {
        return wordManager;
    }

    public static void startRound(String newWord, String scrambled) {
        synchronized (WORD_LOCK) {
            word = newWord;
            scrambledWord = scrambled;
            hasBeenGuessed = false;
        }
    }

    public static GuessResult guess(String attempt) {
        if (attempt == null || attempt.isBlank()) {
            return GuessResult.WRONG;
        }
        synchronized (WORD_LOCK) {
            if (hasBeenGuessed || word.isEmpty()) {
                return GuessResult.NO_WORD;
            }
            if (!word.equalsIgnoreCase(attempt.trim())) {
                return GuessResult.WRONG;
            }
            hasBeenGuessed = true;
            return GuessResult.WINNER;
        }
    }

    public static boolean expireRound(String expectedWord) {
        synchronized (WORD_LOCK) {
            if (hasBeenGuessed || !word.equals(expectedWord)) {
                return false;
            }
            hasBeenGuessed = true;
            return true;
        }
    }

    public static String getWord() {
        synchronized (WORD_LOCK) {
            return word;
        }
    }

    public static String getScrambledWord() {
        synchronized (WORD_LOCK) {
            return scrambledWord;
        }
    }

    public static boolean getGuessed() {
        synchronized (WORD_LOCK) {
            return hasBeenGuessed;
        }
    }
}
