package cz.strazovan.cvut.fel.diploma.agent.sidecar.rabbitmq;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;

import java.io.IOException;
import java.util.Collections;

public class RabbitMQMessageBoxClient implements MessageBoxClient {

    private final Channel channel;
    private final String responseQueue;
    private final String exchangeName;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public RabbitMQMessageBoxClient(Channel channel, String responseQueue, String exchangeName) {
        this.channel = channel;
        this.responseQueue = responseQueue;
        this.exchangeName = exchangeName;
    }

    @Override
    public void askForJob() {
        AMQP.BasicProperties props = new AMQP.BasicProperties
                .Builder()
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
        AMQP.BasicProperties props = new AMQP.BasicProperties
                .Builder()
                .headers(Collections.singletonMap("x-job-id", jobId))
                .build();

        try {
            channel.basicPublish(exchangeName, "", props, result);
        } catch (IOException e) {
            throw new RuntimeException("Failed to send the message", e);
        }
    }
}
