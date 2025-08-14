package com.example.spotlight;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class ResultAdapter extends RecyclerView.Adapter<ResultAdapter.VH> {
  public interface OnOpen { void open(Uri u); }
  private final List<FileRecord> data = new ArrayList<>();
  private final OnOpen onOpen;

  public ResultAdapter(OnOpen onOpen){ this.onOpen = onOpen; }

  public void submit(List<FileRecord> list){
    data.clear(); if(list!=null) data.addAll(list); notifyDataSetChanged();
  }

  @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int vType){
    TextView tv = (TextView) LayoutInflater.from(p.getContext())
      .inflate(android.R.layout.simple_list_item_2, p, false);
    return new VH(tv);
  }

  @Override public void onBindViewHolder(@NonNull VH h, int i){
    FileRecord r = data.get(i);
    TextView tv = (TextView) h.itemView;
    tv.setText(r.name + "\n" + (r.relpath!=null? r.relpath: "") + " • " + r.mime);
    tv.setOnClickListener(v -> onOpen.open(Uri.parse(r.uri)));
  }

  @Override public int getItemCount(){ return data.size(); }

  static class VH extends RecyclerView.ViewHolder { VH(@NonNull TextView item){ super(item);} }
}
