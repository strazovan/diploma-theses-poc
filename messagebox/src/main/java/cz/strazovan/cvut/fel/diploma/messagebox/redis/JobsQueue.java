package cz.strazovan.cvut.fel.diploma.messagebox.redis;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class JobsQueue implements InitializingBean {

    @Resource(name = "redisTemplate")
    private ListOperations<String, String> listOps;

    @Value("${job.type}")
    private String jobTypeName;

    private final MeterRegistry meterRegistry;

    private final AtomicLong queueSize = new AtomicLong(0);

    public JobsQueue(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }


    public void addJobToLocalQueue(String job) {
        this.listOps.rightPush("jobs", job);
        this.queueSize.set(Objects.requireNonNull(this.listOps.size("jobs")));
    }

    public String getJobForAgent() {
        final String jobs = this.listOps.leftPop("jobs", Duration.ofHours(24));
        this.queueSize.set(Objects.requireNonNull(this.listOps.size("jobs")));
        return jobs;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        this.queueSize.set(this.listOps.size("jobs"));
        final String gaugeName = this.jobTypeName + ".queue.size";
        this.meterRegistry.gauge(gaugeName, this.queueSize);
    }
}
