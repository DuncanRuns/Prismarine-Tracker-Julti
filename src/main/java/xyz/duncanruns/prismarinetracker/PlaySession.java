package xyz.duncanruns.prismarinetracker;

import java.util.LinkedList;
import java.util.List;

public class PlaySession {
    public long sessionStartTime = System.currentTimeMillis();
    public long sessionEndTime = sessionStartTime;
    public long lastActivity = sessionStartTime;
    public int resets = 0;

    // Amounts
    public int runsWithGold = 0;
    public int runsWithVillage = 0;
    public int runsWithTrading = 0;
    public int runsWith10Pearls = 0;
    public int runsWithNether = 0;
    public int runsWithFort = 0;
    public int runsWithNetherExit = 0;
    public int runsWithStronghold = 0;
    public int runsWithEndEnter = 0;
    public int runsFinished = 0;

    // Times
    // (Regular insomniac only - mine gold block comes before trading)
    public List<Long> goldBlockPickupTimes = new LinkedList<>();
    public List<Long> villageEnterTimes = new LinkedList<>();
    public List<Long> netherEnterTimes = new LinkedList<>();
    public List<Long> fortressEnterTimes = new LinkedList<>();
    public List<Long> netherExitTimes = new LinkedList<>();

    // (For any runs)
    public List<Long> strongholdEnterTimes = new LinkedList<>();
    public List<Long> endEnterTimes = new LinkedList<>();
    public List<Long> runFinishTimes = new LinkedList<>();

    public List<Long> breaks = new LinkedList<>();
}
