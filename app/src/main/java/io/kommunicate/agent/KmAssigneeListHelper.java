package io.kommunicate.agent;

import android.content.Context;
import android.os.AsyncTask;
import android.util.SparseArray;

import java.util.ArrayList;
import java.util.List;

import io.kommunicate.agent.asyncs.AgentGetStatusTask;
import io.kommunicate.agent.asyncs.KmGetUserListTask;
import io.kommunicate.agent.conversations.viewmodels.KmAppSettingsViewModel;
import io.kommunicate.agent.model.KmAgentContact;
import io.kommunicate.agent.model.KmTeam;
import io.kommunicate.agent.viewmodels.KmTagsViewModel;
import io.kommunicate.agent.viewmodels.KmTeamViewModel;
import io.kommunicate.callbacks.KmCallback;

public class KmAssigneeListHelper {
    public static final int BOT_LIST_CODE = 0;
    public static final int AGENT_LIST_CODE = 1;
    public static final int TEAM_LIST_CODE = 2;
    private static final SparseArray<List<KmAgentContact>> assigneeListSparseArray = new SparseArray<>();
    private static final List<KmTeam> teamList = new ArrayList<>();

    public static List<KmAgentContact> getAssigneeList(int assigneeTypeCode) {
        return assigneeListSparseArray.get(assigneeTypeCode);
    }

    public static List<KmTeam> getTeamList() {
        return teamList;
    }

    public static boolean isListEmpty(int assigneeTypeCode) {
        if (assigneeTypeCode == TEAM_LIST_CODE) {
            return teamList.isEmpty();
        }
        List<KmAgentContact> assigneeList = assigneeListSparseArray.get(assigneeTypeCode);
        return assigneeList == null || assigneeList.isEmpty();
    }

    public static void addAssigneeList(int assigneeTypeCode, List<KmAgentContact> assigneeList) {
        List<KmAgentContact> existingAssigneeList = assigneeListSparseArray.get(assigneeTypeCode);
        if (existingAssigneeList == null) {
            existingAssigneeList = new ArrayList<>();
        }
        if (existingAssigneeList.isEmpty()) {
            existingAssigneeList.addAll(assigneeList);
            assigneeListSparseArray.put(assigneeTypeCode, existingAssigneeList);
        }
    }

    public static void addTeamList(List<KmTeam> teams) {
        if (teamList.isEmpty()) {
            teamList.addAll(teams);
        }
    }

    public static void fetchAgentList(Context context, KmCallback callback) {
        new KmGetUserListTask(context,
                callback,
                new Short[]{AgentGetStatusTask.AgentDetail.RoleType.AGENT.getValue()
                        , AgentGetStatusTask.AgentDetail.RoleType.ADMIN.getValue()
                        , AgentGetStatusTask.AgentDetail.RoleType.SUPER_ADMIN.getValue()
                        , AgentGetStatusTask.AgentDetail.RoleType.OPERATOR.getValue()}
        ).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public static void fetchBotList(Context context, KmCallback callback) {
        new KmGetUserListTask(context,
                callback,
                new Short[]{AgentGetStatusTask.AgentDetail.RoleType.BOT.getValue()}
        ).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public static void fetchAssigneeList(Context context) {

        fetchAgentList(context, new KmCallback() {
            @Override
            public void onSuccess(Object message) {
                addAssigneeList(AGENT_LIST_CODE, (List<KmAgentContact>) message);
            }

            @Override
            public void onFailure(Object error) {

            }
        });

        fetchBotList(context, new KmCallback() {
            @Override
            public void onSuccess(Object message) {
                addAssigneeList(BOT_LIST_CODE, (List<KmAgentContact>) message);
            }

            @Override
            public void onFailure(Object error) {

            }
        });
    }

    public static void clearAll() {
        assigneeListSparseArray.clear();
        teamList.clear();
        KmTeamViewModel.TeamsCache.clearTeams();
        KmTagsViewModel.TagsCache.clearTags();
        KmAppSettingsViewModel.AppSettingsCache.clearAppSettings();
    }
}