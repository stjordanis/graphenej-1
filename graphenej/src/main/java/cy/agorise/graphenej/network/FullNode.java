package cy.agorise.graphenej.network;

import cy.agorise.graphenej.stats.ExponentialMovingAverage;

/**
 * Class that represents a full node and is used to keep track of its round-trip time measured in milliseconds.
 */
public class FullNode implements Comparable {

    private String mUrl;
    private ExponentialMovingAverage mLatency;
    private boolean isConnected;
    private boolean isRemoved;

    private FullNode(){}

    /**
     * Constructor used to specify both the node URL and the alpha parameter that one wishes to set the
     * exponential moving average with.
     * <p>
     * The alpha parameter represents the degree of weighting decrease, and can be specified as any value
     * between 0 and 1. A higher alpha discounts older observations faster.
     *
     * @param url       The node URL.
     * @param alpha     The alpha parameter used to compute the exponential moving average.
     */
    public FullNode(String url, double alpha){
        mLatency = new ExponentialMovingAverage(alpha);
        mUrl = url;
    }

    /**
     * Constructor used to specify only the node URL.
     * <p>
     * The alpha parameter is set to the value specified at {@link ExponentialMovingAverage#DEFAULT_ALPHA}
     *
     * @param url   The node URL.
     */
    public FullNode(String url){
        this(url, ExponentialMovingAverage.DEFAULT_ALPHA);
    }

    /**
     * Full node URL getter
     * @return
     */
    public String getUrl() {
        return mUrl;
    }

    /**
     * Full node URL setter
     * @param mUrl
     */
    public void setUrl(String mUrl) {
        this.mUrl = mUrl;
    }

    /**
     *
     * @return  The latest latency average value. If no measurement has been taken yet, the
     * maximum allows value of a long primitive, or 2<sup>63</sup>-1 will be returned.
     */
    public double getLatencyValue() {
        double average = mLatency.getAverage();
        if(average == 0){
            return Long.MAX_VALUE;
        }else{
            return average;
        }
    }

    public boolean isConnected() {
        return isConnected;
    }

    public void setConnected(boolean connected) {
        isConnected = connected;
    }

    public boolean isRemoved() {
        return isRemoved;
    }

    public void setRemoved(boolean removed) {
        isRemoved = removed;
    }

    /**
     * Method that updates the mLatency average with a new value.
     * @param latency   Most recent mLatency sample to be added to the exponential average
     */
    public void addLatencyValue(double latency) {
        this.mLatency.updateValue(latency);
    }

    @Override
    public int compareTo(Object o) {
        FullNode node = (FullNode) o;
        double doubleResult = getLatencyValue() - node.getLatencyValue();
        if(doubleResult > 0)
            return 1;
        else if(doubleResult < 0)
            return -1;
        else
            return 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FullNode fullNode = (FullNode) o;
        return mUrl.equals(fullNode.getUrl());
    }

    @Override
    public int hashCode() {
        return mUrl.hashCode();
    }
}
