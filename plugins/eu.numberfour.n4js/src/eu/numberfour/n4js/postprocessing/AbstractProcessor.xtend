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
package eu.numberfour.n4js.postprocessing

import com.google.common.base.Throwables
import com.google.inject.Inject
import eu.numberfour.n4js.n4JS.FormalParameter
import eu.numberfour.n4js.n4JS.FunctionDefinition
import eu.numberfour.n4js.n4JS.IdentifierRef
import eu.numberfour.n4js.n4JS.N4JSASTUtils
import eu.numberfour.n4js.n4JS.NamedElement
import eu.numberfour.n4js.n4JS.SetterDeclaration
import eu.numberfour.n4js.n4JS.ThisLiteral
import eu.numberfour.n4js.resource.N4JSResource
import eu.numberfour.n4js.ts.typeRefs.TypeRef
import eu.numberfour.n4js.ts.typeRefs.TypeRefsFactory
import eu.numberfour.n4js.ts.types.IdentifiableElement
import eu.numberfour.n4js.ts.types.TFormalParameter
import eu.numberfour.n4js.ts.types.TStructMember
import eu.numberfour.n4js.ts.types.TypableElement
import eu.numberfour.n4js.typesystem.CustomInternalTypeSystem
import eu.numberfour.n4js.utils.UtilN4
import eu.numberfour.n4js.xsemantics.InternalTypeSystem
import it.xsemantics.runtime.Result
import it.xsemantics.runtime.RuleApplicationTrace
import it.xsemantics.runtime.RuleEnvironment
import java.util.function.BooleanSupplier
import org.eclipse.emf.ecore.EObject
import org.eclipse.emf.ecore.util.EcoreUtil
import org.eclipse.xtext.EcoreUtil2
import org.eclipse.xtext.nodemodel.util.NodeModelUtils

import static extension eu.numberfour.n4js.typesystem.RuleEnvironmentExtensions.*
import static extension eu.numberfour.n4js.utils.N4JSLanguageUtils.*

/**
 * Provides some common base functionality used across all processors (e.g. logging). See {@link ASTProcessor} for more
 * details on processors and post-processing of {@link N4JSResource}s.
 */
package abstract class AbstractProcessor {

	val private static DEBUG_LOG = false;
	val private static DEBUG_LOG_RESULT = false;
	val private static DEBUG_RIGID = true; // if true, more consistency checks are performed and exceptions thrown if wrong

	@Inject
	private InternalTypeSystem ts_internal;

	/**
	 * Processors can call this method to directly invoke the 'type' judgment of the internal, Xsemantics-generated type
	 * system. Normally, this should only be required by {@link TypeProcessor}, so use this sparingly (however,
	 * sometimes it can be helpful to avoid duplication of logic).
	 */
	def protected Result<TypeRef> askXsemanticsForType(RuleEnvironment G, RuleApplicationTrace trace, TypableElement elem) {
		if (elem.eIsProxy) {
			return new Result(TypeRefsFactory.eINSTANCE.createUnknownTypeRef);
		}
		// special case:
		// "this" in the default initializer expression of fpars is not supported yet
		if (elem.isThisKeywordInFparDefaultExpression || elem.isFparRefInFparDefaultExpression) {
			// TODO IDE-1345 remove this work-around when fpar default initializers are properly supported
			// To see why the following work-around is required, remove it and check the following code:
			//
			//     var ol = {
			//         prop1: "p1",
			//         prop2: function(x = this.prop1) {}
			//     };
			//
			// or
			//
			//     var ol = {
			//         prop: function(x = 42, y = x) {}
			//     };
			//
			return new Result(G.undefinedTypeRef);
		}
		// special case:
		// TStructMembers are special in that they may be types (in case of TStructMethod) and appear as AST nodes
		// -> if we are dealing with an AST node, make sure to use the definedMember in the TModule
		val definedMember = if (elem instanceof TStructMember) elem.definedMember;
		if (definedMember !== null && elem.isASTNode) {
			return askXsemanticsForType(G, trace, definedMember);
		}
		return (ts_internal as CustomInternalTypeSystem).use_type_judgment_from_PostProcessors(G, trace, elem);
	}

	def private static boolean isThisKeywordInFparDefaultExpression(TypableElement elem) {
		return elem instanceof ThisLiteral && elem.isContainedInFparDefaultExpression;
	}

	def private static boolean isFparRefInFparDefaultExpression(TypableElement elem) {
		return elem instanceof IdentifierRef
			&& (
				(elem as IdentifierRef).id instanceof FormalParameter
				|| (elem as IdentifierRef).id instanceof TFormalParameter
			)
			&& elem.isContainedInFparDefaultExpression;
	}

	def private static boolean isContainedInFparDefaultExpression(TypableElement elem) {
		val containingFunctionOrAccessor = N4JSASTUtils.getContainingFunctionOrAccessor(elem);
		val containingFunctionOrAccessorFpars = switch (containingFunctionOrAccessor) {
			FunctionDefinition: containingFunctionOrAccessor.fpars
			SetterDeclaration: #[containingFunctionOrAccessor.fpar]
			default: #[]
		};
		val containingFpar = EcoreUtil2.getContainerOfType(elem, FormalParameter);
		if (containingFpar !== null && containingFunctionOrAccessorFpars.contains(containingFpar)) {
			val containingFparInitializer = containingFpar.initializer;
			if (containingFparInitializer !== null && EcoreUtil.isAncestor(containingFparInitializer, elem)) {
				return true;
			}
		}
		return false;
	}


	def protected static String getObjectInfo(EObject obj) {
		if (obj === null) {
			"<null>"
		} else if (obj instanceof IdentifierRef) {
			"IdentifierRef \"" + NodeModelUtils.getTokenText(NodeModelUtils.findActualNodeFor(obj)) + "\""
		} else {
			val name = obj.name;
			if (name !== null) {
				obj.eClass.name + " \"" + name + "\""
			} else {
				obj.eClass.name
			}
		}
	}

	def protected static String getName(EObject obj) {
		switch (obj) {
			NamedElement: obj.name
			IdentifiableElement: obj.name
		}
	}

	def protected static void log(int indentLevel, Result<TypeRef> result) {
		if (!isDEBUG_LOG)
			return;
		log(indentLevel, result.resultToString);
	}

	def protected static void log(int indentLevel, EObject astNode, ASTMetaInfoCache cache) {
		if (!isDEBUG_LOG)
			return;
		if (astNode.isTypableNode) {
			val result = cache.getTypeFailSafe(astNode as TypableElement);
			val resultStr = if (result !== null) result.resultToString else "*** MISSING ***";
			log(indentLevel, astNode.objectInfo + " " + resultStr);
		} else {
			log(indentLevel, astNode.objectInfo);
		}
		for (childNode : astNode.eContents) {
			log(indentLevel + 1, childNode, cache);
		}
	}

	def protected static void log(int indentLevel, String msg) {
		if (!isDEBUG_LOG)
			return;
		println(indent(indentLevel) + msg);
	}

	def protected static void logErr(String msg) {
		// always log errors, even if !isDEBUG_LOG()
		System.out.flush();
		System.err.println(msg);
		System.err.flush();
	}

	def protected static Throwable logException(Throwable th) {
		// always log exceptions, even if !isDEBUG_LOG()
		th.printStackTrace // enforce dumping all exceptions to stderr
		return th;
	}

	def protected static void assertTrueIfRigid(String message, BooleanSupplier check) {
		if (isDEBUG_RIGID) {
			assertTrueIfRigid(message, check.asBoolean)
		}
	}

	def protected static void assertTrueIfRigid(String message, boolean actual) {
		if (isDEBUG_RIGID && !actual) {
			val e = new Error(message);
			UtilN4.reportError(e); // make sure we see this exception on the console, even if it gets caught somewhere
			Throwables.propagate(e);
		}
	}

	// using a method to read field DEBUG_LOG to get rid of Xtend's "Constant condition is always true|false." warnings
	def protected static boolean isDEBUG_LOG() {
		return DEBUG_LOG;
	}

	def protected static boolean isDEBUG_LOG_RESULT() {
		return DEBUG_LOG_RESULT;
	}

	def protected static boolean isDEBUG_RIGID() {
		return DEBUG_RIGID;
	}

	def protected static String resultToString(Result<TypeRef> result) {
		"RESULT: " + if (result !== null && result.failed) {
			"!FAILED! " + result.ruleFailedException.message;
		} else {
			result?.value?.typeRefAsString
		}
	}

	def protected static String indent(int indentLevel) {
		(0 ..< indentLevel).map["    "].join
	}
}
