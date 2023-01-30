package io.kommunicate.agent.conversations;

import android.view.View;

public interface KmClickHandler<T>  {
    void onItemClicked(View view, T data);
}
