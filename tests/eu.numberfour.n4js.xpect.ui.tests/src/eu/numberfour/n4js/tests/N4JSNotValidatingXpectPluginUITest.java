/**
 * Copyright (c) 2016 NumberFour AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   NumberFour AG - Initial API and implementation
 */
package eu.numberfour.n4js.tests;

import org.junit.runner.RunWith;
import org.xpect.XpectImport;
import org.xpect.lib.XpectTestResultTest;
import org.xpect.runner.XpectRunner;
import org.xpect.runner.XpectSuiteClasses;
import org.xpect.runner.XpectTestFiles;
import org.xpect.runner.XpectTestFiles.FileRoot;
import org.xpect.xtext.lib.tests.ValidationTest;

import eu.numberfour.n4js.xpect.ContentAssistXpectMethod;
import eu.numberfour.n4js.xpect.HyperlinkXpectMethod;
import eu.numberfour.n4js.xpect.ProposalXpectMethod;
import eu.numberfour.n4js.xpect.QuickFixXpectMethod;
import eu.numberfour.n4js.xpect.config.Config;
import eu.numberfour.n4js.xpect.config.VarDef;
import eu.numberfour.n4js.xpect.config.XpEnvironmentData;

/**
 * Plugin for proposal test. This plugin turns <b>off</b> validation in xpect-tests by default since most input files
 * are invalid before applying and only some become valid after applying a proposal.
 */
@XpectSuiteClasses({
		// LinkingTest.class,
		// N4JSTypeSystemXpectTestFragment.class,
		// NoerrorsValidationTestFragment.class,
		// PositionAwareScopingXpectTestFragment.class,
		// ResourceDescriptionTest.class,
		ValidationTest.class,
		QuickFixXpectMethod.class,
		ProposalXpectMethod.class,
		HyperlinkXpectMethod.class,
		ContentAssistXpectMethod.class,
		XpectTestResultTest.class
})
@XpectImport({ Config.class, VarDef.class, XpEnvironmentData.class })
@RunWith(XpectRunner.class)
@XpectTestFiles(relativeTo = FileRoot.PROJECT, baseDir = "model_nonvalidating", fileExtensions = "xt")
public class N4JSNotValidatingXpectPluginUITest {
	//

}
