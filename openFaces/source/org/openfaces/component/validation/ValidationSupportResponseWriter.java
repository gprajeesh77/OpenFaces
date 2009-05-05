/*
 * OpenFaces - JSF Component Library 2.0
 * Copyright (C) 2007-2009, TeamDev Ltd.
 * licensing@openfaces.org
 * Unless agreed in writing the contents of this file are subject to
 * the GNU Lesser General Public License Version 2.1 (the "LGPL" License).
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * Please visit http://openfaces.org/licensing/ for more details.
 */
package org.openfaces.component.validation;

import org.openfaces.util.RenderingUtil;
import org.openfaces.util.ResourceUtil;
import org.openfaces.renderkit.validation.BaseMessageRenderer;
import org.openfaces.util.ConverterUtil;
import org.openfaces.util.Log;
import org.openfaces.validator.AbstractClientValidator;
import org.openfaces.validator.ClientValidatorUtil;

import javax.faces.application.FacesMessage;
import javax.faces.component.EditableValueHolder;
import javax.faces.component.UIComponent;
import javax.faces.component.UIForm;
import javax.faces.component.UIMessage;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Pavel Kaplin
 */
public class ValidationSupportResponseWriter extends ResponseWriter {
    private static final String[] ELEMENTS_ALLOWED_RENDER_AFTER_THEIR_START = new String[]{
            "tr", "table", "colgroup", "select", "textarea", "option", "script", "optgroup", "map", "fieldset", "frameset"
    };
    private static final String[] ELEMENTS_ALLOW_RENDERER_AFTER_THEIR_END = new String[]{
            "tr", "td", "th", "col", "colgroup", "caption", "thead", "tfoot", "tbody", "legend",
            "optgroup", "area", "frame"
    };
    private static final String[] ELEMENTS_ALLOWED_FOR_ID = new String[]{
            "input", "textarea", "select"
    };

    private ResponseWriter writer;
    private int bubbleIndex;
    private StringWriter validationScriptWriter;
    private List<String> validationScripts;
    private Set<String> formsHaveOnSubmitRendered;
    private boolean clientIdRendered;
    private String clientId;
    private boolean processingValidation;
    private String element;
    private ValidationProcessor validationProcessor;

    public ValidationSupportResponseWriter(ResponseWriter writer) {
        this.writer = writer;
        formsHaveOnSubmitRendered = new HashSet<String>();
    }

    public ValidationSupportResponseWriter(ResponseWriter writer, ValidationSupportResponseWriter parent) {
        this.writer = writer;
        bubbleIndex = parent.bubbleIndex;
        validationScriptWriter = parent.validationScriptWriter;
        validationScripts = parent.validationScripts;
        formsHaveOnSubmitRendered = parent.formsHaveOnSubmitRendered;
        processingValidation = parent.processingValidation;
        element = parent.element;
    }

    public String getContentType() {
        return writer.getContentType();
    }

    public String getCharacterEncoding() {
        return writer.getCharacterEncoding();
    }

    public void write(char cbuf[], int off, int len) throws IOException {
        flushValidationScriptInElement();
        writer.write(cbuf, off, len);
    }

    public void flush() throws IOException {
        flushValidationScriptInElement();
        writer.flush();
    }

    public void close() throws IOException {
        flushValidationScriptInElement();
        writer.close();
    }

    public void startDocument() throws IOException {
        writer.startDocument();

    }

    public void endDocument() throws IOException {
        flushValidationScriptInElement();
        writer.endDocument();
    }

    public void startElement(String name, UIComponent component) throws IOException {
        /* IMPORTANT: THIS METHOD AND ALL ITS CALLEES IS A PERFORMANCE BOTTLENECK.
MODIFY WITH CARE. ENSURE MINIMAL EXECUTION TIME AND AMOUNT OF OUTPUT */
        flushValidationScriptInElement();
        FacesContext context = FacesContext.getCurrentInstance();
        if (!processingValidation && isValidationNeeded(context, component)) {
            ValidationProcessor processor = getValidationProcessor(context);
            if (processor != null) {
                boolean needProcessGlobalMessages = !processor.isGlobalMessagesProcessed();
                boolean needProcessEditableValueHolder = component instanceof EditableValueHolder && component.isRendered();
                if (needProcessGlobalMessages || needProcessEditableValueHolder) {
                    processingValidation = true;

                    if (validationScriptWriter == null)
                        validationScriptWriter = new StringWriter();

                    ResponseWriter responseWriter = context.getResponseWriter();
                    ResponseWriter clonedResponseWriter = cloneWithWriter(validationScriptWriter);
                    context.setResponseWriter(clonedResponseWriter);
                    List<String> prevRenderedJsLinks = substituteRenderedJsLinks(context);

                    if (needProcessGlobalMessages)
                        processGlobalMessages(context, processor);

                    if (needProcessEditableValueHolder)
                        processEditableValueHolder(context, component, processor);

                    context.setResponseWriter(responseWriter);
                    restoreRenderedJsLinks(context, prevRenderedJsLinks);

                    processingValidation = false;
                }
            }
        }
        writer.startElement(name, component);
        element = name;
    }

    public void endElement(String name) throws IOException {
        renderClientIdIfNecessary();
        writer.endElement(name);
        element = null;
        flushValidationScriptAfterEnd(name);
    }

    public void writeAttribute(String name, Object value, String property) throws IOException {
        writer.writeAttribute(name, value, property);
        if ("id".equals(name) && clientId != null && clientId.equals(value)) {
            clientId = null;
            clientIdRendered = true;
        }
    }

    public void writeURIAttribute(String name, Object value, String property) throws IOException {
        writer.writeURIAttribute(name, value, property);
        if ("id".equals(name) && clientId != null && clientId.equals(value)) {
            clientId = null;
            clientIdRendered = true;
        }
    }

    public void writeComment(Object comment) throws IOException {
        flushValidationScriptInElement();
        writer.writeComment(comment);
    }

    public void writeText(Object text, String property) throws IOException {
        flushValidationScriptInElement();
        writer.writeText(text, property);
    }

    public void writeText(char[] text, int off, int len) throws IOException {
        flushValidationScriptInElement();
        writer.writeText(text, off, len);
    }

    public ResponseWriter cloneWithWriter(Writer writer) {
        return new ValidationSupportResponseWriter(this.writer.cloneWithWriter(writer), this);
    }

    private void flushValidationScriptInElement() throws IOException {
        renderClientIdIfNecessary();
        if (isAllowedToRenderAfterElementStart(element)) {
            flushValidationScriptIfNecessary();
        }
    }

    private void flushValidationScriptAfterEnd(String element) throws IOException {
        if (isAllowedToRenderAfterElementEnd(element)) {
            flushValidationScriptIfNecessary();
        }
    }

    private void flushValidationScriptIfNecessary() throws IOException {
        if (isValidationScriptNotEmpty() && clientIdRendered) {
            String validationScript = validationScriptWriter.toString();
            writer.write(validationScript);
            validationScriptWriter = null;
            putNewJSLinksInRenderedJsLinks();
            validationScripts = null;
        }
    }

    private void renderClientIdIfNecessary() throws IOException {
        if (element != null && clientId != null &&
                !clientIdRendered &&
                isValidationScriptNotEmpty() &&
                !processingValidation &&
                isElementAllowedForId(element)) {
            writeAttribute("id", clientId, null);
            clientId = null;
            clientIdRendered = true;
        }
    }

    private boolean isValidationScriptNotEmpty() {
        return validationScriptWriter != null && validationScriptWriter.getBuffer().length() > 0;
    }

    private ValidationProcessor getValidationProcessor(FacesContext context) {
        if (validationProcessor == null)
            validationProcessor = ValidationProcessor.getInstance(context);
        return validationProcessor;
    }

    private boolean isValidationNeeded(FacesContext context, UIComponent component) {
        ValidationProcessor processor = getValidationProcessor(context);
        return processor != null && !processor.isGlobalMessagesProcessed() || (component instanceof EditableValueHolder);
    }

    private void processEditableValueHolder(FacesContext context, UIComponent component, ValidationProcessor processor) throws IOException {
        EditableValueHolder editableValueHolder = (EditableValueHolder) component;
        VerifiableComponent[] verifiableComponents = processor.getVerifiableComponents(context);
        VerifiableComponent vc = VerifiableComponent.getVerifiableComponent(
                verifiableComponents, editableValueHolder, component.getClientId(context));
        UIForm parentForm = RenderingUtil.getEnclosingForm(component);
        if (parentForm == null) {
            Log.log(context, "Warn: enclosing form cannot be found for component " + component.getClientId(context) + ". Client-side validation will not be available for it.");
            return;
        }
        if (vc != null) {
            clientId = component.getClientId(context);
            clientIdRendered = false;
            vc.setParentForm(parentForm);
            vc.addValidators(editableValueHolder.getValidators());
            vc.setRequired(editableValueHolder.isRequired());
            vc.setConverter(ConverterUtil.getConverter(component, context));
            vc.addMessageFromContext(context);
            vc.updateClientValidatorsScriptsAndLibraries(context);
            processor.addVerifiableComponent(vc);
            handleEditableValueHolder(context, vc, processor);
        }
    }

    private void processGlobalMessages(FacesContext context, ValidationProcessor processor) throws IOException {
        Iterator<FacesMessage> globalMessages = context.getMessages(null);
        if (globalMessages.hasNext()) {
            ResourceUtil.renderJSLinkIfNeeded(ResourceUtil.getUtilJsURL(context), context);
            ResourceUtil.renderJSLinkIfNeeded(ResourceUtil.getInternalResourceURL(context, AbstractClientValidator.class, "validatorUtil.js"), context);
            while (globalMessages.hasNext()) {
                FacesMessage message = globalMessages.next();
                RenderingUtil.renderInitScript(context, ClientValidatorUtil.getScriptAddGlobalMessage(message), null);
            }
        }
        processor.confirmGlobalMessagesProcessing();
    }

    private void handleEditableValueHolder(FacesContext context, VerifiableComponent vc, ValidationProcessor vp) throws IOException {
        ClientValidationMode clientValidationRuleForComponent = vp.getClientValidationRuleForComponent(vc);
        // if client validation switched at least for one validatable component - render client script for component validation
        StringBuilder commonScript = vc.getCommonScript();
        if (commonScript == null || commonScript.length() == 0) return;

        // if component does not have defined presentation in page and default client validation presentation switched on
        // then render default validation presentation for component (currently floating icon message)
        UIForm parentForm = vc.getParentForm();
        if (vp.isUseDefaultClientValidationPresentationForForm(parentForm) || vp.isUseDefaultServerValidationPresentationForForm(parentForm)) {
            addPresentationComponent(vc, bubbleIndex, vp);
            bubbleIndex++;
        }
        if (!vp.getClientValidationRuleForComponent(vc).equals(ClientValidationMode.OFF)) {
            List<String> javascriptLibraries = vc.getJavascriptLibrariesUrls();
            String[] javascriptLibrariesArray = javascriptLibraries.toArray(new String[javascriptLibraries.size()]);
            RenderingUtil.renderInitScript(context, commonScript.toString(), javascriptLibrariesArray);
        }

        if (clientValidationRuleForComponent.equals(ClientValidationMode.ON_SUBMIT)) {
            String formClientId = parentForm.getClientId(context);
            if (!formsHaveOnSubmitRendered.contains(formClientId)) {
                RenderingUtil.renderInitScript(context, "O$.addOnSubmitEvent(O$._autoValidateForm,'" + formClientId + "');\n", null);
                formsHaveOnSubmitRendered.add(formClientId);
            }
        } else if (clientValidationRuleForComponent.equals(ClientValidationMode.ON_DEMAND)) {
            RenderingUtil.renderInitScript(context, "O$.addNotValidatedInput('" + vc.getClientId() + "');", null);
        }
    }

    private void addPresentationComponent(VerifiableComponent vc, int idx, ValidationProcessor vp) throws IOException {
        UIForm parentForm = vc.getParentForm();
        ClientValidationSupport clientValidationSupport = vp.getClientValidationSupport(parentForm);
        createPresentationComponent("dfm" + idx, vc, clientValidationSupport, vp);
    }

    private void createPresentationComponent(String id, VerifiableComponent verifiableComponent, ClientValidationSupport support, ValidationProcessor vp) throws IOException {
        FacesContext context = FacesContext.getCurrentInstance();
        UIMessage defaultMessage = vp.getDefaultPresentationInstance(context, support);

        if (defaultMessage instanceof FloatingIconMessage) {
            createFloatingIconMessage(context, id, verifiableComponent, (FloatingIconMessage) defaultMessage);
        } else {
            throw new IllegalStateException("Illegal default presentation component type. Expected FloatingIconMessage, actual " + defaultMessage.getClass());
        }
    }

    private FloatingIconMessage createFloatingIconMessage(FacesContext context,
                                                          String id,
                                                          VerifiableComponent vc,
                                                          FloatingIconMessage template) throws IOException {
        FloatingIconMessage message = new FloatingIconMessage(template, true);
        message.setId(id);
        message.setParent(vc.getComponent().getParent());
        message.setFor(vc.getComponent().getId());
        message.getAttributes().put(BaseMessageRenderer.DEFAULT_PRESENTATION, Boolean.TRUE);
        message.encodeBegin(context);
        message.encodeChildren(context);
        message.encodeEnd(context);
        return message;
    }

    private boolean isAllowedToRenderAfterElementStart(String element) {
        return element != null && !isStringInArray(element, ELEMENTS_ALLOWED_RENDER_AFTER_THEIR_START);

    }

    private boolean isAllowedToRenderAfterElementEnd(String element) {
        return !isStringInArray(element, ELEMENTS_ALLOW_RENDERER_AFTER_THEIR_END);
    }

    private static boolean isElementAllowedForId(String element) {
        return isStringInArray(element, ELEMENTS_ALLOWED_FOR_ID);
    }

    private static boolean isStringInArray(String str, String[] array) {
        str = str.toLowerCase();
        for (int i = 0, len = array.length; i < len; i++) {
            String s = array[i];
            if (s.equals(str))
                return true;
        }
        return false;
    }

    private List<String> substituteRenderedJsLinks(FacesContext context) {
        if (validationScripts == null) {
            validationScripts = new ArrayList<String>();
        }
        Map<String, Object> requestMap = context.getExternalContext().getRequestMap();
        List<String> renderedJsLinks = (List<String>) requestMap.get(ResourceUtil.RENDERED_JS_LINKS);
        if (renderedJsLinks != null) {
            for (String js : renderedJsLinks) {
                if (!validationScripts.contains(js))
                    validationScripts.add(js);
            }
        }
        requestMap.put(ResourceUtil.RENDERED_JS_LINKS, validationScripts);
        return renderedJsLinks;
    }

    private void restoreRenderedJsLinks(FacesContext context, List<String> prevRenderedJsLinks) {
        Map<String, Object> requestMap = context.getExternalContext().getRequestMap();
        requestMap.put(ResourceUtil.RENDERED_JS_LINKS, prevRenderedJsLinks);
    }

    private void putNewJSLinksInRenderedJsLinks() {
        if (validationScripts == null)
            return;
        List<String> renderedJsLinks = ResourceUtil.getRenderedJsLinks(FacesContext.getCurrentInstance());
        for (String js : validationScripts) {
            if (!renderedJsLinks.contains(js))
                renderedJsLinks.add(js);
        }
    }
}