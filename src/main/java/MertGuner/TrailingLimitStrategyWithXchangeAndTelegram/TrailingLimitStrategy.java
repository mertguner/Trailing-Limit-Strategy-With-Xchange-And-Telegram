package MertGuner.TrailingLimitStrategyWithXchangeAndTelegram;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

import org.knowm.xchange.Exchange;
import org.knowm.xchange.ExchangeFactory;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.Order.OrderType;
import org.knowm.xchange.dto.marketdata.OrderBook;
import org.knowm.xchange.dto.trade.LimitOrder;
import org.knowm.xchange.dto.trade.MarketOrder;
import org.knowm.xchange.service.account.AccountService;
import org.knowm.xchange.service.marketdata.MarketDataService;
import org.knowm.xchange.service.trade.TradeService;

public class TrailingLimitStrategy implements Runnable  {
	
	public enum TradeStates {
	    WaitingForBuy,
	    Nothing,
	    WaitingForSell;
	}

	private MarketDataService marketDataService;
	private TradeService tradeSevice;
	private AccountService accountService;
	private CurrencyPair currencyPair = CurrencyPair.ETH_TRY;
	
	private static TrailingLimitStrategy INTENSE = null;

	private TradeStates TradeState = TradeStates.WaitingForSell;
	private double balance = 0;
	private double avaragePrice = 0;
	private double makerFee = 0;
	private boolean lockTrade = false;
	private double maxPrice = Double.MIN_VALUE;	
	private double minPrice = Double.MAX_VALUE;
	
	
	// region VARIABLES
	public static double getMinPrice() {
		if(INTENSE != null)
			return INTENSE.minPrice;
		return 0;
	}

	public static double getMaxPrice() {
		if(INTENSE != null)
			return INTENSE.maxPrice;
		return 0;
	}

	
	public static double getCurrentPrice() {
		if(INTENSE != null)
			return INTENSE.avaragePrice;
		return 0;
	}

	public static void setBalance(double balance) 
	{
		if(INTENSE != null)
			INTENSE.balance = balance;
	}
	
	public static double getBalance() 
	{
		if(INTENSE != null)
			return INTENSE.balance;
		return 0;
	}
	
	public static TradeStates getTradeState() {
		if(INTENSE != null)
			return INTENSE.TradeState;
		return TradeStates.Nothing;
	}
	
	public static String String()
	{
		if(INTENSE != null)
		return "State : " + getTradeState() + "\n" +
				"Balance : " + getBalance() + "\n" +
				((getTradeState().equals(TradeStates.WaitingForSell)) ? "Max Price : " + getMaxPrice() + "\n" : "Min Price : " + getMinPrice() + "\n") +
				"Price : " + getCurrentPrice();
		return "";
	}

	public static TrailingLimitStrategy getINTENSE() {
		return INTENSE;
	}
	// endregion
	
	public TrailingLimitStrategy() {
		INTENSE = this;

		Exchange _exchange = ExchangeFactory.INSTANCE.createExchange(Config.EXCHANGE,Config.APIKEY,Config.SECRETKEY);

		marketDataService = _exchange.getMarketDataService();
		tradeSevice = _exchange.getTradeService();
		accountService = _exchange.getAccountService();
		
		makerFee = 0.002118644;
		try {
			if(accountService != null)
			{
				balance = getAccountBalance(currencyPair.base);
			}else
			{
				balance = 27;						
			}
			
			//MaxPrice = marketDataService.getTicker(currencyPair).getHigh().doubleValue() - Config.AvaragePriceForTrigger;
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	

	public void run(){
		
		try 
		{
			OrderBook orderBook = GetOrderBook(currencyPair);
			
			
		if(TradeState == TradeStates.WaitingForBuy)
		{
			avaragePrice = getAvaregePrice(orderBook.getAsks(), balance);
			
			double _offset = makerFee * avaragePrice;
			
			minPrice = Double.min(minPrice, avaragePrice);
			
			System.out.println("MinPrice : " + minPrice + " - Current Price : " + avaragePrice);
			
			if(lockTrade && Double.compare(avaragePrice, minPrice) < 0)
				lockTrade = false;

			
			if(!lockTrade && Double.compare((minPrice + _offset), avaragePrice) < 0)
			{
				String _message = avaragePrice + "TL'den " + balance + " adet " + currencyPair.base.getDisplayName() + " Aldın";
				System.out.println(_message);
				TelegramBot.getINTENSE().sendMessage(Config.CHAT_ID, _message);
				
				//BuyMarket(currencyPair, balance);
			    
			    maxPrice = avaragePrice + _offset;
			    
			    lockTrade = true;
			    
				TradeState = TradeStates.WaitingForSell;
			}
			
		}else if(TradeState == TradeStates.WaitingForSell)
		{			
			avaragePrice = getAvaregePrice(orderBook.getBids(), balance);
			
			double _offset = makerFee * avaragePrice;
			
			maxPrice = Double.max(maxPrice, avaragePrice);
			
			System.out.println("MaxPrice : " + maxPrice + " - Current Price : " + avaragePrice);
			
			if(lockTrade && Double.compare(avaragePrice, maxPrice) > 0)
				lockTrade = false;
			
			if(!lockTrade && Double.compare((maxPrice - _offset), avaragePrice) > 0)
			{
				String _message = avaragePrice + "TL'den " + balance + " adet " + currencyPair.base.getDisplayName() + " Sattın";
				System.out.println(_message);
				TelegramBot.getINTENSE().sendMessage(Config.CHAT_ID, _message);
				
				//SellMarket(currencyPair, balance);
				
			    minPrice = avaragePrice - _offset;
			    
			    lockTrade = true;
			    
				TradeState = TradeStates.WaitingForBuy;
			}
		}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		
	}

	private double getAvaregePrice(List<LimitOrder> orders, double Balance)
	{
		double _totalPrice = 0; 
		double _totalAmount = 0; 
		
		for	(LimitOrder _value : orders)
		{
			double _amount = _value.getOriginalAmount().doubleValue();
			double _price = _value.getLimitPrice().doubleValue();
			
			
			if(Double.compare((_totalAmount + _amount), Balance) > 0)
			{					
				_totalPrice += ((Balance - _totalAmount) * _price);
				_totalAmount = Balance;
			}else
			{
				_totalPrice += (_price * _amount);
				_totalAmount += _amount; 
			}
				
			
			if(Double.compare(_totalAmount, Balance) == 0)
			{
				return _totalPrice / _totalAmount;
			}				
		}  
		
		return 0;
	}
	
	public double getAccountBalance(Currency currency) throws IOException{
		return accountService.getAccountInfo().getWallet("Main").getBalance(currency).getAvailable().doubleValue();
	}
	
	public OrderBook GetOrderBook(CurrencyPair currencyPair)throws IOException{
		return marketDataService.getOrderBook(currencyPair);
	}
	
	public void BuyMarket(CurrencyPair currencyPair, double price) throws IOException {
		BuySellMarket(currencyPair, price, OrderType.BID);
	}
	
	public void SellMarket(CurrencyPair currencyPair, double price) throws IOException {
		BuySellMarket(currencyPair, price, OrderType.ASK);
	}
	
	private void BuySellMarket(CurrencyPair currencyPair, double price, OrderType orderType) throws IOException 
	{
		MarketOrder market = new MarketOrder(orderType, new BigDecimal(price), currencyPair);
		tradeSevice.placeMarketOrder(market);
	}
	
	public String BuyLimit(CurrencyPair currencyPair, double price) throws IOException {		
		return BuySellLimit(currencyPair, price, price, OrderType.BID);
	}

	public String SellLimit(CurrencyPair currencyPair, double amount, double price) throws IOException {
		return BuySellLimit(currencyPair, amount, price, OrderType.ASK);
	}

	private String BuySellLimit(CurrencyPair currencyPair, double amount, double price, OrderType orderType) throws IOException {
		LimitOrder limitOrder = new LimitOrder(orderType, new BigDecimal(amount), currencyPair, null, null,
				new BigDecimal(price));
		return tradeSevice.placeLimitOrder(limitOrder);
	}
	
	public boolean CancelLimit(String OrderId) throws IOException {
		return tradeSevice.cancelOrder(OrderId);
	}
	
	
}
