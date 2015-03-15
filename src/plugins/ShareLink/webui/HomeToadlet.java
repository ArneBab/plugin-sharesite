package plugins.ShareLink.webui;

import java.io.IOException;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import plugins.ShareLink.Freesite;
import plugins.ShareLink.Plugin;
import plugins.ShareLink.common.MapToData;
import plugins.ShareLink.common.SmartMap;
import freenet.clients.http.InfoboxNode;
import freenet.clients.http.PageMaker;
import freenet.clients.http.PageNode;
import freenet.clients.http.Toadlet;
import freenet.clients.http.ToadletContext;
import freenet.clients.http.ToadletContextClosedException;
import freenet.keys.FreenetURI;
import freenet.l10n.BaseL10n;
import freenet.pluginmanager.PluginRespirator;
import freenet.support.HTMLNode;
import freenet.support.api.HTTPRequest;
import freenet.support.io.ArrayBucket;

public class HomeToadlet extends Toadlet {
	private PluginRespirator pr;
	private PageMaker pageMaker;
	private BaseL10n l10n;

	public HomeToadlet() {
		super(null);
		
		pr = Plugin.instance.pluginRespirator;
		pageMaker = pr.getPageMaker();
		l10n = Plugin.instance.l10n;
	}

	@Override
	public String path() {
		return "/ShareLink/";
	}

	// Gets called by the freenet node
	public void handleMethodGET(URI uri, HTTPRequest req, ToadletContext ctx) throws ToadletContextClosedException, IOException {
		// Special case when downloading backup
		if (uri.getPath().endsWith(".db")) {
			SmartMap map = Plugin.instance.database.getDatabaseAsMap();
			byte[] file = MapToData.mapToData(map);
			ArrayBucket bucket = new ArrayBucket(file);
			writeReply(ctx, 200, "application/octet-stream", "OK", bucket);
			return;
		}
		
		// If normal request
		String[] attrs;
		String[] vals;
		
		PageNode pageNode = pageMaker.getPageNode(
			l10n.getString("ShareLink.Menu.Name"), ctx);
		HTMLNode homeForm = pr.addFormChild(pageNode.content,
			"/ShareLink/", "homeForm");
		
		// Your freesites
		InfoboxNode listBox = pageMaker.getInfobox(
			l10n.getString("ShareLink.Home.Header"));
		homeForm.addChild(listBox.outer);
		
		if (Plugin.isPreRelease) {
			listBox.content.addChild("p",
				l10n.getString("ShareLink.Home.PreRelease"));
		}
		
		// New freesite, and restore recently deleted
		HTMLNode listNewP = listBox.content.addChild("p");
		attrs = new String[] { "type", "name", "value" };
		vals = new String[] { "submit", "newBtn",
			l10n.getString("ShareLink.Home.NewBtn") };
		listNewP.addChild("input", attrs, vals);
		
		int numDeleted = Plugin.instance.database.numDeleted();
		if (numDeleted > 0) {
			String undelstr =
				l10n.getString("ShareLink.Home.UndeleteBtn",
				"num", Integer.toString(numDeleted));
			attrs = new String[] { "type", "name", "value" };
			vals = new String[] { "submit", "undeleteBtn", undelstr };
			listNewP.addChild("input", attrs, vals);
		}
		
		// Freesite table
		if ( Plugin.instance.database.numFreesites() == 0 ) {
			// If no freesites yet
			listBox.content.addChild("p",
				l10n.getString("ShareLink.Home.Empty"));
		} else {
			// Handle all your freesites
			attrs = new String[] { "type", "name", "value" };
			vals = new String[] { "submit", "insertBtn",
				l10n.getString("ShareLink.Home.InsertBtn") };
			listBox.content.addChild("input", attrs, vals);

			attrs = new String[] { "type", "name", "value" };
			vals = new String[] { "submit", "deleteBtn",
				l10n.getString("ShareLink.Home.DeleteBtn") };
			listBox.content.addChild("input", attrs, vals);

			listBox.content.addChild("span", " " +
				l10n.getString("ShareLink.Home.SelectedLists"));
			
			HTMLNode listTable = listBox.content.addChild("table");
			HTMLNode listHeaders = listTable.addChild("tr");
			listHeaders.addChild("th", "");
			listHeaders.addChild("th",
				l10n.getString("ShareLink.Home.NameHeader"));
			listHeaders.addChild("th",
				l10n.getString("ShareLink.Home.StatusHeader"));
			listHeaders.addChild("th",
				l10n.getString("ShareLink.Home.KeyHeader"));
			
			for (Freesite c : Plugin.instance.database.getFreesites()) {
				HTMLNode listRow = listTable.addChild("tr");
				attrs = new String[] { "type", "name" };
				vals = new String[] { "checkbox", "list-" + c.getUniqueKey() };
				listRow.addChild("td").addChild("input", attrs, vals);
				listRow.addChild("td").addChild("a", "href", "/ShareLink/Edit/" + c.getUniqueKey(), c.getName());
				listRow.addChild("td", c.getStatus());
				if (c.getEdition() >= 0) {
					FreenetURI key = new FreenetURI(c.getRequestSSK() + "site-" + c.getEdition() + "/");
					key = key.uskForSSK();
					listRow.addChild("td").addChild("a", "href", "/" + key.toString(), key.toShortString());
				} else {
					listRow.addChild("td",
						l10n.getString("ShareLink.Home.NoKey"));
				}
			}
			
			l10n.addL10nSubstitution(listBox.content.addChild("p"),
					"ShareLink.Home.NoteAnnounce",
					new String[] {"link"},
					new HTMLNode[] {HTMLNode.link("/chat/")});
		}
		
		// Backup and restore box
		InfoboxNode backupBox = pageMaker.getInfobox(
			l10n.getString("ShareLink.Home.BackupHeader"));
		homeForm.addChild(backupBox.outer);
		
		Calendar calendar = Calendar.getInstance();
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		String todayDate = dateFormat.format(calendar.getTime());
		
		HTMLNode backupP = backupBox.content.addChild("p");
		backupP.addChild("a", "href",
			"/ShareLink/ShareLink-" + todayDate + ".db",
			l10n.getString("ShareLink.Home.BackupBtn"));
		
		backupBox.content.addChild("p",
			l10n.getString("ShareLink.Home.NoteRestore"));
		
		attrs = new String[] { "type", "name", "value" };
		vals = new String[] { "file", "restoreFile", "" };
		backupBox.content.addChild("input", attrs, vals);
		attrs = new String[] { "type", "name", "value" };
		vals = new String[] { "submit", "restoreBtn",
			l10n.getString("ShareLink.Home.RestoreBtn") };
		backupBox.content.addChild("input", attrs, vals);
		
		HTMLNode versionP = homeForm.addChild("p",
				"style", "text-align: right");
		versionP.addChild("span", "style", "font-size: 0.7em;",
				l10n.getString("ShareLink.Home.Version",
				"ver", Plugin.version));
		versionP.addChild("br");
		versionP.addChild("span", "style", "font-size: 0.7em;").addChild(
				"a", "href", "/" + Plugin.freesite,
				l10n.getString("ShareLink.Home.CheckSite"));
		
		// Done
		String ret = pageNode.outer.generate();
		writeHTMLReply(ctx, 200, "OK", ret);
	}
	
	// Gets called by the freenet node
	public void handleMethodPOST(URI uri, HTTPRequest req, ToadletContext ctx) throws ToadletContextClosedException, IOException {
		// Generic buttons
		if (req.isPartSet("newBtn")) {
			Plugin.instance.database.createFreesite();
			Plugin.instance.database.save();
			writeTemporaryRedirect(ctx, "Redirecting...", "/ShareLink/");
			return;
		}
		
		if (req.isPartSet("undeleteBtn")) {
			Plugin.instance.database.restoreDeleted();
			Plugin.instance.database.save();
			writeTemporaryRedirect(ctx, "Redirecting...", "/ShareLink/");
			return;
		}
		
		if (req.isPartSet("restoreBtn")) {
			byte[] got = req.getPartAsBytesFailsafe("restoreFile", 1000000000); // 1GB
			SmartMap map = MapToData.dataToMap(got);
			Plugin.instance.database.addFromMap(map);
			Plugin.instance.database.save();
			writeTemporaryRedirect(ctx, "Redirecting...", "/ShareLink/");
			return;
		}
		
		// Was insert or delete button clicked
		final int INSERT = 1;
		final int DELETE = 2;
		
		int button = 0;
		if (req.isPartSet("insertBtn")) button = INSERT;
		if (req.isPartSet("deleteBtn")) button = DELETE;
		
		// In that case perform for selected
		if (button != 0) {
			for (String part : req.getParts()) {
				if (!part.startsWith("list-")) continue;
				if (!req.getPartAsStringFailsafe(part, 5).equals("on")) continue;
				
				int id = -1;
				try {
					id = Integer.parseInt(part.substring(5));
				} catch (NumberFormatException e) {
				}
				Freesite c = Plugin.instance.database.getFreesiteWithUniqueKey(id);
				if (c == null) continue;  // TODO: pass id below better than collection
				
				if (button == INSERT) Plugin.instance.inserter.add(c);
				if (button == DELETE) Plugin.instance.database.delete(c);
			}
			
			if (button == DELETE) Plugin.instance.database.save();
		}
		
		writeTemporaryRedirect(ctx, "Redirecting...", "/ShareLink/");
	}
}
