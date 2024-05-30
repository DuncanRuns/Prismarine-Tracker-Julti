package xyz.duncanruns.prismarinetracker;

import com.google.gson.*;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.Level;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.JultiOptions;
import xyz.duncanruns.julti.instance.MinecraftInstance;
import xyz.duncanruns.julti.management.InstanceManager;
import xyz.duncanruns.julti.plugin.PluginEvents;
import xyz.duncanruns.julti.util.ExceptionUtil;
import xyz.duncanruns.julti.util.FileUtil;

import java.awt.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

public class PrismarineTracker {
    private static PlaySession session = new PlaySession();
    private static WatchService recordsWatcher = null;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final Path FOLDER_PATH = JultiOptions.getJultiDir().resolve("prismarinetracker");
    private static final Path RECORDS_FOLDER = Paths.get(System.getProperty("user.home")).resolve("speedrunigt").resolve("records");
    private static final Path SESSION_PATH = FOLDER_PATH.resolve("session.json");
    public static final Set<String> MANUAL_RESET_CODES = new HashSet<>(Arrays.asList("wallReset", "wallSingleReset", "wallFocusReset", "reset"));
    private static long lastTick = 0;
    private static boolean benchmarkWasRunning = false;
    private static boolean shouldSave = false;
    private static boolean startedPlaying = false;

    /**
     * Returned object should not be modified.
     */
    public static PlaySession getCurrentSession() {
        return session;
    }

    public static void init() {
        try {
            recordsWatcher = FileSystems.getDefault().newWatchService();
            RECORDS_FOLDER.register(recordsWatcher, StandardWatchEventKinds.ENTRY_CREATE);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (!Files.isDirectory(FOLDER_PATH)) {
            try {
                Files.createDirectory(FOLDER_PATH);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else if (Files.exists(SESSION_PATH)) {
            String s;
            try {
                s = FileUtil.readString(SESSION_PATH);
                PlaySession lastSession = GSON.fromJson(s, PlaySession.class);
                if (lastSession.runsWithGold > 0 && (System.currentTimeMillis() - lastSession.sessionEndTime < 300_000)) {
                    Julti.log(Level.INFO, "(Prismarine Tracker) Last session was less than 5 minutes ago so it will be continued.");
                    session = lastSession;
                }
            } catch (IOException | JsonSyntaxException | NullPointerException e) {
                Julti.log(Level.WARN, "(Prismarine Tracker) Last session couldn't be recovered, so a new one will be started");
            }
        }

        PluginEvents.RunnableEventType.END_TICK.register(PrismarineTracker::tick);
        PluginEvents.MiscEventType.HOTKEY_PRESS.register(o -> {
            String hotkeyCode = ((Pair<String, Point>) o).getLeft();
            if (MANUAL_RESET_CODES.contains(hotkeyCode)) {
                startedPlaying = true;
                updateLastActivity();
            }
        });
    }

    public static void stop() {
        tick();
        if (session.runsWithGold > 0) {
            trySave();
        }
    }

    private static void trySave() {
        try {
            save();
        } catch (IOException e) {
            Julti.log(Level.ERROR, "(Prismarine Tracker) Failed to save session: " + ExceptionUtil.toDetailedString(e));
        }
    }

    private static void save() throws IOException {
        session.sessionEndTime = System.currentTimeMillis();
        String toWrite = GSON.toJson(session);
        FileUtil.writeString(SESSION_PATH, toWrite);
        FileUtil.writeString(FOLDER_PATH.resolve(session.sessionStartTime + ".json"), toWrite);
    }

    private static void processJsonFile(Path recordPath) throws IOException {
        JsonObject json = GSON.fromJson(FileUtil.readString(recordPath), JsonObject.class);
        System.out.println("Processing " + recordPath);
        processJson(json);
    }

    private static void processJson(JsonObject json) {
        // If non-survival, cheats, or coop, or not random seed, don't track
        if (json.get("default_gamemode").getAsInt() != 0) return;
        if (!Objects.equals(json.get("run_type").getAsString(), "random_seed")) return;
        if (json.get("is_cheat_allowed").getAsBoolean()) return;
        if (json.get("is_coop").getAsBoolean()) return;

        if (startedPlaying) session.resets++;

        long openToLanTime = 0;
        boolean hasOpenedToLan = false;
        try {
            openToLanTime = json.get("open_lan").getAsLong();
            hasOpenedToLan = true;
        } catch (Exception ignored) {
        }

        if (json.get("is_completed").getAsBoolean() && (!hasOpenedToLan || (openToLanTime > json.get("final_rta").getAsLong()))) {
            session.runsFinished++;
            session.runFinishTimes.add(json.get("retimed_igt").getAsLong());
        }

        Map<String, Long> timeLineEvents = new HashMap<>();

        for (JsonElement event : json.get("timelines").getAsJsonArray()) {
            JsonObject eventJson = event.getAsJsonObject();
            if (hasOpenedToLan && eventJson.get("rta").getAsLong() > openToLanTime) {
                continue;
            }
            timeLineEvents.put(eventJson.get("name").getAsString(), eventJson.get("igt").getAsLong());
        }

        countRunsWithStuffStats(timeLineEvents);

        if (timeLineEvents.containsKey("trade_with_villager")) {
            countRunsWithPearlsStat(json);
        }

        // Count stronghold and end
        if (timeLineEvents.containsKey("enter_stronghold")) {
            session.strongholdEnterTimes.add(timeLineEvents.get("enter_stronghold"));
        }
        if (timeLineEvents.containsKey("enter_end")) {
            session.endEnterTimes.add(timeLineEvents.get("enter_end"));
        }

        boolean isRegularInsomniac = (timeLineEvents.containsKey("pick_gold_block") && !timeLineEvents.containsKey("found_villager"))
                || (timeLineEvents.containsKey("pick_gold_block") && timeLineEvents.get("found_villager") > timeLineEvents.get("pick_gold_block"));
        if (!isRegularInsomniac) return;

        session.goldBlockPickupTimes.add(timeLineEvents.get("pick_gold_block"));

        if (!timeLineEvents.containsKey("found_villager")) return;
        session.villageEnterTimes.add(timeLineEvents.get("found_villager"));

        if (!timeLineEvents.containsKey("enter_nether")) return;
        session.netherEnterTimes.add(timeLineEvents.get("enter_nether"));

        if (!timeLineEvents.containsKey("enter_fortress")) return;
        session.fortressEnterTimes.add(timeLineEvents.get("enter_fortress"));

        if (!timeLineEvents.containsKey("nether_travel")) return;
        session.netherExitTimes.add(timeLineEvents.get("nether_travel"));
    }

    private static void countRunsWithPearlsStat(JsonObject json) {
        if (!json.has("stats")) return;
        JsonObject exploringJson = json.getAsJsonObject("stats");
        Optional<String> uuid = exploringJson.keySet().stream().findAny();

        if (!uuid.isPresent()) return;
        exploringJson = exploringJson.getAsJsonObject(uuid.get());

        if (!exploringJson.has("stats")) return;
        exploringJson = exploringJson.getAsJsonObject("stats");

        if (!exploringJson.has("minecraft:crafted")) return;
        exploringJson = exploringJson.getAsJsonObject("minecraft:crafted");

        if (!exploringJson.has("minecraft:ender_pearl")) return;
        int pearls = exploringJson.get("minecraft:ender_pearl").getAsInt();
        if (pearls >= 10) {
            session.runsWith10Pearls++;
        }
    }

    private static void countRunsWithStuffStats(Map<String, Long> timeLineEvents) {
        if (timeLineEvents.containsKey("pick_gold_block")) {
            session.runsWithGold++;
            // All of these things could feasibly be done on shit runs, even strongholds
            if (timeLineEvents.containsKey("found_villager")) session.runsWithVillage++;
            if (timeLineEvents.containsKey("trade_with_villager")) session.runsWithTrading++;
            if (timeLineEvents.containsKey("enter_nether")) session.runsWithNether++;
            if (timeLineEvents.containsKey("enter_fortress")) session.runsWithFort++;
            if (timeLineEvents.containsKey("nether_travel") || timeLineEvents.containsKey("enter_end"))
                session.runsWithNetherExit++;
            if (timeLineEvents.containsKey("enter_stronghold")) session.runsWithStronghold++;
            shouldSave = true;
        }
        if ((timeLineEvents.containsKey("enter_end"))) session.runsWithEndEnter++;
    }

    private static void tick() {
        if (System.currentTimeMillis() - lastTick > 5000) {
            lastTick = System.currentTimeMillis();
        } else {
            return;
        }

        boolean benchmarkIsRunning = JultiOptions.getJultiOptions().resetStyle.equals("Benchmark");
        if (benchmarkIsRunning || benchmarkWasRunning) {
            clearWatcher();
            benchmarkWasRunning = benchmarkIsRunning;
            return;
        }

        MinecraftInstance selectedInstance;
        if ((selectedInstance = InstanceManager.getInstanceManager().getSelectedInstance()) != null && "1.15.2".equals(selectedInstance.getVersionString())) {
            updateLastActivity();
        }

        WatchKey watchKey = recordsWatcher.poll();
        if (watchKey == null)
            return; // It's ok to cancel the shouldSave stuff because that can't be true unless some json processing happens

        for (WatchEvent<?> event : watchKey.pollEvents()) {
            if (event.kind() != StandardWatchEventKinds.ENTRY_CREATE || !(event.context() instanceof Path))
                continue;
            try {
                processJsonFile(RECORDS_FOLDER.resolve((Path) event.context()));
            } catch (IOException | JsonSyntaxException | NullPointerException e) {
                Julti.log(Level.ERROR, "Failed to process a world: " + ExceptionUtil.toDetailedString(e));
            }
        }
        watchKey.reset();

        if (shouldSave) {
            shouldSave = false;
            trySave();
        }
    }

    private static void clearWatcher() {
        WatchKey key = recordsWatcher.poll();
        if (key == null) return;
        key.pollEvents();
        key.reset();
    }

    private static synchronized void updateLastActivity() {
        if (!startedPlaying) return;
        long currentTime = System.currentTimeMillis();
        long timeSinceLastActivity = Math.abs(currentTime - session.lastActivity);
        if (timeSinceLastActivity > 120_000 /*2 Minutes*/) {
            session.breaks.add(timeSinceLastActivity);
        }
        session.lastActivity = currentTime;
    }

    public static void clearSession() throws IOException {
        Path potentialPath = FOLDER_PATH.resolve(session.sessionStartTime + ".json");
        if (session.runsWithGold == 0) {
            Files.deleteIfExists(potentialPath);
        }
        Files.deleteIfExists(SESSION_PATH);
        session = new PlaySession();
        clearWatcher();
        startedPlaying = false;
    }

}
