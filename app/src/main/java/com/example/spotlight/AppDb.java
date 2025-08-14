package com.example.spotlight;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = { FileRecord.class }, version = 1, exportSchema = false)
public abstract class AppDb extends RoomDatabase {
  public abstract FileDao fileDao();
  private static volatile AppDb INSTANCE;

  public static AppDb get(Context c){
    if(INSTANCE==null){
      synchronized (AppDb.class){
        if(INSTANCE==null){
          INSTANCE = Room.databaseBuilder(c.getApplicationContext(), AppDb.class, "spotlight.db")
                         .fallbackToDestructiveMigration()
                         .addCallback(new Callback(){})
                         .build();
        }
      }
    }
    return INSTANCE;
  }
}
