package com.abhinavjhanwar.android.popularmovies.fragments;


import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.abhinavjhanwar.android.popularmovies.BuildConfig;
import com.abhinavjhanwar.android.popularmovies.adapters.DataAdapter;
import com.abhinavjhanwar.android.popularmovies.adapters.FavoriteAdapter;
import com.abhinavjhanwar.android.popularmovies.provider.FavoriteProvider;
import com.abhinavjhanwar.android.popularmovies.utils.GridAutofitLayoutManager;
import com.abhinavjhanwar.android.popularmovies.utils.MovieResponse;
import com.abhinavjhanwar.android.popularmovies.api.MovieAPI;
import com.abhinavjhanwar.android.popularmovies.utils.PosterDetail;
import com.abhinavjhanwar.android.popularmovies.R;

import java.util.ArrayList;
import java.util.Arrays;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


/**
 * A simple {@link Fragment} subclass.
 */
public class MovieFragment extends Fragment {

    @BindView(R.id.movie_rv)
    public RecyclerView recyclerView;

    private ArrayList<PosterDetail> data;
    private DataAdapter adapter;
    private FavoriteAdapter favoriteAdapter;
    private ProgressBar progressBar;
    private Unbinder unbinder;
    private AlertDialog levelDialog;

    private String sortOption;
    private final String topRated = "top_rated";
    private final String mostPopular = "popular";
    private final String favorite = "favorite";
    private final String sortEntry = "SORT";

    private Cursor cursor;

    private int savedInstance = 0;

    public MovieFragment() {
        // Required empty public constructor
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (data != null) {
            outState.putParcelableArrayList("movie-data", data);
        }
        super.onSaveInstanceState(outState);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        if (savedInstanceState != null && savedInstanceState.containsKey("movie-data")) {
            savedInstance = 1;
            data = savedInstanceState.getParcelableArrayList("movie-data");
        }

        //Get sort option, either most popular or top rated
        sortOption = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext()).getString(sortEntry, mostPopular);

        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        progressBar = (ProgressBar) getActivity().findViewById(R.id.progressBar);
        unbinder = ButterKnife.bind(this, rootView);
        initViews();
        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.moviefragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_sort) {
            //Get the sort dialog
            sortCreate();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void initViews() {
        //Card width for span count of gridlayoutmanager
        int CARD_WIDTH = 420;
        recyclerView.setHasFixedSize(true);
        //Custom class to dynamically determine span count
        RecyclerView.LayoutManager layoutManager = new GridAutofitLayoutManager(getActivity().getApplicationContext(), CARD_WIDTH);
        recyclerView.setLayoutManager(layoutManager);
        loadJSON();
    }

    public void loadJSON() {
        String baseURL = "http://api.themoviedb.org";

        // Use retrofit for loading URL and getting JSON
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseURL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        // Get JSON values, credits: https://www.learn2crack.com/2016/02/recyclerview-json-parsing.html
        Call<MovieResponse> call;
        MovieAPI movieAPI = retrofit.create(MovieAPI.class);

        if (sortOption.equals(topRated) || sortOption.equals(mostPopular)) {
            if (sortOption.equals(topRated)) {
                call = movieAPI.getMovies(topRated, BuildConfig.MOVIE_DB_API_KEY);
            } else {
                call = movieAPI.getMovies(mostPopular, BuildConfig.MOVIE_DB_API_KEY);
            }

            call.enqueue(new Callback<MovieResponse>() {
                @Override
                public void onResponse(Call<MovieResponse> call, Response<MovieResponse> response) {
                    MovieResponse movieResponse = response.body();
                    if (savedInstance == 0 || data == null) {
                        data = new ArrayList<>(Arrays.asList(movieResponse.getResults()));
                    }
                    if (getActivity() != null) {
                        // Build adapter based on json entries
                        adapter = new DataAdapter(getActivity().getApplicationContext(), data);
                        recyclerView.setVisibility(View.GONE);
                        if (adapter != null) {
                            //Hide progressbar and then show recyclerview after it's loaded
                            recyclerView.setAdapter(adapter);
                            progressBar.setVisibility(View.GONE);
                            setHasOptionsMenu(true);
                            recyclerView.setVisibility(View.VISIBLE);
                        }
                    }
                }

                @Override
                public void onFailure(Call<MovieResponse> call, Throwable t) {
                    Log.d("Error", t.getMessage());
                }
            });
        } else {
            if (getActivity() != null) {
                // Build adapter based on json entries
                retrieveShopsFromDatabase();
            }
        }
    }

    public void sortCreate() {
        final CharSequence[] items = {getResources().getString(R.string.action_sort_by_most_popular), getResources().getString(R.string.action_sort_by_top_rated), getResources().getString(R.string.action_sort_by_favorites)};
        int i = 0;

        // Creating and Building the Dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Sort by");
        if (sortOption.equals(topRated)) {
            i = 1;
        } else if(sortOption.equals(favorite)) {
            i = 2;
        }
        builder.setSingleChoiceItems(items, i, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                switch (item) {
                    case 0:
                        PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext()).edit().putString(sortEntry, mostPopular).apply();
                        break;
                    case 1:
                        PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext()).edit().putString(sortEntry, topRated).apply();
                        break;
                    case 2:
                        PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext()).edit().putString(sortEntry, favorite).apply();
                        break;
                }
                levelDialog.dismiss();
                new Handler().post(new Runnable() {
                    @Override
                    public void run() {
                        Intent intent = getActivity().getIntent();
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK
                                | Intent.FLAG_ACTIVITY_NO_ANIMATION);
                        getActivity().overridePendingTransition(0, 0);
                        getActivity().finish();

                        getActivity().overridePendingTransition(0, 0);
                        startActivity(intent);
                    }
                });
            }
        });
        levelDialog = builder.create();
        levelDialog.show();
    }

    private void retrieveShopsFromDatabase() {
        getActivity().getSupportLoaderManager()
                .initLoader(R.id.loader_favorite, null, new LoaderManager.LoaderCallbacks<Cursor>() {

                    @Override
                    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
                        return new FavoriteCursorLoader(getActivity());
                    }

                    @Override
                    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor dataCursor) {
                        if (!dataCursor.moveToFirst()) {
                            Log.d("demo", "Nothing in DB, returning early");
                        }

                        cursor = dataCursor;

                        favoriteAdapter = new FavoriteAdapter(getActivity().getApplicationContext(), cursor);
                        recyclerView.setVisibility(View.GONE);
                        if (dataCursor.getCount() != 0 && !dataCursor.isAfterLast()) {
                            //Hide progressbar and then show recyclerview after it's loaded
                            dataCursor.moveToFirst();
                            recyclerView.setAdapter(favoriteAdapter);
                            progressBar.setVisibility(View.GONE);
                            setHasOptionsMenu(true);
                            recyclerView.setVisibility(View.VISIBLE);
                        } else {
                            progressBar.setVisibility(View.GONE);
                            setHasOptionsMenu(true);
                            Log.d("FAV", "NONE");
                        }

                    }

                    @Override
                    public void onLoaderReset(Loader<Cursor> cursorLoader) {

                    }
                });
    }

    private static class FavoriteCursorLoader extends CursorLoader {

        public FavoriteCursorLoader(Context context) {
            super(context, FavoriteProvider.FAVORITES, null, null, null, null);
        }
    }

    // When binding a fragment in onCreateView, set the views to null in onDestroyView.
    // Butter Knife has an unbind method to do this automatically.
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }
}