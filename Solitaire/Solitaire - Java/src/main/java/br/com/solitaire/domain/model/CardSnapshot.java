package br.com.solitaire.domain.model;

import java.util.Objects;

/** Estado imutável de uma carta no instante da consulta. */
public record CardSnapshot(Card card, boolean faceUp) {
    public CardSnapshot {
        Objects.requireNonNull(card, "carta não pode ser nula");
    }

    public Suit suit() {
        return card.suit();
    }

    public Rank rank() {
        return card.rank();
    }

    public String id() {
        return card.id();
    }
}
