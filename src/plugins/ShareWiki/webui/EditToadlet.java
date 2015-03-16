package plugins.ShareWiki.webui;

import java.io.IOException;
import java.net.URI;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.io.BufferedReader;
import java.io.StringReader;

import plugins.ShareWiki.Freesite;
import plugins.ShareWiki.Plugin;
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
		return "/ShareWiki/Edit/";
	}

	// Gets called by the freenet node
	public void handleMethodGET(URI uri, HTTPRequest req, ToadletContext ctx) throws ToadletContextClosedException, IOException {
		// Get freesite, or return to the menu
		String[] split = uri.getPath().split("/");
		int siteId = -1;
		try {
			siteId = Integer.parseInt(split[3]);
		} catch (Exception e) {
		}

		Freesite c = Plugin.instance.database.getFreesiteWithUniqueKey(siteId);
		if (c == null) {
			writeTemporaryRedirect(ctx, "Redirecting...", "/ShareWiki/");
			return;
		}

		PageNode pageNode = pageMaker.getPageNode(l10n.getString("ShareWiki.Menu.Name"), ctx);
		HTMLNode editForm = pr.addFormChild(pageNode.content,"/ShareWiki/Edit/" + siteId, "editForm");
		addNodes(editForm,c.getName(),c.getDescription(),c.getText(),c.getCSS(), c.getRequestSSK(),c.getInsertSSK());
		String ret = pageNode.outer.generate();
		writeHTMLReply(ctx, 200, "OK", ret);
	}


	private void addNodes( HTMLNode form, String name, String desc, String text, String css, String rkey, String ikey) {
		String[] attrs;
		String[] vals;


		InfoboxNode editBox = pageMaker.getInfobox(l10n.getString("ShareWiki.Edit.Header"));
		form.addChild(editBox.outer);

		// Menu link
		HTMLNode quickLinks = editBox.content.addChild("p");
		quickLinks.addChild("a", "href", "/ShareWiki/",	l10n.getString("ShareWiki.Edit.MenuLink"));

		// Buttons above
		HTMLNode topBtnDiv = editBox.content.addChild("p");
		attrs = new String[] { "type", "name", "value" };
		vals = new String[] { "submit", "saveBtn",l10n.getString("ShareWiki.Edit.SaveBtn") };
		topBtnDiv.addChild("input", attrs, vals);

		attrs = new String[] { "type", "name", "value" };
		vals = new String[] { "submit", "previewBtn",	l10n.getString("ShareWiki.Edit.PreviewBtn") };
		topBtnDiv.addChild("input", attrs, vals);

		attrs = new String[] { "type", "name", "value" };
		vals = new String[] { "submit", "preprocessBtn",l10n.getString("ShareWiki.Edit.PreprocessBtn") };
		topBtnDiv.addChild("input", attrs, vals);

		// Edit boxes
		HTMLNode nameDiv = editBox.content.addChild("p");
		HTMLNode nameSpan = nameDiv.addChild("span");
		nameSpan.addChild("span",l10n.getString("ShareWiki.Edit.Name"));
		nameSpan.addChild("br");
		attrs = new String[] { "type", "size", "name", "value" };
		vals = new String[] { "text", "80", "nameInput", name };
		nameDiv.addChild("input", attrs, vals);

		HTMLNode descDiv = editBox.content.addChild("p");
		HTMLNode descSpan = descDiv.addChild("span");
		descSpan.addChild("span",
		                  l10n.getString("ShareWiki.Edit.Description"));
		descSpan.addChild("br");
		attrs = new String[] { "name", "rows", "cols", "style" };
		vals = new String[] { "descInput", "3", "80", "font-size: medium;" };
		descDiv.addChild("textarea", attrs, vals, desc);

		HTMLNode textDiv = editBox.content.addChild("p");
		HTMLNode textSpan = textDiv.addChild("span");
		textSpan.addChild("span",l10n.getString("ShareWiki.Edit.Text"));
		textSpan.addChild("br");
		attrs = new String[] { "name", "rows", "cols", "style" };
		vals = new String[] { "textInput", "20", "80", "font-size: medium;" };
		textDiv.addChild("textarea", attrs, vals, text);

		HTMLNode cssDiv = editBox.content.addChild("p");
		HTMLNode cssSpan = cssDiv.addChild("span");
		cssSpan.addChild("span", l10n.getString("ShareWiki.Edit.CSS"));
		cssSpan.addChild("br");
		attrs = new String[] { "name", "rows", "cols", "style" };
		vals = new String[] { "cssInput", "15", "80", "font-size: medium;" };
		try {
			cssDiv.addChild("textarea", attrs, vals, css);
		} catch (Exception e) {
			cssDiv.addChild("textarea", attrs, vals, "");
		}

		// Buttons below
		HTMLNode bottomBtnDiv = editBox.content.addChild("p");
		attrs = new String[] { "type", "name", "value" };
		vals = new String[] { "submit", "saveBtn",
		                      l10n.getString("ShareWiki.Edit.SaveBtn")
		                    };
		bottomBtnDiv.addChild("input", attrs, vals);

		attrs = new String[] { "type", "name", "value" };
		vals = new String[] { "submit", "previewBtn",
		                      l10n.getString("ShareWiki.Edit.PreviewBtn")
		                    };
		bottomBtnDiv.addChild("input", attrs, vals);

		// Syntax
		editBox.content.addChild("p",l10n.getString("ShareWiki.Edit.TextSyntax"));

		// Insert Key
		InfoboxNode advBox = pageMaker.getInfobox(l10n.getString("ShareWiki.Edit.Advanced"));
		form.addChild(advBox.outer);

		HTMLNode backup = advBox.content.addChild("p");
		backup.addChild("span",l10n.getString("ShareWiki.Edit.InsertKey"));
		backup.addChild("br");

		attrs = new String[] { "type", "size", "name", "value" };
		vals = new String[] { "text",  "100", "insertKeyInput",  ikey };
		backup.addChild("input", attrs, vals);

		// Request Key
		backup.addChild("br");
		backup.addChild("span",l10n.getString("ShareWiki.Edit.RequestKey"));
		backup.addChild("br");

		attrs = new String[] { "type", "size", "name", "value" };
		vals = new String[] { "text",  "100", "requestKeyInput",  rkey };
		backup.addChild("input", attrs, vals);


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
			writeTemporaryRedirect(ctx, "Redirecting...", "/ShareWiki/Error/");
			return;
		}

		// Perform action
		if (req.isPartSet("saveBtn") || req.isPartSet("previewBtn")||req.isPartSet("preprocessBtn")) {
			String name = req.getPartAsStringFailsafe("nameInput", 1000).trim(); // 1kB
			String desc = req.getPartAsStringFailsafe("descInput", 10000).trim(); // 10kB
			String text = req.getPartAsStringFailsafe("textInput", 1000000000).trim(); // 1GB
			String css = req.getPartAsStringFailsafe("cssInput", 1000000000).trim(); // 1GB
			String ikey= req.getPartAsStringFailsafe("insertKeyInput", 1000).trim();
			String rkey= req.getPartAsStringFailsafe("requestKeyInput", 1000).trim();

			Plugin.instance.logger.putstr("POST values:");
			Plugin.instance.logger.putstr("   name=\""+name+"\"");
			Plugin.instance.logger.putstr("   desc=\""+desc+"\"");
			//Plugin.instance.logger.putstr("   text=\""+text+"\"");
			//Plugin.instance.logger.putstr("   css=\""+css+"\"");
			Plugin.instance.logger.putstr("   ikey=\""+ikey+"\"");
			Plugin.instance.logger.putstr("   rkey=\""+rkey+"\"");

			boolean changed = false;

			// new keys
			if(! ikey.equals(c.getInsertSSK()) || rkey.equals(c.getRequestSSK()) ) {
				c.setInsertSSK(ikey);
				c.setRequestSSK(rkey);
				changed= true;
			}

			// Update name, text, etc
			if (name != null && name.length() > 0 && !name.equals(c.getName())) {
				c.setName(name);
				changed = true;
			}
			if (desc != null && !desc.equals(c.getDescription())) {
				c.setDescription(desc);
				changed = true;
			}

			if (text != null && text.length() > 0 && !text.equals(c.getText())  ) {
				c.setText(text);
				Plugin.instance.logger.putstr("Source changed!");
				changed=true;
			}

			if (text != null && text.length() > 0 && req.isPartSet("preprocessBtn") )  {
				String pstr=preprocess(text);

				if(!c.getText().equals(pstr)) {
					c.setText(pstr);
					changed=true;
				}
			}

			if (css!= null && !css.equals(c.getCSS())) {
				c.setCSS(css);
				changed = true;
			}

			if (changed) {
				c.setL10nStatus("Status.Modified");
				Plugin.instance.database.save();
			}
		}

		if (req.isPartSet("previewBtn")) {
			writeTemporaryRedirect(ctx, "Redirecting...", "/ShareWiki/Preview/" + siteId + "/index.html");
		}


		writeTemporaryRedirect(ctx, "Redirecting...", "/ShareWiki/Edit/" + siteId);
	}

	private String preprocess(String text) {

		BufferedReader br=new BufferedReader(new StringReader(text));
		StringBuffer builder=new StringBuffer();

		try {

			int lcount=1;
			String line="";
			while((line=br.readLine())!= null) {

				Plugin.instance.logger.putstr(lcount +"\t"+line);

				// Highlight Frost sender
				Pattern frostheader= Pattern.compile("^[-]{5}(.*)[-]{5}(.*)[-]{5}$");
				Matcher m5=frostheader.matcher(line);
				line=m5.replaceAll("p(from). \u2013\u2013\u2013\u2013\u2013 $1 \u2013\u2013\u2013\u2013\u2013 $2 \u2013\u2013\u2013\u2013\u2013");

				// Only one key per line allowed. Everything until the end of line is seen as part of the Freenet key
				Pattern imgpat =  Pattern.compile("^(CHK|USK|SSK|KSK)@([^/]*)/(.*\\.)(jpg|JPG|jpeg|JPEG|gif|GIF|png|PNG)$");
				Pattern linkpat = Pattern.compile("^(CHK|USK|SSK|KSK)@([^/]*)/(.*)$");

				// Image
				Matcher m1=imgpat.matcher(line);
				line=m1.replaceAll("!(preview)/$1@$2/$3$4!");

				// Link
				Matcher m2=linkpat.matcher(line);
				line=m2.replaceAll("\"$3\":/$1@$2/$3" );

				/*
				 *  All keys should have been replaced by "<name>":/<key> or !(preview)/<key>! now
				 */

				// Remove %20 from file names
				Pattern linkdesc = Pattern.compile("(\".*\")(?=:/..K@)");
				Matcher m3=linkdesc.matcher(line);
				if(m3.find()) {
					String desc=m3.group();
					//Plugin.instance.logger.putstr("Unescaping String "+ desc);
					line=m3.replaceFirst(desc.replaceAll("%20"," "));
				}

				// Add %20 to key paths. Otherwise Textile will end the link at the first whitespace
				Pattern keypath = Pattern.compile("((?<=--8/)(.*))|((?<=CAAE/)(.*))");
				Matcher m4=keypath.matcher(line);
				if(m4.find()) {
					String esckey=m4.group();
					//Plugin.instance.logger.putstr("Escaping String "+ esckey);
					line=m4.replaceFirst(esckey.replaceAll("\\ ", "%20"));
				}



				line=line.replaceAll("%28","(");
				line=line.replaceAll("%29",")");
				line=line.replaceAll("%5b","[");
				line=line.replaceAll("%5d","]");
				line=line.replaceAll("http://[^/]+:8888/","");

				builder.append(line + "\n");

				//Plugin.instance.logger.putstr(lcount +"->\t"+line);
				lcount++;
			}
		} catch (IOException e) {
			Plugin.instance.logger.putstr(e.getMessage());
		}

		return builder.toString();
	}
}
