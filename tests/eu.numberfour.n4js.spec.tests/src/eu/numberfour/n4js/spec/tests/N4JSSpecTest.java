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
package eu.numberfour.n4js.spec.tests;

import org.junit.runner.RunWith;
import org.xpect.runner.XpectRunner;
import org.xpect.runner.XpectSuiteClasses;
import org.xpect.runner.XpectTestFiles;
import org.xpect.runner.XpectTestFiles.FileRoot;
import org.xpect.xtext.lib.tests.ResourceDescriptionTest;
import org.xpect.xtext.lib.tests.ValidationTest;

import eu.numberfour.n4js.xpect.OutputXpectMethod;
import eu.numberfour.n4js.xpect.TypeXpectMethod;
import eu.numberfour.n4js.xpect.linking.LinkingXpectMethod;
import eu.numberfour.n4js.xpect.scoping.ScopeXpectMethod;
import eu.numberfour.n4js.xpect.validation.NoerrorsXpectMethod;

/**
 * Test class for all Xpect tests in folder xpect-tests; this test class is also configured in the plugin.xml so that is
 * found by all these xpect tests (even when run as non-OSGi-test).
 */
@XpectSuiteClasses({
	LinkingXpectMethod.class, ResourceDescriptionTest.class,
	TypeXpectMethod.class,
	ValidationTest.class,
	NoerrorsXpectMethod.class,
	ScopeXpectMethod.class,
	OutputXpectMethod.class
})
@RunWith(XpectRunner.class)
@XpectTestFiles(relativeTo = FileRoot.PROJECT, baseDir = "xpect-tests", fileExtensions = { "xt" })
public class N4JSSpecTest {
	// nop
}
