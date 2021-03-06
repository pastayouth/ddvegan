package com.ponk.ddvegan.fragments;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.ponk.ddvegan.R;
import com.ponk.ddvegan.adapters.NewsAdapter;
import com.ponk.ddvegan.models.DataRepo;
import com.ponk.ddvegan.models.VeganNews;
import com.ponk.ddvegan.sync.NewsAndSpotUpdater;


public class NewsFragment extends Fragment {

    public static NewsAdapter newsAdapter;
    public static SwipeRefreshLayout newsRefresher;
    ListView newsList;
    public NewsFragment() {
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        newsAdapter = new NewsAdapter(getActivity(), DataRepo.veganNews);

    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        newsRefresher = (SwipeRefreshLayout) view.findViewById(R.id.news_refresher);
        newsRefresher.setColorSchemeResources(R.color.primary, R.color.primary_bright, R.color.primary_dark);
        newsRefresher.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                NewsAndSpotUpdater updater = new NewsAndSpotUpdater(getActivity().getApplicationContext(), 0);
                updater.execute();
            }
        });
        newsList = (ListView)view.findViewById(R.id.news_list);
        newsList.setAdapter(newsAdapter);
        newsList.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        newsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                VeganNews news = (VeganNews)newsList.getItemAtPosition(i);
                if (!DataRepo.veganSpots.containsKey(news.getSpotId()))
                    if (news.getNewsTypeInt() != 8)
                        Toast.makeText(getActivity(), news.getNewsContent(), Toast.LENGTH_SHORT).show();
                    else {
                        HelpFragment hf = new HelpFragment();
                        hf.show(getFragmentManager(), "HELPFRAG");
                    }
                else {
                    SpotDetailFragment detailFragment = SpotDetailFragment.create(news.getSpotId());
                    getParentFragment().getFragmentManager().beginTransaction()
                            .replace(R.id.content_frame, detailFragment, "SPOTDETAIL")
                            .addToBackStack(null)
                            .commit();
                }
            }
        });
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_news, container, false);
    }


}
