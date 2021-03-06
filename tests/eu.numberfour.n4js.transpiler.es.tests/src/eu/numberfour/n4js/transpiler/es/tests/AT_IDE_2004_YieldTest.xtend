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
package eu.numberfour.n4js.transpiler.es.tests

import com.google.inject.Inject
import eu.numberfour.n4js.N4JSInjectorProviderWithMockProject
import eu.numberfour.n4js.n4JS.Script
import org.eclipse.emf.common.util.URI
import org.eclipse.xtext.junit4.InjectWith
import org.eclipse.xtext.junit4.XtextRunner
import org.eclipse.xtext.junit4.util.ParseHelper
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters

import java.util.regex.Pattern

/**
 */
@RunWith(XtextRunner)
@InjectWith(N4JSInjectorProviderWithMockProject)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class AT_IDE_2004_YieldTest extends AbstractTranspilerTest {

	@Inject extension ParseHelper<Script>

	/**
	 * This test checks extra parenthesis around the yield expression. c.f. IDE-2004
	 */
	@Test
	def void test_CompileArrowToYield_check_extra_parenthesis() throws Throwable{

		val script = ''' var x = async()=>{ return 45; } ''';

	 	// Prepare ResourceSet to contain exportedScript:
		val resSet = installExportedScript;

   		val Script scriptNode = script.parse(URI.createURI("src/A.n4js"),resSet)
		scriptNode.resolveLazyRefs

   		// \\h is a horizontal white-space
   		val pattern = Pattern.compile("\\(\\h*yield\\h*45\\h*\\)");

		// Changed behavior in GH-93 to not create a yield for simple return statements.
		assertCompileResultDoesNotMatch(scriptNode,pattern);
	}

}
