/**
 * Copyright (c) 2016 NumberFour AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package eu.numberfour.n4js.ts.typeRefs;

import eu.numberfour.n4js.ts.types.Type;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Classifier Type Ref</b></em>'.
 * <!-- end-user-doc -->
 *
 * <!-- begin-model-doc -->
 * *
 * A reference to the type of a type. This type can be used
 * to access the static members of a type.
 * It can either be a ParameterizedTypeRefNominal or a ThisTypeNominal
 * <!-- end-model-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link eu.numberfour.n4js.ts.typeRefs.ClassifierTypeRef#getTypeArg <em>Type Arg</em>}</li>
 * </ul>
 *
 * @see eu.numberfour.n4js.ts.typeRefs.TypeRefsPackage#getClassifierTypeRef()
 * @model
 * @generated
 */
public interface ClassifierTypeRef extends BaseTypeRef {
	/**
	 * Returns the value of the '<em><b>Type Arg</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Type Arg</em>' containment reference isn't clear,
	 * there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Type Arg</em>' containment reference.
	 * @see #setTypeArg(TypeArgument)
	 * @see eu.numberfour.n4js.ts.typeRefs.TypeRefsPackage#getClassifierTypeRef_TypeArg()
	 * @model containment="true"
	 * @generated
	 */
	TypeArgument getTypeArg();

	/**
	 * Sets the value of the '{@link eu.numberfour.n4js.ts.typeRefs.ClassifierTypeRef#getTypeArg <em>Type Arg</em>}' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Type Arg</em>' containment reference.
	 * @see #getTypeArg()
	 * @generated
	 */
	void setTypeArg(TypeArgument value);

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * Overrides {@link TypeRef#getTypeRefAsString()}
	 * <!-- end-model-doc -->
	 * @model kind="operation" unique="false"
	 *        annotation="http://www.eclipse.org/emf/2002/GenModel body='<%eu.numberfour.n4js.ts.typeRefs.TypeArgument%> _typeArg = this.getTypeArg();\n<%java.lang.String%> _typeRefAsString = null;\nif (_typeArg!=null)\n{\n\t_typeRefAsString=_typeArg.getTypeRefAsString();\n}\nfinal <%java.lang.String%> refName = _typeRefAsString;\n<%java.lang.String%> _modifiersAsString = this.getModifiersAsString();\nreturn (((\"type{\" + refName) + \"}\") + _modifiersAsString);'"
	 * @generated
	 */
	String getTypeRefAsString();

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * <!-- begin-model-doc -->
	 * * Helper operation to extract the static type 'C' of 'type{C}' or 'type{this[C]}'
	 * in cases of ParemterizedTypeRefs the declared type will be returned,
	 * for bound this-type-refs the actualThisTyperef.declaredtype and
	 * in all other cases null will be returned
	 * <!-- end-model-doc -->
	 * @model unique="false"
	 *        annotation="http://www.eclipse.org/emf/2002/GenModel body='<%eu.numberfour.n4js.ts.types.Type%> _switchResult = null;\n<%eu.numberfour.n4js.ts.typeRefs.TypeArgument%> _typeArg = this.getTypeArg();\nboolean _matched = false;\nif (_typeArg instanceof <%eu.numberfour.n4js.ts.typeRefs.ParameterizedTypeRef%>)\n{\n\t_matched=true;\n\t<%eu.numberfour.n4js.ts.typeRefs.TypeArgument%> _typeArg_1 = this.getTypeArg();\n\t_switchResult = ((<%eu.numberfour.n4js.ts.typeRefs.ParameterizedTypeRef%>) _typeArg_1).getDeclaredType();\n}\nif (!_matched)\n{\n\tif (_typeArg instanceof <%eu.numberfour.n4js.ts.typeRefs.BoundThisTypeRef%>)\n\t{\n\t\t_matched=true;\n\t\t<%eu.numberfour.n4js.ts.typeRefs.TypeArgument%> _typeArg_1 = this.getTypeArg();\n\t\t<%eu.numberfour.n4js.ts.typeRefs.ParameterizedTypeRef%> _actualThisTypeRef = ((<%eu.numberfour.n4js.ts.typeRefs.BoundThisTypeRef%>) _typeArg_1).getActualThisTypeRef();\n\t\t<%eu.numberfour.n4js.ts.types.Type%> _declaredType = null;\n\t\tif (_actualThisTypeRef!=null)\n\t\t{\n\t\t\t_declaredType=_actualThisTypeRef.getDeclaredType();\n\t\t}\n\t\t_switchResult = _declaredType;\n\t}\n}\nif (!_matched)\n{\n\t_switchResult = null;\n}\nreturn _switchResult;'"
	 * @generated
	 */
	Type staticType();

} // ClassifierTypeRef
