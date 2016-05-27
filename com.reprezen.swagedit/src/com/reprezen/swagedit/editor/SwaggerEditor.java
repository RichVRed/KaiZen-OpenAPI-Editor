/*******************************************************************************
 * Copyright (c) 2016 ModelSolv, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    ModelSolv, Inc. - initial API and implementation and/or initial documentation
 *******************************************************************************/
package com.reprezen.swagedit.editor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.dadacoalition.yedit.YEditLog;
import org.dadacoalition.yedit.editor.IDocumentIdleListener;
import org.dadacoalition.yedit.editor.YEdit;
import org.dadacoalition.yedit.editor.YEditSourceViewerConfiguration;
import org.dadacoalition.yedit.preferences.PreferenceConstants;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.IVerticalRuler;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.text.source.projection.ProjectionAnnotation;
import org.eclipse.jface.text.source.projection.ProjectionAnnotationModel;
import org.eclipse.jface.text.source.projection.ProjectionSupport;
import org.eclipse.jface.text.source.projection.ProjectionViewer;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.yaml.snakeyaml.error.YAMLException;

import com.reprezen.swagedit.Activator;
import com.reprezen.swagedit.validation.SwaggerError;
import com.reprezen.swagedit.validation.Validator;

/**
 * SwagEdit editor.
 * 
 */
public class SwaggerEditor extends YEdit {

	public static final String ID = "com.reprezen.swagedit.editor";

	private final Validator validator = new Validator();
	private ProjectionSupport projectionSupport;
	private Annotation[] oldAnnotations;
	private ProjectionAnnotationModel annotationModel;
	private Composite topPanel;
	private SwaggerSourceViewerConfiguration sourceViewerConfiguration;

	private final IDocumentListener changeListener = new IDocumentListener() {
		@Override
		public void documentAboutToBeChanged(DocumentEvent event) {}
		@Override
		public void documentChanged(DocumentEvent event) {
			if (event.getDocument() instanceof SwaggerDocument) {
				final SwaggerDocument document = (SwaggerDocument) event.getDocument();

				Display.getCurrent().asyncExec(new Runnable() {
					@Override
					public void run() {
						document.onChange();
						validate();
					}
				});
			}
		}
	};

	/*
	 * This listener is added to the preference store when the editor is initialized.
	 * It listens to changes to color preferences. Once a color change happens, the editor
	 * is re-initialize. 
	 */
	private final IPropertyChangeListener preferenceChangeListener = new IPropertyChangeListener() {

		private final List<String> preferenceKeys = new ArrayList<>();
		{
			preferenceKeys.add(PreferenceConstants.COLOR_COMMENT);
			preferenceKeys.add(PreferenceConstants.BOLD_COMMENT);
			preferenceKeys.add(PreferenceConstants.ITALIC_COMMENT);
			preferenceKeys.add(PreferenceConstants.UNDERLINE_COMMENT);

			preferenceKeys.add(PreferenceConstants.COLOR_KEY);
			preferenceKeys.add(PreferenceConstants.BOLD_KEY);
			preferenceKeys.add(PreferenceConstants.ITALIC_KEY);
			preferenceKeys.add(PreferenceConstants.UNDERLINE_KEY);

			preferenceKeys.add(PreferenceConstants.COLOR_SCALAR);
			preferenceKeys.add(PreferenceConstants.BOLD_SCALAR);
			preferenceKeys.add(PreferenceConstants.ITALIC_SCALAR);
			preferenceKeys.add(PreferenceConstants.UNDERLINE_SCALAR);

			preferenceKeys.add(PreferenceConstants.COLOR_DEFAULT);
			preferenceKeys.add(PreferenceConstants.BOLD_DEFAULT);
			preferenceKeys.add(PreferenceConstants.ITALIC_DEFAULT);
			preferenceKeys.add(PreferenceConstants.UNDERLINE_DEFAULT);

			preferenceKeys.add(PreferenceConstants.COLOR_DOCUMENT);
			preferenceKeys.add(PreferenceConstants.BOLD_DOCUMENT);
			preferenceKeys.add(PreferenceConstants.ITALIC_DOCUMENT);
			preferenceKeys.add(PreferenceConstants.UNDERLINE_DOCUMENT);

			preferenceKeys.add(PreferenceConstants.COLOR_ANCHOR);
			preferenceKeys.add(PreferenceConstants.BOLD_ANCHOR);
			preferenceKeys.add(PreferenceConstants.ITALIC_ANCHOR);
			preferenceKeys.add(PreferenceConstants.UNDERLINE_ANCHOR);

			preferenceKeys.add(PreferenceConstants.COLOR_ALIAS);
			preferenceKeys.add(PreferenceConstants.BOLD_ALIAS);
			preferenceKeys.add(PreferenceConstants.ITALIC_ALIAS);
			preferenceKeys.add(PreferenceConstants.UNDERLINE_ALIAS);

			preferenceKeys.add(PreferenceConstants.COLOR_TAG_PROPERTY);
			preferenceKeys.add(PreferenceConstants.BOLD_TAG_PROPERTY);
			preferenceKeys.add(PreferenceConstants.ITALIC_TAG_PROPERTY);
			preferenceKeys.add(PreferenceConstants.UNDERLINE_TAG_PROPERTY);

			preferenceKeys.add(PreferenceConstants.COLOR_INDICATOR_CHARACTER);
			preferenceKeys.add(PreferenceConstants.BOLD_INDICATOR_CHARACTER);
			preferenceKeys.add(PreferenceConstants.ITALIC_INDICATOR_CHARACTER);
			preferenceKeys.add(PreferenceConstants.UNDERLINE_INDICATOR_CHARACTER);

			preferenceKeys.add(PreferenceConstants.COLOR_CONSTANT);
			preferenceKeys.add(PreferenceConstants.BOLD_CONSTANT);
			preferenceKeys.add(PreferenceConstants.ITALIC_CONSTANT);
			preferenceKeys.add(PreferenceConstants.UNDERLINE_CONSTANT);
		}

		@Override
		public void propertyChange(PropertyChangeEvent event) {
			if (preferenceKeys.contains(event.getProperty())) {
				if(getSourceViewer() instanceof SourceViewer){
					((SourceViewer) getSourceViewer()).unconfigure();
					initializeEditor();
					getSourceViewer().configure(sourceViewerConfiguration);
				}
			}
		}
	};

	public SwaggerEditor() {
		super();
		setDocumentProvider(new SwaggerDocumentProvider());
	}

	@Override
	protected YEditSourceViewerConfiguration createSourceViewerConfiguration() {
		sourceViewerConfiguration = new SwaggerSourceViewerConfiguration();
		sourceViewerConfiguration.setEditor(this);
		return sourceViewerConfiguration;
	}

	@Override
	protected void doSetInput(IEditorInput input) throws CoreException {
		super.doSetInput(input);

		IDocumentProvider provider = getDocumentProvider();
		IDocument document = provider.getDocument(input);
		if (document != null) {
			document.addDocumentListener(changeListener);
		}
	}

	public ProjectionViewer getProjectionViewer() {
		return (ProjectionViewer) getSourceViewer();
	}

	@Override
	public void createPartControl(Composite parent) {
		super.createPartControl(parent);

		ProjectionViewer viewer = getProjectionViewer();

		projectionSupport = new ProjectionSupport(viewer, getAnnotationAccess(), getSharedColors());
		projectionSupport.install();

		// turn projection mode on
		viewer.doOperation(ProjectionViewer.TOGGLE);

		annotationModel = viewer.getProjectionAnnotationModel();
		
		Activator.getDefault().getPreferenceStore().addPropertyChangeListener(preferenceChangeListener);
	}

	@Override
	public void dispose() {
		super.dispose();

		Activator.getDefault().getPreferenceStore().removePropertyChangeListener(preferenceChangeListener);
	}

    @Override
    protected ISourceViewer createSourceViewer(Composite parent, IVerticalRuler ruler, int styles) {
        Composite composite = new Composite(parent, SWT.NONE);
        GridLayout compositeLayout = new GridLayout(1, false);
        compositeLayout.marginHeight = 0;
        compositeLayout.marginWidth = 0;
        compositeLayout.horizontalSpacing = 0;
        compositeLayout.verticalSpacing = 0;
        composite.setLayout(compositeLayout);

        topPanel = new Composite(composite, SWT.NONE);
        topPanel.setLayout(new StackLayout());
        topPanel.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

        Composite editorComposite = new Composite(composite, SWT.NONE);
        editorComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        FillLayout fillLayout = new FillLayout(SWT.VERTICAL);
        fillLayout.marginHeight = 0;
        fillLayout.marginWidth = 0;
        fillLayout.spacing = 0;
        editorComposite.setLayout(fillLayout);

        ISourceViewer result = doCreateSourceViewer(editorComposite, ruler, styles);

        return result;
        
    }
    
    /**
     * SwaggerEditor provides additional hidden panel on top of the source viewer, where external integrations can put
     * their UI.
     * <p/>
     * The panel is only a placeholder, that is:
     * <ul>
     * <li>it is not visible by default</li>
     * <li>it has a {@link StackLayout} and expect single composite to be created per contribution</li>
     * <li>if there are more than one contributors, it is their responsibility to manage {@link StackLayout#topControl}</li>
     * </ul>
     */
    public Composite getTopPanel() {
        return topPanel;
    }

	protected ISourceViewer doCreateSourceViewer(Composite parent, IVerticalRuler ruler, int styles) {
		ISourceViewer viewer = new ProjectionViewer(parent, ruler, getOverviewRuler(), isOverviewRulerVisible(),
				styles);
		getSourceViewerDecorationSupport(viewer);
		return viewer;
	}

	public void updateFoldingStructure(List<Position> positions) {
		final Map<Annotation, Position> newAnnotations = new HashMap<Annotation, Position>();
		for (Position position: positions) {
			newAnnotations.put(new ProjectionAnnotation(), position);
		}

		annotationModel.modifyAnnotations(oldAnnotations, newAnnotations, null);
		oldAnnotations = newAnnotations.keySet().toArray(new Annotation[0]);
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		super.doSave(monitor);
		validate();
	}

	@Override
	public void doSaveAs() {
		super.doSaveAs();
		validate();
	}

	protected void validate() {
		IEditorInput editorInput = getEditorInput();
		IDocument document = getDocumentProvider().getDocument(getEditorInput());

		// if the file is not part of a workspace it does not seems that it is a
		// IFileEditorInput
		// but instead a FileStoreEditorInput. Unclear if markers are valid for
		// such files.
		if (!(editorInput instanceof IFileEditorInput)) {
			YEditLog.logError("Marking errors not supported for files outside of a project.");
			YEditLog.logger.info("editorInput is not a part of a project.");
			return;
		}
		if (document instanceof SwaggerDocument) {
			IFile file = ((IFileEditorInput) editorInput).getFile();
			clearMarkers(file);
			validateYaml(file, (SwaggerDocument) document);
			validateSwagger(file, (SwaggerDocument) document);
		}
	}

	protected void clearMarkers(IFile file) {
		int depth = IResource.DEPTH_INFINITE;
		try {
			file.deleteMarkers(IMarker.PROBLEM, true, depth);
		} catch (CoreException e) {
			YEditLog.logException(e);
			YEditLog.logger.warning("Failed to delete markers:\n" + e.toString());
		}
	}

	protected void validateYaml(IFile file, SwaggerDocument document) {
		if (document.getYamlError() instanceof YAMLException) {
			addMarker(new SwaggerError((YAMLException) document.getYamlError()), file, document);
		}
	}

	protected void validateSwagger(IFile file, SwaggerDocument document) {
		final Set<SwaggerError> errors = validator.validate(document);

		for (SwaggerError error : errors) {
			addMarker(error, file, document);
		}
	}

	private IMarker addMarker(SwaggerError error, IFile target, IDocument document) {
		IMarker marker = null;
		try {
			marker = target.createMarker(IMarker.PROBLEM);
			marker.setAttribute(IMarker.SEVERITY, error.getLevel());
			marker.setAttribute(IMarker.MESSAGE, error.getMessage());
			marker.setAttribute(IMarker.LINE_NUMBER, error.getLine());
		} catch (CoreException e) {
			YEditLog.logException(e);
			YEditLog.logger.warning("Failed to create marker for syntax error: \n" + e.toString());
		}

		return marker;
	}

	public void redrawViewer() {
		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				if (getSourceViewer() != null) {
					getSourceViewer().getTextWidget().redraw();
				}
			}
		});
	}
	
	@Override
	public void addDocumentIdleListener(IDocumentIdleListener listener) {
		super.addDocumentIdleListener(listener);
	}

}