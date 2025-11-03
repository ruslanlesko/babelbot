package com.leskor.babelbot;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = { BabelBotApplication.class }, properties = {
        "spring.autoconfigure.exclude=org.telegram.telegrambots.longpolling.starter.TelegramBotStarterConfiguration" })
@ActiveProfiles("test")
class BabelBotApplicationTests {

    @Test
    void contextLoads() {
        // This test should fail if the application context cannot start
    }
}
