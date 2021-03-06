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
package eu.numberfour.n4js.validation.validators

import com.google.common.base.Joiner
import com.google.inject.Inject
import eu.numberfour.n4js.AnnotationDefinition
import eu.numberfour.n4js.n4JS.AbstractAnnotationList
import eu.numberfour.n4js.n4JS.AnnotableElement
import eu.numberfour.n4js.n4JS.AnnotableExpression
import eu.numberfour.n4js.n4JS.AnnotableN4MemberDeclaration
import eu.numberfour.n4js.n4JS.AnnotablePropertyAssignment
import eu.numberfour.n4js.n4JS.AnnotableScriptElement
import eu.numberfour.n4js.n4JS.Annotation
import eu.numberfour.n4js.n4JS.ExportDeclaration
import eu.numberfour.n4js.n4JS.FormalParameter
import eu.numberfour.n4js.n4JS.FunctionDeclaration
import eu.numberfour.n4js.n4JS.FunctionDefinition
import eu.numberfour.n4js.n4JS.N4ClassDeclaration
import eu.numberfour.n4js.n4JS.N4MethodDeclaration
import eu.numberfour.n4js.n4JS.Script
import eu.numberfour.n4js.n4JS.VariableDeclaration
import eu.numberfour.n4js.projectModel.IN4JSCore
import eu.numberfour.n4js.utils.PromisifyHelper
import eu.numberfour.n4js.validation.AbstractN4JSDeclarativeValidator
import eu.numberfour.n4js.validation.JavaScriptVariant
import eu.numberfour.n4js.n4mf.ProjectType
import eu.numberfour.n4js.ts.scoping.N4TSQualifiedNameProvider
import eu.numberfour.n4js.ts.types.TypesPackage
import java.util.HashSet
import org.eclipse.emf.ecore.EObject
import org.eclipse.emf.ecore.EStructuralFeature
import org.eclipse.xtext.EcoreUtil2
import org.eclipse.xtext.naming.IQualifiedNameProvider
import org.eclipse.xtext.resource.impl.ResourceDescriptionsProvider
import org.eclipse.xtext.validation.Check
import org.eclipse.xtext.validation.EValidatorRegistrar

import static eu.numberfour.n4js.AnnotationDefinition.*
import static eu.numberfour.n4js.n4JS.N4JSPackage.Literals.*
import static eu.numberfour.n4js.validation.IssueCodes.*
import static eu.numberfour.n4js.validation.JavaScriptVariant.*
import static extension eu.numberfour.n4js.utils.N4JSLanguageUtils.*

/**
 * Annotation validation rules for N4JS.
 */
class N4JSAnnotationValidator extends AbstractN4JSDeclarativeValidator {

	@Inject IN4JSCore n4jsCore;

	@Inject IQualifiedNameProvider qnProvider

	@Inject ResourceDescriptionsProvider indexAccess

	@Inject private PromisifyHelper promisifyHelper;

	/**
	 * NEEEDED
	 *
	 * when removed check methods will be called twice once by N4JSValidator, and once by
	 * AbstractDeclarativeN4JSValidator
	 */
	override register(EValidatorRegistrar registrar) {
		// nop
	}

	/**
	 * check all annotable elements and do various checks for special types.
	 */
	@Check
	def void checkAnnotations(AnnotableElement annotableElement) {

		val currentVariant = getVariant(annotableElement)

		if (!( currentVariant === n4js || currentVariant === external )) {

			// Annotation not allowed in other then N4JS modes:
			if (annotableElement.annotations.size > 0) {
				addIssue(messageForANN__ONLY_IN_N4JS, annotableElement, annotableElement.annoFeature, ANN__ONLY_IN_N4JS)
			}
			return
		}

		// prevent double-checking left-factored AnnotationLists.
		if (annotableElement instanceof AbstractAnnotationList) return;

		val foundNames = new HashSet<String>();
		for (a : annotableElement.annotations) {
			if (a.name !== null) {
				val def = AnnotationDefinition.find(a.name);
				if (def === null) {
					addIssue(getMessageForANN_NOT_DEFINED(a.name), a, ANNOTATION__NAME, ANN_NOT_DEFINED);
				} else {
					if (def.repeatable) {
						if (foundNames.add(a.name)) {
							internalCheckRepeatableAnnotations(def,
								annotableElement.annotations.filter[e|e.name == a.name]);
						}
					} else {
						if (! foundNames.add(a.name)) {
							addIssue(getMessageForANN_NON_REPEATABLE(a.name), a, ANNOTATION__NAME, ANN_NON_REPEATABLE);
						} else {
							internalCheckAnnotation(def, a);
						}
					}
				}
			}
		}
	}

	/**
	 * Checks all repeatable annotations with same name.
	 */
	private def void internalCheckRepeatableAnnotations(AnnotationDefinition definition,
		Iterable<Annotation> annotations) {
		if (annotations.forall[holdsTargets(definition, it)] &&
			annotations.forall[holdsArgumentTypes(definition, it)]) {
			if (definition.transitive && !definition.repeatable) {
				annotations.forEach[checkUnnecessaryAnnotation(definition, it)]
			}

		// no special validations yet.
		}

	}

	/**
	 * Checks a single non-repeatable annotation
	 */
	private def void internalCheckAnnotation(AnnotationDefinition definition, Annotation annotation) {

		if (holdsTargets(definition, annotation) && holdsArgumentTypes(definition, annotation)) {
			checkUnnecessaryAnnotation(definition, annotation)

			// special validations:
			switch (annotation.name) {
				// no special checks for uncommented:
				// case THIS.NAME:
				case PROVIDED_BY_RUNTIME.name:
					internalCheckProvidedByRuntime(annotation)
				case IGNORE_IMPLEMENTATION.name:
					internalCheckIgnoreImplementation(annotation)
				case STATIC_POLYFILL_MODULE.name:
					internalCheckStaticPolyfillModule(annotation)
				case STATIC_POLYFILL.name:
					internalCheckStaticPolyfill(annotation)
			}
		}
	}


	private def checkUnnecessaryAnnotation(AnnotationDefinition definition, Annotation annotation) {
		if (definition.transitive && !definition.repeatable) {
			if (definition.hasAnnotation(
				EcoreUtil2.getContainerOfType(annotation.annotatedElement.eContainer, AnnotableElement))) {
				addIssue(getMessageForANN_UNNECESSARY(annotation.name), annotation, ANNOTATION__NAME, ANN_UNNECESSARY);
			}
		}
	}

	private def boolean holdsArgumentTypes(AnnotationDefinition definition, Annotation annotation) {

		val actualSize = annotation.args.size;
		val expectedSize = definition.argtypes.length;
		val variadic = definition.argsVariadic;

		if (variadic) {

			//less actual arguments than specified in the definition
			if (expectedSize - actualSize >= 2) {
				addIssue(getMessageForANN_WRONG_NUMBER_OF_ARGUMENTS(definition.name, expectedSize - 1, actualSize),
					annotation, ANNOTATION__NAME, ANN_WRONG_NUMBER_OF_ARGUMENTS);
					return false;
			}

			//missing last argument due to variadic arguments or equal size
			if (1 == expectedSize - actualSize || expectedSize == actualSize) {
				return validateArgumentTypes(definition, annotation);
			}

			var valid = true
			for (var i = 0; i < actualSize; i++) {
				val arg = annotation.args.get(i).value();
				val argType = if (i + 1 > definition.argtypes.length) definition.argtypes.last else definition.argtypes.get(i);
				if (!argType.isInstance(arg)) {
					addIssue(getMessageForANN_WRONG_ARGUMENT_TYPE(definition.name, argType.name), annotation,
						ANNOTATION__ARGS, i, ANN_WRONG_ARGUMENT_TYPE);
					valid = false;
				}
			}
			return valid;

		} else {
			if (actualSize > expectedSize ||
				(!definition.argsOptional && actualSize !== expectedSize)) {
				addIssue(getMessageForANN_WRONG_NUMBER_OF_ARGUMENTS(definition.name, expectedSize, actualSize),
					annotation, ANNOTATION__NAME, ANN_WRONG_NUMBER_OF_ARGUMENTS);
				return false;
			}
		}

		return validateArgumentTypes(definition, annotation);
	}

	private def validateArgumentTypes(AnnotationDefinition definition, Annotation annotation) {
		val actualSize = annotation.args.size;
		val expectedSize = definition.argtypes.length;
		var valid = true
		val min = Math.min(expectedSize, actualSize);
		for (var i = 0; i < min; i++) {
			val arg = annotation.args.get(i).value();
			val argType = definition.argtypes.get(i);
			if (!argType.isInstance(arg)) {

				addIssue(getMessageForANN_WRONG_ARGUMENT_TYPE(definition.name, argType.name), annotation,
					ANNOTATION__ARGS, i, ANN_WRONG_ARGUMENT_TYPE);
				valid = false;
			}

		}

		return valid;
	}

	private def boolean holdsTargets(AnnotationDefinition definition, Annotation annotation) {
		if (definition.targets === null || definition.targets.size === 0) {
			return true; // undefined targets, will probably be tested in special validation
		}
		val element = annotation.annotatedElement;
		if (element === null) {
			return true; // robustness
		}
		val isElementAmongValidTargets =
				definition.targets.exists[it.isInstance(element)] ||
				definition.targetsWithCustomError.exists[it.isInstance(element)];
		if (!isElementAmongValidTargets) {
			// second chance:
			// if we have an ExportDeclaration, it is sufficient if the exported element is among the valid targets
			val exportedElement = if(element instanceof ExportDeclaration) element.exportedElement else null;
			val isExportedElementAmongValidTargets = exportedElement!==null && (
					definition.targets.exists[it.isInstance(exportedElement)] ||
					definition.targetsWithCustomError.exists[it.isInstance(exportedElement)]);
			if(!isExportedElementAmongValidTargets) {
				addWrongLocationIssue(annotation);
				return false;
			}
		}
		return true;
	}

	private def addWrongLocationIssue(Annotation annotation) {
		addIssue(getMessageForANN_DISALLOWED_AT_LOCATION(annotation.name), annotation, ANNOTATION__NAME,
			ANN_DISALLOWED_AT_LOCATION);
	}

	/**
	 * Mapping from EObject to the Feature containing the Annotations. Used for the correct location of error-markings.
	 */
	private def EStructuralFeature annoFeature(AnnotableElement element) {
		return switch (element) {
			AnnotableExpression: ANNOTABLE_EXPRESSION__ANNOTATION_LIST
			AnnotableScriptElement: ANNOTABLE_SCRIPT_ELEMENT__ANNOTATION_LIST
			AnnotableN4MemberDeclaration: ANNOTABLE_N4_MEMBER_DECLARATION__ANNOTATION_LIST
			AnnotablePropertyAssignment: ANNOTABLE_PROPERTY_ASSIGNMENT__ANNOTATION_LIST
			FormalParameter: FORMAL_PARAMETER__ANNOTATIONS
			Script: SCRIPT__ANNOTATIONS
			VariableDeclaration: VARIABLE_DECLARATION__ANNOTATIONS
			default: null
		}
	}

	/**
	 * Constraints 120 (Provided By Runtime)
	 */
	private def internalCheckProvidedByRuntime(Annotation annotation) {
		val element = annotation.annotatedElement;
		if (element === null) {
			return
		}
		if (! JavaScriptVariant.external.isActive(element)) {
			addIssue(getMessageForANN_DISALLOWED_IN_NONDEFINTION_FILE(annotation.name), annotation, ANNOTATION__NAME,
				ANN_DISALLOWED_IN_NONDEFINTION_FILE);
			return
		}
		val projectType = n4jsCore.findProject(element.eResource.URI).get?.projectType;
		if (projectType !== ProjectType.RUNTIME_ENVIRONMENT && projectType !== ProjectType.RUNTIME_LIBRARY) {
			addIssue(getMessageForANN_DISALLOWED_IN_NON_RUNTIME_COMPONENT(annotation.name), annotation, ANNOTATION__NAME,
				ANN_DISALLOWED_IN_NON_RUNTIME_COMPONENT);
			return
		}
	}

	/**
	 * 11.1.3. Implementation of External Declarations
	 */
	private def internalCheckIgnoreImplementation(Annotation annotation) {
		val element = annotation.annotatedElement;
		if (element === null) {
			return
		}
		if (! JavaScriptVariant.external.isActive(element)) {
			addIssue(getMessageForANN_DISALLOWED_IN_NONDEFINTION_FILE(annotation.name), annotation, ANNOTATION__NAME,
				ANN_DISALLOWED_IN_NONDEFINTION_FILE);
			return
		}
	}

	/**
	 * Constraints 140 (Restrictions on static-polyfilling)  §140.3
	 */
	private def internalCheckStaticPolyfillModule(Annotation annotation) {
		val element = annotation.annotatedElement;
		if (element === null) {
			return
		}

		if( ! ( element instanceof Script ) ) { return ; }
		val Script script = element as Script

		// mutual exclusive with poly-fill aware.
		if( script.isContainedInStaticPolyfillAware ) {
			addIssue( messageForANN_POLY_AWARE_AND_MODULE_MUTUAL_EXCLUSIVE, annotation, ANNOTATION__NAME, ANN_POLY_AWARE_AND_MODULE_MUTUAL_EXCLUSIVE )
			return
		}



		val moduleQN = qnProvider.getFullyQualifiedName(script.module)
		// check
		if( ! N4TSQualifiedNameProvider.isModulePolyfill( moduleQN) ) {
			println("### strange should start with '!MPOLY' for "+script?.module?.eResource?.URI);
			return
		}


		if( moduleQN === null ) return;

		val currentProject = n4jsCore.findProject(script.eResource.URI).orNull
		if( currentProject === null ) {
			return
		}

		val index = indexAccess.getResourceDescriptions( script.eResource.resourceSet )

		val currentResDesc = index.getResourceDescription( script.eResource.URI );

		val sameResdesc = index.getExportedObjectsByType( TypesPackage.Literals.TMODULE )
		.filter[ it.qualifiedName !== null && it.qualifiedName.startsWith(moduleQN)]
		.map[ it.EObjectURI ]
		.map[ it.trimFragment ]
		.map[ index.getResourceDescription(it)]
		.filter[ currentResDesc != it ] // not me
		.filter[ val prj = n4jsCore.findProject(it.URI).orNull; currentProject == prj  ] // same Project
		.toList

		if( ! sameResdesc.empty ) {
			// multiple Resources with Static Polyfill Module.
			val msg_prefix = "module"+(if(sameResdesc.size>1) "s"else"")+" in "
			val clashes = Joiner.on(", ").join(sameResdesc.map[
				val srcCon = n4jsCore.findN4JSSourceContainer( it.URI );
				if( srcCon.isPresent ) {srcCon.get.relativeLocation} else {it.URI.toString}	])
			addIssue( getMessageForPOLY_CLASH_IN_STATIC_POLYFILL_MODULE(msg_prefix+clashes), annotation, ANNOTATION__NAME, POLY_CLASH_IN_STATIC_POLYFILL_MODULE )
		}
	}

	/**
	 * Constraints 139 (static polyfill layout)  §139
	 *
	 */
	private def internalCheckStaticPolyfill(Annotation annotation) {
		val AnnotableElement element = annotation.annotatedElement as AnnotableElement;
		if (element === null) {
			return
		}

		// inside a static-polyfill module (IDE-1735)
		if( ! element.isContainedInStaticPolyfillModule ) { //transitively inherited
			// not in a polyfill-module
			addIssue( messageForANN_POLY_STATIC_POLY_ONLY_IN_POLYFILL_MODULE, annotation, ANNOTATION__NAME, ANN_POLY_STATIC_POLY_ONLY_IN_POLYFILL_MODULE )
			return
		}

		var EObject annoTarget = element
		if( element instanceof ExportDeclaration ) {
			annoTarget = element.exportedElement
		}


		// only classes can be polyfilled (class expressions not supported)
		if( ! ( annoTarget instanceof N4ClassDeclaration) ) {
			// n.t.d.
			// annotation-definition only allowes StaticPolyFill on N4ClassDeclarations.
		}

		// val N4ClassDeclaration classDecl = annoTarget as N4ClassDeclaration

		// §139.4 class must be top-level:
		//  -  is ensured due to Annotation-definition restricted to N4ClassDeclarations.

	}


	@Check
	def checkPromisifiableMethod(FunctionDeclaration methodDecl) {
		if(PROMISIFIABLE.hasAnnotation(methodDecl)) {
			holdsPromisifiablePreconditions(methodDecl);
		}
	}
	@Check
	def checkPromisifiableMethod(N4MethodDeclaration methodDecl) {
		if(PROMISIFIABLE.hasAnnotation(methodDecl)) {
			holdsPromisifiablePreconditions(methodDecl);
		}
	}

	def private boolean holdsPromisifiablePreconditions(FunctionDefinition funDef) {
		return switch(promisifyHelper.checkPromisifiablePreconditions(funDef)) {
			case MISSING_CALLBACK: {
				addIssue(getMessageForANN_PROMISIFIABLE_MISSING_CALLBACK, PROMISIFIABLE.getAnnotation(funDef), ANN_PROMISIFIABLE_MISSING_CALLBACK);
				false
			}
			case BAD_CALLBACK__MORE_THAN_ONE_ERROR: {
				addIssue(getMessageForANN_PROMISIFIABLE_BAD_CALLBACK_MORE_THAN_ONE_ERROR, PROMISIFIABLE.getAnnotation(funDef), ANN_PROMISIFIABLE_BAD_CALLBACK_MORE_THAN_ONE_ERROR);
				false
			}
			case BAD_CALLBACK__ERROR_NOT_FIRST_ARG: {
				addIssue(getMessageForANN_PROMISIFIABLE_BAD_CALLBACK_ERROR_NOT_FIRST_ARG, PROMISIFIABLE.getAnnotation(funDef), ANN_PROMISIFIABLE_BAD_CALLBACK_ERROR_NOT_FIRST_ARG);
				false
			}
			case OK: {
				true
			}
		};
	}


}
