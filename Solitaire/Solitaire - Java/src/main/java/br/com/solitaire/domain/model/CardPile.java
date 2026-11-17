package br.com.solitaire.domain.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Estrutura de pilha; a lista interna mantém a ordem da base para o topo. */
public final class CardPile {
    private final List<Card> cards = new ArrayList<>();

    public boolean isEmpty() {
        return cards.isEmpty();
    }

    public int size() {
        return cards.size();
    }

    public Optional<Card> top() {
        return cards.isEmpty() ? Optional.empty() : Optional.of(cards.getLast());
    }

    public void push(Card card) {
        cards.add(Objects.requireNonNull(card, "carta não pode ser nula"));
    }

    public void pushAll(List<Card> cardsToAdd) {
        Objects.requireNonNull(cardsToAdd, "cartas não podem ser nulas");
        cards.addAll(cardsToAdd);
    }

    public Card pop() {
        if (cards.isEmpty()) {
            throw new IllegalStateException("Não é possível remover cartas de uma pilha vazia");
        }
        return cards.removeLast();
    }

    /** Remove a sequência do topo, retornando-a da base para o topo. */
    public List<Card> removeTop(int quantity) {
        if (quantity < 0 || quantity > cards.size()) {
            throw new IllegalArgumentException("Quantidade inválida para remoção da pilha");
        }
        int start = cards.size() - quantity;
        List<Card> removed = List.copyOf(cards.subList(start, cards.size()));
        cards.subList(start, cards.size()).clear();
        return removed;
    }

    public List<Card> removeAll() {
        return removeTop(cards.size());
    }

    /** Retorna uma cópia somente leitura, da base para o topo. */
    public List<Card> cards() {
        return List.copyOf(cards);
    }

    public List<Card> sequenceFrom(Card card) {
        int index = indexOfIdentity(card);
        if (index < 0) {
            return List.of();
        }
        return List.copyOf(cards.subList(index, cards.size()));
    }

    public boolean containsIdentity(Card card) {
        return indexOfIdentity(card) >= 0;
    }

    private int indexOfIdentity(Card card) {
        for (int index = 0; index < cards.size(); index++) {
            if (cards.get(index) == card) {
                return index;
            }
        }
        return -1;
    }
}
