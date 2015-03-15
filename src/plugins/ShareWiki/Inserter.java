package plugins.ShareWiki;

import java.util.HashMap;
import java.util.LinkedList;

import freenet.client.HighLevelSimpleClient;
import freenet.keys.FreenetURI;
import freenet.node.RequestStarter;
import freenet.support.io.ArrayBucket;

/**
 * Inserts new or the first editions of freesites.
 * The ShareWiki freesites is inserted as
 * normal freesites, and can be accessed as such.
 */
public class Inserter extends Thread {
	private boolean running;
	private LinkedList<Freesite> queuedInserts;
	
	public Inserter() {
		running = true;
		queuedInserts = new LinkedList<Freesite>();
	}
	
	@Override
	public void run() {
		Freesite nextToInsert;
		
		while (true) {
			// Quick do everything that requires locking
			synchronized (this) {
				nextToInsert = null;
				
				if (!queuedInserts.isEmpty()) {
					nextToInsert = queuedInserts.removeFirst();
				}
			}
			
			// Now safely perform the blocking inserts
			if (nextToInsert != null) {
				performInsert(nextToInsert);
			}
			
			// If nothing to do, let the thread sleep until notify
			try {
				synchronized (this) {
					if (!running) break;
					if (!queuedInserts.isEmpty()) continue;

					wait();
				}
			} catch (InterruptedException e) {
			}
		}
	}

	public synchronized void terminate() {
		running = false;
		notify();
		// TODO: how do we terminate a running insert? it is blocking.
	}
	
	private synchronized boolean isTerminated() {
		return running == false;
	}
	
	public synchronized void add(Freesite freesite) {
		freesite.setL10nStatus("Status.Queue", freesite.getRealStatus());
		queuedInserts.addLast(freesite);
		notify();
	}
	
	private void performInsert(Freesite c) {
		c.setL10nStatus("Status.Inserting", "Status.InsertFailed");
		Plugin.instance.database.save();
		
		try {
			// Make buckets with the freesite
			ArrayBucket html = new ArrayBucket(c.getHTML().getBytes("UTF-8"));
			ArrayBucket css = new ArrayBucket(c.getCSS().getBytes("UTF-8"));
			ArrayBucket text = new ArrayBucket(c.getText().getBytes("UTF-8"));
			
			HashMap<String,Object> bucketsByName = new HashMap<String,Object>();
			bucketsByName.put("index.html", html);
			bucketsByName.put("style.css", css);
			bucketsByName.put("source.txt", text);
			
			// Insert the new edition
			String suffix = "site-" + (c.getEdition() + 1);
			FreenetURI insertURI = new FreenetURI(c.getInsertSSK() + suffix);
			insertURI = insertURI.uskForSSK();
			
			HighLevelSimpleClient simpleClient = Plugin.instance.pluginRespirator.getHLSimpleClient();
			FreenetURI resultURI = simpleClient.insertManifest(insertURI, bucketsByName, "index.html", RequestStarter.INTERACTIVE_PRIORITY_CLASS);
			if (isTerminated()) return;
			
			// Mark as successfully updated
			c.setL10nStatus("Status.Current");
			c.setEdition(resultURI.getEdition());
			Plugin.instance.database.save();
		} catch (Exception e) {
			// Mark as update failed
			c.setL10nStatus("Status.InsertFailed");
		}
	}
}
