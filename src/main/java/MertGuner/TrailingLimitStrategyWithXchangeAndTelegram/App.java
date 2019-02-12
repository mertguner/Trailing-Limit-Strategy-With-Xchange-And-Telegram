package MertGuner.TrailingLimitStrategyWithXchangeAndTelegram;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.meta.TelegramBotsApi;

//import org.telegram.telegrambots.meta.TelegramBotsApi;
//import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args )
    {
        System.out.println( "Starting Application" );
        
        try {
        	ApiContextInitializer.init();
        	new TelegramBotsApi().registerBot(new TelegramBot());
        	
        	Runnable strategy = new TrailingLimitStrategy();
        	
        	ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        	scheduler.scheduleAtFixedRate(strategy, 0, 1, TimeUnit.SECONDS);
        	
		} catch (Exception e) {
			e.printStackTrace();
		}
    }
}
