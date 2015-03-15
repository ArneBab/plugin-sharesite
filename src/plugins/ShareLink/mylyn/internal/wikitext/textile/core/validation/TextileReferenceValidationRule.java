/*******************************************************************************
 * Copyright (c) 2007, 2008 David Green and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     David Green - initial API and implementation
 *******************************************************************************/

package plugins.ShareWiki.mylyn.internal.wikitext.textile.core.validation;

import plugins.ShareWiki.mylyn.wikitext.core.parser.markup.MarkupLanguage;
import plugins.ShareWiki.mylyn.wikitext.core.validation.DocumentLocalReferenceValidationRule;
import plugins.ShareWiki.mylyn.wikitext.textile.core.TextileLanguage;

public class TextileReferenceValidationRule extends DocumentLocalReferenceValidationRule {

	@Override
	protected MarkupLanguage createMarkupLanguage() {
		return new TextileLanguage();
	}

}
