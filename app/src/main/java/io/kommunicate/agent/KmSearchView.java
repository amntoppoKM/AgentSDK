package io.kommunicate.agent;

import android.app.Activity;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.applozic.mobicommons.commons.core.utils.Utils;

public class KmSearchView implements TextWatcher {

    private Activity activity;
    private EditText searchEditText;
    private ImageButton searchBackButton;
    private ImageButton searchCancelButton;
    private KmSearchInterface searchInterface;
    private ConstraintLayout kmSearchViewLayout;
    public static final int SEARCH_STATUS_CODE = -1;

    public KmSearchView(Activity activity, KmSearchInterface searchInterface) {
        this.activity = activity;
        this.searchInterface = searchInterface;
        initViews();
    }

    private void initViews() {
        searchEditText = activity.findViewById(R.id.searchEditText);
        searchBackButton = activity.findViewById(R.id.searchBackButton);
        searchCancelButton = activity.findViewById(R.id.searchCancelButton);
        kmSearchViewLayout = activity.findViewById(R.id.kmSearchViewLayout);

        searchCancelButton.setOnClickListener(v -> {
            if (searchInterface != null) {
                searchEditText.setText("");
                searchInterface.onSearchTextCleared();
            }
        });

        searchBackButton.setOnClickListener(v -> {
            if (searchInterface != null) {
                Utils.toggleSoftKeyBoard(activity, true);
                clearSearchData();
                searchInterface.onSearchBackPressed();
            }
        });

        searchEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                if (searchInterface != null) {
                    searchInterface.onSearchSubmit(searchEditText.getText().toString().trim());
                }
            }
            return false;
        });

        showSearchView();
    }

    public void hideSearchView() {
        if (kmSearchViewLayout != null) {
            kmSearchViewLayout.setVisibility(View.GONE);
            clearSearchData();
        }
    }

    public void toggleClearButtonVisibility(boolean hide) {
        if (searchCancelButton != null) {
            searchCancelButton.setVisibility(hide ? View.GONE : View.VISIBLE);
        }
    }

    public void showSearchView() {
        if (kmSearchViewLayout != null) {
            searchEditText.addTextChangedListener(this);
            searchEditText.callOnClick();
            searchEditText.requestFocus();
        }
    }

    public boolean isSearchViewVisible() {
        return kmSearchViewLayout != null && kmSearchViewLayout.getVisibility() == View.VISIBLE;
    }

    private void clearSearchData() {
        if (searchEditText != null) {
            searchEditText.removeTextChangedListener(this);
            searchEditText.setText("");
        }
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

    }

    @Override
    public void afterTextChanged(Editable s) {
        if (searchInterface != null) {
            searchInterface.onSearchTextChange(!TextUtils.isEmpty(s) ? s.toString().trim() : "");
        }
    }

    public interface KmSearchInterface {
        void onSearchTextCleared();

        void onSearchBackPressed();

        void onSearchSubmit(String text);

        void onSearchTextChange(String text);
    }
}
