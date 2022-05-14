package cz.strazovan.cvut.fel.diploma.configuration.controllers;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("configuration")
public class ConfigurationController {

    private final Map<String, String> configurationMap = new ConcurrentHashMap<>();

    @GetMapping(value = "/{key}", produces = MediaType.APPLICATION_JSON_VALUE)
    public String get(@PathVariable("key") String key) {
        return this.configurationMap.getOrDefault(key, "{}");
    }

    @PostMapping(value = "/{key}", consumes= MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public String post(@PathVariable("key") String key,
                     @RequestBody String body) {
        this.configurationMap.put(key, body);
        return body;
    }
}
