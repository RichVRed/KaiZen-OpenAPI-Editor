package com.reprezen.swagedit.openapi3.assist;
/*******************************************************************************
 *  Copyright (c) 2016 ModelSolv, Inc. and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *  
 *  Contributors:
 *     ModelSolv, Inc. - initial API and implementation and/or initial documentation
 *******************************************************************************/
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.templates.ContextTypeRegistry;
import org.eclipse.jface.text.templates.TemplateContextType;
import org.eclipse.jface.text.templates.persistence.TemplateStore;

import com.reprezen.swagedit.core.assist.JsonContentAssistProcessor;
import com.reprezen.swagedit.core.assist.JsonProposalProvider;
import com.reprezen.swagedit.core.model.Model;
import com.reprezen.swagedit.core.schema.CompositeSchema;
import com.reprezen.swagedit.openapi3.Activator;
import com.reprezen.swagedit.openapi3.assist.ext.CallbacksContentAssistExt;
import com.reprezen.swagedit.openapi3.assist.ext.SchemaFormatContentAssistExt;
import com.reprezen.swagedit.openapi3.assist.ext.SchemaTypeContentAssistExt;
import com.reprezen.swagedit.openapi3.templates.OpenApi3ContextType;

public class OpenApi3ContentAssistProcessor extends JsonContentAssistProcessor {

	public OpenApi3ContentAssistProcessor(ContentAssistant ca) {
        super(ca, new JsonProposalProvider(new CallbacksContentAssistExt(), new SchemaTypeContentAssistExt(),
                new SchemaFormatContentAssistExt()),
                new OpenApi3ReferenceProposalProvider());
	}

    public OpenApi3ContentAssistProcessor(ContentAssistant ca, CompositeSchema schema) {
        super(ca, new JsonProposalProvider(new CallbacksContentAssistExt(), new SchemaTypeContentAssistExt(),
                new SchemaFormatContentAssistExt()), new OpenApi3ReferenceProposalProvider(schema));
    }

	@Override
	protected TemplateStore getTemplateStore() {
        return Activator.getDefault().getTemplateStore();
	}

	@Override
	protected ContextTypeRegistry getContextTypeRegistry() {
        return Activator.getDefault().getContextTypeRegistry();
	}

    @Override
    protected String getContextTypeId(Model model, String path) {
        TemplateContextType contextType = OpenApi3ContextType.getContextType(model, path);
        return contextType != null ? contextType.getId() : null;
    }

}
