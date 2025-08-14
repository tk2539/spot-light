package com.example.spotlight;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface FileDao {
  @Insert void insertAll(java.util.List<FileRecord> list);
  @Query("DELETE FROM file_index") void clear();

  @Query("SELECT rowid, name, uri, mime, relpath, snippet(file_index, 4, '[', ']', '…', 8) AS text " +
         "FROM file_index WHERE file_index MATCH :q ORDER BY rank LIMIT 100")
  java.util.List<FileRecord> search(String q);
}
