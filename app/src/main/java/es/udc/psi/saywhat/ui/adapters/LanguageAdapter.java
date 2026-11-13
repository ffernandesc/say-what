package es.udc.psi.saywhat.ui.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import es.udc.psi.saywhat.R;

public class LanguageAdapter extends ArrayAdapter<String> {

    private final String[] languageNames;
    private final int[] flagIds;
    private final Context context;

    public LanguageAdapter(Context context, String[] languageNames, int[] flagIds) {
        super(context, R.layout.item_language, languageNames);
        this.context = context;
        this.languageNames = languageNames;
        this.flagIds = flagIds;
    }

    @Override
    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return getCustomView(position, convertView, parent);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return getCustomView(position, convertView, parent);
    }

    private View getCustomView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_language, parent, false);
        }

        TextView tvName = convertView.findViewById(R.id.tv_language_name);
        ImageView ivFlag = convertView.findViewById(R.id.iv_flag);

        tvName.setText(languageNames[position]);
        ivFlag.setImageResource(flagIds[position]);

        return convertView;
    }
}
