/*******************************************************************************
 * Copyright (c) 2009 David Green and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     David Green - initial API and implementation
 *******************************************************************************/

package plugins.ShareWiki.mylyn.internal.wikitext.core.util.css;

/**
 * A rule (name and value) as defined by CSS
 * 
 * @author David Green
 */
public class CssRule {
	/**
	 * the name of the rule
	 */
	public final String name;

	/**
	 * the value of the rule
	 */
	public final String value;

	/**
	 * the offset at which the rule was declared
	 */
	public final int offset;

	/**
	 * the length of the rule declaration
	 */
	public final int length;

	/**
	 * the offset of the name of the rule
	 */
	public final int nameOffset;

	/**
	 * the offset of the value of the rule
	 */
	public final int valueOffset;

	public CssRule(String name, String value, int offset, int length, int nameOffset, int valueOffset) {
		this.name = name;
		this.value = value;
		this.offset = offset;
		this.length = length;
		this.nameOffset = nameOffset;
		this.valueOffset = valueOffset;
	}

}
