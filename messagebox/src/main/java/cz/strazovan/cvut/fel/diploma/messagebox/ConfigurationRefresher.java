package cz.strazovan.cvut.fel.diploma.messagebox;


import cz.strazovan.cvut.fel.diploma.messagebox.rabbitmq.IncomingJobsReceiver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.SmartLifecycle;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class ConfigurationRefresher implements SmartLifecycle {

    @Value("${configurationservice.url}")
    private String configurationServiceHost;

    @Value("${configurationservice.key}")
    private String configurationServiceKey;

    private ScheduledExecutorService executorService;

    private volatile boolean running;

    @Override
    public void start() {
        this.executorService = Executors.newScheduledThreadPool(1);
        this.executorService.scheduleAtFixedRate(new ConfigJob(this.configurationServiceHost, this.configurationServiceKey), 0, 1, TimeUnit.MINUTES);
        this.running = true;
    }

    @Override
    public void stop() {
        this.executorService.shutdownNow();
        this.running = false;
    }

    @Override
    public boolean isRunning() {
        return this.running;
    }

    private static class ConfigJob implements Runnable {
        private static final Logger logger = LoggerFactory.getLogger(ConfigJob.class);

        private final String configurationServiceHost;
        private final String configurationServiceKey;

        private String currentConfig = "";

        public ConfigJob(String configurationServiceHost, String configurationServiceKey) {
            this.configurationServiceHost = configurationServiceHost;
            this.configurationServiceKey = configurationServiceKey;
        }

        @Override
        public void run() {
            try {
                final RestTemplate template = new RestTemplate();
                final ResponseEntity<String> configuration = template.getForEntity(this.configurationServiceHost + "/configuration/" + this.configurationServiceKey, String.class);
                final String body = configuration.getBody();
                if (body == null) {
                    return;
                }
                if (!body.equals(this.currentConfig)) {
                    logger.info("Received new configuration {}", body);
                    this.currentConfig = body;
                }
            } catch (Exception e) {
                logger.error("Failed to fetch new configuration.", e);
            }
        }
    }
}
