package me.krunsh.kcraft.compiled;

/** Cellule non-vide d'un pattern SHAPED compile. */
public final class CompiledPatternCell {

    private final int row;
    private final int column;
    private final char symbol;
    private final CompiledIngredient ingredient;

    public CompiledPatternCell(
            int row,
            int column,
            char symbol,
            CompiledIngredient ingredient) {

        this.row = row;
        this.column = column;
        this.symbol = symbol;
        this.ingredient = ingredient;
    }

    public int getRow() {
        return row;
    }

    public int getColumn() {
        return column;
    }

    public char getSymbol() {
        return symbol;
    }

    public CompiledIngredient getIngredient() {
        return ingredient;
    }
}
