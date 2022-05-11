package cz.strazovan.cvut.fel.diploma.messagebox;

import io.micrometer.core.instrument.ImmutableTag;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class VersionGauge implements InitializingBean {

    @Value("${app.version}")
    private String appVersion;

    private final MeterRegistry meterRegistry;

    public VersionGauge(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }


    @Override
    public void afterPropertiesSet() throws Exception {
        Tag versionTag = new ImmutableTag("application_version", this.appVersion);
        this.meterRegistry.gauge("application_info",
                List.of(versionTag),
                new AtomicInteger(1));
    }
}
