package ch.ethz.inf.vs.android.siwehrli.a3;

import java.util.ArrayList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class MyArrayAdapter extends ArrayAdapter<TextMessage> {
	private final Context context;
	ArrayList<TextMessage> messages = new ArrayList<TextMessage>();

	public MyArrayAdapter(Context context, ArrayList<TextMessage> messages) {
		super(context, R.layout.message_layout, messages);
		this.context = context;
		this.messages = messages;
	}

	static class ViewHolder {
		public TextView textViewMessage;
		public TextView textViewTime;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// Performance optimization: reuse views outside of visible area if
		// possible
		View messageView = convertView;
		if (messageView == null) {
			LayoutInflater inflater = (LayoutInflater) context
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			messageView = inflater.inflate(R.layout.message_layout, parent,
					false);
			// Performance optimization: enables faster access to view via
			// static class
			ViewHolder viewHolder = new ViewHolder();
			viewHolder.textViewMessage = (TextView) messageView
					.findViewById(R.id.textViewMessage);
			viewHolder.textViewTime = (TextView) messageView
					.findViewById(R.id.textViewTime);
			messageView.setTag(viewHolder);
		}

		ViewHolder holder = (ViewHolder) messageView.getTag();

		holder.textViewMessage.setText(this.messages.get(position)
				.getFormatedMessage());
		holder.textViewTime.setText(this.messages.get(position)
				.getFormatedTime());
		if (this.messages.get(position).isDelayedPublished())
			messageView.setBackgroundColor(0xFFFFFF66);
		if (this.messages.get(position).isErrorMessage())
			messageView.setBackgroundColor(0xFFFF0000);

		return messageView;
	}
}
