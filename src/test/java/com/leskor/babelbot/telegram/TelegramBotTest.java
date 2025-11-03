package com.leskor.babelbot.telegram;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.leskor.babelbot.model.Definition;
import com.leskor.babelbot.service.DefinitionService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.longpolling.BotSession;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@ExtendWith(MockitoExtension.class)
class TelegramBotTest {

    @Mock
    private DefinitionService definitionService;

    @Mock
    private TelegramClient telegramClient;

    @Mock
    private Update update;

    @Mock
    private Message message;

    @Mock
    private BotSession botSession;

    private TelegramBot telegramBot;

    private static final String BOT_TOKEN = "test-token";
    private static final long CHAT_ID = 12345L;

    @BeforeEach
    void setUp() {
        telegramBot = new TelegramBot(BOT_TOKEN, definitionService, telegramClient);
    }

    @Test
    void getBotToken_shouldReturnCorrectToken() {
        assertEquals(BOT_TOKEN, telegramBot.getBotToken());
    }

    @Test
    void getUpdatesConsumer_shouldReturnSelf() {
        assertEquals(telegramBot, telegramBot.getUpdatesConsumer());
    }

    @Test
    void consume_withStartCommand_shouldSendWelcomeMessage() throws Exception {
        setupMocks("/start");

        telegramBot.consume(update);

        verify(telegramClient).execute(any(SendMessage.class));
    }

    @Test
    void consume_withRegularText_shouldCallDefinitionService() throws Exception {
        String testWord = "hello";
        setupMocks(testWord);

        Definition mockDefinition = new Definition("hello", "noun", List.of("greeting"), "", List.of());
        when(definitionService.getDefinition(testWord)).thenReturn(Optional.of(mockDefinition));

        telegramBot.consume(update);

        verify(definitionService).getDefinition(testWord);
        verify(telegramClient).execute(any(SendMessage.class));
    }

    @Test
    void consume_withEmptyText_shouldReturnErrorMessage() throws Exception {
        setupMocks("   ");

        telegramBot.consume(update);

        verify(telegramClient).execute(any(SendMessage.class));
    }

    @Test
    void consume_withUnknownWord_shouldReturnNotFoundMessage() throws Exception {
        String unknownWord = "unknownword";
        setupMocks(unknownWord);

        when(definitionService.getDefinition(unknownWord)).thenReturn(Optional.empty());

        telegramBot.consume(update);

        verify(definitionService).getDefinition(unknownWord);
        verify(telegramClient).execute(any(SendMessage.class));
    }

    @Test
    void consume_withoutMessage_shouldNotProcess() throws Exception {
        when(update.hasMessage()).thenReturn(false);

        telegramBot.consume(update);

        verify(telegramClient, never()).execute(any(SendMessage.class));
    }

    @Test
    void consume_withoutText_shouldNotProcess() throws Exception {
        when(update.hasMessage()).thenReturn(true);
        when(update.getMessage()).thenReturn(message);
        when(message.hasText()).thenReturn(false);

        telegramBot.consume(update);

        verify(telegramClient, never()).execute(any(SendMessage.class));
    }

    @Test
    void afterRegistration_shouldLogBotState() {
        when(botSession.isRunning()).thenReturn(true);

        assertDoesNotThrow(() -> telegramBot.afterRegistration(botSession));

        verify(botSession).isRunning();
    }

    private void setupMocks(String messageText) {
        when(update.hasMessage()).thenReturn(true);
        when(update.getMessage()).thenReturn(message);
        when(message.hasText()).thenReturn(true);
        when(message.getText()).thenReturn(messageText);
        when(message.getChatId()).thenReturn(CHAT_ID);
    }
}
