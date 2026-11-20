package com.waterwise.app.ui.species;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.waterwise.app.R;
import com.waterwise.app.data.SpeciesCache;
import com.waterwise.app.data.SpeciesRepository;

import java.util.Collections;
import java.util.List;

/**
 * Search state (loading, results, message). Kept in a ViewModel so a
 * screen rotation doesn't restart the request.
 */
public class SpeciesSearchViewModel extends AndroidViewModel {

    private final SpeciesRepository repository;

    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<List<SpeciesCache>> results =
            new MutableLiveData<>(Collections.emptyList());
    /** String resource id of the message to show, or 0 for none. */
    private final MutableLiveData<Integer> message = new MutableLiveData<>(0);

    public SpeciesSearchViewModel(@NonNull Application application) {
        super(application);
        repository = new SpeciesRepository(application);
    }

    public LiveData<Boolean> isLoading() {
        return loading;
    }

    public LiveData<List<SpeciesCache>> getResults() {
        return results;
    }

    public LiveData<Integer> getMessage() {
        return message;
    }

    public void search(String query) {
        if (query.length() < 3) {
            message.setValue(R.string.search_min_chars);
            return;
        }
        loading.setValue(true);
        message.setValue(0);
        repository.search(query, new SpeciesRepository.Callback<SpeciesRepository.SearchResult>() {
            @Override
            public void onResult(SpeciesRepository.SearchResult result) {
                loading.setValue(false);
                results.setValue(result.species);
                if (result.species.isEmpty()) {
                    message.setValue(R.string.search_no_results);
                } else if (result.fromCache) {
                    message.setValue(R.string.search_offline_cache);
                }
            }

            @Override
            public void onError(SpeciesRepository.Error error) {
                loading.setValue(false);
                results.setValue(Collections.emptyList());
                message.setValue(messageFor(error));
            }
        });
    }

    static int messageFor(SpeciesRepository.Error error) {
        switch (error) {
            case OFFLINE:
                return R.string.error_offline;
            case QUOTA:
                return R.string.error_quota;
            case LOCKED:
                return R.string.error_locked;
            default:
                return R.string.error_unknown;
        }
    }
}
