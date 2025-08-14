package com.example.spotlight;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import java.util.List;

public class MainActivity extends AppCompatActivity {

  private EditText query;
  private TextView status;
  private ResultAdapter adapter;

  private final ActivityResultLauncher<Intent> pickTreeLauncher =
      registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
          Uri tree = result.getData().getData();
          if (tree != null) {
            getContentResolver().takePersistableUriPermission(
                tree, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            SavedTrees.add(this, tree);
            toast("Folder added: " + tree);
            reindex();
          }
        }
      });

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    query = findViewById(R.id.query);
    status = findViewById(R.id.status);

    RecyclerView list = findViewById(R.id.list);
    list.setLayoutManager(new LinearLayoutManager(this));
    adapter = new ResultAdapter(u -> {
      // open file via ACTION_VIEW. Some types might need chooser.
      Intent i = new Intent(Intent.ACTION_VIEW);
      i.setData(u);
      i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
      try { startActivity(i); } catch (Exception e) { toast("No app to open this file."); }
    });
    list.setAdapter(adapter);

    findViewById(R.id.btn_pick).setOnClickListener(v -> {
      Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
      i.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
      pickTreeLauncher.launch(i);
    });

    findViewById(R.id.btn_index).setOnClickListener(v -> reindex());

    findViewById(R.id.btn_search).setOnClickListener(v -> {
      String q = query.getText().toString().trim();
      doSearch(q);
    });

    requestMediaPermsIfNeeded();
    reindex(); // 初回
  }

  private void reindex() {
    status.setText("Indexing...");
    WorkManager.getInstance(this).enqueue(new OneTimeWorkRequest.Builder(IndexWorker.class).build());
    // 簡易に少し待ってから完了表示（実際はWorkの完了をObserveするのが◎）
    status.postDelayed(() -> status.setText("Indexed."), 1500);
  }

  private void doSearch(String q) {
    status.setText("Searching...");
    new Thread(() -> {
      List<FileRecord> hits = AppDb.get(this).fileDao().search(q + "*");
      runOnUiThread(() -> {
        adapter.submit(hits);
        status.setText("Hits: " + hits.size());
      });
    }).start();
  }

  private void requestMediaPermsIfNeeded(){
    if (Build.VERSION.SDK_INT >= 33) {
      String[] ps = { Manifest.permission.READ_MEDIA_IMAGES,
                      Manifest.permission.READ_MEDIA_VIDEO,
                      Manifest.permission.READ_MEDIA_AUDIO };
      boolean need=false;
      for(String p: ps){ if (ContextCompat.checkSelfPermission(this, p)!= PackageManager.PERMISSION_GRANTED) need=true; }
      if(need) requestPermissions(ps, 200);
    } else {
      if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED) {
        requestPermissions(new String[]{ Manifest.permission.READ_EXTERNAL_STORAGE }, 200);
      }
    }
  }

  private void toast(String s){ Toast.makeText(this, s, Toast.LENGTH_SHORT).show(); }
}
