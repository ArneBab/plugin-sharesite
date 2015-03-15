package plugins.ShareLink.webui;

import plugins.ShareLink.Plugin;
import freenet.clients.http.PageMaker;
import freenet.clients.http.ToadletContainer;

/**
 * Register all the plugin's menus and pages to the Freenet node.
 * All pages are under "/ShareLink/" and not under "/plugins/".
 * They therefore cannot be accessed from freesites, and we don't
 * have to check formPassword.
 * 
 * You find ShareLink to the right in the top menu on
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

		pageMaker.addNavigationCategory("/ShareLink/",
			"ShareLink.Menu.Name", "ShareLink.Menu.Tooltip",
			Plugin.instance);
		container.register(homeToadlet, "ShareLink.Menu.Name",
			"/ShareLink/", true, "ShareLink.Menu.Name",
			"ShareLink.Menu.Tooltip", false, null);
		container.register(editToadlet, null, "/ShareLink/Edit/", true, false);
		container.register(previewToadlet, null, "/ShareLink/Preview/", true, false);
	}

	public void removeInterface() {
		container.unregister(previewToadlet);
		container.unregister(editToadlet);
		container.unregister(homeToadlet);
		pageMaker.removeNavigationCategory("ShareLink.Menu.Name");
	}
}
