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

            viewHolder.textViewIndex = (TextView) convertView.findViewById(R.id.textview_index);
            viewHolder.textViewTitle = (TextView) convertView.findViewById(R.id.textview_title);
            viewHolder.imageStatus = (ImageView) convertView.findViewById(R.id.ic_state);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        final Integer signIn_id = (int) signInList.get(position).get("signIn_id");
        final String signIn_name = signInList.get(position).get("signIn_name").toString();

        viewHolder.textViewIndex.setText("第" + signIn_id.toString() + "次");
        viewHolder.textViewTitle.setText(signIn_name);

        boolean enable = (boolean) signInList.get(position).get("signIn_enable");
        if (enable) {
            viewHolder.imageStatus.setImageResource(R.drawable.ic_bullet_point_active);
        } else {
            viewHolder.imageStatus.setImageResource(R.drawable.ic_bullet_point_inactive);
        }
        return convertView;
    }

    private class ViewHolder {
        TextView textViewIndex;
        TextView textViewTitle;
        ImageView imageStatus;
    }
}