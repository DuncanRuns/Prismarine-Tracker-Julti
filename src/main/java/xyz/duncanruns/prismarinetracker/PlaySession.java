package xyz.duncanruns.prismarinetracker;

import xyz.duncanruns.prismarinetracker.util.FormattingUtil;

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

    public CalculatedStats toCalculatedStats() {
        CalculatedStats cs = new CalculatedStats();

        cs.sessionLengthMillis = lastActivity - sessionStartTime;
        cs.timePlayedMillis = cs.sessionLengthMillis;
        for (Long b : breaks) {
            cs.timePlayedMillis -= b;
        }
        cs.sessionLengthFormatted = FormattingUtil.formatMillis(cs.sessionLengthMillis);
        cs.timePlayedFormatted = FormattingUtil.formatMillis(cs.timePlayedMillis);

        cs.breaks = breaks.size();
        cs.averageBreakMillis = FormattingUtil.getAverageTime(breaks);
        cs.averageBreakFormatted = FormattingUtil.formatMillis(cs.averageBreakMillis);

        cs.resets = resets;

        cs.runsWithMonument = runsWithGold;
        cs.averageMonumentMillis = FormattingUtil.getAverageTime(goldBlockPickupTimes);
        cs.averageMonumentFormatted = FormattingUtil.formatMillis(cs.averageMonumentMillis);
        cs.monumentsPerHour = FormattingUtil.getPerHour(cs.runsWithMonument, cs.timePlayedMillis);

        cs.runsWithVillage = runsWithVillage;
        cs.averageVillageMillis = FormattingUtil.getAverageTime(villageEnterTimes);
        cs.averageVillageFormatted = FormattingUtil.formatMillis(cs.averageVillageMillis);
        cs.villagesPerHour = FormattingUtil.getPerHour(cs.runsWithVillage, cs.timePlayedMillis);

        cs.runsWithTrading = runsWithTrading;
        cs.runsWith10Pearls = runsWith10Pearls;

        cs.runsWithNether = runsWithFort;
        cs.averageNetherMillis = FormattingUtil.getAverageTime(netherEnterTimes);
        cs.averageNetherFormatted = FormattingUtil.formatMillis(cs.averageNetherMillis);

        cs.runsWithFortress = runsWithFort;
        cs.averageFortressMillis = FormattingUtil.getAverageTime(fortressEnterTimes);
        cs.averageFortressFormatted = FormattingUtil.formatMillis(cs.averageFortressMillis);

        cs.runsWithNetherExit = runsWithNetherExit;
        cs.averageNetherExitMillis = FormattingUtil.getAverageTime(netherExitTimes);
        cs.averageNetherExitFormatted = FormattingUtil.formatMillis(cs.averageNetherExitMillis);

        cs.runsWithStronghold = runsWithStronghold;
        cs.averageStrongholdMillis = FormattingUtil.getAverageTime(strongholdEnterTimes);
        cs.averageStrongholdFormatted = FormattingUtil.formatMillis(cs.averageStrongholdMillis);

        cs.runsWithEnd = runsWithEndEnter;
        cs.averageEndMillis = FormattingUtil.getAverageTime(endEnterTimes);
        cs.averageEndFormatted = FormattingUtil.formatMillis(cs.averageEndMillis);

        cs.runsWithFinish = runsFinished;
        cs.averageFinishMillis = FormattingUtil.getAverageTime(runFinishTimes);
        cs.averageFinishFormatted = FormattingUtil.formatMillis(cs.averageFinishMillis);

        return cs;
    }

    public static class CalculatedStats {

        public long sessionLengthMillis;
        public String sessionLengthFormatted;

        public long timePlayedMillis;
        public String timePlayedFormatted;

        public int breaks;
        public long averageBreakMillis;
        public String averageBreakFormatted;

        public int resets;

        public int runsWithMonument;
        public long averageMonumentMillis;
        public String averageMonumentFormatted;
        public String monumentsPerHour;

        public int runsWithVillage;
        public long averageVillageMillis;
        public String averageVillageFormatted;
        public String villagesPerHour;

        public int runsWithTrading;
        public int runsWith10Pearls;

        public int runsWithNether;
        public long averageNetherMillis;
        public String averageNetherFormatted;

        public int runsWithFortress;
        public long averageFortressMillis;
        public String averageFortressFormatted;

        public int runsWithNetherExit;
        public long averageNetherExitMillis;
        public String averageNetherExitFormatted;

        public int runsWithStronghold;
        public long averageStrongholdMillis;
        public String averageStrongholdFormatted;

        public int runsWithEnd;
        public long averageEndMillis;
        public String averageEndFormatted;

        public int runsWithFinish;
        public long averageFinishMillis;
        public String averageFinishFormatted;

        private CalculatedStats() {
        }
    }
}
