package com.furnibuilder.wordguesser.commands;

import com.eu.habbo.habbohotel.commands.Command;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.rooms.RoomChatMessageBubbles;
import com.furnibuilder.wordguesser.WordGuesser;
import com.furnibuilder.wordguesser.WordManager;

public class UpdateWordsCommand extends Command {
    public UpdateWordsCommand(String permission, String[] keys) {
        super(permission, keys);
    }

    @Override
    public boolean handle(GameClient gameClient, String[] strings) throws Exception {
        if (gameClient.getHabbo() == null) {
            return true;
        }

        WordManager wordManager = WordGuesser.getWordManager();
        if (wordManager != null) {
            wordManager.reload();
        }

        gameClient.getHabbo().whisper(WordGuesser.text("randomword.cmd_update_words.success",
                "Successfully updated random words"), RoomChatMessageBubbles.ALERT);
        return true;
    }
}
