package io.kommunicate.agent.model;

/**
 * model for a resource(data object) whose source is a network or database
 * in other words, the retrieval and setting of data takes some time
 * to be used with {@link androidx.lifecycle.LiveData}
 * @param <T> the data
 *
 * @author shubham
 * 25th April, 2020
 */
public class NetworkBoundResourceModel<T> {
    //NOTE: for ease, all "success" states are positive and and "failure" states are negative
    public static final int RESOURCE_RETRIEVING = -4;
    public static final int RESOURCE_SETTING = -3;
    public static final int RESOURCE_FAILURE_GET = -2;
    public static final int RESOURCE_FAILURE_PUT = -1;
    public static final int RESOURCE_OK = 0; //neutral
    public static final int RESOURCE_SUCCESS_GET = 1;
    public static final int RESOURCE_SUCCESS_PUT = 2;

    private T resource;
    private int resourceState;

    public NetworkBoundResourceModel(T resource, int resourceState) {
        this.resource = resource;
        this.resourceState = resourceState;
    }

    public int getResourceState() {
        return resourceState;
    }

    public void setResourceState(int resourceState) {
        this.resourceState = resourceState;
    }

    public T getResource() {
        return resource;
    }

    public void setResource(T resource) {
        this.resource = resource;
    }
}
