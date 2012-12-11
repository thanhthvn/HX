package cnc.hx.adapters;

import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;
import cnc.hx.MainActivity;
import cnc.hx.R;
import cnc.hx.VideoConversationActivity;
import cnc.hx.entities.Device;
import cnc.hx.utils.Constants;

public class DeviceAdapter extends BaseAdapter {

	MainActivity context;
	
	List<Device> data;
	
	public DeviceAdapter(MainActivity context, List<Device> data) {
		this.context = context;
		this.data = data;
	}
	
	public int getCount() {
		return data.size();
	}

	public Device getItem(int position) {
		return data.get(position);
	}

	public long getItemId(int position) {
		return 0;
	}
	
	public View getView(int position, View convertView, ViewGroup parent) {
		if (convertView == null) {
			convertView = LayoutInflater.from(context).inflate(R.layout.device_item, null);
		}
		
		final Device device = data.get(position);
		
		TextView tvIp = (TextView) convertView.findViewById(R.id.tvIp);
		Button btConnect = (Button) convertView.findViewById(R.id.btConnect);
		tvIp.setText(device.getIp());
		btConnect.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				String ip = device.getIp();
				context.setClientIp(ip);
				context.startConnect();
			}
		});
		return convertView;
	}
	
	@Override
	public boolean isEmpty() {
		return data.isEmpty();
	}

}
