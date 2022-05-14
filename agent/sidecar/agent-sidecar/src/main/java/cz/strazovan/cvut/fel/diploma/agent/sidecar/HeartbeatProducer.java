package cz.strazovan.cvut.fel.diploma.agent.sidecar;


import cz.strazovan.cvut.fel.diploma.agent.sidecar.rabbitmq.MessageBoxClient;
import io.micronaut.scheduling.annotation.Scheduled;
import jakarta.inject.Singleton;

@Singleton
public class HeartbeatProducer {

    private volatile MessageBoxClient client;

    @Scheduled(fixedDelay = "5s", initialDelay = "5s")
    public void sendHeartbeat() {
        if (this.client != null)
            this.client.sendHeartbeat();
    }

    public void setClient(MessageBoxClient client) {
        this.client = client;
    }
}
