package plugins.ShareWiki.webui;

import plugins.ShareWiki.Plugin;
import freenet.clients.http.PageMaker;
import freenet.clients.http.ToadletContainer;

/**
 * Register all the plugin's menus and pages to the Freenet node.
 * All pages are under "/ShareWiki/" and not under "/plugins/".
 * They therefore cannot be accessed from freesites, and we don't
 * have to check formPassword.
 * 
 * You find ShareWiki to the right in the top menu on
 * FProxys startpage.
 */
public class WebInterface {
	private PageMaker pageMaker;
	private ToadletContainer container;
	
	private HomeToadlet homeToadlet;
	private EditToadlet editToadlet;
	private PreviewToadlet previewToadlet;

	public WebInterface() {
		pageMaker = Plugin.instance.pluginRespirator.getPageMaker();
		container = Plugin.instance.pluginRespirator.getToadletContainer();
	}

	public void createInterface() {
		homeToadlet = new HomeToadlet();
		editToadlet = new EditToadlet();
		previewToadlet = new PreviewToadlet();

		pageMaker.addNavigationCategory("/ShareWiki/",
			"ShareWiki.Menu.Name", "ShareWiki.Menu.Tooltip",
			Plugin.instance);
		container.register(homeToadlet, "ShareWiki.Menu.Name",
			"/ShareWiki/", true, "ShareWiki.Menu.Name",
			"ShareWiki.Menu.Tooltip", false, null);
		container.register(editToadlet, null, "/ShareWiki/Edit/", true, false);
		container.register(previewToadlet, null, "/ShareWiki/Preview/", true, false);
	}

	public void removeInterface() {
		container.unregister(previewToadlet);
		container.unregister(editToadlet);
		container.unregister(homeToadlet);
		pageMaker.removeNavigationCategory("ShareWiki.Menu.Name");
	}
}
