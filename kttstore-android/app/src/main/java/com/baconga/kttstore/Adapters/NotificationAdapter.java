package com.baconga.kttstore.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.baconga.kttstore.Models.Notification;
import com.baconga.kttstore.R;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {
    private List<Notification> notifications;
    private NotificationListener listener;
    private SimpleDateFormat dateFormat;

    public interface NotificationListener {
        void onNotificationClicked(Notification notification);
    }

    public NotificationAdapter(NotificationListener listener) {
        this.notifications = new ArrayList<>();
        this.listener = listener;
        this.dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", new Locale("vi", "VN"));
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        holder.bind(notifications.get(position));
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    public void setNotifications(List<Notification> notifications) {
        this.notifications = notifications;
        notifyDataSetChanged();
    }

    class NotificationViewHolder extends RecyclerView.ViewHolder {
        private TextView txtTitle;
        private TextView txtMessage;
        private TextView txtTime;
        private View unreadIndicator;

        NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            txtTitle = itemView.findViewById(R.id.txtTitle);
            txtMessage = itemView.findViewById(R.id.txtMessage);
            txtTime = itemView.findViewById(R.id.txtTime);
            unreadIndicator = itemView.findViewById(R.id.unreadIndicator);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    listener.onNotificationClicked(notifications.get(position));
                }
            });
        }

        void bind(Notification notification) {
            txtTitle.setText(notification.getTitle());
            txtMessage.setText(notification.getMessage());
            txtTime.setText(dateFormat.format(notification.getCreatedAt()));
            
            // Hiển thị chỉ báo chưa đọc
            unreadIndicator.setVisibility(notification.isRead() ? View.GONE : View.VISIBLE);
            
            // Set màu nền dựa trên trạng thái đã đọc
            itemView.setBackgroundResource(notification.isRead() ? 
                R.color.white : R.color.unread_notification_background);
        }
    }
} 