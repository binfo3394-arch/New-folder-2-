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
            if (!isAdded() || getActivity() == null) return;
            getActivity().runOnUiThread(() -> {
                if (!isAdded()) return;
                if (recyclerView == null || tvEmpty == null) return;
                if (msgs != null && !msgs.isEmpty()) {
                    adapter.setData(msgs);
                    recyclerView.setVisibility(View.VISIBLE);
                    tvEmpty.setVisibility(View.GONE);
                } else {
                    recyclerView.setVisibility(View.GONE);
                    tvEmpty.setVisibility(View.VISIBLE);
                }
            });
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

            String appName = formatAppName(app);

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

    static String formatAppName(String app) {
        if (app == null) return "";
        if (app.contains("com.whatsapp")) return "WhatsApp";
        if (app.contains("com.facebook")) return "Facebook";
        if (app.contains("com.google.android.apps.messaging")) return "SMS";
        if (app.contains("com.android.mms")) return "SMS";
        if (app.contains("com.skype")) return "Skype";
        if (app.contains("com.viber")) return "Viber";
        if (app.contains("com.telegram")) return "Telegram";
        if (app.contains("com.tencent.mm")) return "WeChat";
        if (app.contains("com.snapchat")) return "Snapchat";
        if (app.contains("com.instagram")) return "Instagram";
        if (app.contains("com.twitter")) return "Twitter/X";
        if (app.contains("com.linkedin")) return "LinkedIn";
        if (app.contains("im.vector")) return "Element";
        if (app.contains("org.thoughtcrime")) return "Signal";
        if (app.length() > 15) return app.substring(app.lastIndexOf('.') + 1);
        return app;
    }
}
