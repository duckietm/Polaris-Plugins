package com.furnibuilder.wordguesser;

import com.eu.habbo.Emulator;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WordManager {
    private static final Logger log = LoggerFactory.getLogger(WordManager.class);

    private static final int MAX_WORD_LENGTH = 64;
    private volatile List<String> words = List.of();

    public WordManager() {
        reload();
    }

    public void reload() {
        List<String> loaded = new ArrayList<>();
        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT `word` FROM `random_words`");
             ResultSet set = statement.executeQuery()) {
            while (set.next()) {
                String word = set.getString("word");
                if (word == null) {
                    continue;
                }
                word = word.trim();
                if (word.isEmpty() || word.length() > MAX_WORD_LENGTH) {
                    continue;
                }
                loaded.add(word);
            }
        } catch (SQLException e) {
            log.error("[WordGuesser] Failed to load words from `random_words`. Did the wordguesser migration run?", e);
        }

        this.words = List.copyOf(loaded);

        if (this.words.isEmpty()) {
            log.warn("[WordGuesser] The `random_words` table is empty or missing — no words will be announced.");
        }
    }

    public int size() {
        return this.words.size();
    }

    public String getRandomWord() {
        List<String> current = this.words;
        if (current.isEmpty()) {
            return null;
        }
        return current.get(Emulator.getRandom().nextInt(current.size()));
    }
}
