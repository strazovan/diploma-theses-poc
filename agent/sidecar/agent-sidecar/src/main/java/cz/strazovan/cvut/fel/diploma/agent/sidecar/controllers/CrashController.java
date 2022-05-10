package cz.strazovan.cvut.fel.diploma.agent.sidecar.controllers;

import io.micronaut.context.ApplicationContext;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import jakarta.inject.Inject;

@Controller("/crash")
public class CrashController {

    @Inject
    private ApplicationContext context;


    @Get
    public void crash() {
        this.context.stop();
        System.exit(-1);
    }

}
