package cz.strazovan.cvut.fel.diploma.agent.sidecar;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Value;
import io.micronaut.scheduling.annotation.Scheduled;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

@Singleton
public class InactivityController {

    private static final Logger logger = LoggerFactory.getLogger(InactivityController.class);

    @Inject
    private ApplicationContext context;

    @Value("${ephemeral}")
    protected boolean ephemeral;

    private volatile long lastJob = -1;

    public void notifyJob() {
        this.lastJob = new Date().getTime();
    }

    @Scheduled(fixedDelay = "10s", initialDelay = "20s")
    void checkActivity() {
        if (!ephemeral)
            return;

        final long deltaMs = new Date().getTime() - lastJob;
        logger.debug("Time since last job received: {}ms", deltaMs);
        if (deltaMs > 20000) {
            // if there was no job for more than 20s, exit
            logger.info("Inactivity detected, exiting the application.");
            this.stop();
        }
    }

    private void stop() {
        this.context.stop();
        System.exit(0);
    }
}
