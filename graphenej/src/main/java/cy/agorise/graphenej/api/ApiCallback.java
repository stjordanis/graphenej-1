package cy.agorise.graphenej.api;

import javax.annotation.Nullable;

import cy.agorise.graphenej.models.JsonRpcResponse;
import okhttp3.Response;

/**
 * Interface defining the basic contract an API request can expect.
 */
public interface ApiCallback {

    /**
     * Method called whenever we have a successful response from the node.
     *
     * @param response  Parsed response
     * @param text      Raw text response
     */
    <T> void onResponse(JsonRpcResponse<T> response, String text);

    /**
     * Method called whenever there was an error and the response could not be delivered.
     *
     * @param t         Error Throwable, potentially with some details about the problem.
     * @param response  Node response, if any
     */
    void onFailure(Throwable t, @Nullable Response response);
}