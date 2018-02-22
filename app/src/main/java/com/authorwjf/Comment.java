package com.authorwjf;
public class Comment {
  private long id;
  private String status;
  private String datetime;
  private String devicename;
  private String clientip;
  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String comment) {
	    this.status = comment;
  }
  
  public void setDeviceName(String comment) {
    this.devicename = comment;
  }
  
  public String getDeviceName() {
	    return devicename;
  }
  
  public String getClientIp() {
	    return clientip;
  }

  public void setClientIp(String comment) {
	    this.clientip = comment;
  }
  
  public void setDateTime(String comment) {
	    this.datetime = comment;
  }
	  
  public String getDateTime() {
		    return datetime;
  }
  
  

  // Will be used by the ArrayAdapter in the ListView
  @Override
  public String toString() {
    return status;
  }
}