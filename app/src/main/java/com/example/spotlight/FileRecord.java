package com.example.spotlight;

import androidx.room.Entity;
import androidx.room.Fts4;
import androidx.room.PrimaryKey;

@Fts4
@Entity(tableName = "file_index")
public class FileRecord {
  @PrimaryKey(autoGenerate = true) public int rowid;
  public String name;
  public String uri;
  public String mime;
  public String relpath;
  public String text;
}
