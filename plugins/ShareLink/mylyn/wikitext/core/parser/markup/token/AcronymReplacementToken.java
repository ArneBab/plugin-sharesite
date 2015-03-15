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
package plugins.ShareWiki.mylyn.wikitext.core.parser.markup.token;

import plugins.ShareWiki.mylyn.wikitext.core.parser.markup.PatternBasedElement;
import plugins.ShareWiki.mylyn.wikitext.core.parser.markup.PatternBasedElementProcessor;

/**
 * A token for acronyms, which are defined as 3 or more upper-case letters followed immediately by parentheses with one
 * or more words. For example: ABW(A Better Way)
 * 
 * @author David Green
 * @since 1.0
 */
public class AcronymReplacementToken extends PatternBasedElement {

	@Override
	protected String getPattern(int groupOffset) {
		return "(?:(?:(?<=\\W)|^)([A-Z]{3,})\\(([^\\)]+)\\))"; //$NON-NLS-1$
	}

	@Override
	protected int getPatternGroupCount() {
		return 2;
	}

	@Override
	protected PatternBasedElementProcessor newProcessor() {
		return new AcronymReplacementTokenProcessor();
	}

	private static class AcronymReplacementTokenProcessor extends PatternBasedElementProcessor {

		@Override
		public void emit() {
			String acronym = group(1);
			String acronymDef = group(2);
			state.addGlossaryTerm(acronym, acronymDef);
			builder.acronym(acronym, acronymDef);
		}

	}
}
