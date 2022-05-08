package cz.strazovan.cvut.fel.diploma.messagebox.rabbitmq;


import com.fasterxml.jackson.core.JsonProcessingException;
import cz.strazovan.cvut.fel.diploma.messagebox.redis.JobsQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class AgentsListener implements SmartLifecycle {
    private static final Logger logger = LoggerFactory.getLogger(AgentsListener.class);

    private final JobsQueue jobsQueue;

    private final RabbitTemplate rabbitTemplate;

    @Value("${configuration.rabbitmq.jobs-results-exchange}")
    private String jobResultsExchange;

    private ExecutorService executorService;
    private volatile boolean running;


    public AgentsListener(JobsQueue jobsQueue, RabbitTemplate rabbitTemplate) {
        this.jobsQueue = jobsQueue;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void start() {
        this.executorService = Executors.newSingleThreadExecutor();
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

    @RabbitListener(queues = "#{agentsResultsQueue.name}", ackMode = "AUTO")
    public void receive(String payload, Message rawMessage) throws JsonProcessingException {
        final String replyTo = rawMessage.getMessageProperties().getReplyTo();
        if (replyTo != null) {
            // agent requests work
            this.executorService.submit(() -> {
                // this is async because me might have to wait for a job to appear
                logger.info("Trying to retrieve a job");
                final String job = this.jobsQueue.getJobForAgent();
                if (job == null) {
                    logger.warn("job is null");
                    return;
                }
                logger.info("Sending job to the agent");
                this.rabbitTemplate.convertAndSend(replyTo, job, message -> {
                    message.getMessageProperties().setHeader("x-jobid", UUID.randomUUID().toString());
                    return message;
                });
            });
        } else {
            // agent sends result
            logger.info("Sending result {} to {}", payload, this.jobResultsExchange);
            this.rabbitTemplate.convertAndSend(this.jobResultsExchange, "", payload);
        }
    }

}
