package cz.strazovan.cvut.fel.diploma.messagebox.redis;

import org.springframework.data.redis.core.ListOperations;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

@Component
public class JobsQueue {

    @Resource(name = "redisTemplate")
    private ListOperations<String, String> listOps;

    public void addJobToLocalQueue(String job) {
        this.listOps.rightPush("jobs", job);
    }

    public String getJobForAgent() {
        return this.listOps.leftPop("jobs");
    }
}
