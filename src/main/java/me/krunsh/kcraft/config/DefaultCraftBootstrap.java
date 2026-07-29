package me.krunsh.kcraft.config;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Exporte les exemples embarques une seule fois. */
public final class DefaultCraftBootstrap {
    public static final int DEFAULTS_VERSION = 1;
    public static final String STATE_FILE = "defaults-state.yml";

    private static final List<String> DEFAULT_FILES = Collections.unmodifiableList(Arrays.asList(
        "armor.yml", "azurite.yml", "block.yml", "collectors.yml", "compacting.yml",
        "evolutive_tools.yml", "hunter.yml", "tables.yml", "tools.yml"
    ));

    public interface ResourceSource {
        InputStream open(String path) throws IOException;
    }

    public enum ExportStatus { EXPORTED, ALREADY_EXISTS, UNKNOWN_DEFAULT }

    public static final class Initialization {
        private final boolean alreadyInitialized;
        private final boolean existingInstallation;
        private final int exportedCount;

        private Initialization(boolean alreadyInitialized, boolean existingInstallation, int exportedCount) {
            this.alreadyInitialized = alreadyInitialized;
            this.existingInstallation = existingInstallation;
            this.exportedCount = exportedCount;
        }

        public boolean isAlreadyInitialized() { return alreadyInitialized; }
        public boolean isExistingInstallation() { return existingInstallation; }
        public int getExportedCount() { return exportedCount; }
    }

    private final File dataFolder;
    private final File craftsFolder;
    private final ResourceSource resources;

    public DefaultCraftBootstrap(File dataFolder, File craftsFolder, ResourceSource resources) {
        this.dataFolder = dataFolder;
        this.craftsFolder = craftsFolder;
        this.resources = resources;
    }

    public List<String> listDefaults() { return DEFAULT_FILES; }

    public Initialization initializeOnce() throws IOException {
        File state = new File(dataFolder, STATE_FILE);
        if (isInitialized(state)) return new Initialization(true, false, 0);
        ensureDirectory(craftsFolder);
        boolean existing = containsCraftFiles(craftsFolder);
        int exported = 0;
        if (!existing) {
            for (String fileName : DEFAULT_FILES) {
                if (export(fileName) == ExportStatus.EXPORTED) exported++;
            }
        }
        writeStateAtomically(state);
        return new Initialization(false, existing, exported);
    }

    public ExportStatus export(String requestedFile) throws IOException {
        String fileName = normalize(requestedFile);
        if (fileName == null || !DEFAULT_FILES.contains(fileName)) return ExportStatus.UNKNOWN_DEFAULT;
        ensureDirectory(craftsFolder);
        File destination = new File(craftsFolder, fileName);
        if (destination.exists()) return ExportStatus.ALREADY_EXISTS;
        try (InputStream input = resources.open("crafts/" + fileName)) {
            if (input == null) throw new IOException("Ressource embarquee absente: crafts/" + fileName);
            Files.copy(input, destination.toPath());
        }
        return ExportStatus.EXPORTED;
    }

    private static String normalize(String requestedFile) {
        if (requestedFile == null) return null;
        String value = requestedFile.trim().toLowerCase();
        if (value.isEmpty() || value.contains("/") || value.contains("\\") || value.contains("..")) return null;
        return value.endsWith(".yml") ? value : value + ".yml";
    }

    private static boolean containsCraftFiles(File folder) {
        File[] files = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".yml"));
        return files != null && files.length > 0;
    }

    private static boolean isInitialized(File state) throws IOException {
        if (!state.isFile()) return false;
        String content = new String(Files.readAllBytes(state.toPath()), StandardCharsets.UTF_8);
        for (String line : content.split("\\r?\\n")) {
            if ("initialized:true".equalsIgnoreCase(line.replace(" ", "").trim())) return true;
        }
        return false;
    }

    private static void writeStateAtomically(File state) throws IOException {
        ensureDirectory(state.getParentFile());
        File temporary = new File(state.getParentFile(), STATE_FILE + ".tmp");
        String content = "initialized: true\n" + "defaults-version: " + DEFAULTS_VERSION + "\n";
        Files.write(temporary.toPath(), content.getBytes(StandardCharsets.UTF_8));
        try {
            Files.move(temporary.toPath(), state.toPath(), StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException unsupported) {
            Files.move(temporary.toPath(), state.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void ensureDirectory(File folder) throws IOException {
        if (folder == null) throw new IOException("Dossier parent absent");
        if (folder.isDirectory()) return;
        if (!folder.mkdirs() && !folder.isDirectory()) {
            throw new IOException("Impossible de creer le dossier " + folder.getAbsolutePath());
        }
    }
}
