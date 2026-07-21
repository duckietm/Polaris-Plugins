package com.furnibuilder.wordguesser.commands;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.commands.Command;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.rooms.RoomChatMessageBubbles;
import com.eu.habbo.habbohotel.users.Habbo;
import com.furnibuilder.wordguesser.WordGuesser;

public class RandomWordCommand extends Command {
    public RandomWordCommand(String permission, String[] keys) {
        super(permission, keys);
    }

    @Override
    public boolean handle(GameClient gameClient, String[] strings) throws Exception {
        Habbo habbo = gameClient.getHabbo();
        if (habbo == null) {
            return true;
        }

        if (strings.length != 2) {
            habbo.whisper(WordGuesser.text("randomword.cmd_randomword.error",
                    "Please fill in a word"), RoomChatMessageBubbles.WIRED);
            return true;
        }

        switch (WordGuesser.guess(strings[1])) {
            case NO_WORD -> habbo.whisper(WordGuesser.text("randomword.cmd_randomword.no_word",
                    "There is no word to guess right now"), RoomChatMessageBubbles.BOT_GUIDE);
            case WRONG -> habbo.whisper(WordGuesser.text("randomword.cmd_randomword.wrong",
                    "You guessed the wrong word"), RoomChatMessageBubbles.FRANK);
            case WINNER -> rewardWinner(habbo);
        }
        return true;
    }

    private static void rewardWinner(Habbo winner) {
        int currencyType = Emulator.getConfig().getInt("randomword.prize.currency_type", 5);
        int currencyAmount = Math.max(0, Emulator.getConfig().getInt("randomword.prize.currency_amount", 1));

        if (currencyAmount > 0) {
            if (currencyType == -1) {
                winner.giveCredits(currencyAmount);
            } else {
                winner.givePoints(currencyType, currencyAmount);
            }
        }

        String badgeCode = Emulator.getConfig().getValue("randomword.prize.badge", "");
        if (badgeCode != null && !badgeCode.isBlank()) {
            winner.addBadge(badgeCode);
        }

        String announcement = WordGuesser.text("randomword.winner",
                "User %username% got the right word \"%word%\" and won the prize.")
                .replace("%word%", WordGuesser.getWord())
                .replace("%username%", winner.getHabboInfo().getUsername());

        for (Habbo habbo : Emulator.getGameEnvironment().getHabboManager().getOnlineHabbos().values()) {
            if (habbo.getHabboInfo().getCurrentRoom() == null) {
                habbo.alert(announcement);
            } else {
                habbo.whisper(announcement, RoomChatMessageBubbles.ALERT);
            }
        }

        winner.whisper(WordGuesser.text("randomword.cmd_randomword.success",
                "You got the word right! Congrats."), RoomChatMessageBubbles.SANTA);
    }
}
