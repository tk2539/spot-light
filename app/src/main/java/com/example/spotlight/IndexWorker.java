package com.example.spotlight;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;

public class IndexWorker extends Worker {
  public IndexWorker(@NonNull Context ctx, @NonNull WorkerParameters p){ super(ctx, p); }

  @NonNull @Override
  public Result doWork() {
    Context c = getApplicationContext();
    AppDb db = AppDb.get(c);
    FileDao dao = db.fileDao();

    java.util.List<FileRecord> batch = new java.util.ArrayList<>(1024);
    try { dao.clear(); } catch(Exception ignored){}

    // MediaStore
    Uri files = MediaStore.Files.getContentUri("external");
    String[] proj = {
      MediaStore.Files.FileColumns._ID,
      MediaStore.Files.FileColumns.DISPLAY_NAME,
      MediaStore.Files.FileColumns.MIME_TYPE,
      MediaStore.Files.FileColumns.RELATIVE_PATH
    };
    try(Cursor cur = c.getContentResolver().query(files, proj, null, null, null)){
      if(cur!=null) while(cur.moveToNext()){
        long id = cur.getLong(0);
        String name = cur.getString(1);
        String mime = cur.getString(2);
        String rel  = cur.getString(3);
        Uri u = Uri.withAppendedPath(files, String.valueOf(id));

        FileRecord r = new FileRecord();
        r.name = name; r.mime = mime; r.relpath = rel; r.uri = u.toString();
        if (isTextLike(mime, name)) r.text = safeReadText(c, u, 64*1024);
        batch.add(r);
        if(batch.size()>=500){ try{ dao.insertAll(new java.util.ArrayList<>(batch)); }catch(Exception ignored){} batch.clear(); }
      }
    } catch(Exception ignored){}

    // SAF で保存したツリーを再帰走査
    for(Uri treeUri : SavedTrees.loadAll(c)){
      DocumentFile root = DocumentFile.fromTreeUri(c, treeUri);
      if(root!=null) walk(c, root, batch, dao);
    }
    if(!batch.isEmpty()) try{ dao.insertAll(batch); }catch(Exception ignored){}

    return Result.success();
  }

  private static boolean isTextLike(String mime, String name){
    if(mime!=null && mime.startsWith("text/")) return true;
    if(name==null) return false;
    String n = name.toLowerCase(Locale.US);
    return n.endsWith(".txt")||n.endsWith(".md")||n.endsWith(".json")||n.endsWith(".xml")||n.endsWith(".csv")||n.endsWith(".log");
  }

  private static String safeReadText(Context c, Uri u, int max){
    try(BufferedReader br = new BufferedReader(new InputStreamReader(c.getContentResolver().openInputStream(u)))){
      StringBuilder sb = new StringBuilder();
      String line; int left = max;
      while((line=br.readLine())!=null && left>0){
        int cut = Math.min(left, line.length());
        sb.append(line, 0, cut).append('\n'); left -= (cut+1);
      }
      return sb.toString();
    }catch(Exception e){ return null; }
  }

  private static void walk(Context c, DocumentFile dir, java.util.List<FileRecord> batch, FileDao dao){
    for(DocumentFile f : dir.listFiles()){
      if(f.isDirectory()){ walk(c, f, batch, dao); }
      else{
        FileRecord r = new FileRecord();
        r.name = f.getName();
        r.mime = f.getType();
        r.relpath = ""; // 任意
        r.uri = f.getUri().toString();
        if (isTextLike(r.mime, r.name)) r.text = safeReadText(c, f.getUri(), 64*1024);
        batch.add(r);
        if(batch.size()>=500){ try{ dao.insertAll(new java.util.ArrayList<>(batch)); }catch(Exception ignored){} batch.clear(); }
      }
    }
  }
}
