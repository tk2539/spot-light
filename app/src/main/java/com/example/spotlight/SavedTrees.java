package com.example.spotlight;

import android.content.Context;
import android.net.Uri;
import java.util.*;

public class SavedTrees {
  private static final String PREF="trees", KEY="uris";
  public static void add(Context c, Uri u){
    Set<String> s = new HashSet<>(loadRaw(c)); s.add(u.toString());
    c.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putStringSet(KEY, s).apply();
  }
  public static java.util.List<Uri> loadAll(Context c){
    java.util.List<Uri> out=new java.util.ArrayList<>(); for(String s: loadRaw(c)) out.add(Uri.parse(s)); return out;
  }
  private static Set<String> loadRaw(Context c){
    return c.getSharedPreferences(PREF, Context.MODE_PRIVATE).getStringSet(KEY, java.util.Collections.emptySet());
  }
}
