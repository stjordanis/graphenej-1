package de.bitsharesmunich.graphenej.api;

import com.google.common.primitives.UnsignedLong;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketFactory;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import javax.net.ssl.SSLContext;

import de.bitsharesmunich.graphenej.Asset;
import de.bitsharesmunich.graphenej.AssetAmount;
import de.bitsharesmunich.graphenej.Converter;
import de.bitsharesmunich.graphenej.LimitOrder;
import de.bitsharesmunich.graphenej.OrderBook;
import de.bitsharesmunich.graphenej.UserAccount;
import de.bitsharesmunich.graphenej.interfaces.WitnessResponseListener;
import de.bitsharesmunich.graphenej.models.BaseResponse;
import de.bitsharesmunich.graphenej.models.WitnessResponse;
import de.bitsharesmunich.graphenej.operations.LimitOrderCreateOperation;
import de.bitsharesmunich.graphenej.test.NaiveSSLContext;

import static org.hamcrest.CoreMatchers.is;

/**
 * Created by nelson on 3/24/17.
 */
public class GetLimitOrdersTest {
    private String BLOCK_PAY_DE = System.getenv("BLOCKPAY_DE");
    private UserAccount seller = new UserAccount("1.2.143563");
    private final Asset base = new Asset("1.3.0", "BTS", 5);
    private final Asset quote = new Asset("1.3.120", "EUR", 4);

    private SSLContext context;
    private WebSocket mWebSocket;

    @Before
    public void setUp() throws Exception {
        context = NaiveSSLContext.getInstance("TLS");
        WebSocketFactory factory = new WebSocketFactory();

        // Set the custom SSL context.
        factory.setSSLContext(context);

        mWebSocket = factory.createSocket(BLOCK_PAY_DE);
    }

    @Test
    public void testGetLimitOrders(){
        try {
            mWebSocket.addListener(new GetLimitOrders(base.getObjectId(), quote.getObjectId(), 100, new WitnessResponseListener() {
                @Override
                public void onSuccess(WitnessResponse response) {
                    List<LimitOrder> orders = (List<LimitOrder>) response.result;
                    Assert.assertThat("Checking that we have orders", orders.isEmpty(), is(false));
                    Converter converter = new Converter();
                    double baseToQuoteExchange, quoteToBaseExchange;

                    for(LimitOrder order : orders){
                        if(order.getSellPrice().base.getAsset().getObjectId().equals(base.getObjectId())){
                            order.getSellPrice().base.getAsset().setPrecision(base.getPrecision());
                            order.getSellPrice().quote.getAsset().setPrecision(quote.getPrecision());

                            baseToQuoteExchange = converter.getConversionRate(order.getSellPrice(), Converter.BASE_TO_QUOTE);
                            quoteToBaseExchange = converter.getConversionRate(order.getSellPrice(), Converter.QUOTE_TO_BASE);
                            System.out.println(String.format("> id: %s, base to quote: %.5f, quote to base: %.5f", order.getObjectId(), baseToQuoteExchange, quoteToBaseExchange));
                        }else{
                            order.getSellPrice().base.getAsset().setPrecision(quote.getPrecision());
                            order.getSellPrice().quote.getAsset().setPrecision(base.getPrecision());

                            baseToQuoteExchange = converter.getConversionRate(order.getSellPrice(), Converter.BASE_TO_QUOTE);
                            quoteToBaseExchange = converter.getConversionRate(order.getSellPrice(), Converter.QUOTE_TO_BASE);
                            System.out.println(String.format("< id: %s, base to quote: %.5f, quote to base: %.5f", order.getObjectId(), baseToQuoteExchange, quoteToBaseExchange));
                        }
                    }

                    synchronized (GetLimitOrdersTest.this){
                        GetLimitOrdersTest.this.notifyAll();
                    }
                }

                @Override
                public void onError(BaseResponse.Error error) {
                    System.out.println("onError. Msg: "+error.message);
                    synchronized (GetLimitOrdersTest.this){
                        GetLimitOrdersTest.this.notifyAll();
                    }
                }
            }));

            mWebSocket.connect();

            synchronized (this){
                wait();
            }
        } catch (WebSocketException e) {
            System.out.println("WebSocketException. Msg: " + e.getMessage());
        } catch (InterruptedException e) {
            System.out.println("InterruptedException. Msg: "+e.getMessage());
        }
    }

    /**
     * This method is designed to test how the OrderBook class handles data obtained
     * from the GetLimitOrders API call.
     */
    @Test
    public void testOrderBook(){
        LimitOrderCreateOperation limitOrderOperation;

        try {
            mWebSocket.addListener(new GetLimitOrders(base.getObjectId(), quote.getObjectId(), 100, new WitnessResponseListener() {
                @Override
                public void onSuccess(WitnessResponse response) {
                    List<LimitOrder> orders = (List<LimitOrder>) response.result;
                    OrderBook orderBook = new OrderBook(orders);

                    AssetAmount toBuy = new AssetAmount(UnsignedLong.valueOf(9850000), quote);
                    int expiration = (int) ((System.currentTimeMillis() + 60000) / 1000);
                    LimitOrderCreateOperation operation = orderBook.exchange(seller, base, toBuy, expiration);

                    // Testing the successfull creation of a limit order create operation
                    Assert.assertTrue(operation != null);

                    synchronized (GetLimitOrdersTest.this){
                        GetLimitOrdersTest.this.notifyAll();
                    }
                }

                @Override
                public void onError(BaseResponse.Error error) {
                    System.out.println("onError. Msg: "+error.message);
                    synchronized (GetLimitOrdersTest.this){
                        GetLimitOrdersTest.this.notifyAll();
                    }
                }
            }));

            mWebSocket.connect();

            synchronized (this){
                wait();
            }

        } catch (WebSocketException e) {
            System.out.println("WebSocketException. Msg: " + e.getMessage());
        } catch (InterruptedException e) {
            System.out.println("InterruptedException. Msg: "+e.getMessage());
        }
    }

    @After
    public void tearDown() throws Exception {
        mWebSocket.disconnect();
    }
}