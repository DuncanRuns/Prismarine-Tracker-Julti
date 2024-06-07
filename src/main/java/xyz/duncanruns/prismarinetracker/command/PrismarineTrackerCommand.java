package xyz.duncanruns.prismarinetracker.command;

import org.apache.logging.log4j.Level;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.cancelrequester.CancelRequester;
import xyz.duncanruns.julti.command.Command;
import xyz.duncanruns.julti.command.CommandFailedException;
import xyz.duncanruns.prismarinetracker.PrismarineTracker;
import xyz.duncanruns.prismarinetracker.gui.PrismarineTrackerGUI;

import java.io.IOException;

public class PrismarineTrackerCommand extends Command {
    @Override
    public String helpDescription() {
        return "pris clear - Clears the current session of any stats, useful for after a warmup script" +
                "\npris show - Opens the Prismarine Tracker GUI";
    }

    @Override
    public int getMinArgs() {
        return 1;
    }

    @Override
    public int getMaxArgs() {
        return 1;
    }

    @Override
    public String getName() {
        return "pris";
    }

    @Override
    public void run(String[] args, CancelRequester cancelRequester) {
        String command = args[0];
        if ("clear".equals(command)) {
            try {
                synchronized (Julti.getJulti()) {
                    PrismarineTracker.clearSession();
                }
                Julti.log(Level.INFO, "(Prismarine Tracker) Session cleared");
            } catch (IOException e) {
                throw new CommandFailedException(e);
            }
        } else if ("show".equals(command)) {
            PrismarineTrackerGUI.open();
        } else {
            throw new CommandFailedException("Invalid argument for pris command");
        }
    }
}
