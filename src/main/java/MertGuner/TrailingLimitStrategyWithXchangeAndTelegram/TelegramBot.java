package MertGuner.TrailingLimitStrategyWithXchangeAndTelegram;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

public class TelegramBot extends TelegramLongPollingBot {
	
	private static TelegramBot INTENSE;

	public static TelegramBot getINTENSE() {
		return INTENSE;
	}

	public TelegramBot() {
		super();
		INTENSE = this;
	}

	public void sendMessage(Long chatID, String message) {
		sendMessage(new SendMessage().setChatId(chatID).setText(message));
	}

	public void sendMessage(SendMessage message) {
		try {
			if (message != null)
				execute(message);
		} catch (TelegramApiException e) {
			e.printStackTrace();
		}
	}

	//@Override
	public String getBotUsername() {
		return Config.BOT_USERNAME;
	}

	@Override
	public String getBotToken() {
		return Config.BOT_TOKEN;
	}

	//@Override
	public void onUpdateReceived(Update update) {	
		if (update.hasMessage() && update.getMessage().hasText()) {
			Message receivedMessage = update.getMessage();
			Long ChatId = receivedMessage.getChatId();
			//String UserName = receivedMessage.getFrom().getUserName();
			String UserMessage = update.getMessage().getText();
			//int MessageId = receivedMessage.getMessageId();

			if (UserMessage.startsWith("/getbalance"))
			{
				sendMessage(ChatId, Double.toString(TrailingLimitStrategy.getBalance()));
			}else if (UserMessage.startsWith("/setbalance"))
			{
				TrailingLimitStrategy.setBalance(Double.parseDouble(UserMessage.split(" ")[1].trim().replace(",", ".")));
				sendMessage(ChatId, "OK");
			}else if (UserMessage.startsWith("/status"))
			{
				sendMessage(ChatId, TrailingLimitStrategy.String());
			}
		}
	}
}
