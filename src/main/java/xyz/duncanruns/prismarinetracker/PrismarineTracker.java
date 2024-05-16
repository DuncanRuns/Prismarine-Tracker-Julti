package xyz.duncanruns.prismarinetracker;

import com.google.gson.*;
import org.apache.logging.log4j.Level;
import xyz.duncanruns.julti.Julti;
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
    private static final HashMap<Path, Integer> LAST_WORLD_MAP = new HashMap<>();
    private static final PlaySessionStats STATS = new PlaySessionStats();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static int cycle = 0;

    public static void load() {
        // TODO: check if continue session
        tick();
        PluginEvents.RunnableEventType.END_TICK.register(() -> {
            if (++cycle > 100) {
                cycle = 0;
                tick();
            }
        });
    }

    public static void stop() {
        tick();
        // Todo: Save tracked stats and also write the output
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

        STATS.resets++;

        long openToLanTime = 0;
        boolean hasOpenedToLan = false;
        try {
            openToLanTime = json.get("open_lan").getAsLong();
            hasOpenedToLan = true;
        } catch (Exception ignored) {
        }

        if (json.get("is_completed").getAsBoolean() && (!hasOpenedToLan || (openToLanTime > json.get("final_rta").getAsLong()))) {
            STATS.runsFinished++;
            STATS.runFinishTimes.add(json.get("retimed_igt").getAsLong());
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
            STATS.strongholdEnterTimes.add(timeLineEvents.get("enter_stronghold"));
            if (timeLineEvents.containsKey("enter_end")) {
                STATS.endEnterTimes.add(timeLineEvents.get("enter_end"));
            }
        }

        boolean isRegularInsomniac = timeLineEvents.containsKey("pick_gold_block") && !timeLineEvents.containsKey("found_villager");
        if (!isRegularInsomniac) {
            isRegularInsomniac = timeLineEvents.containsKey("pick_gold_block") && timeLineEvents.get("found_villager") > timeLineEvents.get("pick_gold_block");
        }
        if (!isRegularInsomniac) return;

        STATS.goldBlockPickupTimes.add(timeLineEvents.get("pick_gold_block"));

        if (!timeLineEvents.containsKey("found_villager")) return;
        STATS.villageEnterTimes.add(timeLineEvents.get("found_villager"));

        if (!timeLineEvents.containsKey("enter_nether")) return;
        STATS.netherEnterTimes.add(timeLineEvents.get("enter_nether"));

        if (!timeLineEvents.containsKey("enter_fortress")) return;
        STATS.fortressEnterTimes.add(timeLineEvents.get("enter_fortress"));

        if (!timeLineEvents.containsKey("nether_travel")) return;
        STATS.netherExitTimes.add(timeLineEvents.get("nether_travel"));

    }

    private static void countRunsWithStuffStats(Map<String, Long> timeLineEvents) {
        if (!timeLineEvents.containsKey("pick_gold_block")) return;
        STATS.runsWithGold++;

        if (!timeLineEvents.containsKey("trade_with_villager")) return;
        STATS.runsWithTrading++;

        if (!timeLineEvents.containsKey("enter_nether")) return;
        STATS.runsWithNether++;

        if (!timeLineEvents.containsKey("enter_fortress")) return;
        STATS.runsWithFort++;

        if ((!timeLineEvents.containsKey("nether_travel")) && (!timeLineEvents.containsKey("enter_stronghold")))
            return;
        STATS.runsWithNetherExit++;

        if ((!timeLineEvents.containsKey("enter_stronghold"))) return;
        STATS.runsWithStronghold++;

        if ((!timeLineEvents.containsKey("enter_end"))) return;
        STATS.runsWithEndEnter++;
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

    static class PlaySessionStats {
        long sessionStartTime = System.currentTimeMillis();
        int resets;

        // Amounts
        int runsWithGold;
        int runsWithTrading;
        int runsWithNether;
        int runsWithFort;
        int runsWithNetherExit; // if nether_travel_home OR (enter_fortress and enter_stronghold)
        int runsWithStronghold;
        int runsWithEndEnter;
        int runsFinished;

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
    }
}
