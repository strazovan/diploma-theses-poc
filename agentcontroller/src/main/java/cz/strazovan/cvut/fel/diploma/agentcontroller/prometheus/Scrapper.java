package cz.strazovan.cvut.fel.diploma.agentcontroller.prometheus;

import cz.strazovan.cvut.fel.diploma.agentcontroller.kubernetes.KubernetesClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.SmartLifecycle;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class Scrapper implements SmartLifecycle {

    private static final Logger logger = LoggerFactory.getLogger(Scrapper.class);
    private volatile boolean running;

    @Value("${metric.name}")
    private String metricName;

    @Value("${metric.limit}")
    private Float metricLimit;

    private ScheduledExecutorService scrapper;

    private final KubernetesClient kubernetesClient;

    public Scrapper(KubernetesClient kubernetesClient) {
        this.kubernetesClient = kubernetesClient;
    }

    @Override
    public void start() {
        this.scrapper = Executors.newScheduledThreadPool(1);
        this.scrapper.scheduleAtFixedRate(new ScrapeJob(), 0L, 20, TimeUnit.SECONDS);
        this.running = true;
    }

    @Override
    public void stop() {
        this.scrapper.shutdownNow();
        this.running = false;
    }

    @Override
    public boolean isRunning() {
        return this.running;
    }

    private class ScrapeJob implements Runnable {
        @Override
        public void run() {
            logger.info("Scraping...");
            final RestTemplate template = new RestTemplate();
            final ResponseEntity<VectorResponse> response = template.getForEntity("http://localhost:9090/api/v1/query?query=" + metricName, VectorResponse.class);
            final VectorResponse body = response.getBody();
            final List<VectorResponse.VectorResult> result = body.getData().getResult();
            final VectorResponse.VectorResult metric = result.stream().filter(vectorResult -> vectorResult.getMetric().get("__name__").equals(metricName)).findFirst().orElseThrow();
            if (metric.getValue().size() > 0) {
                final Float value = metric.getValue().get(0);
                logger.info("{} value is {}", metricName, value);
                if (value > metricLimit) {
                    kubernetesClient.runJob();
                }
            }
        }
    }
}
