package com.example.droidfileexplorer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Date;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.app.ActivityManager;
import android.app.ListActivity;
import android.app.ActivityManager.MemoryInfo;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

public class FileSelector extends ListActivity {

	private File currentDir;
    private FileListAdapter adapter;
    
    private final String TAG = "FileExplorer";
    private final String ROOT_DIR = "/";
    
    private final String TAG_FOLDER_ICON = "folder_icon";
    private final String TAG_FILE_ICON = "file_icon";
    private final String TAG_DIR_BACK = "directory_back";
    private final String TAG_PARENT_DIRECTORY = "Parent Directory";
    private final String TAG_EXT_MEM = "external_memory";
    private final String TAG_INT_MEM = "internal_memory";
    private final String TAG_PROCESS_ICON = "process_icon";
    private final String TAG_RAM_ICON = "ram_icon";
    private final String TAG_PROC_ICON = "processor_icon";
    
    private static int dirDepthLevel = 0;    
    private String extSDPath = "";
    
    private final int BYTES_IN_KILO = 1024;
    
    private int clickedItem = 0;
    private final int RAM_CLICKED = 1;
    private final int PROC_CLICKED = 2;
    
    public static PackageManager pk = null;
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		currentDir = new File(ROOT_DIR);
		Log.d(TAG, "Current directory selected : " + currentDir.isDirectory());
		
		pk = getPackageManager();
		
		//populate the list by directory contents
		//Set the depth 0
		dirDepthLevel = 0;
		
        fill(currentDir);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	private String getMountedPaths(){
	    String sdcardPath = "";
	    Runtime runtime = Runtime.getRuntime();
	    Process proc = null;
	    try {
	        proc = runtime.exec("mount");
	    } catch (IOException e1) {
	        e1.printStackTrace();
	    }
	    InputStream is = proc.getInputStream();
	    InputStreamReader isr = new InputStreamReader(is);
	    String line;
	    BufferedReader br = new BufferedReader(isr);
	    try {
	        while ((line = br.readLine()) != null) {
	            if (line.contains("secure")) continue;
	            if (line.contains("asec")) continue;

	            if (line.contains("fat") && line.contains("rw,") && !line.contains(",discard")) {//external card
	            	Log.d(TAG, "Adding line1 : " + line);
	                String columns[] = line.split(" ");
	                if (columns != null && columns.length > 1) {
	                    sdcardPath=columns[1];
	                }
	            } else if (line.contains("fuse")) {//internal storage
	                String columns[] = line.split(" ");
	                if (columns != null && columns.length > 1) {
	                }
	            }
	        }
	    } catch (IOException e) {
	        e.printStackTrace();
	    }
	    return sdcardPath;
	}
	
	private double getAvailMemory(Context context) {
		ActivityManager am = (ActivityManager) context
				.getSystemService(Context.ACTIVITY_SERVICE);
		MemoryInfo mi = new MemoryInfo();
		am.getMemoryInfo(mi);
		return (double)mi.availMem / (1024 * 1024);
	}

	private double getTotalMemory(Context context) {
		String str1 = "/proc/meminfo";
		String str2;
		String[] arrayOfString;
		long initial_memory = 0;

		try {
			FileReader localFileReader = new FileReader(str1);
			BufferedReader localBufferedReader = new BufferedReader(localFileReader, 8192);
			str2 = localBufferedReader.readLine();

			arrayOfString = str2.split("\\s+");
			initial_memory = Integer.valueOf(arrayOfString[1]).intValue() * BYTES_IN_KILO;
			localBufferedReader.close();

		} catch (IOException e) {
		}
		return (double)initial_memory / (BYTES_IN_KILO * BYTES_IN_KILO);
	}
	
	private void fill(File f)
    {
		if(clickedItem == RAM_CLICKED)
        {
	       	 List<Item>dir = new ArrayList<Item>();
	       	 this.setTitle("Memory Details");
	       	 
	       	 dir.add(0,new Item("Go Back","","",ROOT_DIR, TAG_DIR_BACK));
	       	 double val = getTotalMemory(getApplicationContext());
	       	 dir.add(1,new Item("Total Memory","" + new DecimalFormat("##.##").format(val) + " MB","","",TAG_RAM_ICON));
	       	 val = getAvailMemory(getApplicationContext());
	       	 dir.add(2,new Item("Available Memory","" + new DecimalFormat("##.##").format(val) + " MB","","",TAG_RAM_ICON));
	       	 
	       	 adapter = new FileListAdapter(FileSelector.this, R.layout.folder_list, dir);
		     this.setListAdapter(adapter);
        }
        else if(clickedItem == PROC_CLICKED)
        {
        	List<Item>dir = new ArrayList<Item>();
	       	this.setTitle("Running Processes");
	       	
	       	dir.add(0,new Item("Go Back","","",ROOT_DIR, TAG_DIR_BACK));
	       	
	       	final ActivityManager activityManger = (ActivityManager) this.getSystemService(ACTIVITY_SERVICE);
    		List<ActivityManager.RunningAppProcessInfo> list = activityManger.getRunningAppProcesses();
    		StringBuilder sb = new StringBuilder("");
    		if (list != null) {
    			for (int i = 0; i < list.size(); i++) {
    				ActivityManager.RunningAppProcessInfo apinfo = list.get(i);    				
    				dir.add(i+1,new Item(apinfo.processName, "PID: " + apinfo.pid, "Importance: " + apinfo.importance,"",TAG_PROCESS_ICON));
    			}
    		}
    		   		
    		adapter = new FileListAdapter(FileSelector.this, R.layout.folder_list, dir);
		    this.setListAdapter(adapter);
        }
		else if(dirDepthLevel <= 0)
        {
			 dirDepthLevel = 0;
			
	       	 Log.d(TAG, "Adding home menu ------ ");
	       	 
	       	 String extPath = getMountedPaths();
	       	 List<Item>dir = new ArrayList<Item>();
	       	 this.setTitle("HOME");
	       	 
	       	 //add internal memory list item.        	 
	       	 File[]fbuf = f.listFiles();  
	       	         	 
	       	 Date lastModDate = new Date(f.lastModified());
	            DateFormat formater = DateFormat.getDateTimeInstance();
	            String date_modify = formater.format(lastModDate);
	            
	       	 int buf = 0;
	            if(fbuf != null){
	                    buf = fbuf.length;
	            }
	            else buf = 0;
	            String num_item = String.valueOf(buf);
	            if(buf == 0) num_item = num_item + " item";
	            else num_item = num_item + " items";
	            
	       	 dir.add(new Item("Internal Storage", num_item, date_modify, ROOT_DIR, TAG_INT_MEM));
	       	 
	       	 
	       	 //if string is empty means no external SD card available.
	       	 if(!extPath.equals(""))
	       	 {
	       		 extSDPath = extPath;
	       		 
	       		 File ff = new File(extPath);
	       		 File[]fbuf1 = ff.listFiles();     
	           	 
	           	 lastModDate = new Date(ff.lastModified());
	                formater = DateFormat.getDateTimeInstance();
	                date_modify = formater.format(lastModDate);
	                
	           	 	buf = 0;
	                if(fbuf1 != null){
	                        buf = fbuf1.length;
	                }
	                else buf = 0;
	                num_item = String.valueOf(buf);
	                if(buf == 0) num_item = num_item + " item";
	                else num_item = num_item + " items";
	                
	           	 dir.add(new Item("External Storage", num_item, date_modify, extPath, TAG_EXT_MEM));
	       	 }
	       	 
  	         //adding memory item
        	 dir.add(new Item("Memory Details", "", "", ROOT_DIR, TAG_RAM_ICON));
        	 
        	 //adding processor item
        	 dir.add(new Item("Running Processes", "", "", ROOT_DIR, TAG_PROC_ICON));
        	 
	       	 adapter = new FileListAdapter(FileSelector.this, R.layout.folder_list, dir);
	         this.setListAdapter(adapter);
        }
        else
        { 
	         File[]dirs = f.listFiles();
	         
	         //Set current directory path 
	         String curDirName = f.getPath();
	         this.setTitle(curDirName);
	         
	         List<Item>dir = new ArrayList<Item>();
	         List<Item>fls = new ArrayList<Item>();
	         try{
	                 for(File ff: dirs)
	                 {
	                	//Get last modified date
	                    Date lastModDate = new Date(ff.lastModified());
	                    DateFormat formater = DateFormat.getDateTimeInstance();
	                    String date_modify = formater.format(lastModDate);
	                    
	                    //if this item id a directory then display corresponding icon.
	                    //Also show the directory contents again
	                    if(ff.isDirectory()){
	                        File[] fbuf = ff.listFiles();
	                        int buf = 0;
	                        if(fbuf != null)
	                            buf = fbuf.length;                        
	                        else 
	                        	buf = 0;
	                        
	                        //get number of items in under this directory                        
	                        String num_item = String.valueOf(buf);
	                        
	                        if(buf == 0) 
	                        	num_item = num_item + " item";
	                        else 
	                        	num_item = num_item + " items";
	                       
	                        dir.add(new Item(ff.getName(), num_item, date_modify, ff.getAbsolutePath(), TAG_FOLDER_ICON));
	                    }
	                    else
	                    {
	                        fls.add(new Item(ff.getName(), ff.length() + " Bytes", date_modify, ff.getAbsolutePath(), TAG_FILE_ICON));
	                    }
	                 }
	         }catch(Exception e)
	         {
	        	 Log.d(TAG, "Exception while showing list." + e.getMessage());
	         }
	         
	         //Sort the list alphabetically
	         Collections.sort(dir);
	         Collections.sort(fls);
	         dir.addAll(fls);
	
	         if(f.getName().trim().equals(""))
	         {
	        	 dir.add(0,new Item("..",TAG_PARENT_DIRECTORY,"",ROOT_DIR,TAG_DIR_BACK));
	         }
	         else if(f.getName().trim().equals(extSDPath))
	         {
	        	 dir.add(0,new Item("..",TAG_PARENT_DIRECTORY,"",extSDPath,TAG_DIR_BACK));
	         }
	         //If not showing home screen then add parent directory icon
	         else
	         {
	        	 dir.add(0,new Item("..",TAG_PARENT_DIRECTORY,"",f.getParent(),TAG_DIR_BACK));
	         }
	         
	         //Add list adapter for the folder list.
	         adapter = new FileListAdapter(FileSelector.this, R.layout.folder_list,dir);
	         this.setListAdapter(adapter);
        }
    }
	
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
            super.onListItemClick(l, v, position, id);
            
            //Get the current selected item object
            Item o = adapter.getItem(position);
            Log.d(TAG, "Clicked  " + o.getImage());
            
            //If this is a folder item then show the contents of this
            //folder again
            if(o.getImage().equalsIgnoreCase(TAG_INT_MEM)||o.getImage().equalsIgnoreCase(TAG_EXT_MEM)){
            	dirDepthLevel++;
            	currentDir = new File(o.getPath());
                fill(currentDir);
            }            
            else if(o.getImage().equalsIgnoreCase(TAG_FOLDER_ICON)||o.getImage().equalsIgnoreCase(TAG_DIR_BACK)){            	
            	//If going upward means decreasing depth 
            	if(o.getImage().equalsIgnoreCase(TAG_DIR_BACK))
            	{
            		if(clickedItem == 0)
            			dirDepthLevel--;
            		else
            			clickedItem = 0;
            	}
            	else
            		dirDepthLevel++;
            	
                currentDir = new File(o.getPath());
                fill(currentDir);
            }
            else if(o.getImage().equalsIgnoreCase(TAG_RAM_ICON))
            {
            	clickedItem = RAM_CLICKED;
            	currentDir = new File(ROOT_DIR);
                fill(currentDir);
            }
            else if(o.getImage().equalsIgnoreCase(TAG_PROC_ICON))
            {
            	clickedItem = PROC_CLICKED;
            	currentDir = new File(ROOT_DIR);
                fill(currentDir);
            }
            //Else this if file item so perform the action accordingly
            else if(!o.getImage().equalsIgnoreCase(TAG_PROCESS_ICON))
            {
                onFileClick(o);
            }
    }
    
    private void onFileClick(Item o)
    {
        Toast.makeText(this, "File Clicked: " + o.getName(), Toast.LENGTH_SHORT).show();

        //Perform the action.
    }
    
}
