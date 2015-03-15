/*******************************************************************************
 * Copyright (c) 2009, 2012 David Green and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     David Green - initial API and implementation
 *******************************************************************************/

package plugins.ShareWiki.mylyn.wikitext.core.parser.markup.block;

import java.util.regex.Pattern;

import plugins.ShareWiki.mylyn.wikitext.core.parser.Attributes;
import plugins.ShareWiki.mylyn.wikitext.core.parser.DocumentBuilder.BlockType;
import plugins.ShareWiki.mylyn.wikitext.core.parser.markup.Block;

/**
 * A block for Java stack traces. Matches any text that looks like a stack trace, even if it is only a portion (it's
 * common for stack traces to be clipped to eliminate unrelated text).
 * 
 * @author David Green
 * @since 1.1
 */
public class JavaStackTraceBlock extends Block {

//java.lang.Exception: java.lang.IllegalStateException
//	at plugins.ShareWiki.mylyn.internal.wikitext.tasks.ui.util.Test.main(Test.java:21)
//Caused by: java.lang.IllegalStateException
//	... 1 more

	private static final String PACKAGE_PART = "([a-z][a-z0-9]*)"; //$NON-NLS-1$

	private static final String CLASS_PART = "[A-Za-z][a-zA-Z0-9_$]*"; //$NON-NLS-1$

	private static final String FQN_PART = "((" + PACKAGE_PART + "(\\." + PACKAGE_PART + ")*\\." + CLASS_PART + ")|(\\$Proxy\\d+))"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

	private static final Pattern STACK_TRACE_PATTERN = Pattern.compile("\\s*((" + "((Caused by:\\s+)|(at\\s+))?" //$NON-NLS-1$//$NON-NLS-2$
			+ FQN_PART + "((:\\s+\\w.*)|(\\.((\\<(?:cl)?init\\>)|([a-zA-Z0-9_$]+))\\(.*?\\)))?" //$NON-NLS-1$
			+ ")|(\\.\\.\\.\\s\\d+\\smore))"); //$NON-NLS-1$ 

	private int blockLineCount = 0;

	@Override
	public boolean canStart(String line, int lineOffset) {
		if (lineOffset == 0 && STACK_TRACE_PATTERN.matcher(line).matches()) {
			blockLineCount = 0;
			return true;
		}
		return false;
	}

	@Override
	protected int processLineContent(String line, int offset) {
		if (blockLineCount++ == 0) {
			Attributes attributes = new Attributes();
			attributes.setCssClass("javaStackTrace"); //$NON-NLS-1$
			builder.beginBlock(BlockType.PREFORMATTED, attributes);
		} else {
			if (!STACK_TRACE_PATTERN.matcher(line).matches()) {
				setClosed(true);
				return 0;
			}
		}

		builder.characters(offset > 0 ? line.substring(offset) : line);
		builder.characters("\n"); //$NON-NLS-1$

		return -1;
	}

	@Override
	public void setClosed(boolean closed) {
		if (closed && !isClosed()) {
			builder.endBlock();
		}
		super.setClosed(closed);
	}
}
