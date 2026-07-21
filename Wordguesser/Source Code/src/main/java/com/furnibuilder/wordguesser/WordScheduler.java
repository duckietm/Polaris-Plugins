package com.furnibuilder.wordguesser;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.rooms.RoomChatMessageBubbles;
import com.eu.habbo.habbohotel.users.Habbo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WordScheduler implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(WordScheduler.class);

    private static final int MIN_INTERVAL_SECONDS = 60;
    private static final int MIN_GUESS_WINDOW_SECONDS = 30;

    private final long intervalMs;
    private final long guessWindowMs;

    public WordScheduler() {
        this.intervalMs = 1000L * Math.max(MIN_INTERVAL_SECONDS,
                Emulator.getConfig().getInt("randomword.interval_seconds", 1800));
        this.guessWindowMs = 1000L * Math.max(MIN_GUESS_WINDOW_SECONDS,
                Emulator.getConfig().getInt("randomword.guess_window_seconds", 300));
        Emulator.getThreading().run(this, this.intervalMs);
    }

    @Override
    public void run() {
        try {
            sendNewRandomWord();
        } catch (Exception e) {
            log.error("[WordGuesser] Failed to announce a new word", e);
        } finally {
            Emulator.getThreading().run(this, this.intervalMs);
        }
    }

    public void sendNewRandomWord() {
        WordManager wordManager = WordGuesser.getWordManager();
        String newWord = (wordManager == null) ? null : wordManager.getRandomWord();
        if (newWord == null) {
            return;
        }

        String scrambled = scramble(newWord);
        WordGuesser.startRound(newWord, scrambled);

        String announcement = WordGuesser.text("randomword.new_word",
                "The word is scrambled. Beat it! \"%word%\" use :rw (word)")
                .replace("%word%", scrambled);
        broadcast(announcement, RoomChatMessageBubbles.RADIO);

        Emulator.getThreading().run(() -> {
            if (WordGuesser.expireRound(newWord)) {
                broadcast(WordGuesser.text("randomword.nobody",
                        "Nobody got the right word, the word was %word%")
                        .replace("%word%", newWord), RoomChatMessageBubbles.GOAT);
            }
        }, this.guessWindowMs);
    }

    private static void broadcast(String message, RoomChatMessageBubbles bubble) {
        for (Habbo habbo : Emulator.getGameEnvironment().getHabboManager().getOnlineHabbos().values()) {
            if (habbo.getHabboInfo().getCurrentRoom() == null) {
                habbo.alert(message);
            } else {
                habbo.whisper(message, bubble);
            }
        }
    }

    public String scramble(String inputString) {
        char[] a = inputString.toCharArray();
        for (int attempt = 0; attempt < 10; attempt++) {
            for (int i = a.length - 1; i > 0; i--) {
                int j = Emulator.getRandom().nextInt(i + 1);
                char temp = a[i];
                a[i] = a[j];
                a[j] = temp;
            }
            String scrambled = new String(a);
            if (!scrambled.equalsIgnoreCase(inputString)) {
                return scrambled;
            }
        }
        return new String(a);
    }
}
