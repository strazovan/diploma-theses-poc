package cz.strazovan.cvut.fel.diploma.agentcontroller.kubernetes;


import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

@Component
public class KubernetesClient {

    private static final Logger logger = LoggerFactory.getLogger(KubernetesClient.class);

    @Value("${rabbitmq.address}")
    private String rabbitMQAddress;
    @Value("${rabbitmq.username}")
    private String rabbitMQUsername;
    @Value("${rabbitmq.password}")
    private String rabbitMQPassword;

    @Value("${messagebox.exchange}")
    private String messageBoxExchange;

    @Value("${messagebox.jobs.queue}")
    private String messageBoxJobsQueue;

    private final AtomicLong jobsCount = new AtomicLong();


    public KubernetesClient() {

    }

    public void runJob() {
        logger.info("running job");
        try (DefaultKubernetesClient client = new DefaultKubernetesClient()) {
            final String namespace = "default";
            final String jobName = "support-ping-agent-" + jobsCount.addAndGet(1);
            final Job job = new JobBuilder()
                    .withApiVersion("batch/v1")
                    .withNewMetadata()
                    .withName(jobName)
                    .endMetadata()
                    .withNewSpec()
                    .withNewTemplate()
                    .withNewSpec()
                    .addNewContainer()
                    .withName("job-container")
                    .withImage("ping-agent")
                    .withImagePullPolicy("Never")
                    .withEnv(
                            new EnvVarBuilder().withName("EPHEMERAL").withValue("true").build(),
                            new EnvVarBuilder().withName("JOB_SCRIPT_PATH").withValue("/scripts/ping.py").build(),
                            new EnvVarBuilder().withName("JOBS_DESCRIPTIONS_FOLDER").withValue("/jobs/descriptions").build(),
                            new EnvVarBuilder().withName("JOBS_OUTPUTS_FOLDER").withValue("/jobs/results").build(),
                            new EnvVarBuilder().withName("MESSAGE_BOX_EXCHANGE").withValue(this.messageBoxExchange).build(),
                            new EnvVarBuilder().withName("RABBIT_MQ_ADDRESS").withValue(this.rabbitMQAddress).build(),
                            new EnvVarBuilder().withName("RABBITMQ_USERNAME").withValue(this.rabbitMQUsername).build(),
                            new EnvVarBuilder().withName("RABBITMQ_PASSWORD").withValue(this.rabbitMQPassword).build(),
                            new EnvVarBuilder().withName("MESSAGE_BOX_JOBS_QUEUE").withValue(this.messageBoxJobsQueue).build()
                    )
                    .endContainer()
                    .withRestartPolicy("Never")
                    .endSpec()
                    .endTemplate()
                    .endSpec()
                    .build();

            logger.info("Creating agent job.");
            client.batch().v1().jobs().inNamespace(namespace).createOrReplace(job);

        }
    }
}
