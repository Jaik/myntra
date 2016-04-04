package jai.com.memorygame;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

import java.util.List;

/**
 * Created by Jainendra Kumar on 4/1/2016.
 */
public class ImageGridViewAdapter extends BaseAdapter {

    List<String> mImageUrls;
    Context mContext;

    public ImageGridViewAdapter(List<String> imageUrls, Context context) {
        mImageUrls = imageUrls;
        mContext = context;
    }

    @Override
    public int getCount() {
        return mImageUrls.size();
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ImageView imageView;
        if (convertView == null) {
            LayoutInflater mInflater = (LayoutInflater) mContext
                    .getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
            convertView = mInflater.inflate(R.layout.item_image_grid, parent, false);
            imageView = (ImageView) convertView.findViewById(R.id.item_image);
            convertView.setTag(R.id.item_image, imageView);
        } else {
            imageView = (ImageView) convertView.getTag(R.id.item_image);
        }
        Picasso.with(mContext).load(mImageUrls.get(position)).into(imageView);


        return convertView;
    }
}
