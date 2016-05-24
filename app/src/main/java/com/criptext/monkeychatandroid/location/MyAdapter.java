package com.criptext.monkeychatandroid.location;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import com.criptext.monkeychatandroid.R;
import com.criptext.monkeykitui.recycler.MonkeyAdapter;
import com.criptext.monkeykitui.recycler.MonkeyItem;
import com.criptext.monkeykitui.recycler.holders.MonkeyHolder;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by daniel on 5/23/16.
 */
public class MyAdapter extends MonkeyAdapter{

    List<MonkeyItem> messagesList;

    public MyAdapter(@NotNull Context ctx, @NotNull ArrayList<MonkeyItem> list) {
        super(ctx, list);
        messagesList = list;
    }

    @Override
    public int getItemViewType(int position) {
        MonkeyItem monkeyItem = messagesList.get(position);
        if(monkeyItem.getMessageType() > getViewTypes())
            return monkeyItem.getMessageType();
        return super.getItemViewType(position);
    }

    @Nullable
    @Override
    public MonkeyHolder onCreateViewHolder(@Nullable ViewGroup p0, int viewtype) {
        View view;
        view = inflateView(false, R.layout.item_location_in, R.layout.item_location_out);
        if(viewtype > getViewTypes())
            return new LocationHolder(view);

        return super.onCreateViewHolder(p0, viewtype);
    }

    @Override
    public void onBindViewHolder(@NotNull MonkeyHolder holder, int position) {

        MonkeyItem monkeyItem = messagesList.get(position);
        if(monkeyItem.getMessageType() > getViewTypes()) {
            LocationHolder locationHolder = (LocationHolder)holder;
            bindCommonMonkeyHolder(position, monkeyItem, locationHolder);
        }
        else
            super.onBindViewHolder(holder, position);
    }
}
