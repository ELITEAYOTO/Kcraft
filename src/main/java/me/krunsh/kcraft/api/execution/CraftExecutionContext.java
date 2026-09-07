package me.krunsh.kcraft.api.execution;

import java.util.UUID;

/**
 * Contexte main-thread / thread-local de l'exécution en cours.
 *
 * Il permet au CraftManager historique de continuer à créer
 * KcraftPostCraftEvent avec son ancien constructeur, tout en enrichissant
 * automatiquement l'event avec source/transaction/batchIndex.
 *
 * Aucune donnée Player n'est retenue.
 */
public final class CraftExecutionContext {

    private static final ThreadLocal<State> CURRENT =
        new ThreadLocal<State>();

    private CraftExecutionContext() {}

    public static Scope enter(
            CraftExecutionSource source,
            UUID transactionId,
            int batchIndex,
            int plannedBatchSize) {

        State previous =
            CURRENT.get();

        CURRENT.set(
            new State(
                source == null
                    ? CraftExecutionSource.UNKNOWN
                    : source,
                transactionId,
                batchIndex,
                plannedBatchSize
            )
        );

        return new Scope(previous);
    }

    public static Snapshot snapshot() {
        State state =
            CURRENT.get();

        if (state == null) {
            return Snapshot.unknown();
        }

        return new Snapshot(
            state.source,
            state.transactionId,
            state.batchIndex,
            state.plannedBatchSize
        );
    }

    private static final class State {
        private final CraftExecutionSource source;
        private final UUID transactionId;
        private final int batchIndex;
        private final int plannedBatchSize;

        private State(
                CraftExecutionSource source,
                UUID transactionId,
                int batchIndex,
                int plannedBatchSize) {

            this.source = source;
            this.transactionId = transactionId;
            this.batchIndex = batchIndex;
            this.plannedBatchSize = plannedBatchSize;
        }
    }

    public static final class Snapshot {
        private final CraftExecutionSource source;
        private final UUID transactionId;
        private final int batchIndex;
        private final int plannedBatchSize;

        private Snapshot(
                CraftExecutionSource source,
                UUID transactionId,
                int batchIndex,
                int plannedBatchSize) {

            this.source = source;
            this.transactionId = transactionId;
            this.batchIndex = batchIndex;
            this.plannedBatchSize = plannedBatchSize;
        }

        private static Snapshot unknown() {
            return new Snapshot(
                CraftExecutionSource.UNKNOWN,
                null,
                -1,
                1
            );
        }

        public CraftExecutionSource getSource() {
            return source;
        }

        public UUID getTransactionId() {
            return transactionId;
        }

        public int getBatchIndex() {
            return batchIndex;
        }

        public int getPlannedBatchSize() {
            return plannedBatchSize;
        }
    }

    public static final class Scope
            implements AutoCloseable {

        private final State previous;
        private boolean closed;

        private Scope(State previous) {
            this.previous = previous;
        }

        @Override
        public void close() {
            if (closed) {
                return;
            }

            closed = true;

            if (previous == null) {
                CURRENT.remove();
            } else {
                CURRENT.set(previous);
            }
        }
    }
}
