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
package plugins.ShareWiki.mylyn.wikitext.core.parser.util;

/**
 * Adapt a regex matcher to a {@link Matcher}.
 * 
 * @author David Green
 * @since 1.0
 */
public class MatcherAdaper implements Matcher {

	private final java.util.regex.Matcher delegate;

	public MatcherAdaper(java.util.regex.Matcher delegate) {
		this.delegate = delegate;
	}

	public int end(int group) {
		return delegate.end(group);
	}

	public String group(int group) {
		return delegate.group(group);
	}

	public int start(int group) {
		return delegate.start(group);
	}

}
