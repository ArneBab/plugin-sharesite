/*******************************************************************************
 * Copyright (c) 2007, 2009 David Green and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     David Green - initial API and implementation
 *******************************************************************************/
package plugins.ShareWiki.mylyn.wikitext.core.util;

import java.io.IOException;
import java.io.StringReader;

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * An entity resolver that resolves all requests for DTD content, thus preventing network access when resolving DTDs.
 * 
 * @author David Green
 * @since 1.0
 */
public class IgnoreDtdEntityResolver implements EntityResolver {
	protected static final IgnoreDtdEntityResolver instance = new IgnoreDtdEntityResolver();

	public static IgnoreDtdEntityResolver getInstance() {
		return instance;
	}

	public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
		if ((publicId != null && publicId.indexOf("//DTD") != -1) || (systemId != null && systemId.endsWith(".dtd"))) { //$NON-NLS-1$ //$NON-NLS-2$
			return new InputSource(new StringReader("")); //$NON-NLS-1$
		}
		return null;
	}
}
