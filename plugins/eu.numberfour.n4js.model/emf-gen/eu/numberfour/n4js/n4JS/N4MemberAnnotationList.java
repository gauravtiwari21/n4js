/**
 * Copyright (c) 2016 NumberFour AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package eu.numberfour.n4js.n4JS;

import eu.numberfour.n4js.ts.typeRefs.TypeRef;

import eu.numberfour.n4js.ts.types.TMember;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>N4 Member Annotation List</b></em>'.
 * <!-- end-user-doc -->
 *
 * <!-- begin-model-doc -->
 * A {@link N4MemberAnnotationList} holds annotations and can be a placeholder
 * where a {@link N4MemberDeclaration} is expected.
 * This allows to handle syntax errors in the input file gracefully while
 * being able to left factor the grammar to make it parseable.
 * <!-- end-model-doc -->
 *
 *
 * @see eu.numberfour.n4js.n4JS.N4JSPackage#getN4MemberAnnotationList()
 * @model
 * @generated
 */
public interface N4MemberAnnotationList extends AbstractAnnotationList, N4MemberDeclaration {
	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @model kind="operation" unique="false"
	 *        annotation="http://www.eclipse.org/emf/2002/GenModel body='final <%org.eclipse.emf.ecore.EObject%> c = this.eContainer();\nif ((c instanceof <%eu.numberfour.n4js.n4JS.N4MemberDeclaration%>))\n{\n\treturn ((<%eu.numberfour.n4js.n4JS.N4MemberDeclaration%>)c).getDefinedTypeElement();\n}\nreturn null;'"
	 * @generated
	 */
	TMember getDefinedTypeElement();

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @model kind="operation" unique="false"
	 *        annotation="http://www.eclipse.org/emf/2002/GenModel body='final <%org.eclipse.emf.ecore.EObject%> c = this.eContainer();\nif ((c instanceof <%eu.numberfour.n4js.n4JS.N4MemberDeclaration%>))\n{\n\treturn ((<%eu.numberfour.n4js.n4JS.N4MemberDeclaration%>)c).getDeclaredTypeRef();\n}\nreturn null;'"
	 * @generated
	 */
	TypeRef getDeclaredTypeRef();

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @model kind="operation" unique="false"
	 *        annotation="http://www.eclipse.org/emf/2002/GenModel body='return null;'"
	 * @generated
	 */
	String getName();

} // N4MemberAnnotationList
