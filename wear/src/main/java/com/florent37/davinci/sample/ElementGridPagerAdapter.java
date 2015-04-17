package com.florent37.davinci.sample;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.wearable.view.CardFragment;
import android.support.wearable.view.FragmentGridPagerAdapter;

import com.github.florent37.davinci.DaVinci;
import com.github.florent37.davinci.transformation.BlurTransformation;

import java.util.ArrayList;
import java.util.List;

public class ElementGridPagerAdapter extends FragmentGridPagerAdapter {

    private List<Row> mRows;
    private List<Element> elementList;
    private Context mContext;

    public ElementGridPagerAdapter(Context context, List<Element> elements, FragmentManager fm) {
        super(fm);

        this.mContext = context;
        this.mRows = new ArrayList<Row>();
        this.elementList = new ArrayList<>(elements);

        for (Element element : elementList) {
            mRows.add(new Row(
                            CardFragment.create(element.getTitle(), element.getText())
                    )
            );
        }
    }

    @Override
    public Fragment getFragment(int row, int col) {
        Row adapterRow = mRows.get(row);
        return adapterRow.getColumn(col);
    }

    @Override
    public Drawable getBackgroundForRow(final int row) {
        //return DaVinci.with(mContext).load("/image/0").into(this, row);
        if(row%2==0) {
            return DaVinci.with(mContext).load(elementList.get(row).getUrl()).transform(new BlurTransformation()).into(this, row);
        }else
            return DaVinci.with(mContext).load(elementList.get(row).getUrl()).into(this, row);
    }

    @Override
    public Drawable getBackgroundForPage(final int row, final int column) {
        return BACKGROUND_NONE;
    }

    @Override
    public int getRowCount() {
        return mRows.size();
    }

    @Override
    public int getColumnCount(int rowNum) {
        return mRows.get(rowNum).getColumnCount();
    }

    private class Row {
        final List<Fragment> columns = new ArrayList<Fragment>();

        public Row(Fragment... fragments) {
            for (Fragment f : fragments) {
                add(f);
            }
        }

        public void add(Fragment f) {
            columns.add(f);
        }

        Fragment getColumn(int i) {
            return columns.get(i);
        }

        public int getColumnCount() {
            return columns.size();
        }
    }

}