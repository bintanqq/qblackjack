package me.bintanq.qBlackjack.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Deck {
    private final List<Card> cards;
    private static final int NUM_DECKS = 1;

    public Deck() {
        this.cards = new ArrayList<>();
        initializeDeck(NUM_DECKS);
        shuffle();
    }

    private void initializeDeck(int numDecks) {
        String[] suits = {"Hearts", "Diamonds", "Clubs", "Spades"};
        String[] ranks = {"Ace", "2", "3", "4", "5", "6", "7", "8", "9", "10", "Jack", "Queen", "King"};

        for (int d = 0; d < numDecks; d++) {
            for (String suit : suits) {
                for (String rank : ranks) {
                    int value = determineValue(rank);
                    cards.add(new Card(rank, suit, value));
                }
            }
        }
    }

    private int determineValue(String rank) {
        try {
            return Integer.parseInt(rank);
        } catch (NumberFormatException e) {
            switch (rank) {
                case "Ace": return 11;
                case "King": case "Queen": case "Jack": return 10;
                default: return 0;
            }
        }
    }

    public void shuffle() { Collections.shuffle(cards); }

    public Card drawCard() {
        if (cards.isEmpty()) {
            initializeDeck(NUM_DECKS);
            shuffle();
        }
        return cards.remove(cards.size() - 1);
    }
}