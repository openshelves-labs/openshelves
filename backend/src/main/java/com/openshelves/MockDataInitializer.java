package com.openshelves;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MockDataInitializer {

    @Async
    @EventListener(ApplicationReadyEvent.class)
    public void loadData() {
        try {
            log.info("Loading mock data...");

            // Simulate loading mock data (e.g., from a file or database)
            Thread.sleep(2000); // Simulate time-consuming data loading

            log.info("Mock data loaded successfully.");
        } catch (Exception ex) {
            log.info("Failed to load mock data: {}", ex.getMessage(), ex);
        }
    }
}
