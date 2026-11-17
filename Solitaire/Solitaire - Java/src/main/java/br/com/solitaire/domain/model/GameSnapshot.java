package br.com.solitaire.domain.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Fotografia somente leitura do estado. Os atributos de visibilidade são
 * copiados para que uma fotografia antiga não mude após uma jogada posterior.
 */
public record GameSnapshot(
        List<CardSnapshot> stock,
        List<CardSnapshot> waste,
        Map<Suit, List<CardSnapshot>> foundations,
        List<List<CardSnapshot>> tableau,
        int historySize,
        int score,
        boolean won) {

    public GameSnapshot {
        stock = List.copyOf(Objects.requireNonNull(stock));
        waste = List.copyOf(Objects.requireNonNull(waste));

        EnumMap<Suit, List<CardSnapshot>> foundationCopy = new EnumMap<>(Suit.class);
        Objects.requireNonNull(foundations).forEach((suit, cards) ->
                foundationCopy.put(suit, List.copyOf(cards)));
        for (Suit suit : Suit.values()) {
            foundationCopy.putIfAbsent(suit, List.of());
        }
        foundations = Collections.unmodifiableMap(foundationCopy);

        List<List<CardSnapshot>> tableauCopy = new ArrayList<>();
        for (List<CardSnapshot> column : Objects.requireNonNull(tableau)) {
            tableauCopy.add(List.copyOf(column));
        }
        tableau = List.copyOf(tableauCopy);
    }

    public Optional<CardSnapshot> stockTop() {
        return stock.isEmpty() ? Optional.empty() : Optional.of(stock.getLast());
    }

    public Optional<CardSnapshot> wasteTop() {
        return waste.isEmpty() ? Optional.empty() : Optional.of(waste.getLast());
    }

    public Optional<CardSnapshot> foundationTop(Suit suit) {
        List<CardSnapshot> pile = foundations.get(suit);
        return pile.isEmpty() ? Optional.empty() : Optional.of(pile.getLast());
    }

    public Optional<CardSnapshot> tableauTop(int columnIndex) {
        List<CardSnapshot> column = tableau.get(columnIndex);
        return column.isEmpty() ? Optional.empty() : Optional.of(column.getLast());
    }
}
