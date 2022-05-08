package cz.strazovan.cvut.fel.diploma.messagebox.rabbitmq;


import com.fasterxml.jackson.core.JsonProcessingException;
import cz.strazovan.cvut.fel.diploma.messagebox.redis.JobsQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class IncomingJobsReceiver {

    private static final Logger logger = LoggerFactory.getLogger(IncomingJobsReceiver.class);

    private final JobsQueue jobsQueue;

    @Autowired
    public IncomingJobsReceiver(JobsQueue jobsQueue) {
        this.jobsQueue = jobsQueue;
    }

    @RabbitListener(queues = "#{incomingJobsQueue.name}", ackMode = "AUTO")
    public void receive(String message) throws JsonProcessingException {
        logger.info(message);
        this.jobsQueue.addJobToLocalQueue(message);
    }

}
