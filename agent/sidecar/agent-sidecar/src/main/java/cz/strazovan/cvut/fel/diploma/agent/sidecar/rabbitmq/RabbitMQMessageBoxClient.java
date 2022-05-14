package cz.strazovan.cvut.fel.diploma.agent.sidecar.rabbitmq;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import cz.strazovan.cvut.fel.diploma.agent.sidecar.HeartbeatProducer;
import io.micronaut.context.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class RabbitMQMessageBoxClient implements MessageBoxClient {

    private static final Logger logger = LoggerFactory.getLogger(RabbitMQMessageBoxClient.class);

    private final Channel channel;
    private final String responseQueue;
    private final String exchangeName;

    private String agentsIdentifier;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public RabbitMQMessageBoxClient(String agentsIdentifier, Channel channel, String responseQueue, String exchangeName) {
        this.agentsIdentifier = agentsIdentifier;
        this.channel = channel;
        this.responseQueue = responseQueue;
        this.exchangeName = exchangeName;
    }

    @Override
    public void askForJob() {
        AMQP.BasicProperties props = new AMQP.BasicProperties
                .Builder()
                .headers(Collections.singletonMap("x-agent", this.agentsIdentifier))
                .replyTo(responseQueue)
                .build();

        try {
            channel.basicPublish(exchangeName, "", props, this.objectMapper.writeValueAsBytes(Collections.singletonMap("state", "ready")));
        } catch (IOException e) {
            throw new RuntimeException("Failed to send the message", e);
        }
    }

    @Override
    public void sendResult(String jobId, byte[] result) {
        final Map<String, Object> headers = new HashMap<>();
        headers.put("x-job-id", jobId);
        headers.put("x-agent", this.agentsIdentifier);
        AMQP.BasicProperties props = new AMQP.BasicProperties
                .Builder()
                .headers(headers)
                .build();

        try {
            channel.basicPublish(exchangeName, "", props, result);
        } catch (IOException e) {
            throw new RuntimeException("Failed to send the message", e);
        }
    }

    @Override
    public void sendHeartbeat() {
        final Map<String, Object> headers = new HashMap<>();
        headers.put("x-heartbeat", "true");
        headers.put("x-agent", this.agentsIdentifier);
        AMQP.BasicProperties props = new AMQP.BasicProperties
                .Builder()
                .headers(headers)
                .build();

        logger.info("Sending hb from agent {}", this.agentsIdentifier);
        try {
            channel.basicPublish(exchangeName, "", props, "hb".getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException("Failed to send the message", e);
        }
    }
}
