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
package com.reprezen.swagedit.openapi3.editor;

import com.reprezen.swagedit.common.editor.JsonDocument;
import com.reprezen.swagedit.openapi3.schema.OpenApi3Schema;

public class OpenApi3Document extends JsonDocument {

	public OpenApi3Document() {
		super(io.swagger.util.Yaml.mapper(), new OpenApi3Schema());
	}

}
