package com.leskor.babelbot.telegram;

import com.leskor.babelbot.service.DefinitionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.BotSession;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.AfterBotRegistration;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Component
public class TelegramBot implements SpringLongPollingBot, LongPollingSingleThreadUpdateConsumer {
    private static final Logger logger = LoggerFactory.getLogger(TelegramBot.class);

    private static final String WELCOME_TEXT = "👋 Welcome to BabelBot!\n\nSend me a word, and I'll provide you with its definition.";

    private final String botToken;
    private final DefinitionService definitionService;
    private final TelegramClient telegramClient;

    public TelegramBot(@Value("${babelbot.telegram.bot-token}") String botToken, DefinitionService definitionService,
            TelegramClient telegramClient) {
        this.botToken = botToken;
        this.definitionService = definitionService;
        this.telegramClient = telegramClient;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public LongPollingUpdateConsumer getUpdatesConsumer() {
        return this;
    }

    @Override
    public void consume(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            String normalizedText = messageText.trim().toLowerCase();
            String responseText = switch (normalizedText) {
                case "/start" -> WELCOME_TEXT;
                default -> handleDefinition(normalizedText);
            };

            SendMessage message = SendMessage.builder().chatId(chatId).text(responseText).parseMode("HTML").build();
            try {
                telegramClient.execute(message);
            }
            catch (TelegramApiException e) {
                logger.error("Failed to respond to user in Telegram", e);
            }
        }
    }

    private String handleDefinition(String query) {
        if (query.isEmpty()) {
            return "Please provide a word to define.";
        }

        return definitionService.getDefinition(query).map(d -> {
            StringBuilder builder = new StringBuilder();
            builder.append(String.format("<b>%s</b> (<i>%s</i>)%n%n", d.term(), d.partOfSpeech().toLowerCase()));
            d.meanings().forEach(m -> builder.append(String.format("• %s%n", m)));
            if (!d.etymology().isEmpty()) {
                builder.append(String.format("%n<i>Etymology:</i> %s", d.etymology()));
            }
            if (!d.synonyms().isEmpty()) {
                builder.append(String.format("%n%n<i>Synonyms:</i> %s", String.join(", ", d.synonyms())));
            }
            return builder.toString();
        }).orElse(String.format("Cannot find definition for word '%s'", query));
    }

    @AfterBotRegistration
    public void afterRegistration(BotSession botSession) {
        logger.info("Registered bot running state is: {}", botSession.isRunning());
    }
}
