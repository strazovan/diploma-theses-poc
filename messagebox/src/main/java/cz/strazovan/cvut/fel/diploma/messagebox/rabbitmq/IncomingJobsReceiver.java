package cz.strazovan.cvut.fel.diploma.messagebox.rabbitmq;


import com.fasterxml.jackson.core.JsonProcessingException;
import cz.strazovan.cvut.fel.diploma.messagebox.redis.JobsQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class IncomingJobsReceiver {

    private static final Logger logger = LoggerFactory.getLogger(IncomingJobsReceiver.class);

    private final JobsQueue jobsQueue;

    @Value("${configuration.rabbitmq.jobs-results-exchange}")
    private String jobResultsExchange;
    private final RabbitTemplate rabbitTemplate;


    @Autowired
    public IncomingJobsReceiver(JobsQueue jobsQueue, RabbitTemplate rabbitTemplate) {
        this.jobsQueue = jobsQueue;
        this.rabbitTemplate = rabbitTemplate;
    }

    @RabbitListener(queues = "#{incomingJobsQueue.name}", ackMode = "AUTO")
    public void receive(String message) throws JsonProcessingException {
        logger.info(message);
        if (message != null && message.contains("invalid")) {
            logger.error("Invalid request {}", message);
            this.rabbitTemplate.convertAndSend(this.jobResultsExchange, "", "{\"status\": \"rejected\"}");
            return;
        }
        if (message != null && message.contains("unauthorized")) {
            logger.error("Unauthorized request {}", message);
            this.rabbitTemplate.convertAndSend(this.jobResultsExchange, "", "{\"status\": \"rejected\"}");
            return;
        }
        this.jobsQueue.addJobToLocalQueue(message);
    }

}
