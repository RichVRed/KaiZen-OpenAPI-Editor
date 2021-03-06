package com.reprezen.swagedit.openapi3.validation

import com.google.common.collect.ImmutableMap
import com.reprezen.swagedit.core.json.references.JsonDocumentManager
import com.reprezen.swagedit.core.json.references.JsonReferenceFactory
import com.reprezen.swagedit.core.json.references.JsonReferenceValidator
import com.reprezen.swagedit.core.model.AbstractNode
import com.reprezen.swagedit.core.validation.Messages
import com.reprezen.swagedit.core.validation.SwaggerError
import com.reprezen.swagedit.openapi3.editor.OpenApi3Document
import com.reprezen.swagedit.openapi3.schema.OpenApi3Schema
import java.net.URI
import org.eclipse.core.resources.IMarker
import org.junit.Test

import static org.hamcrest.CoreMatchers.*
import static org.junit.Assert.*

class ValidatorTest {

	val docManager = new JsonDocumentManager() {
		override getFile(URI uri) {
			null
		}
	}
	val factory = new JsonReferenceFactory() {
		override create(AbstractNode node) {
			val reference = super.create(node)
			if (reference != null) {
				reference.documentManager = docManager
			}
			reference
		}

		override createSimpleReference(URI baseURI, AbstractNode valueNode) {
			val reference = super.createSimpleReference(baseURI, valueNode)
			if (reference != null) {
				reference.documentManager = docManager
			}
			reference
		}
	}
	val validator = new OpenApi3Validator(
		new JsonReferenceValidator(factory),
		ImmutableMap.of("http://openapis.org/v3/schema.json", new OpenApi3Schema().asJson)
	)
	val document = new OpenApi3Document(new OpenApi3Schema)

	@Test
	def void testValidationShouldPass_IfRefIsCorrectType() {
		val content = '''
			openapi: '3.0.0'
			info:
			  version: 0.0.0
			  title: Simple API
			paths:
			  /:
			    get:
			      responses:
			        '200':
			          $ref: "#/components/responses/ok"
			components:
			  responses:
			    ok:
			      description: Ok
			  schemas:
			    Foo:
			      type: object
		'''

		document.set(content)
		val errors = validator.validate(document, null as URI)
		assertEquals(0, errors.size())
	}

	@Test
	def void testValidationShouldFail_IfRefIsNotCorrectType() {
		val content = '''
			openapi: '3.0.0'
			info:
			  version: 0.0.0
			  title: Simple API
			paths:
			  /:
			    get:
			      responses:
			        '200':
			          $ref: "#/components/schemas/Foo"
			components:
			  responses:
			    ok:
			      description: Ok
			  schemas:
			    Foo:
			      type: object
		'''

		document.set(content)
		val errors = validator.validate(document, null as URI)
		assertEquals(1, errors.size())
		assertTrue(errors.map[message].forall[it.equals(Messages.error_invalid_reference_type)])
	}

	@Test
	def void testValidationShouldPass_ForArrays() {
		val content = '''
			openapi: '3.0.0'
			info:
			  version: 0.0.0
			  title: Simple API
			paths:
			  {}
			components:
			  schemas:
			    Foo:
			      type: array
			      items:
			        $ref: "#/components/schemas/Bar"
			    Bar:
			      type: object
		'''

		document.set(content)
		val errors = validator.validate(document, null as URI)
		assertEquals(0, errors.size())
	}

	@Test
	def void testValidationShouldFail_ForArraysInWrongItemType() {
		val content = '''
			openapi: '3.0.0'
			info:
			  version: 0.0.0
			  title: Simple API
			paths:
			  {}
			components:
			  responses:
			    ok:
			      description: Ok
			  schemas:
			    Foo:
			      type: array
			      items:
			        $ref: "#/components/responses/ok"
			    Bar:
			      type: object
		'''

		document.set(content)
		val errors = validator.validate(document, null as URI)
		assertEquals(1, errors.size())
		assertTrue(errors.map[message].forall[it.equals(Messages.error_invalid_reference_type)])
	}

//  Disable until remote references are validated
//	@Test
	def void testValidationShouldFail_ForInvalidPointers() {
		val content = '''
			openapi: "3.0.0"
			info:
			  title: Broken links Object
			  version: "1.0.0"
			  
			paths: {}
			components: 
			  schemas:
			  
			    MyType1:
			      properties:
			        property:
			          $ref: "INVALID"
		'''

		document.set(content)
		val errors = validator.validate(document, null as URI)
		assertEquals(2, errors.size())
		assertThat(
			errors,
			hasItems(
				new SwaggerError(13, IMarker.SEVERITY_ERROR, Messages.error_invalid_reference_type),
				new SwaggerError(13, IMarker.SEVERITY_WARNING, Messages.error_missing_reference)
			)
		)
	}

	@Test
	def void testValidationShouldFail_refNotJson() {
		val content = '''
			openapi: "3.0.0"
			info:
			  title: Broken links Object
			  version: "1.0.0"
			  
			paths: {}
			components: 
			  schemas:
			  
			    MyType1:
			      properties:
			        property:
			          $ref: "https://www.reprezen.com/#"
		'''

		document.set(content)
		val errors = validator.validate(document, null as URI)
		// Update with #353 Validation of external $ref property values should show error on unexpected object type"
//		assertEquals(2, errors.size())
		assertThat(
			errors,
			hasItems(
//				new SwaggerError(13, IMarker.SEVERITY_ERROR, Messages.error_invalid_reference_type),
				new SwaggerError(13, IMarker.SEVERITY_WARNING, Messages.error_missing_reference)
			)
		)
	}

	@Test
	def void testValidationShouldFail_pathInNotJson() {
		val content = '''
			openapi: "3.0.0"
			info:
			  title: Broken links Object
			  version: "1.0.0"
			  
			paths: {}
			components: 
			  schemas:
			  
			    MyType1:
			      properties:
			        property:
			          $ref: "https://www.reprezen.com/#components/schemas/Pet"
		'''

		document.set(content)
		val errors = validator.validate(document, null as URI)
		// Update with #353 Validation of external $ref property values should show error on unexpected object type"
//		assertEquals(2, errors.size())
		assertThat(
			errors,
			hasItems(
//				new SwaggerError(13, IMarker.SEVERITY_ERROR, Messages.error_invalid_reference_type),
				new SwaggerError(13, IMarker.SEVERITY_ERROR, Messages.error_invalid_reference)
			)
		)
	}

	@Test
	def void testValidationShouldPass_LinksOperationIdIsOperation() {
		val content = '''
			openapi: "3.0.0"
			info:
			  title: Broken links Object
			  version: "1.0.0"
			paths:
			  /:
			    get:
			      operationId: opId
			      responses:
			        200:
			          description: Ok
			components: 
			  links:
			    myLink:
			      operationId: opId
		'''

		document.set(content)
		val errors = validator.validate(document, null as URI)
		assertEquals(0, errors.size())
	}

	@Test
	def void testValidationShouldFail_LinksOperationIdNotOperation() {
		val content = '''
			openapi: "3.0.0"
			info:
			  title: Broken links Object
			  version: "1.0.0"
			paths:
			  /:
			    get:
			      operationId: opId
			      responses:
			        200:
			          description: Ok
			components: 
			  links:
			    myLink:
			      operationId: fail
		'''

		document.set(content)
		val errors = validator.validate(document, null as URI)
		assertEquals(1, errors.size())
	}

	@Test
	def void testValidationShouldPass_LinksOperationRefIsOperation() {
		val content = '''
			openapi: "3.0.0"
			info:
			  title: Broken links Object
			  version: "1.0.0"
			paths:
			  /:
			    get:
			      operationId: opId
			      responses:
			        200:
			          description: Ok
			components: 
			  links:
			    myLink:
			      operationRef: "#/paths/~1/get/"
		'''

		document.set(content)
		val errors = validator.validate(document, null as URI)
		assertEquals(0, errors.size())
	}

	@Test
	def void testValidationShouldFail_LinksOperationRefNotOperation() {
		val content = '''
			openapi: "3.0.0"
			info:
			  title: Broken links Object
			  version: "1.0.0"
			paths:
			  /:
			    get:
			      operationId: opId
			      responses:
			        200:
			          description: Ok
			components: 
			  links:
			    myLink:
			      operationRef: "#/paths/~1"
		'''

		document.set(content)
		val errors = validator.validate(document, null as URI)
		assertEquals(1, errors.size())
		assertThat(errors, hasItems(
				new SwaggerError(15, IMarker.SEVERITY_ERROR, Messages.error_invalid_reference_type)		
			)
		)
	}

	@Test
	def void testValidationShouldPass_SecuritySchemes() {
		val content = '''
			openapi: "3.0.0"
			info:
			  title: Broken links Object
			  version: "1.0.0"
			paths:
			  /:
			    get:
			      operationId: opId
			      security:
			        - open:
			          - a:a
			      responses:
			        200:
			          description: Ok
			components: 
			  securitySchemes:
			    open:
			      type: a
		'''

		document.set(content)
		val errors = validator.validate(document, null as URI)
		assertEquals(0, errors.size())
	}
	
	@Test
	def void testValidationShouldFail_SecuritySchemes() {
		val content = '''
			openapi: "3.0.0"
			info:
			  title: Broken links Object
			  version: "1.0.0"
			paths:
			  /:
			    get:
			      operationId: opId
			      security:
			        - foo:
			          - a:a
			      responses:
			        200:
			          description: Ok
			components: 
			  securitySchemes:
			    open:
			      type: a
		'''

		document.set(content)
		val errors = validator.validate(document, null as URI)
		assertEquals(1, errors.size())
		assertThat(errors, hasItems(
				new SwaggerError(10, IMarker.SEVERITY_ERROR, Messages.error_invalid_reference_type)		
			)
		)
	}
}
