package br.com.solitaire.domain.model;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;

class CardSequenceSerializationTest {

    @Test
    void shouldPreserveCardIdentityAndSequenceOrderAfterSerialization() throws Exception {
        Card first = new Card(Suit.HEARTS, Rank.ACE);
        Card second = new Card(Suit.SPADES, Rank.TWO);
        CardSequence original = new CardSequence(List.of(first, second, first));

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream output = new ObjectOutputStream(bytes)) {
            output.writeObject(original);
        }

        CardSequence restored;
        try (ObjectInputStream input = new ObjectInputStream(
                new ByteArrayInputStream(bytes.toByteArray()))) {
            restored = (CardSequence) input.readObject();
        }

        assertSame(restored.cards().get(0), restored.cards().get(2));
        assertSame(restored.cards().get(0).suit(), Suit.HEARTS);
        assertSame(restored.cards().get(1).suit(), Suit.SPADES);
        assertSame(restored.cards().get(0).rank(), Rank.ACE);
        assertSame(restored.cards().get(1).rank(), Rank.TWO);
    }
}
