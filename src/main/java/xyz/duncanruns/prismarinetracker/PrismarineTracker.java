package xyz.duncanruns.prismarinetracker;

import com.google.gson.*;
import org.apache.logging.log4j.Level;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.JultiOptions;
import xyz.duncanruns.julti.instance.MinecraftInstance;
import xyz.duncanruns.julti.management.InstanceManager;
import xyz.duncanruns.julti.plugin.PluginEvents;
import xyz.duncanruns.julti.util.ExceptionUtil;
import xyz.duncanruns.julti.util.FileUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class PrismarineTracker {
    private static PlaySession session = new PlaySession();
    private static final HashMap<Path, Integer> LAST_WORLD_MAP = new HashMap<>();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static int cycle = 0;
    private static final Path FOLDER_PATH = JultiOptions.getJultiDir().resolve("prismarinetracker");
    private static final Path SESSION_PATH = FOLDER_PATH.resolve("session.json");

    public static void init() {
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
                if (System.currentTimeMillis() - lastSession.sessionEndTime < 300_000) {
                    session = lastSession;
                    Julti.log(Level.WARN, "(Prismarine Tracker) Last session was less than 5 minutes ago so it will be continued.");
                }
            } catch (IOException | JsonSyntaxException | NullPointerException e) {
                Julti.log(Level.WARN, "(Prismarine Tracker) Last session couldn't be recovered, so a new one will be started");
            }
        }

        tick();
        PluginEvents.RunnableEventType.END_TICK.register(() -> {
            if (++cycle > 4000) {
                cycle = 0;
                tick();
            }
        });
    }

    public static void stop() {
        tick();
        session.sessionEndTime = System.currentTimeMillis();
        try {
            String toWrite = GSON.toJson(session);
            FileUtil.writeString(SESSION_PATH, toWrite);
            FileUtil.writeString(FOLDER_PATH.resolve(session.sessionStartTime + ".json"), toWrite);
        } catch (IOException e) {
            Julti.log(Level.ERROR, "(Prismarine Tracker) Failed to save session: " + ExceptionUtil.toDetailedString(e));
        }
    }

    private static void processWorld(Path worldPath) throws IOException, JsonSyntaxException {
        Path recordPath = worldPath.resolve("speedrunigt").resolve("record.json");
        if (!Files.exists(recordPath)) {
            return;
        }
        JsonObject json = GSON.fromJson(FileUtil.readString(recordPath), JsonObject.class);

        // If non-survival, cheats, or coop, don't track
        if (json.get("default_gamemode").getAsInt() != 0) return;
        if (json.get("is_cheat_allowed").getAsBoolean()) return;
        if (json.get("is_coop").getAsBoolean()) return;

        session.resets++;

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
            if (hasOpenedToLan && eventJson.get("rta").getAsLong() < openToLanTime) {
                continue;
            }
            timeLineEvents.put(eventJson.get("name").getAsString(), eventJson.get("igt").getAsLong());
        }

        countRunsWithStuffStats(timeLineEvents);

        // Count stronghold and end
        if (timeLineEvents.containsKey("enter_stronghold")) {
            session.strongholdEnterTimes.add(timeLineEvents.get("enter_stronghold"));
            if (timeLineEvents.containsKey("enter_end")) {
                session.endEnterTimes.add(timeLineEvents.get("enter_end"));
            }
        }

        boolean isRegularInsomniac = timeLineEvents.containsKey("pick_gold_block") && !timeLineEvents.containsKey("found_villager");
        if (!isRegularInsomniac) {
            isRegularInsomniac = timeLineEvents.containsKey("pick_gold_block") && timeLineEvents.get("found_villager") > timeLineEvents.get("pick_gold_block");
        }
        if (!isRegularInsomniac) return;

        countRunsWithPearlsStat(json);

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
        if (!timeLineEvents.containsKey("pick_gold_block")) return;
        session.runsWithGold++;

        if (!timeLineEvents.containsKey("found_villager")) return;
        session.runsWithVillage++;

        if (!timeLineEvents.containsKey("trade_with_villager")) return;
        session.runsWithTrading++;

        if (!timeLineEvents.containsKey("enter_nether")) return;
        session.runsWithNether++;

        if (!timeLineEvents.containsKey("enter_fortress")) return;
        session.runsWithFort++;

        if ((!timeLineEvents.containsKey("nether_travel")) && (!timeLineEvents.containsKey("enter_stronghold")))
            return;
        session.runsWithNetherExit++;

        if ((!timeLineEvents.containsKey("enter_stronghold"))) return;
        session.runsWithStronghold++;

        if ((!timeLineEvents.containsKey("enter_end"))) return;
        session.runsWithEndEnter++;
    }

    private static void tick() {
        List<Path> instancePaths = getAllInstancePaths();

        for (Path instancePath : instancePaths) {
            try {
                processInstance(instancePath);
            } catch (IOException e) {
                Julti.log(Level.ERROR, "(Prismarine Tracker) Failed to process an instance! " + ExceptionUtil.toDetailedString(e));
            }
        }
    }

    private static List<Path> getAllInstancePaths() {
        List<Path> instancePaths;
        instancePaths = new ArrayList<>(InstanceManager.getInstanceManager().getInstances())
                .stream()
                .filter(MinecraftInstance::hasWindow)
                .filter(instance -> "1.15.2".equals(instance.getVersionString()))
                .map(MinecraftInstance::getPath)
                .map(Path::toAbsolutePath)
                .collect(Collectors.toList());
        return instancePaths;
    }

    private static void processInstance(Path instancePath) throws IOException {
        Path savesPath = instancePath.resolve("saves");
        if (!Files.exists(savesPath)) {
            return;
        }
        int lastCheckedWorld = LAST_WORLD_MAP.getOrDefault(instancePath, -1);

        int attempts = extractAttempts(instancePath);
        if (lastCheckedWorld == -1) {
            LAST_WORLD_MAP.put(instancePath, attempts == -1 ? -1 : attempts - 1);
            return;
        }

        int scanGoal = attempts - 1;
        for (int i = lastCheckedWorld + 1; i <= scanGoal; i++) {
            Path worldPath = savesPath.resolve("Random Speedrun #" + i);
            try {
                if (Files.exists(worldPath)) processWorld(worldPath);
            } catch (IOException | JsonSyntaxException | NullPointerException ignored) {
                // Should log an error but fuck it
            }
        }
        LAST_WORLD_MAP.put(instancePath, Math.max(scanGoal, lastCheckedWorld));
    }

    private static int extractAttempts(Path instancePath) {
        String atumProp;
        try {
            atumProp = FileUtil.readString(instancePath.resolve("config").resolve("atum").resolve("atum.properties"));
        } catch (IOException e) {
            return -1;
        }
        for (String s : atumProp.split("\n")) {
            s = s.trim();
            if (!s.startsWith("rsgAttempts=")) continue;
            String[] parts = s.split("=");
            try {
                return Integer.parseInt(parts[1]);
            } catch (NumberFormatException | ArrayIndexOutOfBoundsException ignored) {
            }
        }
        return -1;
    }

    static class PlaySession {
        long sessionStartTime = System.currentTimeMillis();
        long sessionEndTime;
        int resets = 0;

        // Amounts
        int runsWithGold = 0;
        int runsWithVillage = 0;
        int runsWithTrading = 0;
        int runsWith10Pearls = 0;
        int runsWithNether = 0;
        int runsWithFort = 0;
        int runsWithNetherExit = 0;
        int runsWithStronghold = 0;
        int runsWithEndEnter = 0;
        int runsFinished = 0;

        // Times
        // (Regular insomniac only - mine gold block comes before trading)
        List<Long> goldBlockPickupTimes = new LinkedList<>();
        List<Long> villageEnterTimes = new LinkedList<>();
        List<Long> netherEnterTimes = new LinkedList<>();
        List<Long> fortressEnterTimes = new LinkedList<>();
        List<Long> netherExitTimes = new LinkedList<>();

        // (For any runs)
        List<Long> strongholdEnterTimes = new LinkedList<>();
        List<Long> endEnterTimes = new LinkedList<>();
        List<Long> runFinishTimes = new LinkedList<>();

        List<Long> breaks = new LinkedList<>();
    }
}
