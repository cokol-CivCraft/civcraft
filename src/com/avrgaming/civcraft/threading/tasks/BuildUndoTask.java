package com.avrgaming.civcraft.threading.tasks;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.LinkedList;
import java.util.Queue;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.template.Template;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.threading.sync.SyncBuildUpdateTask;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.ItemManager;
import com.avrgaming.civcraft.util.KeyValue;
import com.avrgaming.civcraft.util.SimpleBlock;
import com.avrgaming.civcraft.util.TimeTools;
 
public class BuildUndoTask implements Runnable {
        private String undoTemplatePath;
        private String undoTemplateId;
        private String undoTownName;
        private BlockCoord cornerBlock;
        private int savedBlockCount;
 
        private final int MAX_BLOCKS_PER_TICK = 300;
        private final int DELAY_SPEED = 100;
        private final static String undoProgressFolder = "templates/undo/inprogress";
 
        private int builtBlockCount = 0;
       
	    public BuildUndoTask(String undoTemplatePath, String undoTemplateId, BlockCoord cornerBlock, int savedBlockCount, String townName) {
	        this.undoTemplatePath = undoTemplatePath;
	        this.undoTemplateId = undoTemplateId;
	        this.cornerBlock = cornerBlock;
	        this.savedBlockCount = savedBlockCount;
	        this.undoTownName = townName;
	    }
	       
	    public BuildUndoTask(String input) {
	        /* Deseralize data from string and resume build task. */
	    KeyValue kv = new KeyValue();
	    kv.deserialize(input);
	   
	    this.undoTemplatePath = kv.getString("templatePath");
	    this.undoTemplateId = kv.getString("templateId");
	    this.savedBlockCount = kv.getInt("savedBlockCount");
	    this.cornerBlock = new BlockCoord(kv.getString("cornerBlock"));
	    }
	   
	    @Override
	    public void run() {
	        Template undo_tpl = new Template();
	        Queue<SimpleBlock> syncBlockQueue = new LinkedList<SimpleBlock>();
	        try {
	    		undo_tpl.initUndoTemplate(this.cornerBlock.toString(), this.undoTownName);
	            /*
	             * Save progress before we start so undo is properly resumed if the
	             * server is rebooted before the next save.
	             */
	            saveProgress();
	
	            /* For 1.0 Templates, SimpleBlocks are inside 3D array called 'blocks' */
	            for (int y = undo_tpl.size_y-1; y >= 0; y--) {
	            	for (int x = 0; x < undo_tpl.size_x; x++) {
	            		for (int z = 0; z < undo_tpl.size_z; z++) {
	            			undo_tpl.blocks[x][y][z].x = x;
	                        undo_tpl.blocks[x][y][z].y = y;
	                        undo_tpl.blocks[x][y][z].z = z;
	                        build(syncBlockQueue, undo_tpl.blocks[x][y][z]);
	            		}
	            	}
	            }
	            /* Build last remaining blocks. */
	            SyncBuildUpdateTask.queueSimpleBlock(syncBlockQueue);
	            syncBlockQueue.clear();
	            undo_tpl.deleteUndoTemplate(undoTemplateId, this.undoTownName);
	            this.deleteProgress();
	
	        } catch (IOException e) {
	                e.printStackTrace();
	        } catch (InterruptedException e) {
	                e.printStackTrace();
	                return;
	        } catch (CivException e) {
				e.printStackTrace();
			}
	    }
	   
	    public void build(Queue<SimpleBlock> syncBlockQueue, SimpleBlock sb) throws InterruptedException {
	        builtBlockCount++;
	        if (builtBlockCount < savedBlockCount || CivSettings.restrictedUndoBlocks.contains(ItemManager.getMaterial(sb.getType()))) {
	                /* We're resuming an undo task after reboot and this block is already built.
	                 * Or This block is restricted */
	                return;
	        }
	       
	        /* Convert relative template x,y,z to real x,y,z in world. */
	        sb.x += cornerBlock.getX();
	        sb.y += cornerBlock.getY();
	        sb.z += cornerBlock.getZ();
	        sb.worldname = cornerBlock.getWorldname();
	        /* Add block to sync queue, will be built on next sync tick. */
	        syncBlockQueue.add(sb);
	
	        if (builtBlockCount > MAX_BLOCKS_PER_TICK) {
	                saveProgress();
	                delay();
	                SyncBuildUpdateTask.queueSimpleBlock(syncBlockQueue);
	                syncBlockQueue.clear();
	                builtBlockCount = 0;
	        }
	        return;
	    }
	   
	    private void delay() throws InterruptedException {
	        /* Wait for a period of time. */
	        int timeleft = DELAY_SPEED;
	        while (timeleft > 0) {
	                int min = Math.min(10000, timeleft);
	                Thread.sleep(min);
	                timeleft -= 10000;
	        }
	    }
	           
	    private String getSaveFileName() {
	    	return "inprogress_"+this.undoTemplateId;
	    }
	   
	    private String getSaveFilePath() {
	        return undoProgressFolder+"/"+this.getSaveFileName();
	    }
	   
	    private void saveProgress() {
	
	        /*
	         * Convert everything we need to resume a task into a single string and store it in the session db.
	         */
	        KeyValue kv = new KeyValue();
	        kv.setString("templateId", this.undoTemplateId);
	        kv.setString("templatePath", this.undoTemplatePath);
	        kv.setInt("savedBlockCount", this.builtBlockCount);
	        kv.setString("cornerBlock", this.cornerBlock.toString());
	       
	        File f = new File(undoProgressFolder);
	        if (!f.exists()) {
	                f.mkdirs();
	        }
	       
	        try {
	
	                PrintWriter writer = new PrintWriter(this.getSaveFilePath(), "UTF-8");
	                writer.println(kv.serialize());
	                writer.close();
	
	        } catch (FileNotFoundException e) {
	                e.printStackTrace();
	        } catch (UnsupportedEncodingException e) {
	                e.printStackTrace();
	        }
	    }
	   
	    private void deleteProgress() {
	        File f = new File(this.getSaveFilePath());
	        if (f.exists()) {
	                f.delete();
	        }
	    }
	   
	    public static void resumeUndoTasks() {
	        File folder = new File(undoProgressFolder);
	        if (!folder.exists()) {
	                return;
	        }
	       
	        for (File f : folder.listFiles()) {
	                try {
	                        BufferedReader br = new BufferedReader(new FileReader(f));
	                        String input = br.readLine();
	                        BuildUndoTask task = new BuildUndoTask(input);
	                       
	                        /* Resume undo task after 30 seconds. Give server some time to start. */
	                        TaskMaster.asyncTask(task, TimeTools.toTicks(30));
	                        br.close();
	                } catch (IOException e) {
	                        /* Should never happen but print error and continue. */
	                        e.printStackTrace();
	                }
	        }
	    }
}