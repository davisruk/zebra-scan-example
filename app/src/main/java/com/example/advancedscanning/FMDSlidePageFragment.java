package com.example.advancedscanning;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.advancedscanning.fmdslider.model.FMDSliderPageModel;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * A simple {@link Fragment} subclass.
 */
@AllArgsConstructor
@Data
public class FMDSlidePageFragment extends Fragment {

    private FMDSliderPageModel pageData;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fmd_slider_page, container, false);
        TextView paging = rootView.findViewById(R.id.fmdPagingInfo);
        TextView fmdContent = rootView.findViewById(R.id.fmdContent);
        paging.setText(pageData.getCurrentPage() + " of " + pageData.getTotalPages());
        fmdContent.setText(pageData.getLorumIpsum());
        return rootView;
    }
}
