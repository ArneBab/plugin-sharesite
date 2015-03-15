package plugins.ShareLink.webui;

import java.io.IOException;
import java.net.URI;

import plugins.ShareLink.Freesite;
import plugins.ShareLink.Plugin;
import freenet.clients.http.InfoboxNode;
import freenet.clients.http.PageMaker;
import freenet.clients.http.PageNode;
import freenet.clients.http.Toadlet;
import freenet.clients.http.ToadletContext;
import freenet.clients.http.ToadletContextClosedException;
import freenet.l10n.BaseL10n;
import freenet.pluginmanager.PluginRespirator;
import freenet.support.HTMLNode;
import freenet.support.api.HTTPRequest;

public class EditToadlet extends Toadlet {
	private PluginRespirator pr;
	private PageMaker pageMaker;
	private BaseL10n l10n;

	protected EditToadlet() {
		super(null);
		
		pr = Plugin.instance.pluginRespirator;
		pageMaker = pr.getPageMaker();
		l10n = Plugin.instance.l10n;
	}

	@Override
	public String path() {
		return "/ShareLink/Edit/";
	}

	// Gets called by the freenet node
	public void handleMethodGET(URI uri, HTTPRequest req, ToadletContext ctx) throws ToadletContextClosedException, IOException {
		String[] attrs;
		String[] vals;
		
		// Get freesite, or return to the menu
		String[] split = uri.getPath().split("/");
		int siteId = -1;
		try {
			siteId = Integer.parseInt(split[3]);
		} catch (Exception e) {
		}
		Freesite c = Plugin.instance.database.getFreesiteWithUniqueKey(siteId);
		if (c == null) {
			writeTemporaryRedirect(ctx, "Redirecting...", "/ShareLink/");
			return;
		}
		
		// Prepare
		PageNode pageNode = pageMaker.getPageNode(
			l10n.getString("ShareLink.Menu.Name"), ctx);
		HTMLNode editForm = pr.addFormChild(pageNode.content,
			"/ShareLink/Edit/" + siteId, "editForm");
		
		InfoboxNode editBox = pageMaker.getInfobox(
			l10n.getString("ShareLink.Edit.Header"));
		editForm.addChild(editBox.outer);
		
		// Menu link
		HTMLNode quickLinks = editBox.content.addChild("p");
		quickLinks.addChild("a", "href", "/ShareLink/",
			l10n.getString("ShareLink.Edit.MenuLink"));

		// Buttons above
		HTMLNode topBtnDiv = editBox.content.addChild("p");
		attrs = new String[] { "type", "name", "value" };
		vals = new String[] { "submit", "saveBtn",
			l10n.getString("ShareLink.Edit.SaveBtn") };
		topBtnDiv.addChild("input", attrs, vals);

		attrs = new String[] { "type", "name", "value" };
		vals = new String[] { "submit", "previewBtn",
			l10n.getString("ShareLink.Edit.PreviewBtn") };
		topBtnDiv.addChild("input", attrs, vals);
		
		// Edit boxes
		HTMLNode nameDiv = editBox.content.addChild("p");
		HTMLNode nameSpan = nameDiv.addChild("span");
		nameSpan.addChild("span",
			l10n.getString("ShareLink.Edit.Name"));
		nameSpan.addChild("br");
		attrs = new String[] { "type", "size", "name", "value" };
		vals = new String[] { "text", "80", "nameInput", c.getName() };
		nameDiv.addChild("input", attrs, vals);
		
		HTMLNode descDiv = editBox.content.addChild("p");
		HTMLNode descSpan = descDiv.addChild("span");
		descSpan.addChild("span",
			l10n.getString("ShareLink.Edit.Description"));
		descSpan.addChild("br");
		attrs = new String[] { "name", "rows", "cols", "style" };
		vals = new String[] { "descInput", "3", "80", "font-size: medium;" };
		descDiv.addChild("textarea", attrs, vals, c.getDescription());
		
		HTMLNode textDiv = editBox.content.addChild("p");
		HTMLNode textSpan = textDiv.addChild("span");
		textSpan.addChild("span",
			l10n.getString("ShareLink.Edit.Text"));
		textSpan.addChild("br");
		attrs = new String[] { "name", "rows", "cols", "style" };
		vals = new String[] { "textInput", "20", "80", "font-size: medium;" };
		textDiv.addChild("textarea", attrs, vals, c.getText());
		
		// Buttons below
		HTMLNode bottomBtnDiv = editBox.content.addChild("p");
		attrs = new String[] { "type", "name", "value" };
		vals = new String[] { "submit", "saveBtn",
			l10n.getString("ShareLink.Edit.SaveBtn") };
		bottomBtnDiv.addChild("input", attrs, vals);

		attrs = new String[] { "type", "name", "value" };
		vals = new String[] { "submit", "previewBtn",
			l10n.getString("ShareLink.Edit.PreviewBtn") };
		bottomBtnDiv.addChild("input", attrs, vals);
		
		// Syntax
		editBox.content.addChild("p",
			l10n.getString("ShareLink.Edit.TextSyntax"));
		
		// Insert Key
		InfoboxNode advBox = pageMaker.getInfobox(
			l10n.getString("ShareLink.Edit.Advanced"));
		editForm.addChild(advBox.outer);
		
		HTMLNode backup = advBox.content.addChild("p");
		backup.addChild("span",
			l10n.getString("ShareLink.Edit.InsertKey"));
		backup.addChild("br");
		backup.addChild("span", c.getInsertSSK());

		// Done
		String ret = pageNode.outer.generate();
		writeHTMLReply(ctx, 200, "OK", ret);
	}
	
	// Gets called by the freenet node
	public void handleMethodPOST(URI uri, HTTPRequest req, ToadletContext ctx) throws ToadletContextClosedException, IOException {
		// Get freesite, or display an error message
		String[] split = uri.getPath().split("/");
		int siteId = -1;
		try {
			siteId = Integer.parseInt(split[3]);
		} catch (Exception e) {
		}
		Freesite c = Plugin.instance.database.getFreesiteWithUniqueKey(siteId);
		if (c == null) {
			writeTemporaryRedirect(ctx, "Redirecting...", "/ShareLink/Error/");
			return;
		}
		
		// Perform action
		if (req.isPartSet("saveBtn") || req.isPartSet("previewBtn")) {
			String name = req.getPartAsStringFailsafe("nameInput", 1000).trim(); // 1kB
			String desc = req.getPartAsStringFailsafe("descInput", 10000).trim(); // 10kB
			String text = req.getPartAsStringFailsafe("textInput", 1000000000).trim(); // 1GB
			
			boolean changed = false;
			
			// Update name, text, etc
			if (name != null && name.length() > 0 && !name.equals(c.getName())) {
				c.setName(name);
				changed = true;
			}
			if (desc != null && desc.length() > 0 && !desc.equals(c.getDescription())) {
				c.setDescription(desc);
				changed = true;
			}
			if (text != null && text.length() > 0 && !text.equals(c.getText())) {
				c.setText(text);
				changed = true;
			}
			
			if (changed) {
				c.setL10nStatus("Status.Modified");
				Plugin.instance.database.save();
			}
		}
		
		if (req.isPartSet("previewBtn")) {
			writeTemporaryRedirect(ctx, "Redirecting...", "/ShareLink/Preview/" + siteId + "/index.html");
		}
		
		writeTemporaryRedirect(ctx, "Redirecting...", "/ShareLink/Edit/" + siteId);
	}
}
