package cz.strazovan.cvut.fel.diploma.agent.sidecar.rabbitmq;

import com.rabbitmq.client.*;
import cz.strazovan.cvut.fel.diploma.agent.sidecar.InactivityController;
import io.micrometer.core.instrument.MeterRegistry;
import io.micronaut.core.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

public class QueueConsumer extends DefaultConsumer {
    private static final Logger logger = LoggerFactory.getLogger(QueueConsumer.class);
    private final MessageBoxClient client;

    private final String jobsDescriptionsFolder;
    private final String jobsOutputFolder;

    private final String jobScriptPath;
    private final InactivityController inactivityController;
    private final MeterRegistry meterRegistry;

    public QueueConsumer(Channel channel, String jobsDescriptionsFolder, String jobsOutputFolder, String jobScriptPath, MessageBoxClient client, InactivityController inactivityController, MeterRegistry meterRegistry) {
        super(channel);
        this.jobsDescriptionsFolder = jobsDescriptionsFolder;
        this.jobsOutputFolder = jobsOutputFolder;
        this.jobScriptPath = jobScriptPath;
        this.client = client;
        this.inactivityController = inactivityController;
        this.meterRegistry = meterRegistry;
    }

    @Override
    public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
        this.inactivityController.notifyJob();
        this.meterRegistry.counter("received_jobs").increment();
        final String jobId = properties.getHeaders().get("x-jobid").toString();
        final String bodyAsString = new String(body);
        logger.info(bodyAsString);
        if (bodyAsString.contains("swallow")) {
            logger.info("Job swallowed.");
            return;
        }
        final String descriptionFile = this.jobsDescriptionsFolder + "/" + jobId + ".json";
        final String outputFolder = this.jobsOutputFolder + "/" + jobId;
        new File(outputFolder).mkdir();
        try (OutputStream fos = Files.newOutputStream(Paths.get(descriptionFile))) {
            fos.write(body);
        }

        ProcessBuilder processBuilder = new ProcessBuilder("python3", this.jobScriptPath, descriptionFile, outputFolder);
        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();
        try {
            int exitCode = process.waitFor();
            logger.info("Return code is {}, output is {}", exitCode, readInputStream(process.getInputStream()));
            this.meterRegistry.counter("processed_jobs").increment();
            // collect the results and send them back
            try (final Stream<Path> list = Files.list(Paths.get(outputFolder))) {
                list.forEach(path -> {
                    try (final InputStream fis = Files.newInputStream(path.toFile().toPath())) {
                        final String result = readInputStream(fis);
                        client.sendResult(jobId, result.getBytes(StandardCharsets.UTF_8));
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to read the result.", e);
                    }
                });
            }
            // after sending the results, ask for more work
            this.client.askForJob();
        } catch (InterruptedException e) {
            throw new RuntimeException("Job script has failed", e);
        }

    }

    private String readInputStream(InputStream inputStream) throws IOException {
        return IOUtils.readText(new BufferedReader(new InputStreamReader(inputStream)));
    }
}
