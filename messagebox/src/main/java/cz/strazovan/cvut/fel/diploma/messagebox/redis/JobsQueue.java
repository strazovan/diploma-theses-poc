package cz.strazovan.cvut.fel.diploma.messagebox.redis;

import org.springframework.data.redis.core.ListOperations;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class JobsQueue {

    @Resource(name = "redisTemplate")
    private ListOperations<String, String> listOps;

    public void addJobToLocalQueue(String job) {
        this.listOps.rightPush("jobs", job);
        final String storedJob = this.listOps.leftPop("jobs");
        System.out.println("Retrieved job " + storedJob);
    }
}
