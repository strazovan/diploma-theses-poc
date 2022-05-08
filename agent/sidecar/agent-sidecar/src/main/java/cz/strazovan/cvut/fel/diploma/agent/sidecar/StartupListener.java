package cz.strazovan.cvut.fel.diploma.agent.sidecar;


import com.rabbitmq.client.*;
import cz.strazovan.cvut.fel.diploma.agent.sidecar.rabbitmq.QueueConsumer;
import cz.strazovan.cvut.fel.diploma.agent.sidecar.rabbitmq.RabbitMQMessageBoxClient;
import io.micrometer.core.instrument.MeterRegistry;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Value;
import io.micronaut.context.event.ApplicationEventListener;
import io.micronaut.runtime.server.event.ServerStartupEvent;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

@Singleton
public class StartupListener implements ApplicationEventListener<ServerStartupEvent> {

    private static final Logger logger = LoggerFactory.getLogger(StartupListener.class);

    @Value("${messagebox.exchange}")
    protected String exchangeName;

    @Value("${messagebox.jobs.queue}")
    protected String jobsQueue;

    @Value("${jobs.descriptions}")
    protected String jobsDescriptionsFolder;
    @Value("${jobs.outputs}")
    protected String jobsOutputFolder;

    @Value("${job.script.path}")
    protected String jobScriptPath;

    @Inject
    private Connection connection;

    @Inject
    private InactivityController inactivityController;

    @Inject
    private MeterRegistry meterRegistry;

    @Override
    public void onApplicationEvent(ServerStartupEvent event) {
        final Channel channel;
        try {

            channel = this.connection.createChannel();
            channel.exchangeDeclare(exchangeName, BuiltinExchangeType.DIRECT, true);
            final AMQP.Queue.DeclareOk queueDeclare = channel.queueDeclare(this.jobsQueue, false, false, true, null);
            channel.queueBind(queueDeclare.getQueue(), exchangeName, exchangeName + queueDeclare.getQueue());
            logger.info("Created queue {}", queueDeclare.getQueue());
            final RabbitMQMessageBoxClient client = new RabbitMQMessageBoxClient(channel, queueDeclare.getQueue(), exchangeName);
            channel.basicConsume(queueDeclare.getQueue(), new QueueConsumer(channel, jobsDescriptionsFolder, jobsOutputFolder, jobScriptPath, client, inactivityController, meterRegistry));
            client.askForJob();
        } catch (IOException e) {
            throw new RuntimeException("Failed to init channel and communication.", e);
        }
    }
}
