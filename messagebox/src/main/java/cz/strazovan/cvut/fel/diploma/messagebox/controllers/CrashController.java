package cz.strazovan.cvut.fel.diploma.messagebox.controllers;


import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/crash")
public class CrashController {

    private final ApplicationContext applicationContext;

    public CrashController(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @GetMapping
    public void crash() {
        ((ConfigurableApplicationContext) this.applicationContext).close();
        System.exit(-1);
    }
}
