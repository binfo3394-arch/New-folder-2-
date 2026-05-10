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

public class CallLogFragment extends Fragment {
    private RecyclerView recyclerView;
    private TextView tvEmpty;
    private CallLogAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_list, container, false);

        recyclerView = view.findViewById(R.id.recycler_view);
        tvEmpty = view.findViewById(R.id.tv_empty);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new CallLogAdapter();
        recyclerView.setAdapter(adapter);

        FirebaseManager.getInstance().listenCallLogs(logs -> {
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (logs != null && !logs.isEmpty()) {
                        adapter.setData(logs);
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

    static class CallLogAdapter extends RecyclerView.Adapter<CallLogAdapter.ViewHolder> {
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
            String number = (String) entry.get("number");
            String type = (String) entry.get("type");
            Object duration = entry.get("duration");
            Object date = entry.get("date");

            String line1 = (type != null ? type.toUpperCase() + " - " : "") +
                    (number != null ? number : "Unknown");
            String line2 = "";
            if (duration != null) {
                long secs = ((Number) duration).longValue();
                line2 = "Duration: " + (secs / 60) + "m " + (secs % 60) + "s";
            }
            if (date != null) {
                String dateStr = DateFormat.getDateTimeInstance()
                        .format(new Date(((Number) date).longValue()));
                line2 += (line2.isEmpty() ? "" : " | ") + dateStr;
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
