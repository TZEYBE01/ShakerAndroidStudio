package com.authorwjf;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class CommentsDataSource {

  // Database fields
  private SQLiteDatabase database;
  private MySQLiteHelper dbHelper;
  private String[] allColumns = { MySQLiteHelper.COLUMN_ID, MySQLiteHelper.COLUMN_DEVICE_NAME,
      MySQLiteHelper.COLUMN_COMMENT,  MySQLiteHelper.COLUMN_CLIENTIP, MySQLiteHelper.COLUMN_DATETIME};

  public CommentsDataSource(Context context) {
    dbHelper = new MySQLiteHelper(context);
  }

  public void open() throws SQLException {
    database = dbHelper.getWritableDatabase();
  }

  public void close() {
    dbHelper.close();
  }

  public Comment createComment(String device_name, String status , String client_ip,String datetime) {
    ContentValues values = new ContentValues();
    values.put(MySQLiteHelper.COLUMN_DEVICE_NAME, device_name);
    values.put(MySQLiteHelper.COLUMN_COMMENT, status);
    values.put(MySQLiteHelper.COLUMN_CLIENTIP, client_ip);
    values.put(MySQLiteHelper.COLUMN_DATETIME, datetime);
    long insertId = database.insert(MySQLiteHelper.TABLE_COMMENTS, null,
        values);
    Cursor cursor = database.query(MySQLiteHelper.TABLE_COMMENTS,
        allColumns, MySQLiteHelper.COLUMN_ID + " = " + insertId, null,
        null, null, null);
    cursor.moveToFirst();
    Comment newComment = cursorToComment(cursor);
    cursor.close();
    return newComment;
  }

  public void deleteComment(Comment comment) {
    long id = comment.getId();
    System.out.println("Comment deleted with id: " + id);
    database.delete(MySQLiteHelper.TABLE_COMMENTS, MySQLiteHelper.COLUMN_ID
        + " = " + id, null);
  }

  public List<Comment> getAllComments(int a) {
    List<Comment> comments = new ArrayList<Comment>();
   
    
    String temp_string = "";
    if(a < 1)//get all
    {
    	Log.i("sqlquery", "getting it all");
    }
    else
    {
    	temp_string = " limit " +Integer.toString(a) ;
    	Log.i("sqlquery", temp_string);
    }
    
    
	Cursor cursor = database.rawQuery("select * from "+MySQLiteHelper.TABLE_COMMENTS+" order by "+MySQLiteHelper.COLUMN_ID+" DESC"+temp_string+";", null);
	    	
    cursor.moveToFirst();
    while (!cursor.isAfterLast()) {
      Comment comment = cursorToComment(cursor);
      comments.add(comment);
      cursor.moveToNext();
    }
    // make sure to close the cursor
    cursor.close();
    return comments;
  }

  private Comment cursorToComment(Cursor cursor) {
    Comment comment = new Comment();
    comment.setId(cursor.getLong(0));
    comment.setDeviceName(cursor.getString(1));
    comment.setStatus(cursor.getString(2));
    comment.setClientIp(cursor.getString(3));
    comment.setDateTime(cursor.getString(4));
    return comment;
  }
} 