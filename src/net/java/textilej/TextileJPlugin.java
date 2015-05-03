package net.java.textilej;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import net.java.textilej.parser.markup.Dialect;
import net.java.textilej.validation.MarkupValidator;
import net.java.textilej.validation.ValidationRule;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.osgi.framework.BundleContext;


public class TextileJPlugin extends Plugin {

	private static final String EXTENSION_MARKUP_DIALECT = "markupDialect";

	private static final String EXTENSION_VALIDATION_RULES = "markupValidationRule";

	private static TextileJPlugin plugin;
	
	private SortedMap<String,Class<? extends Dialect>> dialectByName;
	private Map<String,Class<? extends Dialect>> dialectByFileExtension;

	private Map<String, List<ValidationRule>> validationRulesByDialectName;
	
	public TextileJPlugin() {
	}

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		if (plugin == this) {
			plugin = null;
		}
		super.stop(context);
	}

	public static TextileJPlugin getDefault() {
		return plugin;
	}
	
	/**
	 * Get a dialect by name.
	 * 
	 * @param name the name of the dialect to retrieve
	 * 
	 * @return the dialect or null if there is no dialect known by the given name
	 * 
	 * @see #getDialectNames()
	 */
	public Dialect getDialect(String name) {
		if (dialectByName == null) {
			initializeDialects();
		}
		Class<? extends Dialect> dialectClass = dialectByName.get(name);
		if (dialectClass != null) {
			return instantiateDialect(name, dialectClass);
		}
		return null;
	}

	private Dialect instantiateDialect(String name, Class<? extends Dialect> dialectClass) {
		try {
			Dialect dialect = dialectClass.newInstance();
			dialect.setName(name);
			return dialect;
		} catch (Exception e) {
			log(IStatus.ERROR,String.format("Cannot instantiate dialect '%' (class '%s'): %s",name,dialectClass.getName(),e.getMessage()),e);
		}
		return null;
	}

	/**
	 * Get a dialect for a file.  A dialect is selected based on the registered dialects and their
	 * expected file extensions.
	 * 
	 * @param name the name of the file for which a dialect is desired
	 *  
	 * @return the dialect, or null if no dialect is registered for the specified file name
	 */
	public Dialect getDialectForFilename(String name) {
		if (dialectByFileExtension == null) {
			initializeDialects();
		}
		int lastIndexOfDot = name.lastIndexOf('.');
		String extension = lastIndexOfDot==-1?name:name.substring(lastIndexOfDot+1);
		Class<? extends Dialect> dialectClass = dialectByFileExtension.get(extension);
		if (dialectClass != null) {
			String dialectName = null;
			for (Map.Entry<String, Class<? extends Dialect>> ent: dialectByName.entrySet()) {
				if (ent.getValue() == dialectClass) {
					dialectName = ent.getKey();
					break;
				}
			}
			return instantiateDialect(dialectName, dialectClass);
		}
		return null;
	}
	
	/**
	 * Get the names of all dialects
	 * 
	 * @see #getDialect(String)
	 */
	public Set<String> getDialectNames() {
		if (dialectByName == null) {
			initializeDialects();
		}
		return dialectByName.keySet();
	}

	/**
	 * Get a markup validator by dialect name.
	 * 
	 * @param name the name of the dialect for which a validator is desired
	 * 
	 * @return the markup validator
	 * 
	 * @see #getDialectNames()
	 */
	public MarkupValidator getMarkupValidator(String name) {
		MarkupValidator markupValidator = new MarkupValidator();
		
		if (validationRulesByDialectName == null) {
			initializeValidationRules();
		}
		List<ValidationRule> rules = validationRulesByDialectName.get(name);
		if (rules != null) {
			markupValidator.getRules().addAll(rules);
		}
		
		return markupValidator;
	}
	
	private void initializeValidationRules() {
		initializeDialects();
		synchronized (this) {
			if (validationRulesByDialectName == null) {
				Map<String,List<ValidationRule>> validationRulesByDialectName = new HashMap<String, List<ValidationRule>>();
				
				IExtensionPoint extensionPoint = Platform.getExtensionRegistry().getExtensionPoint(getPluginId(), EXTENSION_VALIDATION_RULES);
				if (extensionPoint != null) { 
					IConfigurationElement[] configurationElements = extensionPoint.getConfigurationElements();
					for (IConfigurationElement element: configurationElements) {
						try {
							String markupLanguage = element.getAttribute("markupLanguage");
							if (markupLanguage == null || markupLanguage.length() == 0) {
								throw new Exception("Must specify markupLanguage");
							}
							if (!dialectByName.containsKey(markupLanguage)) {
								throw new Exception(String.format("'%s' is not a valid markupLanguage",dialectByName));
							}
							Object extension;
							try {
								extension = element.createExecutableExtension("class");
							} catch (CoreException e) {
								getLog().log(e.getStatus());
								continue;
							}
							if (!(extension instanceof ValidationRule)) {
								throw new Exception(String.format("%s is not a validation rule",extension.getClass().getName()));
							}
							List<ValidationRule> rules = validationRulesByDialectName.get(markupLanguage);
							if (rules == null) {
								rules = new ArrayList<ValidationRule>();
								validationRulesByDialectName.put(markupLanguage, rules);
							}
							rules.add((ValidationRule) extension);
						} catch (Exception e) {
							log(IStatus.ERROR,String.format("Plugin '%s' extension '%s' invalid: %s",element.getDeclaringExtension().getContributor().getName(),EXTENSION_VALIDATION_RULES,e.getMessage()),e);
						}
					}
				}
				
				this.validationRulesByDialectName = validationRulesByDialectName;
			}
		}
	}
	
	private void initializeDialects() {
		synchronized (this) {
			if (this.dialectByName == null) {
				SortedMap<String, Class<? extends Dialect>> dialectByName = new TreeMap<String, Class<? extends Dialect>>();
				Map<String, Class<? extends Dialect>> dialectByFileExtension = new HashMap<String,Class<? extends Dialect>>();
				
				IExtensionPoint extensionPoint = Platform.getExtensionRegistry().getExtensionPoint(getPluginId(), EXTENSION_MARKUP_DIALECT);
				if (extensionPoint != null) { 
					IConfigurationElement[] configurationElements = extensionPoint.getConfigurationElements();
					for (IConfigurationElement element: configurationElements) {
						String name = element.getAttribute("name");
						if (name == null || name.length() == 0) {
							log(IStatus.ERROR,String.format(EXTENSION_MARKUP_DIALECT+"/@name must be specified by plugin '%s'",element.getDeclaringExtension().getContributor().getName()));
							continue;
						}
						Object dialect;
						try {
							dialect = element.createExecutableExtension("class");
						} catch (CoreException e) {
							getLog().log(e.getStatus());
							continue;
						}
						if (!(dialect instanceof Dialect)) {
							log(IStatus.ERROR,String.format("%s is not a dialect",dialect.getClass().getName()));
							continue;
						}
						Dialect d = (Dialect) dialect;
						{
							Class<? extends Dialect> previous = dialectByName.put(name, d.getClass());
							if (previous != null) { 
								log(IStatus.ERROR,String.format(EXTENSION_MARKUP_DIALECT+"/@name '%s' specified by plugin '%s' is ignored: name '%s' is already registered",name,element.getDeclaringExtension().getContributor().getName(),name));
								dialectByName.put(name, previous);
								continue;
							}
						}
						String fileExtensions = element.getAttribute("fileExtensions");
						if (fileExtensions != null) {
							String[] parts = fileExtensions.split("\\s*,\\s*");
							for (String part: parts) {
								if (part.length() != 0) {
									Class<? extends Dialect> previous = dialectByFileExtension.put(part, d.getClass());
									if (previous != null) {
										log(IStatus.ERROR,String.format(EXTENSION_MARKUP_DIALECT+"/@fileExtensions '%s' specified by plugin '%s' is ignored: extension '%s' is already registered",part,element.getDeclaringExtension().getContributor().getName(),part));
										dialectByFileExtension.put(part, previous);
										continue;
									}
								}
							}
						}
					}
				}

				this.dialectByFileExtension = dialectByFileExtension;
				this.dialectByName = dialectByName;
			}
		}
	}

	public void log(int severity, String message) {
		log(severity,message,null);
	}
	
	public void log(int severity, String message, Throwable t) {
		getLog().log(new Status(severity,getPluginId(),message,t));
	}

	public String getPluginId() {
		return getBundle().getSymbolicName();
	}
}
