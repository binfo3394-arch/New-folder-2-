package com.monitor.parent.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.monitor.parent.R;
import com.monitor.parent.manager.FirebaseManager;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class MessagesFragment extends Fragment {
    private RecyclerView recyclerView;
    private TextView tvEmpty;
    private MessagesAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_list, container, false);

        recyclerView = view.findViewById(R.id.recycler_view);
        tvEmpty = view.findViewById(R.id.tv_empty);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new MessagesAdapter();
        recyclerView.setAdapter(adapter);

        FirebaseManager.getInstance().listenMessages(msgs -> {
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (msgs != null && !msgs.isEmpty()) {
                        adapter.setData(msgs);
                        recyclerView.setVisibility(View.VISIBLE);
                        tvEmpty.setVisibility(View.GONE);
                    } else {
                        recyclerView.setVisibility(View.GONE);
                        tvEmpty.setVisibility(View.VISIBLE);
                    }
                });
            }
        });

        return view;
    }

    static class MessagesAdapter extends RecyclerView.Adapter<MessagesAdapter.ViewHolder> {
        private List<Map<String, Object>> data = new ArrayList<>();

        void setData(List<Map<String, Object>> data) {
            this.data = data;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(android.R.layout.simple_list_item_2, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Map<String, Object> entry = data.get(position);
            String from = (String) entry.get("from");
            String body = (String) entry.get("body");
            String app = (String) entry.get("app");
            Object timestamp = entry.get("timestamp");

            String appName = app != null ? app : "";
            if (appName.contains("com.whatsapp")) appName = "WhatsApp";
            else if (appName.contains("com.facebook")) appName = "Facebook";
            else if (appName.contains("com.google.android.apps.messaging")) appName = "SMS";
            else if (appName.contains("com.android.mms")) appName = "SMS";
            else if (appName.contains("com.skype")) appName = "Skype";
            else if (appName.contains("com.viber")) appName = "Viber";
            else if (appName.contains("com.telegram")) appName = "Telegram";
            else if (appName.contains("com.tencent.mm")) appName = "WeChat";
            else if (appName.contains("com.snapchat")) appName = "Snapchat";
            else if (appName.contains("com.instagram")) appName = "Instagram";
            else if (appName.contains("com.twitter")) appName = "Twitter/X";
            else if (appName.contains("com.linkedin")) appName = "LinkedIn";
            else if (appName.contains("im.vector")) appName = "Element";
            else if (appName.contains("org.thoughtcrime")) appName = "Signal";
            else if (appName.length() > 15) appName = appName.substring(appName.lastIndexOf('.') + 1);

            String line1 = (from != null ? from + " " : "") + "[" + appName + "]";
            String line2 = body != null ? body : "";
            if (timestamp != null) {
                String dateStr = DateFormat.getDateTimeInstance()
                        .format(new Date(((Number) timestamp).longValue()));
                line2 += (line2.isEmpty() ? "" : " - ") + dateStr;
            }

            holder.text1.setText(line1);
            holder.text2.setText(line2);
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView text1, text2;
            ViewHolder(View v) {
                super(v);
                text1 = v.findViewById(android.R.id.text1);
                text2 = v.findViewById(android.R.id.text2);
            }
        }
    }
}
