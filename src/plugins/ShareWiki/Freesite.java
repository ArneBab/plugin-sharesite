package plugins.ShareWiki;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.regex.Matcher;

import plugins.ShareWiki.common.SmartMap;
import freenet.client.HighLevelSimpleClient;
import freenet.keys.FreenetURI;

/**
 * This is the freesite. Besides containing the actual configuration
 * and content, the freesite know which inserted edition we are on
 * and can generate all data that we need for inserting it.
 * 
 * This class implements Comparable. The natural order for
 * this class is sorted by uniqueKey, that is the first
 * created is first, and most recent created last.
 * This is the order they appear in in the front end.
 */
public class Freesite implements Comparable<Freesite> {
	private String name;
	private String description;
	private String text;
	
	private String requestSSK;
	private String insertSSK;
	private long edition;

	private String l10nStatus;
	private String l10nStatusChangeToOnRestart;
	
	private int uniqueKey;
	
	public Freesite(int uniqueKey) {
		this.uniqueKey = uniqueKey;
		
		name = "ShareWiki freesite";
		description = "Write a short description shown in search results here.";
		text = "";
		
		HighLevelSimpleClient simpleClient = Plugin.instance.pluginRespirator.getHLSimpleClient();
		FreenetURI[] keys = simpleClient.generateKeyPair("");
		requestSSK = keys[1].toString();
		insertSSK = keys[0].toString();
		edition = -1;  // first inserted edition is 0
		
		l10nStatus = "Status.New";
		l10nStatusChangeToOnRestart = "Status.New";
	}
	
	public synchronized String getName() {
		return name;
	}
	
	public synchronized void setName(String name) {
		this.name = name;
	}
	
	public synchronized String getDescription() {
		return description;
	}
	
	public synchronized void setDescription(String description) {
		this.description = description;
	}
	
	public synchronized String getText() {
		return text;
	}
	
	public synchronized void setText(String text) {
		this.text = text;
	}
	
	public int getUniqueKey() {
		return uniqueKey;
	}
	
	public synchronized String getRequestSSK() {
		return requestSSK;
	}
	
	public synchronized String getInsertSSK() {
		return insertSSK;
	}
	
	public synchronized long getEdition() {
		return edition;
	}
	
	public synchronized void setEdition(long edition) {
		this.edition = edition;
	}
	
	public synchronized String getHTML() throws Exception {
		// Prepare content
		String content = textToHTML(text);
		
		// Generate URIs we need
		FreenetURI requestURI = new FreenetURI(requestSSK + "site-" + (edition + 1) + "/");
		FreenetURI uskURI = requestURI.uskForSSK();
		String nextEdition = uskURI.toString();
		
		long updateEdition = (edition >= 0) ? -(edition + 1) : -1;
		String checkUpdates = uskURI.setSuggestedEdition(updateEdition).toString();
		
		// We need todays date too
		Calendar calendar = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"));
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		String insertDate = dateFormat.format(calendar.getTime());
		
		// Pass through the HTML file, substituting in the real content
		String template = "/plugins/ShareWiki/html/index.html";
		
		InputStream is = Plugin.class.getClassLoader().getResourceAsStream(template);
		if (is == null) throw new Exception("Couldn't load \"" + template + "\"");
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		
		StringBuilder sb = new StringBuilder();
		
		while (true) {
			String line = reader.readLine();
			if (line == null) break;
			
			line = line.replaceAll("\\$NAME\\$", Matcher.quoteReplacement(name));
			line = line.replaceAll("\\$DESCRIPTION\\$", Matcher.quoteReplacement(description));
			line = line.replaceAll("\\$CONTENT\\$", Matcher.quoteReplacement(content));
			line = line.replaceAll("\\$INSERT_URI\\$", Matcher.quoteReplacement(nextEdition));
			line = line.replaceAll("\\$CHECK_UPDATES_URI\\$", Matcher.quoteReplacement(checkUpdates));
			line = line.replaceAll("\\$INSERT_DATE\\$", Matcher.quoteReplacement(insertDate));
			
			sb.append(line + "\n");
		}
		
		reader.close();
		return sb.toString();
	}
	
	public synchronized String getCSS() throws Exception {
		String template = "/plugins/ShareWiki/html/style.css";
		
		InputStream is = Plugin.class.getClassLoader().getResourceAsStream(template);
		if (is == null) throw new Exception("Couldn't load \"" + template + "\"");
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		
		StringBuilder sb = new StringBuilder();
		
		while (true) {
			String line = reader.readLine();
			if (line == null) break;
			sb.append(line + "\n");
		}
		
		reader.close();
		return sb.toString();
	}
	
	public synchronized String getStatus() {
		return Plugin.instance.l10n.getString("ShareWiki." + l10nStatus);
	}

	public synchronized String getRealStatus() {
		return l10nStatus;
	}
	
	public void setL10nStatus(String status) {
		setL10nStatus(status, status);
	}
	
	public synchronized void setL10nStatus(String status, String changeToOnRestart) {
		this.l10nStatus = status;
		this.l10nStatusChangeToOnRestart = changeToOnRestart;
	}

	synchronized void save(SmartMap map) {
		String prefix = "collection-" + uniqueKey + "/";
		
		map.putstr(prefix + "name", name);
		map.putstr(prefix + "description", description);
		map.putstr(prefix + "text", text);
		
		map.putstr(prefix + "requestSSK", requestSSK);
		map.putstr(prefix + "insertSSK", insertSSK);
		map.putlong(prefix + "edition", edition);
		
		map.putstr(prefix + "l10nStatus", l10nStatusChangeToOnRestart);
	}
	
	void load(SmartMap map, int uniqueKeyInMap) {
		String prefix = "collection-" + uniqueKeyInMap + "/";
		
		name = map.getstr(prefix + "name", name);
		description = map.getstr(prefix + "description", description);
		text = map.getstr(prefix + "text", text);
		
		requestSSK = map.getstr(prefix + "requestSSK", requestSSK);
		insertSSK = map.getstr(prefix + "insertSSK", insertSSK);
		edition = map.getlong(prefix + "edition", edition);
		
		l10nStatus = map.getstr(prefix + "l10nStatus", l10nStatus);
		l10nStatusChangeToOnRestart = l10nStatus;
	}
	
	private static String textToHTML(String text) {
		String[] lines = text.split("\n");
		StringBuilder sb = new StringBuilder();
		boolean paragraph = false;
		boolean textlinkbox = false;
		boolean imglinkbox = false;
		
		for (int i = 0; i < lines.length; i++) {
			String line = lines[i].trim();
			line = line.replaceAll("<", "&lt;");
			line = line.replaceAll(">", "&gt;");
			
			if (line.equals("")) {
				if (textlinkbox || imglinkbox) sb.append("</div>\n");
				textlinkbox = false;
				imglinkbox = false;
				
				if (paragraph) sb.append("</p>\n\n");
				paragraph = false;
				continue;
			}
			
			if (line.startsWith("***")) {
				if (textlinkbox || imglinkbox) sb.append("</div>\n");
				textlinkbox = false;
				imglinkbox = false;
				
				if (paragraph) sb.append("</p>\n\n");
				paragraph = false;
				
				String header = line.substring(3).trim();
				sb.append("<h2>" + header + "</h2>\n\n");
				continue;
			}
			
			if (!paragraph) sb.append("<p>\n");
			paragraph = true;
			
			if (line.startsWith("CHK@") || line.startsWith("SSK@") ||
					line.startsWith("USK@") || line.startsWith("KSK@") ) {
				String name;

				if (line.startsWith("KSK@") || line.indexOf("/") < 15) {
					name = line;
				} else {
					name = line.substring(0, 10) + "...";
					name = name + line.substring(line.indexOf("/"));
				}

				String lcase = name.toLowerCase();

				if (lcase.endsWith(".jpg") || lcase.endsWith(".png") ||
						lcase.endsWith(".gif") || lcase.endsWith(".jpeg") ||
						lcase.endsWith(".bmp")) {
					if (textlinkbox) sb.append("</div>\n");
					if (!imglinkbox) sb.append("<div class=\"imglinkbox\">\n");
					textlinkbox = false;
					imglinkbox = true;

					sb.append("<div class=\"imglink\">");
					sb.append("<a href=\"/" + line + "\" title=\"" + name + "\">");
					sb.append("<img src=\"/" + line + "\" />");
					sb.append("</a></div>\n");
				} else {
					if (imglinkbox) sb.append("</div>\n");
					if (!textlinkbox) sb.append("<div class=\"textlinkbox\">\n");
					textlinkbox = true;
					imglinkbox = false;

					sb.append("<div class=\"textlink\">");
					sb.append("<a href=\"/" + line + "\">" + name + "</a>");
					sb.append("</div>\n");
				}

				continue;
			}
			
			if (textlinkbox || imglinkbox) sb.append("</div>\n");
			textlinkbox = false;
			imglinkbox = false;
			
			sb.append("<div class=\"normalline\">" + line + "</div>\n");
		}
		
		if (textlinkbox || imglinkbox) sb.append("</div>\n");
		if (paragraph) sb.append("</p>");
		
		return sb.toString();
	}

	@Override
	public int compareTo(Freesite other) {
		return uniqueKey - other.uniqueKey;
	}
}
