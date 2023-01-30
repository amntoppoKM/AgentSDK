package io.kommunicate.agent.conversations;

public interface KmConversationStatusListener {

    void onStatusChange(int newStatus);

    void onAssigneeChange(String newAssigneeId, String teamId);
}
