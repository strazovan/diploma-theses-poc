package cz.strazovan.cvut.fel.diploma.messagebox.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan("cz.strazovan.cvut.fel.diploma.messagebox.rabbitmq")
public class RabbitMqConfig {

    @Value("${configuration.rabbitmq.jobs-exchange}")
    private String jobExchange;
    @Value("${configuration.rabbitmq.jobs-results-exchange}")
    private String jobResultsExchange;
    @Value("${configuration.rabbitmq.agents-exchange}")
    private String agentsExchange;

    @Value("${configuration.rabbitmq.incoming-jobs-queue}")
    private String incomingJobsQueue;

    @Value("${configuration.rabbitmq.incoming-jobs-routingKey}")
    private String incomingJobsRoutingKey;

    @Value("${configuration.rabbitmq.agents-results-queue}")
    private String agentsResultsQueue;

    @Bean(name = "jobsResultsExchange")
    public TopicExchange jobsResultsExchange() {
        return new TopicExchange(this.jobResultsExchange, true, true);
    }

    @Bean(name = "jobsExchange")
    public TopicExchange jobsExchange() {
        return new TopicExchange(this.jobExchange, true, true);
    }

    @Bean(name = "agentsExchange")
    public DirectExchange agentsExchange() {
        return new DirectExchange(this.agentsExchange, true, false);
    }

    @Bean(name = "incomingJobsQueue")
    public Queue incomingJobsQueue() {
        return new Queue(incomingJobsQueue, true, true, true);
    }

    @Bean(name = "agentsResultsQueue")
    public Queue agentsResultsQueue() {
        return new Queue(agentsResultsQueue, true, true, true);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(final ConnectionFactory connectionFactory) {
        final var rabbitTemplate = new RabbitTemplate(connectionFactory);
//        rabbitTemplate.setMessageConverter(producerJackson2MessageConverter());
        return rabbitTemplate;
    }

//    @Bean
//    public Jackson2JsonMessageConverter producerJackson2MessageConverter() {
//        return new Jackson2JsonMessageConverter();
//    }
//

    @Bean
    public Binding incomingJobsBinding(@Qualifier("jobsExchange") TopicExchange jobsExchange,
                                       @Qualifier("incomingJobsQueue") Queue incomingJobsQueue) {
        return BindingBuilder.bind(incomingJobsQueue)
                .to(jobsExchange)
                .with(this.incomingJobsRoutingKey);
    }

    @Bean
    public Binding agentsResultsBinding(@Qualifier("agentsExchange") DirectExchange agentsExchange,
                                        @Qualifier("agentsResultsQueue") Queue agentsResultsQueue) {
        return BindingBuilder.bind(agentsResultsQueue)
                .to(agentsExchange)
                .with("");
    }
}
