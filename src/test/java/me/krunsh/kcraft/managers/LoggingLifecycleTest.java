package me.krunsh.kcraft.managers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

import org.bukkit.Server;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;

import me.krunsh.kcraft.Kcraft;
import me.krunsh.kcraft.config.ConfigManager;
import me.krunsh.kcraft.logging.CraftLogRecord;

public class LoggingLifecycleTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    @Test public void enablingLoggingAfterStartupCreatesOnlyOneWorker() throws Exception {
        Fixture fixture = new Fixture(false, temporary.newFolder());
        fixture.manager.ensureWorker();
        verify(fixture.scheduler, never()).runTaskTimerAsynchronously(
            eq(fixture.plugin), any(Runnable.class), eq(40L), eq(40L));

        when(fixture.config.isLoggingEnabled()).thenReturn(true);
        fixture.manager.ensureWorker();
        fixture.manager.ensureWorker();
        when(fixture.config.isLoggingEnabled()).thenReturn(false);
        fixture.manager.ensureWorker();
        when(fixture.config.isLoggingEnabled()).thenReturn(true);
        fixture.manager.ensureWorker();

        verify(fixture.scheduler, times(1)).runTaskTimerAsynchronously(
            eq(fixture.plugin), any(Runnable.class), eq(40L), eq(40L));
        verify(fixture.task, never()).cancel();
    }

    @Test(timeout = 5000) public void saveAllStopsAfterOneDiskFailureAndKeepsEveryPendingRecord() throws Exception {
        Fixture fixture = new Fixture(true, temporary.newFile("blocked-logs"));
        Queue<CraftLogRecord> pending = queue(fixture.manager);
        for (int i = 1; i <= 501; i++) pending.offer(record(i));

        fixture.manager.saveAll();

        verify(fixture.task, times(1)).cancel();
        assertEquals(501, fixture.manager.getPendingLogCount());
        assertEquals(1L, fixture.manager.getMetrics().getFailures());
        assertEquals(0L, fixture.manager.getMetrics().getWritten());
        boolean[] retained = new boolean[502];
        for (CraftLogRecord record : pending) {
            int id = ((Long) record.toMap().get("id")).intValue();
            assertFalse("Record duplicated: " + id, retained[id]);
            retained[id] = true;
        }
        for (int i = 1; i <= 501; i++) assertTrue("Record lost: " + i, retained[i]);
        verify(fixture.logger).warning(contains("Erreur flush logs KCraft"));
        verify(fixture.logger).severe(contains("501 événement(s) non écrits"));
    }

    @Test(timeout = 5000) public void emptyQueueDoesNotProduceAFlushFailureOnAnInvalidDiskPath() throws Exception {
        Fixture fixture = new Fixture(true, temporary.newFile("blocked-empty-logs"));

        fixture.manager.saveAll();

        assertEquals(0, fixture.manager.getPendingLogCount());
        assertEquals(0L, fixture.manager.getMetrics().getFailures());
        verify(fixture.task).cancel();
        verify(fixture.logger, never()).warning(contains("Erreur flush logs KCraft"));
        verify(fixture.logger, never()).severe(any(String.class));
        verify(fixture.logger).warning(contains("Erreur sauvegarde stats KCraft"));
    }

    @Test(timeout = 5000) public void aLaterSaveRetriesRetainedRecordsEvenIfLoggingWasDisabled() throws Exception {
        Fixture fixture = new Fixture(true, temporary.newFile("blocked-retry-logs"));
        queue(fixture.manager).offer(record(1));
        fixture.manager.saveAll();
        assertEquals(1, fixture.manager.getPendingLogCount());

        File repairedFolder = temporary.newFolder("repaired-logs");
        when(fixture.config.getLogsFolder()).thenReturn(repairedFolder);
        when(fixture.config.isLoggingEnabled()).thenReturn(false);
        fixture.manager.saveAll();

        assertEquals(0, fixture.manager.getPendingLogCount());
        assertEquals(1L, fixture.manager.getMetrics().getWritten());
        assertEquals(1L, fixture.manager.getMetrics().getFailures());
        List<String> lines = Files.readAllLines(new File(repairedFolder, "history.jsonl").toPath(),
            StandardCharsets.UTF_8);
        assertEquals(1, lines.size());
        assertTrue(lines.get(0).contains("\"id\":1"));
        assertTrue(lines.get(0).contains("Élodie"));
        verify(fixture.task, times(1)).cancel();
        assertFalse(new File(repairedFolder, "stats-global.json").exists());
    }

    @Test(timeout = 15000) public void saveAllWaitsForTheInFlightWorkerBeforeCancellingAndWritingStats() throws Exception {
        File logsFolder = temporary.newFolder("in-flight-logs");
        Fixture fixture = new Fixture(true, logsFolder);
        queue(fixture.manager).offer(record(1));
        ArgumentCaptor<Runnable> workerAction = ArgumentCaptor.forClass(Runnable.class);
        verify(fixture.scheduler).runTaskTimerAsynchronously(
            eq(fixture.plugin), workerAction.capture(), eq(40L), eq(40L));

        CountDownLatch workerEntered = new CountDownLatch(1);
        CountDownLatch releaseWorker = new CountDownLatch(1);
        CountDownLatch saveStarted = new CountDownLatch(1);
        AtomicBoolean firstAccess = new AtomicBoolean(true);
        AtomicReference<Throwable> workerFailure = new AtomicReference<Throwable>();
        AtomicReference<Throwable> saveFailure = new AtomicReference<Throwable>();
        when(fixture.config.getLogsFolder()).thenAnswer(invocation -> {
            if (firstAccess.compareAndSet(true, false)) {
                assertTrue("Worker must hold the lifecycle monitor", Thread.holdsLock(fixture.manager));
                workerEntered.countDown();
                assertTrue("Worker was not released", releaseWorker.await(8, TimeUnit.SECONDS));
            }
            return logsFolder;
        });
        Thread worker = new Thread(() -> {
            try { workerAction.getValue().run(); }
            catch (Throwable failure) { workerFailure.set(failure); }
        }, "kcraft-test-flush");
        Thread saver = new Thread(() -> {
            saveStarted.countDown();
            try { fixture.manager.saveAll(); }
            catch (Throwable failure) { saveFailure.set(failure); }
        }, "kcraft-test-save");
        worker.setDaemon(true);
        saver.setDaemon(true);

        try {
            worker.start();
            assertTrue("Worker did not enter its write", workerEntered.await(3, TimeUnit.SECONDS));
            saver.start();
            assertTrue(saveStarted.await(3, TimeUnit.SECONDS));
            awaitBlocked(saver);
            verify(fixture.task, never()).cancel();
            assertFalse(new File(logsFolder, "stats-global.json").exists());
        } finally {
            releaseWorker.countDown();
            worker.join(3000L);
            saver.join(3000L);
        }

        assertFalse("Worker did not finish", worker.isAlive());
        assertFalse("Save did not finish", saver.isAlive());
        assertNull(workerFailure.get());
        assertNull(saveFailure.get());
        verify(fixture.task).cancel();
        assertEquals(0, fixture.manager.getPendingLogCount());
        assertEquals(1L, fixture.manager.getMetrics().getWritten());
        assertEquals(1, Files.readAllLines(new File(logsFolder, "history.jsonl").toPath(),
            StandardCharsets.UTF_8).size());
        assertTrue(new File(logsFolder, "stats-global.json").isFile());
    }

    private static void awaitBlocked(Thread thread) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
        while (thread.getState() != Thread.State.BLOCKED && thread.isAlive()
                && System.nanoTime() < deadline) {
            Thread.yield();
        }
        assertEquals("saveAll must wait for the active flush monitor", Thread.State.BLOCKED, thread.getState());
    }

    @SuppressWarnings("unchecked")
    private static Queue<CraftLogRecord> queue(LoggingManager manager) throws Exception {
        Field field = LoggingManager.class.getDeclaredField("queue");
        field.setAccessible(true);
        return (Queue<CraftLogRecord>) field.get(manager);
    }

    private static CraftLogRecord record(long id) {
        return new CraftLogRecord(id, 1720000000000L, "Élodie",
            "00000000-0000-0000-0000-000000000001", "recipe-" + id,
            "DIAMOND", true, "world", 1, 64, 2, "PLAINS");
    }

    private static final class Fixture {
        private final Kcraft plugin = mock(Kcraft.class);
        private final ConfigManager config = mock(ConfigManager.class);
        private final Server server = mock(Server.class);
        private final BukkitScheduler scheduler = mock(BukkitScheduler.class);
        private final BukkitTask task = mock(BukkitTask.class);
        private final Logger logger = mock(Logger.class);
        private final LoggingManager manager;

        private Fixture(boolean enabled, File logsFolder) {
            when(plugin.getConfigManager()).thenReturn(config);
            when(plugin.getServer()).thenReturn(server);
            when(plugin.getLogger()).thenReturn(logger);
            when(server.getScheduler()).thenReturn(scheduler);
            when(config.isLoggingEnabled()).thenReturn(enabled);
            when(config.getLogsFolder()).thenReturn(logsFolder);
            when(config.getLogFile()).thenReturn("history.json");
            when(scheduler.runTaskTimerAsynchronously(
                eq(plugin), any(Runnable.class), eq(40L), eq(40L))).thenReturn(task);
            manager = new LoggingManager(plugin);
            assertNotNull(manager.getMetrics());
        }
    }
}
