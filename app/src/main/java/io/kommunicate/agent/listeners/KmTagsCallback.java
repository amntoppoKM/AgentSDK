package io.kommunicate.agent.listeners;

import io.kommunicate.agent.model.KmTag;

public interface KmTagsCallback {
    void onSingleClick(KmTag kmTag);

    boolean onLongClick(KmTag kmTag);

    void onDeleteTag(KmTag kmTag);

    void onRenameTag(KmTag kmTag);

    void onRenameSuccess(KmTag kmTag, String newTagName);

    void onCancel();
}
