package xyz.duncanruns.julti.exampleplugin;

import org.apache.logging.log4j.Level;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.plugin.PluginEvents;

import java.util.concurrent.atomic.AtomicLong;

public class InitStuff {
    public static void init() {
        PluginEvents.RunnableEventType.RELOAD.register(() -> {
            // This gets run when Julti launches and every time the profile is switched
            Julti.log(Level.INFO, "Example Plugin Reloaded!");
        });

        AtomicLong timeTracker = new AtomicLong(System.currentTimeMillis());

        PluginEvents.RunnableEventType.END_TICK.register(() -> {
            // This gets run every tick (1 ms)
            long currentTime = System.currentTimeMillis();
            if (currentTime - timeTracker.get() > 3000) {
                // This gets ran every 3 seconds
                // Julti.log(Level.INFO, "Example Plugin ran for another 3 seconds.");
                timeTracker.set(currentTime);
            }
        });

        PluginEvents.RunnableEventType.STOP.register(() -> {
            // This gets run when Julti is shutting down
            Julti.log(Level.INFO, "Example plugin shutting down...");
        });

        PluginEvents.InstanceEventType.ACTIVATE.register(instance -> {
            Julti.log(Level.INFO, "ExamplePlugin: Instance activated: " + instance);
        });
    }
}
