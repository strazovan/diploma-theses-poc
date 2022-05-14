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

import java.util.*;
import java.util.concurrent.*;

@Component
public class AgentsListener implements SmartLifecycle {
    private static final Logger logger = LoggerFactory.getLogger(AgentsListener.class);

    private final JobsQueue jobsQueue;

    private final RabbitTemplate rabbitTemplate;

    @Value("${configuration.rabbitmq.jobs-results-exchange}")
    private String jobResultsExchange;

    private Map<String, Map<String, String>> jobsSentToClient;

    private ExecutorService executorService;
    private ScheduledExecutorService hbExecutor;
    private HbJob hbJob;
    private volatile boolean running;


    public AgentsListener(JobsQueue jobsQueue, RabbitTemplate rabbitTemplate) {
        this.jobsQueue = jobsQueue;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void start() {
        this.hbJob = new HbJob();
        this.executorService = Executors.newSingleThreadExecutor();
        this.hbExecutor = Executors.newScheduledThreadPool(1);
        this.hbExecutor.scheduleAtFixedRate(this.hbJob, 0, 10, TimeUnit.SECONDS);
        this.jobsSentToClient = new ConcurrentHashMap<>();
        this.running = true;
    }

    @Override
    public void stop() {
        this.executorService.shutdownNow();
        this.hbExecutor.shutdownNow();
        this.running = false;
    }

    @Override
    public boolean isRunning() {
        return this.running;
    }

    @RabbitListener(queues = "#{agentsResultsQueue.name}", ackMode = "AUTO")
    public void receive(String payload, Message rawMessage) throws JsonProcessingException {
        final String replyTo = rawMessage.getMessageProperties().getReplyTo();
        final String agent = rawMessage.getMessageProperties().getHeader("x-agent").toString();
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
                final String jobId = UUID.randomUUID().toString();
                this.jobsSentToClient.computeIfAbsent(agent, s -> new HashMap<>()).put(jobId, job);
                this.rabbitTemplate.convertAndSend(replyTo, job, message -> {
                    message.getMessageProperties().setHeader("x-jobid", jobId);
                    return message;
                });
            });
        } else {

            if (rawMessage.getMessageProperties().getHeader("x-heartbeat") != null) {
                // hb
                logger.info("Received hb from agent {}", agent);
                this.hbJob.notifyHb(agent);
            } else {
                // agent sends result
                final String jobId = rawMessage.getMessageProperties().getHeader("x-job-id").toString();
                this.jobsSentToClient.computeIfAbsent(agent, s -> new HashMap<>()).remove(jobId);
                logger.info("Sending result {} to {}", payload, this.jobResultsExchange);
                this.rabbitTemplate.convertAndSend(this.jobResultsExchange, "", payload);
            }
        }
    }

    private class HbJob implements Runnable {

        private final Map<String, Long> lastHb = new HashMap<>();

        @Override
        public synchronized void run() {
            final Long now = new Date().getTime();
            final List<String> deadAgents = new ArrayList<>();
            lastHb.entrySet()
                    .stream()
                    .filter(entry -> now - entry.getValue() > 10000)
                    .forEach(entry -> deadAgents.add(entry.getKey()));
            logger.info("Removing {} dead agents", deadAgents.size());
            for (String deadAgent : deadAgents) {
                this.lastHb.remove(deadAgent);
                final Map<String, String> deadAgentJobs = jobsSentToClient.remove(deadAgent);
                logger.info("Returning {} jobs back to queue", deadAgentJobs.values().size());
                deadAgentJobs.values().forEach(jobsQueue::addJobToLocalQueue);
            }
        }

        public synchronized void notifyHb(String agent) {
            this.lastHb.put(agent, new Date().getTime());
        }
    }

}
