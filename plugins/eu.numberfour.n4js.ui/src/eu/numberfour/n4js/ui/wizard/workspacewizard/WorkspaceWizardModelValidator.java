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
package eu.numberfour.n4js.ui.wizard.workspacewizard;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import org.eclipse.emf.common.util.URI;

import com.google.common.base.Optional;
import com.google.inject.Inject;

import eu.numberfour.n4js.projectModel.IN4JSCore;
import eu.numberfour.n4js.projectModel.IN4JSProject;

/**
 * An abstract wizard model validator for {@link WorkspaceWizardModel}s
 *
 * Subclasses may implement additional validation logic by overriding {@link #prepare()} and {@link #validate()}.
 *
 * As the {@link #setModel(WorkspaceWizardModel)} method remains exposed when subclassing, subclasses need to handle the
 * case of a too generic model themselves. This means whenever a validator isn't meant to also validate
 * {@link WorkspaceWizardModel}s, it needs to manually prohibit the use of this method.
 *
 * @author luca.beurer-kellner - Initial contribution and API
 */
public abstract class WorkspaceWizardModelValidator {

	/**
	 * Error Messages for model validation of the {@link WorkspaceWizardModel}
	 */
	@SuppressWarnings("javadoc")
	protected static class ErrorMessages {

		// Project errors
		public static final String PROJECT_DOES_NOT_EXIST = "The given project does not exist";
		public static final String INVALID_PROJECT = "Not a valid project";
		public static final String PROJECT_MUST_NOT_BE_EMPTY = "The project field must not be empty";

		// Source folder errors
		public static final String SOURCE_FOLDER_MUST_NOT_BE_EMPTY = "The source folder field must not be empty";
		public static final String SOURCE_FOLDER_IS_NOT_A_VALID_FOLDER_NAME = "The source folder is not a valid folder name";

		// Module specifier errors
		public static final String MODULE_SPECIFIER_MUST_NOT_BE_EMPTY = "The module specifier field must not be empty";
		public static final String INVALID_MODULE_SPECIFIER_MUST_NOT_BEGIN_WITH = "Invalid module specifier. Must not begin with a \"/\" ";
		public static final String INVALID_MODULE_SPECIFIER_EMPTY_PATH_SEGMENT = "Invalid module specifier. Empty path segment:";
		public static final String INVALID_MODULE_SPECIFIER_INVALID_SEGMENT = "Invalid module specifier. Invalid segment. ";
	}

	/**
	 * Helper type for a validation results.
	 */
	public static class ValidationResult {

		/** A success status. Valid and without any message. */
		public static final ValidationResult SUCCESS = new ValidationResult();

		/**
		 * True if model content is valid
		 */
		public final boolean valid;
		/**
		 * Contains error message if result is negative
		 */
		public final String errorMessage;

		/**
		 * Initiate a successful validation result. (No errors)
		 */
		public ValidationResult() {
			this.valid = true;
			this.errorMessage = "";
		}

		/**
		 * Initiate a invalid validation result
		 *
		 * @param errorMessage
		 *            Error message to report
		 */
		public ValidationResult(String errorMessage) {
			this.valid = false;
			this.errorMessage = errorMessage;
		}

	}

	/**
	 * An exception to be thrown by validating logic in case of a validation error.
	 *
	 * @author luca.beurer-kellner - Initial contribution and API
	 */
	protected static class ValidationExcep extends Exception {

		private final String propertyName = "";

		/**
		 * Creates a new ValidationException with given error message and model property name
		 *
		 * @param message
		 *            The error message
		 */
		public ValidationExcep(String message) {
			super(message);
		}

		/**
		 * Creates a new ValidationException with given error message and model property name.
		 *
		 * @param message
		 *            The error message
		 * @param propertyName
		 *            The model property name
		 */
		public ValidationExcep(String message, String propertyName) {
			super(message);
		}

		/** The name of the property of the model for which the validation failed */
		public String getPropertyName() {
			return propertyName;
		}

	}

	/** The validation result property constant */
	public static final String VALIDATION_RESULT = "validationResult";
	/** The validity of the project property */
	public static final String PROJECT_PROPERTY_VALID = "projectValid";
	/** The validity of the source folder property */
	public static final String SOURCE_FOLDER_PROPERTY_VALID = "sourceFolderValid";

	private static final ValidationResult NO_MODEL_VALIDATION_RESULT = new ValidationResult("No model set");

	private ValidationResult validationResult;

	private boolean projectValid = false;
	private boolean sourceFolderValid = false;

	WorkspaceWizardModel model;

	@Inject
	IN4JSCore n4jsCore;

	/**
	 * PropertyChangeListenerSupport
	 */
	private final PropertyChangeSupport changeSupport = new PropertyChangeSupport(this);

	/**
	 * @param listener
	 *            listener to be called on every change of any property
	 */
	public void addPropertyChangeListener(PropertyChangeListener listener) {
		this.changeSupport.addPropertyChangeListener(listener);
	}

	/**
	 * @param listener
	 *            remove listener
	 */
	public void removePropertyChangeListener(PropertyChangeListener listener) {
		this.changeSupport.removePropertyChangeListener(listener);
	}

	/**
	 * @param propertyName
	 *            bean name of the property
	 * @param newValue
	 *            new value of the property
	 * @param oldValue
	 *            old value of the property
	 */
	protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
		this.changeSupport.firePropertyChange(propertyName, oldValue, newValue);
	}

	/**
	 * @return The model currently validated
	 */
	protected WorkspaceWizardModel getModel() {
		return model;
	}

	/**
	 * Set the model to validate
	 *
	 * @param model
	 *            The new model to validate
	 */
	public void setModel(WorkspaceWizardModel model) {
		this.model = model;

		// Reset state and revalidate
		this.setSourceFolderValid(false);
		this.setProjectValid(false);

		this.validate();
	}

	/**
	 * Run the validator.
	 *
	 * @return The validation result
	 */
	public final ValidationResult validate() {
		if (this.model == null) {
			setValidationResult(NO_MODEL_VALIDATION_RESULT);
			return NO_MODEL_VALIDATION_RESULT;
		}

		boolean success = true;

		// Preprocess the model
		prepare();

		// Run the validation methods
		try {
			run();
		} catch (ValidationExcep e) {
			setValidationResult(new ValidationResult(e.getMessage()));
			success = false;
		} catch (Exception e) {
			// 'Throwthrough' all other exceptions to explicitly abort validation
			throw e;
		}

		if (success) {
			setValidationResult(ValidationResult.SUCCESS);
		}

		return validationResult;
	}

	/**
	 * This method is invoked on every validation. It is used to delegate validation to specific validation methods. It
	 * can be overridden by subclasses to add custom validation logic.
	 *
	 * @throws ValidationExcep
	 *             Exception to be thrown by validating methods on validation issues
	 */
	protected void run() throws ValidationExcep {
		validateProject();
		validateSourceFolder();
		validateModuleSpecifier();
	}

	/**
	 * This method is invoked before every validation. It can be used to automatically preprocess the model.
	 */
	abstract protected void prepare();

	/**
	 * @return The last validation result
	 */
	public ValidationResult getValidationResult() {
		return validationResult;
	}

	private void setValidationResult(ValidationResult validationResult) {
		this.firePropertyChange(VALIDATION_RESULT, this.validationResult, this.validationResult = validationResult);
	}

	/**
	 * Project property constraints
	 */
	private void validateProject() throws ValidationExcep {
		this.setProjectValid(false);

		// 1. It must not be empty
		if (getModel().getProject().toString().trim().equals("")) {
			throw new ValidationExcep(ErrorMessages.PROJECT_MUST_NOT_BE_EMPTY,
					WorkspaceWizardModel.PROJECT_PROPERTY);
		}

		// 2. It is a path of a valid project in the current workspace
		URI projectURI = URI.createPlatformResourceURI(getModel().getProject().toString(), true);
		Optional<? extends IN4JSProject> n4jsProject = n4jsCore.findProject(projectURI);
		if (!n4jsProject.isPresent()) {
			throw new ValidationExcep(ErrorMessages.INVALID_PROJECT, WorkspaceWizardModel.PROJECT_PROPERTY);
		} else if (!n4jsProject.get().exists()) {
			throw new ValidationExcep(ErrorMessages.PROJECT_DOES_NOT_EXIST, WorkspaceWizardModel.PROJECT_PROPERTY);
		} else {
			// The path points to a resource inside the project
			if (!n4jsProject.get().getLocation().equals(projectURI)) {
				throw new ValidationExcep(ErrorMessages.INVALID_PROJECT + n4jsProject.get().getLocation(),
						WorkspaceWizardModel.PROJECT_PROPERTY);
			}
		}

		this.setProjectValid(true);
	}

	private void validateSourceFolder() throws ValidationExcep {
		this.setSourceFolderValid(false);

		// 1. The source folder property must not be empty
		String sourceFolder = getModel().getSourceFolder().toString();

		if (sourceFolder.equals("")) {
			throw new ValidationExcep(ErrorMessages.SOURCE_FOLDER_MUST_NOT_BE_EMPTY,
					WorkspaceWizardModel.SOURCE_FOLDER_PROPERTY);
		}

		// 2. The folder must be a valid folder name
		if (!isValidFolderName(sourceFolder)) {
			throw new ValidationExcep(
					ErrorMessages.SOURCE_FOLDER_IS_NOT_A_VALID_FOLDER_NAME,
					WorkspaceWizardModel.SOURCE_FOLDER_PROPERTY);
		}

		this.setSourceFolderValid(true);
	}

	private void validateModuleSpecifier() throws ValidationExcep {

		String effectiveModuleSpecifier = getModel().getModuleSpecifier();

		// 1. The module specifier property must not be empty
		if (effectiveModuleSpecifier.trim().equals("")) {
			throw new ValidationExcep(ErrorMessages.MODULE_SPECIFIER_MUST_NOT_BE_EMPTY,
					WorkspaceWizardModel.MODULE_SPECIFIER_PROPERTY);
		}

		// 2. The module specifier is properly formed
		String[] moduleSpecifierSegments = effectiveModuleSpecifier.split("/", -1);
		String validatedSpecifier = "";
		for (int i = 0; i < moduleSpecifierSegments.length; i++) {

			String segment = moduleSpecifierSegments[i];
			boolean last = i == moduleSpecifierSegments.length - 1;
			boolean first = i == 0;
			boolean empty = segment.trim().equals("");

			// First segment is empty that means the specifier begins with a '/'
			if (first && empty) {
				throw new ValidationExcep(ErrorMessages.INVALID_MODULE_SPECIFIER_MUST_NOT_BEGIN_WITH
						+ errorPointer(effectiveModuleSpecifier,
								validatedSpecifier),
						WorkspaceWizardModel.MODULE_SPECIFIER_PROPERTY);
			}
			// Segment is empty and not the last one
			if (empty && !last) {
				throw new ValidationExcep(ErrorMessages.INVALID_MODULE_SPECIFIER_EMPTY_PATH_SEGMENT
						+ errorPointer(effectiveModuleSpecifier, validatedSpecifier),
						WorkspaceWizardModel.MODULE_SPECIFIER_PROPERTY);
			}
			// The segment is an invalid folder name, not the last segment and not empty
			if (!isValidFolderName(segment) && !(empty && last)) {
				throw new ValidationExcep(ErrorMessages.INVALID_MODULE_SPECIFIER_INVALID_SEGMENT
						+ errorPointer(effectiveModuleSpecifier, validatedSpecifier),
						WorkspaceWizardModel.MODULE_SPECIFIER_PROPERTY);
			} else {
				validatedSpecifier += segment;
			}
			validatedSpecifier += "/";
		}

	}

	/**
	 * @return True if the project property is valid
	 */
	public boolean getProjectValid() {
		return projectValid;
	}

	/**
	 *
	 * @param projectValid
	 *            The new validity of the project property
	 */
	private void setProjectValid(boolean projectValid) {
		this.firePropertyChange(PROJECT_PROPERTY_VALID, this.projectValid, this.projectValid = projectValid);
	}

	/**
	 * @return True if the source folder property is valid
	 */
	public boolean getSourceFolderValid() {
		return sourceFolderValid;
	}

	/**
	 *
	 * @param sourceFolderValid
	 *            The new validity of the source folder property
	 */
	private void setSourceFolderValid(boolean sourceFolderValid) {
		this.firePropertyChange(SOURCE_FOLDER_PROPERTY_VALID, this.sourceFolderValid,
				this.sourceFolderValid = sourceFolderValid);
	}

	/**
	 * Check whether name is a valid folder name.
	 *
	 * For now this means: Letter or underscore in the beginning, no dot at the end or beginning
	 *
	 * @param name
	 *            Name to check
	 * @return valid state
	 */
	private static boolean isValidFolderName(String name) {
		return name.matches("[a-zA-z_](([\\.][a-zA-z_0-9\\-])|[a-zA-z_0-9\\-])*");
	}

	private static String errorPointer(String original, String validated) {
		if (validated.length() > original.length() - 1) {
			return validated + "̫";
		}
		return validated + original.charAt(validated.length()) + "̫" + original.substring(validated.length() + 1);
	}
}
