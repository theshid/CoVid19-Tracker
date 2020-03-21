package com.shid.covid19.ui.dashboard;

import android.app.SearchManager;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.shid.covid19.Database.AppDatabase;
import com.shid.covid19.Model.Countries;
import com.shid.covid19.R;
import com.shid.covid19.Utils.AppExecutor;
import com.shid.covid19.ui.home.HomeViewModel;

import java.util.ArrayList;
import java.util.List;

import static androidx.constraintlayout.widget.Constraints.TAG;
import static com.shid.covid19.Utils.Constant.ERROR;
import static com.shid.covid19.Utils.Constant.FAILURE;
import static com.shid.covid19.Utils.Constant.SUCCESS;

public class DashboardFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {
    private RecyclerView recyclerView;
    private RecyclerView.LayoutManager layoutManager;
    public static List<Countries> countriesList = new ArrayList<>();
    public static List<Countries> searchList = new ArrayList<>();
    private CountryAdapter adapter;
    private String TAG = getClass().getSimpleName();
    private TextView title;
    private SwipeRefreshLayout swipeRefreshLayout;
    private RelativeLayout errorLayout;
    private ImageView errorImage;
    private TextView errorTitle, errorMessage;
    private Button btnRetry;
    private Toolbar toolbar;
    private SearchView searchView;
    Countries pays;
    AppDatabase database;

    private DashboardViewModel dashboardViewModel;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        dashboardViewModel =
                ViewModelProviders.of(this).get(DashboardViewModel.class);
        View root = inflater.inflate(R.layout.fragment_dashboard, container, false);

        setUI(root);
        database = AppDatabase.getInstance(getContext());
        /*
        dashboardViewModel.getCountries().observe(getViewLifecycleOwner(), new Observer<List<Countries>>() {
            @Override
            public void onChanged(List<Countries> countries) {
                countriesList = countries;
            }
        });

         */
        dashboardViewModel.getCountries().observe(getViewLifecycleOwner(), new Observer<List<Countries>>() {
            @Override
            public void onChanged(List<Countries> countries) {
                setUiBasedOnStatus(DashboardViewModel.status);
                if (countries != null) {
                    countriesList = countries;
                    adapter.setClips(countriesList);
                }
            }
        });
        /*
        dashboardViewModel.getListOfCountries().observe(getViewLifecycleOwner(), new Observer<List<Countries>>() {
            @Override
            public void onChanged(List<Countries> countries) {
                if (countries!= null){
                    //countriesList.clear();
                    setUiBasedOnStatus(DashboardViewModel.status);
                    countriesList = countries;
                    adapter.setClips(countriesList);
                }



            }
        });

         */
        MenuItem menuItem = toolbar.getMenu().findItem(R.id.action_search);
        menuItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                if (isNetworkAvailable(getContext())) {
                    restartApp();
                } else {
                    adapter.setClips(countriesList);
                    restartApp();
                }

                return true;
            }
        });

        searchView.setQueryHint("Recherche...");
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                AppExecutor appExecutor = AppExecutor.getInstance();
                Handler mHandler = new Handler();
                appExecutor.diskIO().execute(new Runnable() {
                    @Override
                    public void run() {

                        //searchList = countriesList;
                        searchList.clear();
                        String formatQuery = query.substring(0, 1).toUpperCase() + query.substring(1).toLowerCase();
                        pays = database.coronaDAO().loadCountry(formatQuery);

                        if (pays != null) {
                            searchList.add(pays);
                        }

                    }
                });
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (pays == null) {
                            Toast.makeText(getContext(), "mauvaise entrée", Toast.LENGTH_LONG).show();
                        } else {
                            adapter.setClips(searchList);
                        }
                    }
                }, 1000);


                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                //adapter.filter(newText);

                return true;
            }
        });
        //final TextView textView = root.findViewById(R.id.text_dashboard);
        //
        /*
        dashboardViewModel.getText().observe(getViewLifecycleOwner(), new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                textView.setText(s);
            }
        });

         */
        return root;
    }

    private void setUI(View root) {
        toolbar = root.findViewById(R.id.toolbar);
        //Setting up the Toolbar

        toolbar.inflateMenu(R.menu.menu_countries);
        toolbar.setTitle("Situation par pays");


        searchView = (SearchView) toolbar.getMenu().findItem(R.id.action_search).getActionView();
        swipeRefreshLayout = root.findViewById(R.id.swipe_refresh_layout);
        swipeRefreshLayout.setOnRefreshListener(this);
        swipeRefreshLayout.setColorSchemeResources(R.color.colorAccent);
        adapter = new CountryAdapter(getContext());


        title = root.findViewById(R.id.title_countries);
        recyclerView = root.findViewById(R.id.recyclerView);
        layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setNestedScrollingEnabled(false);
        recyclerView.setAdapter(adapter);

        errorLayout = root.findViewById(R.id.errorLayout);
        errorImage = root.findViewById(R.id.errorImage);
        errorTitle = root.findViewById(R.id.errorTitle);
        errorMessage = root.findViewById(R.id.errorMessage);
        btnRetry = root.findViewById(R.id.btnRetry);
    }

    @Override
    public void onRefresh() {
        restartApp();

    }

    private void restartApp() {
        getActivity().recreate();
        dashboardViewModel.getListOfCountries();
    }


    private void showErrorMessage(int imageView, String title, String message) {

        if (errorLayout.getVisibility() == View.GONE) {
            errorLayout.setVisibility(View.VISIBLE);
            hideLayout();
        }

        errorImage.setImageResource(imageView);
        errorTitle.setText(title);
        errorMessage.setText(message);

        btnRetry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                restartApp();
                errorLayout.setVisibility(View.GONE);
                revealLayout();

            }
        });

    }

    private void hideLayout() {
        recyclerView.setVisibility(View.GONE);
    }

    private void revealLayout() {
        recyclerView.setVisibility(View.VISIBLE);
    }


    private void setUiBasedOnStatus(String value) {
        switch (value) {
            case SUCCESS:

                swipeRefreshLayout.setRefreshing(false);
                DashboardViewModel.status = "";
                break;
            case FAILURE:
                swipeRefreshLayout.setRefreshing(false);
                DashboardViewModel.status = "";
                showErrorMessage(
                        R.drawable.oops,
                        "Oops..",
                        "Pas de réseau, Réessayer\n" +
                                DashboardViewModel.errorCode);
                break;
            case ERROR:
                Log.d(TAG, "error is : " + DashboardViewModel.errorCode);
                Log.d(TAG, "status is: " + DashboardViewModel.status);

                DashboardViewModel.status = "";
                swipeRefreshLayout.setRefreshing(false);
                showErrorMessage(
                        R.drawable.no_result,
                        "Pas de Résultat",
                        "Réessayer!\n" +
                                DashboardViewModel.errorCode);
                break;
            default:
                DashboardViewModel.status = "";
                break;

        }
    }

    public static boolean isNetworkAvailable(Context context) {
        if (context == null) return false;


        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager != null) {


            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
                if (capabilities != null) {
                    if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                        return true;
                    } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                        return true;
                    } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                        return true;
                    }
                }
            } else {

                try {
                    NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
                    if (activeNetworkInfo != null && activeNetworkInfo.isConnected()) {
                        Log.i("update_statut", "Network is available : true");
                        return true;
                    }
                } catch (Exception e) {
                    Log.i("update_statut", "" + e.getMessage());
                }
            }
        }
        Log.i("update_statut", "Network is available : FALSE ");
        return false;
    }
}
