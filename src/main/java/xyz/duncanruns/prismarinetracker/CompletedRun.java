package xyz.duncanruns.prismarinetracker;

import xyz.duncanruns.prismarinetracker.util.FormattingUtil;

public class CompletedRun {
    public long date;
    public long mineMonument = -1;
    public long villageEnter = -1;
    public long firstTrade = -1;
    public long netherEnter = -1;
    public long fortressEnter = -1;
    public long netherExit = -1;
    public long strongholdEnter = -1;
    public long endEnter = -1;
    public long completionRTA = -1;
    public long completionIGT = -1;
    public long completionRetime = -1;

    public FormattedTimes formatTimes() {
        FormattedTimes ft = new FormattedTimes();
        ft.date = FormattingUtil.formatDate(date);
        if (mineMonument != -1) ft.mineMonument = FormattingUtil.formatMillis(mineMonument);
        if (villageEnter != -1) ft.villageEnter = FormattingUtil.formatMillis(villageEnter);
        if (firstTrade != -1) ft.firstTrade = FormattingUtil.formatMillis(firstTrade);
        if (netherEnter != -1) ft.netherEnter = FormattingUtil.formatMillis(netherEnter);
        if (fortressEnter != -1) ft.fortressEnter = FormattingUtil.formatMillis(fortressEnter);
        if (netherExit != -1) ft.netherExit = FormattingUtil.formatMillis(netherExit);
        if (strongholdEnter != -1) ft.strongholdEnter = FormattingUtil.formatMillis(strongholdEnter);
        if (endEnter != -1) ft.endEnter = FormattingUtil.formatMillis(endEnter);
        if (completionRTA != -1) ft.completionRTA = FormattingUtil.formatMillis(completionRTA);
        if (completionIGT != -1) ft.completionIGT = FormattingUtil.formatMillis(completionIGT);
        if (completionRetime != -1) ft.completionRetime = FormattingUtil.formatMillis(completionRetime);
        return ft;
    }

    public static class FormattedTimes {
        public String date;
        public String mineMonument = "N/A";
        public String villageEnter = "N/A";
        public String firstTrade = "N/A";
        public String netherEnter = "N/A";
        public String fortressEnter = "N/A";
        public String netherExit = "N/A";
        public String strongholdEnter = "N/A";
        public String endEnter = "N/A";
        public String completionRTA = "N/A";
        public String completionIGT = "N/A";
        public String completionRetime = "N/A";
    }
}
