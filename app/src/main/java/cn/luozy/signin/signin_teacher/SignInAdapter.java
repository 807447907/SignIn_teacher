package cn.luozy.signin.signin_teacher;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;

public class SignInAdapter extends BaseAdapter {

    private Context mContext;
    private ArrayList<HashMap<String, Object>> signInList = new ArrayList<>();

    SignInAdapter (Context context, ArrayList<HashMap<String, Object>> arrayList) {
        mContext = context;
        signInList = arrayList;
    }

    @Override
    public int getCount() {
        return signInList.size();
    }

    @Override///////////////
    public Object getItem(int position) {
        return null;
    }

    @Override//////////////////
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;

        if (convertView == null) {
            convertView = View.inflate(mContext, R.layout.sign_in_list_item, null);
            viewHolder = new ViewHolder();

            viewHolder.textViewName = (TextView) convertView.findViewById(R.id.text_view_name);
            viewHolder.textViewCourse = (TextView) convertView.findViewById(R.id.text_view_course);
            viewHolder.textViewTime = (TextView) convertView.findViewById(R.id.text_view_time);
            viewHolder.textViewNumRecords = (TextView) convertView.findViewById(R.id.text_view_num_records);
            viewHolder.imageStatus = (ImageView) convertView.findViewById(R.id.ic_state);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        final int sign_in_id = (int) signInList.get(position).get("sign_in_id");
        final int sign_in_num_records = (int) signInList.get(position).get("sign_in_num_records");
        final String sign_in_name = signInList.get(position).get("sign_in_name").toString();
        final String sign_in_course = signInList.get(position).get("sign_in_course").toString();
        final String sign_in_time = signInList.get(position).get("sign_in_time").toString();
        boolean enable = (boolean) signInList.get(position).get("sign_in_state");

        viewHolder.textViewCourse.setText(sign_in_course);
        viewHolder.textViewName.setText(sign_in_name);
        viewHolder.textViewTime.setText(sign_in_time);
        viewHolder.textViewNumRecords.setText(""+sign_in_num_records);

        if (enable) {
            viewHolder.imageStatus.setImageResource(R.drawable.ic_bullet_point_active);
        } else {
            viewHolder.imageStatus.setImageResource(R.drawable.ic_bullet_point_inactive);
        }
        return convertView;
    }

    private class ViewHolder {
        TextView textViewName;
        TextView textViewCourse;
        TextView textViewTime;
        TextView textViewNumRecords;
        ImageView imageStatus;
    }
}