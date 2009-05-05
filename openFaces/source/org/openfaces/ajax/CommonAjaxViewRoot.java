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
package org.openfaces.ajax;

import org.openfaces.ajax.plugins.AjaxPluginIncludes;
import org.openfaces.ajax.plugins.PluginsLoader;
import org.openfaces.component.OUIObjectIterator;
import org.openfaces.component.ajax.AjaxSettings;
import org.openfaces.component.ajax.DefaultSessionExpiration;
import org.openfaces.component.ajax.SilentSessionExpiration;
import org.openfaces.component.table.AbstractTable;
import org.openfaces.component.util.AjaxLoadBundleComponent;
import org.openfaces.org.json.JSONException;
import org.openfaces.org.json.JSONObject;
import org.openfaces.renderkit.AjaxPortionRenderer;
import org.openfaces.util.RenderingUtil;
import org.openfaces.util.ResourceUtil;
import org.openfaces.util.StringInspector;
import org.openfaces.util.StyleUtil;
import org.openfaces.util.*;

import javax.el.ELContext;
import javax.el.MethodExpression;
import javax.faces.FacesException;
import javax.faces.FactoryFinder;
import javax.faces.application.StateManager;
import javax.faces.component.NamingContainer;
import javax.faces.component.UIComponent;
import javax.faces.component.UIData;
import javax.faces.component.UIForm;
import javax.faces.component.UIViewRoot;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.ActionEvent;
import javax.faces.event.FacesEvent;
import javax.faces.event.PhaseId;
import javax.faces.render.RenderKit;
import javax.faces.render.RenderKitFactory;
import javax.faces.render.Renderer;
import javax.faces.render.ResponseStateManager;
import javax.portlet.ActionRequest;
import javax.portlet.RenderRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Eugene Goncharov
 */
public abstract class CommonAjaxViewRoot {
    private static final String APPLICATION_SESSION_EXPIRATION_PARAM_NAME = "org.openfaces.ajax.sessionExpiration";
    private static final String SILENT_SESSION_EXPIRATION_HANDLING = "silent";
    private static final String DEFAULT_SESSION_EXPIRATION_HANDLING = "default";

    private static final String PARAM_SUBMITTED_COMPONENT_IDS = "_of_submittedComponentIds";
    private static final String PARAM_SERVER_ACTION = "_of_serverAction";
    private static final String PARAM_SERVER_ACTION_COMPONENT_ID = "serverActionSourceComponentId";// see JSFC-1516
    private static final String PARAM_ACTION_COMPONENT = "_of_actionComponent";
    private static final String PARAM_ACTION_LISTENER = "_of_actionListener";
    private static final String PARAM_IMMEDIATE = "_of_immediate";
    // copy of org.apache.myfaces.shared_impl.renderkit.RendererUtils.SEQUENCE_PARAM
    private static final String MYFACES_SEQUENCE_PARAM = "jsf_sequence";
    public static final long MAX_PORTLET_PARALLEL_REQUEST_TIMEOUT = 20 * 1000;
    private static final String COM_SUN_FACES_FORM_CLIENT_ID_ATTR = "com.sun.faces.FORM_CLIENT_ID_ATTR";
    private static final String VALUE_ATTR_STRING = "value=\"";

    private static long tempIdCounter = 0;

    private static final Pattern JS_VAR_PATTERN = Pattern.compile("\\bvar\\b");

    private UIViewRoot viewRoot;
    private List<FacesEvent> events;

    protected CommonAjaxViewRoot(UIViewRoot viewRoot) {
        this.viewRoot = viewRoot;
    }

    protected abstract void parentProcessDecodes(FacesContext context);

    protected abstract void parentProcessValidators(FacesContext context);

    protected abstract void parentProcessUpdates(FacesContext context);

    protected abstract void parentProcessApplication(FacesContext context);

    protected abstract void parentEncodeChildren(FacesContext context) throws IOException;

    protected abstract int parentGetChildCount();

    protected abstract List<UIComponent> parentGetChildren();

    protected abstract Iterator<UIComponent> parentGetFacetsAndChildren();

    public void processDecodes(FacesContext context, boolean specialSessionExpirationHandling) {
        Map<String, Object> requestMap = context.getExternalContext().getRequestMap();
        if (!AjaxUtil.isAjaxRequest(context) ||
                (specialSessionExpirationHandling && requestMap.containsKey(AjaxViewHandler.SESSION_EXPIRATION_PROCESSING))) {
            parentProcessDecodes(context);
            return;
        }

        try {
            // The try-catch block is required to handle errors and exceptions
            // during the processing of the ajax request.
            //
            // The handling of errors and exceptions is done on each phase of JSF request lifecycle
            // If the exception is catched here, the appropriate message is sent back to the client
            // in ajax response
            doProcessDecodes(context, this);
            broadcastForPhase(PhaseId.APPLY_REQUEST_VALUES);
        } catch (RuntimeException e) {
            processExceptionDuringAjax(e);
            if (e.getMessage() != null) {
                Log.log(context, e.getMessage(), e);
            }
        }
        catch (Error e) {
            processExceptionDuringAjax(e);
            if (e.getMessage() != null) {
                Log.log(context, e.getMessage(), e);
            }
        }

    }

    public void processValidators(FacesContext context, boolean specialSessionExpirationHandling) {
        Map<String, Object> requestMap = context.getExternalContext().getRequestMap();
        if (!AjaxUtil.isAjaxRequest(context)
                || (specialSessionExpirationHandling && requestMap.containsKey(AjaxViewHandler.SESSION_EXPIRATION_PROCESSING))) {
            parentProcessValidators(context);
            return;
        }

        try {
            // The try-catch block is required to handle errors and exceptions
            // during the processing of the ajax request.
            //
            // The handling of errors and exceptions is done on each phase of JSF request lifecycle
            // If the exception is catched here, the appropriate message is sent back to the client
            // in ajax response
            doProcessValidators(context);
            broadcastForPhase(PhaseId.PROCESS_VALIDATIONS);
        } catch (RuntimeException e) {
            processExceptionDuringAjax(e);
            if (e.getMessage() != null) {
                Log.log(context, e.getMessage(), e);
            }
        }
        catch (Error e) {
            processExceptionDuringAjax(e);
            if (e.getMessage() != null) {
                Log.log(context, e.getMessage(), e);
            }
        }

    }

    public void processUpdates(FacesContext context, boolean specialSessionExpirationHandling) {
        Map<String, Object> requestMap = context.getExternalContext().getRequestMap();
        if (!AjaxUtil.isAjaxRequest(context) ||
                (specialSessionExpirationHandling && requestMap.containsKey(AjaxViewHandler.SESSION_EXPIRATION_PROCESSING))) {
            parentProcessUpdates(context);
            return;
        }

        try {
            // The try-catch block is required to handle errors and exceptions
            // during the processing of the ajax request.
            //
            // The handling of errors and exceptions is done on each phase of JSF request lifecycle
            // If the exception is catched here, the appropriate message is sent back to the client
            // in ajax response
            doProcessUpdates(context);
            broadcastForPhase(PhaseId.UPDATE_MODEL_VALUES);
        } catch (RuntimeException e) {
            processExceptionDuringAjax(e);
            if (e.getMessage() != null) {
                Log.log(context, e.getMessage(), e);
            }
        }
        catch (Error e) {
            processExceptionDuringAjax(e);
            if (e.getMessage() != null) {
                Log.log(context, e.getMessage(), e);
            }
        }

    }

    public void processApplication(FacesContext context, boolean specialSessionExpirationHandling) {
        Map<String, Object> requestMap = context.getExternalContext().getRequestMap();
        if (!AjaxUtil.isAjaxRequest(context)
                || (specialSessionExpirationHandling && requestMap.containsKey(AjaxViewHandler.SESSION_EXPIRATION_PROCESSING))) {
            parentProcessApplication(context);
            return;
        }

        try {
            // The try-catch block is required to handle errors and exceptions
            // during the processing of the ajax request.
            //
            // The handling of errors and exceptions is done on each phase of JSF request lifecycle
            // If the exception is catched here, the appropriate message is sent back to the client
            // in ajax response
            doProcessApplication(context);
            broadcastForPhase(PhaseId.INVOKE_APPLICATION);
        } catch (RuntimeException e) {
            processExceptionDuringAjax(e);
            if (e.getMessage() != null) {
                Log.log(context, e.getMessage(), e);
            }
        }
        catch (Error e) {
            processExceptionDuringAjax(e);
            if (e.getMessage() != null) {
                Log.log(context, e.getMessage(), e);
            }
        }

    }

    public void encodeChildren(FacesContext context) throws IOException {
        if (!AjaxUtil.isAjaxRequest(context)) {
            parentEncodeChildren(context);
            return;
        }

        try {
            // The try-catch block is required to handle errors and exceptions
            // during the processing of the ajax request.
            //
            // The handling of errors and exceptions is done on each phase of JSF request lifecycle
            // If the exception is catched here, the appropriate message is sent back to the client
            // in ajax response
            doEncodeChildren(context);
        } catch (RuntimeException e) {
            processExceptionDuringAjax(e);
            if (e.getMessage() != null) {
                Log.log(context, e.getMessage(), e);
            }
        }
        catch (Error e) {
            processExceptionDuringAjax(e);
            if (e.getMessage() != null) {
                Log.log(context, e.getMessage(), e);
            }
        }

    }

    private void doProcessDecodes(FacesContext context, Object objectInstanceForSynchronizeOn) {
        ExternalContext externalContext = context.getExternalContext();
        RequestFacade request = RequestFacade.getInstance(externalContext.getRequest());

        Map<String, Object> requestMap = externalContext.getRequestMap();
        if (requestMap.containsKey(AjaxViewHandler.SESSION_EXPIRATION_PROCESSING)) {
            return;
        }

        ResponseFacade response = ResponseFacade.getInstance(externalContext.getResponse());

        String componentId = request.getParameter(AjaxUtil.PARAM_COMPONENT_IDS);
        String[] componentIds = componentId != null ? componentId.split(";") : null;
        String[] submittedComponentIds = extractSubmittedComponentIds(request);

        assertComponentId(componentId);

        if (response instanceof ResponseFacade.ActionResponseFacade) {
            Map<String, Object> sessionMap = context.getExternalContext().getSessionMap();

            boolean shouldWaitForPreviousAjaxCompletion = true;
            long timeBefore = System.currentTimeMillis();
            do {

                synchronized (objectInstanceForSynchronizeOn) {
                    long timeElapsed = System.currentTimeMillis() - timeBefore;
                    if (timeElapsed > MAX_PORTLET_PARALLEL_REQUEST_TIMEOUT) {
                        Log.log(context, "CommonAjaxViewRoot.doProcessDecodes: waiting for parallel ajax request timed out");
                        sessionMap.remove(AjaxUtil.AJAX_REQUEST_MARKER);
                    }

                    if (sessionMap.get(AjaxUtil.AJAX_REQUEST_MARKER) == null) {
                        sessionMap.put(AjaxUtil.AJAX_REQUEST_MARKER, request.getParameter(AjaxUtil.AJAX_REQUEST_MARKER));
                        sessionMap.put(AjaxUtil.PARAM_COMPONENT_IDS, componentId);
                        sessionMap.put(AjaxUtil.UPDATE_PORTIONS_SUFFIX, request.getParameter(AjaxUtil.UPDATE_PORTIONS_SUFFIX));
                        sessionMap.put(AjaxUtil.CUSTOM_JSON_PARAM, request.getParameter(AjaxUtil.CUSTOM_JSON_PARAM));
                        shouldWaitForPreviousAjaxCompletion = false;
                    } else
                        try {
                            Thread.sleep(50);
                        } catch (InterruptedException e) {
                            // prevoius ajax request completion should be waited for anyway...
                        }
                }
            } while (shouldWaitForPreviousAjaxCompletion);

        }

        UIViewRoot viewRoot = context.getViewRoot();
        assertChildren(viewRoot);


        UIComponent[] components = new UIComponent[componentIds.length];
        for (int i = 0; i < componentIds.length; i++) {
            String component = componentIds[i];
            components[i] = findComponentByPath(viewRoot, component, true, false);
            Log.log(context, "doProcessDecodes find component by " + component + ", it is " + components[i]);
        }
        ajaxApplyRequestValues(context, components, viewRoot, submittedComponentIds);
        if (Boolean.valueOf(request.getParameter(PARAM_IMMEDIATE))) {
            doProcessApplication(context);
        }
    }

    private void doProcessUpdates(FacesContext context) {
        ExternalContext externalContext = context.getExternalContext();
        RequestFacade request = RequestFacade.getInstance(externalContext.getRequest());

        Map<String, Object> requestMap = externalContext.getRequestMap();
        if (requestMap.containsKey(AjaxViewHandler.SESSION_EXPIRATION_PROCESSING)) {
            return;
        }

        String componentId = request.getParameter(AjaxUtil.PARAM_COMPONENT_IDS);
        assertComponentId(componentId);
        String[] componentIds = componentId.split(";");
        if(componentIds == null) {
            throw new IllegalStateException(AjaxUtil.PARAM_COMPONENT_IDS + " not found at request");
        }
        String[] submittedComponentIds = extractSubmittedComponentIds(request);




        UIViewRoot viewRoot = context.getViewRoot();
        assertChildren(viewRoot);

        UIComponent[] components = new UIComponent[componentIds.length];
        for (int i = 0; i < componentIds.length; i++) {
            String component = componentIds[i];
            components[i] = findComponentByPath(viewRoot, component, false, false);
            Log.log(context, "doProcessUpdates find component by " + component + ", it is " + components[i]);
        }
        ajaxUpdateModelValues(context, components, viewRoot, submittedComponentIds);
    }

    private void doProcessValidators(FacesContext context) {
        ExternalContext externalContext = context.getExternalContext();
        RequestFacade request = RequestFacade.getInstance(externalContext.getRequest());


        Map<String, Object> requestMap = externalContext.getRequestMap();
        if (requestMap.containsKey(AjaxViewHandler.SESSION_EXPIRATION_PROCESSING)) {
            return;
        }


        String componentId = request.getParameter(AjaxUtil.PARAM_COMPONENT_IDS);
        assertComponentId(componentId);
        String[] componentIds = componentId.split(";");
        String[] submittedComponentIds = extractSubmittedComponentIds(request);

        UIViewRoot viewRoot = context.getViewRoot();
        assertChildren(viewRoot);

        UIComponent[] components = new UIComponent[componentIds.length];
        for (int i = 0; i < componentIds.length; i++) {
            String component = componentIds[i];
            components[i] = findComponentByPath(viewRoot, component, false, false);
            Log.log(context, "doProcessValidators find component by " + component + ", it is " + components[i]);
        }
        ajaxProcessValidations(context, components, viewRoot, submittedComponentIds);
    }

    private String[] extractSubmittedComponentIds(RequestFacade request) {
        String idsStr = request.getParameter(PARAM_SUBMITTED_COMPONENT_IDS);
        String[] submittedComponentIds = !RenderingUtil.isNullOrEmpty(idsStr) ? idsStr.split(";") : null;
        return submittedComponentIds;
    }

    private void doProcessApplication(FacesContext context) {
        ExternalContext externalContext = context.getExternalContext();
        RequestFacade request = RequestFacade.getInstance(externalContext.getRequest());


        Map<String, Object> requestMap = externalContext.getRequestMap();
        if (requestMap.containsKey(AjaxViewHandler.SESSION_EXPIRATION_PROCESSING)) {
            return;
        }

        String componentId = request.getParameter(AjaxUtil.PARAM_COMPONENT_IDS);
        assertComponentId(componentId);
        String[] componentIds = componentId.split(";");
        String serverAction = request.getParameter(PARAM_SERVER_ACTION);
        String serverActionComponentId = request.getParameter(PARAM_SERVER_ACTION_COMPONENT_ID);

        UIViewRoot viewRoot = context.getViewRoot();
        assertChildren(viewRoot);


        String listener = request.getParameter(PARAM_ACTION_LISTENER);
        String actionComponentId = request.getParameter(PARAM_ACTION_COMPONENT);
        Log.log(context, "try invoke listener");
        if (listener != null) {
            ELContext elContext = context.getELContext();
            MethodExpression methodExpression = context.getApplication().getExpressionFactory().createMethodExpression(
                    elContext, "#{" + listener + "}", void.class, new Class[]{ActionEvent.class});
            ActionEvent event = new ActionEvent(findComponentByPath(viewRoot, actionComponentId));
            event.setPhaseId(Boolean.valueOf(request.getParameter(PARAM_IMMEDIATE)) ? PhaseId.APPLY_REQUEST_VALUES : PhaseId.INVOKE_APPLICATION);
            methodExpression.invoke(elContext, new Object[]{event});
        }
        // invoke application should be after notification listeners
        Log.log(context, "invoke listener finished, invoke application");
        ajaxInvokeApplication(context, viewRoot, serverAction, serverActionComponentId);
        Log.log(context, "invoke application finished");
        UIComponent[] components = new UIComponent[componentIds.length];
        for (int i = 0; i < componentIds.length; i++) {
            String component = componentIds[i];
            components[i] = findComponentByPath(viewRoot, component, false, false);
            Log.log(context, "doProcessApplication find component by " + component + ", it is " + components[i]);
        }
        if (serverActionComponentId != null) {
            // todo: if component is an iterator its rowIndex should be reset so that the following id check succeed (JSFC-1974)
            // [DPikhulya Oct-15] it's possible that after moving ajax from AjaxRequestsPhaseListener there are no additional
            // actions are required for this check to succeed, because of the added findComponetByPath above.
        }
        for (int i = 0; i < components.length; i++) {
            UIComponent component = components[i];
            String thisComponentId = componentIds[i];
            Class clazz = null;
            try {
                clazz = Class.forName("com.sun.facelets.component.UIRepeat");
            } catch (ClassNotFoundException e) {
                //do nothing - it's ok - not facelets environment
            }
            if (!component.getClientId(context).equals(thisComponentId) &&
                    !(component instanceof UIData || component instanceof OUIObjectIterator || (clazz != null && clazz.isInstance(component))))
                throw new IllegalStateException("component.getClientId [" + component.getClientId(context) + "] " +
                        "is supposed to be equal to componentId [" + thisComponentId + "]");
        }
    }

    protected void assertChildren(UIViewRoot viewRoot) {
        if (viewRoot.getChildCount() == 0) {
            throw new IllegalStateException("View should have been already restored.");
        }
    }

    private void doEncodeChildren(FacesContext context) throws IOException {
        ExternalContext externalContext = context.getExternalContext();
        RequestFacade request = RequestFacade.getInstance(externalContext.getRequest());

        Map<String, Object> requestMap = externalContext.getRequestMap();
        if (requestMap.containsKey(AjaxViewHandler.SESSION_EXPIRATION_PROCESSING)) {
            handleSessionExpirationOnEncodeChildren(context, request);
            releaseSyncObject();
            return;
        }

        if (AjaxUtil.isPortletRequest(context)) {
            renderPortletsAjaxResponse(context);
            releaseSyncObject();
            return;
        }

        String componentId = request.getParameter(AjaxUtil.PARAM_COMPONENT_IDS);
        assertComponentId(componentId);
        String[] componentIds = componentId.split(";");

        UIViewRoot viewRoot = context.getViewRoot();

        assertChildren(viewRoot);

        loadBundles(context);

        UIComponent[] components = new UIComponent[componentIds.length];
        for (int i = 0; i < componentIds.length; i++) {
            String component = componentIds[i];
            components[i] = findComponentByPath(viewRoot, component, false, true);
            Log.log(context, "doEncodeChildren find component by " + component + ", it is " + components[i]);
        }
        Object originalResponse = externalContext.getResponse();
        ResponseFacade response = ResponseFacade.getInstance(originalResponse);
        Integer sequence = getSequenceIdForMyFaces(context);
        finishProcessAjaxRequest(context, request, response, components, true, sequence);

        releaseSyncObject();

    }

    /**
     * @param componentId it to be verified
     * @throws IllegalStateException if the passed component id is {@code null}
     */
    private void assertComponentId(String componentId) {
        if (componentId == null)
            throw new IllegalStateException("processAjaxRequest: " + AjaxUtil.PARAM_COMPONENT_IDS + " is null");
    }

    private void handleSessionExpirationOnEncodeChildren(FacesContext context, RequestFacade request) throws IOException {
        ExternalContext externalContext = context.getExternalContext();
        Object originalResponse = externalContext.getResponse();
        if (originalResponse instanceof HttpServletResponse) {
            ResponseWrapper response = new ResponseWrapper((HttpServletResponse) originalResponse);
            response.setHeader(AjaxViewHandler.AJAX_EXPIRED_HEADER, AjaxViewHandler.AJAX_VIEW_EXPIRED);
        }

        UIViewRoot viewRoot = context.getViewRoot();
        List<UIComponent> children = viewRoot.getChildren();
        AjaxSettings ajaxSettings = null;

        String componentId = request.getParameter(AjaxUtil.PARAM_COMPONENT_IDS);
        Map<String, Object> requestMap = externalContext.getRequestMap();
        if (!requestMap.containsKey(AjaxViewHandler.SESSION_EXPIRATION_PROCESSING)
                && componentId == null)
            throw new IllegalStateException("processAjaxRequest: " + AjaxUtil.PARAM_COMPONENT_IDS + " == null");

        assertChildren(viewRoot);

        UIComponent component = findComponentByPath(viewRoot, componentId, false, false);
        if (component != null && component.getChildCount() > 0) {
            List<UIComponent> ajaxSubmittedComponentChildren = component.getChildren();
            ajaxSettings = iterateThroughChildrenForComponentAjaxSettings(ajaxSubmittedComponentChildren);
        }

        if (ajaxSettings == null) {
            ajaxSettings = iterateThroughChildrenForPageAjaxSettings(children);
        }

        if (ajaxSettings == null) {
            Map initParameterMap = externalContext.getInitParameterMap();
            String sessionExpirationHandling = (initParameterMap != null)
                    ? (String) initParameterMap.get(APPLICATION_SESSION_EXPIRATION_PARAM_NAME)
                    : null;

            if (sessionExpirationHandling != null && sessionExpirationHandling.length() > 0) {
                if (sessionExpirationHandling.equalsIgnoreCase(SILENT_SESSION_EXPIRATION_HANDLING)) {
                    ajaxSettings = createSilentSessionExpirationSettings();
                } else if (sessionExpirationHandling.equalsIgnoreCase(DEFAULT_SESSION_EXPIRATION_HANDLING)) {
                    ajaxSettings = createDefaultSessionExpirationSettings(context);
                }
            } else {
                ajaxSettings = createDefaultSessionExpirationSettings(context);
            }
        }

        if (ajaxSettings != null) {
            boolean isNonPortletRequest = !AjaxUtil.isPortletRequest(context);
            AbstractResponseFacade responseFacade =
                    finishSessionExpirationAjaxResponse(context, request, new UIComponent[]{ajaxSettings},
                            isNonPortletRequest);
            String sessionExpiredResponse = null;

            if (responseFacade.getOutputStream() != null) {
                ByteArrayOutputStream byteArrayOutputStream = (ByteArrayOutputStream) responseFacade.getOutputStream();
                sessionExpiredResponse = byteArrayOutputStream.toString("UTF-8");
            } else if (responseFacade.getWriter() != null) {
                sessionExpiredResponse = responseFacade.getWriter().toString();
            }

            if (sessionExpiredResponse != null) {
                requestMap.put(AjaxViewHandler.SESSION_EXPIRED_RESPONSE, sessionExpiredResponse);
            }
        }
    }

    private static void releaseSyncObject() {
        Map<String, Object> sessionMap = FacesContext.getCurrentInstance().getExternalContext().getSessionMap();
        // TODO [sanders] (Apr 1, 2009, 5:09 AM): Can't we synchronize on something shorter?.. :)
        // TODO [sanders] (Apr 1, 2009, 5:09 AM): Won't java.util.concurrent help?
        synchronized (sessionMap.get(AjaxViewHandler.SESSION_SYNCHRONIZATION)) {
            ((RequestsSyncObject) sessionMap.get(AjaxViewHandler.SESSION_SYNCHRONIZATION)).
                    setAjaxRequestProcessing(false);
            sessionMap.get(AjaxViewHandler.SESSION_SYNCHRONIZATION).notifyAll();
        }
    }

    // TODO [sanders] (Apr 1, 2009, 5:10 AM): Far too long method name
    private AjaxSettings iterateThroughChildrenForComponentAjaxSettings(List<UIComponent> children) {
        AjaxSettings result = null;
        for (Object iteratedChild : children) {
            if (iteratedChild instanceof AjaxSettings) {
                result = (AjaxSettings) iteratedChild;
                return result;
            }
            UIComponent uiComponent = (UIComponent) iteratedChild;
            if (uiComponent.getChildCount() > 0) {
                result = iterateThroughChildrenForComponentAjaxSettings(uiComponent.getChildren());
                if (result != null) {
                    return result;
                }
            }
        }
        return result;
    }

    // TODO [sanders] (Apr 1, 2009, 5:10 AM): Far too long method name
    private AjaxSettings iterateThroughChildrenForPageAjaxSettings(List<UIComponent> children) {
        AjaxSettings result = null;
        for (Object iteratedChild : children) {
            if (iteratedChild instanceof AjaxSettings && isPageSettings((AjaxSettings) iteratedChild)) {
                result = (AjaxSettings) iteratedChild;
                return result;
            }
            UIComponent uiComponent = (UIComponent) iteratedChild;
            if (uiComponent.getChildCount() > 0) {
                result = iterateThroughChildrenForPageAjaxSettings(uiComponent.getChildren());
                if (result != null) {
                    return result;
                }
            }
        }
        return result;
    }

    public static void processExceptionDuringAjax(Throwable exception) {
        FacesContext context = FacesContext.getCurrentInstance();
        ExternalContext externalContext = context.getExternalContext();
        Map<String, Object> requestMap = externalContext.getRequestMap();
        Map<String, Object> sessionMap = context.getExternalContext().getSessionMap();
        if (AjaxUtil.isPortletRequest(context) && externalContext.getRequest() instanceof RenderRequest) {
            try {
                if (!EnvironmentUtil.isRI()) {
                    finishProccessErrorUnderPortletsDuringAjax(context,
                            exception);
                } else if (sessionMap.containsKey(
                        AjaxViewHandler.ERROR_OCCURED_UNDER_PORTLETS)) {
                    finishProccessErrorUnderPortletsDuringAjax(context,
                            exception);
                }
            } catch (IOException e) {
                Log.log(context, "An attempt to process exception during ajax failed.IOException was thrown during processing.");
            }
        } else if (!(sessionMap.containsKey(AjaxViewHandler.ERROR_OCCURED_UNDER_PORTLETS))
                && AjaxUtil.isPortletRequest(context)
                && externalContext.getRequest() instanceof ActionRequest) {
            sessionMap.put(AjaxViewHandler.ERROR_OCCURED_UNDER_PORTLETS, Boolean.TRUE);
            sessionMap.put(AjaxViewHandler.ERROR_OBJECT_UNDER_PORTLETS, exception);
        }
        if (!requestMap.containsKey(AjaxViewHandler.ERROR_OCCURED)) {
            requestMap.put(AjaxViewHandler.ERROR_OCCURED, Boolean.TRUE.toString());
            requestMap.put(AjaxViewHandler.ERROR_MESSAGE_HEADER, exception.getMessage());
            requestMap.put(AjaxViewHandler.ERROR_CAUSE_MESSAGE_HEADER, exception.getCause());
        }
        releaseSyncObject();
    }

    private boolean isPageSettings(AjaxSettings ajaxSettings) {
        return (ajaxSettings.getParent() instanceof UIViewRoot || ajaxSettings.getParent() instanceof UIForm);
    }

    private AjaxSettings createSilentSessionExpirationSettings() {
        AjaxSettings result = new AjaxSettings();
        result.setSessionExpiration(new SilentSessionExpiration());
        return result;
    }

    private AjaxSettings createDefaultSessionExpirationSettings(FacesContext context) {
        AjaxSettings result = new AjaxSettings();
        DefaultSessionExpiration dse = new DefaultSessionExpiration();
        dse.createSubComponents(context);
        result.setSessionExpiration(dse);
        return result;
    }

    public int getChildCount() {
        int childCount = parentGetChildCount();
        UIViewRoot delegate = ((WrappedAjaxRoot) viewRoot).getDelegate();
        if (childCount == 0 && delegate != null) {
            childCount = delegate.getChildCount();
        }
        return childCount;
    }

    public List<UIComponent> getChildren() {
        List<UIComponent> children = parentGetChildren();
        UIViewRoot delegate = ((WrappedAjaxRoot) viewRoot).getDelegate();

        if (children == null || children.isEmpty() && delegate != null) {
            List delegateChildren = delegate.getChildren();
            if (children == null) {
                children = new ArrayList<UIComponent>();
            }
            children.addAll(delegateChildren);
        }

        return children;
    }

    public Iterator<UIComponent> getFacetsAndChildren() {
        Iterator<UIComponent> facetsAndChildren = parentGetFacetsAndChildren();
        UIViewRoot delegate = ((WrappedAjaxRoot) viewRoot).getDelegate();
        if (facetsAndChildren == null || !facetsAndChildren.hasNext() && delegate != null) {
            facetsAndChildren = delegate.getFacetsAndChildren();
        }
        return facetsAndChildren;
    }


    /**
     * Find all instances of {@link org.openfaces.component.util.LoadBundle} in view tree and load bundles
     * to request-scope map.
     *
     * @param context
     */
    private void loadBundles(FacesContext context) {
        loadBundles(context, context.getViewRoot());
    }

    /**
     * Recursive helper for {@link #loadBundles(FacesContext)}
     *
     * @param context
     * @param component
     */
    private void loadBundles(FacesContext context, UIComponent component) {
        // Iterate over cildrens
        for (UIComponent child : component.getChildren()) {
            loadChildBundles(context, child);
        }
        // Iterate over facets
        for (UIComponent child : component.getFacets().values()) {
            loadChildBundles(context, child);
        }
    }

    /**
     * @param context
     * @param child
     */
    private void loadChildBundles(FacesContext context, UIComponent child) {
        if (child instanceof AjaxLoadBundleComponent) {
            try {
                child.encodeBegin(context);
            } catch (IOException e) {
                Log.log(context, "Exception while invoking LoadBundle", e);
            }
        } else {
            loadBundles(context, child);
        }
    }


    private void ajaxApplyRequestValues(FacesContext context,
                                        UIComponent[] components,
                                        UIViewRoot viewRoot,
                                        String[] submittedComponentIds)
            throws FacesException {
        if (components != null) {
            for (UIComponent component : components) {
                Log.log(context, "start ajaxApplyRequestValues for " + component);
                component.processDecodes(context);
                Log.log(context, "finish ajaxApplyRequestValues for " + component);
            }
        }

        if (submittedComponentIds != null) {
            for (String submittedComponentId : submittedComponentIds) {
                UIComponent submittedComponent = findComponentByPath(viewRoot, submittedComponentId);
                Log.log(context, "start ajaxApplyRequestValues for " + submittedComponent);
                submittedComponent.processDecodes(context);
                Log.log(context, "finish ajaxApplyRequestValues for " + submittedComponent);
            }
        }
    }

    private void ajaxProcessValidations(FacesContext context,
                                        UIComponent[] components,
                                        UIViewRoot viewRoot,
                                        String[] submittedComponentIds) throws FacesException {
        if (components != null) {
            for (UIComponent component : components) {
                Log.log(context, "start ajaxProcessValidations for " + component);
                component.processValidators(context);
                Log.log(context, "finish ajaxProcessValidations for " + component);
            }
        }
        if (submittedComponentIds != null) {
            for (String submittedComponentId : submittedComponentIds) {
                UIComponent submittedComponent = findComponentByPath(viewRoot, submittedComponentId);
                Log.log(context, "start ajaxProcessValidations for " + submittedComponent);
                submittedComponent.processValidators(context);
                Log.log(context, "finish ajaxProcessValidations for " + submittedComponent);
            }
        }
    }

    private void ajaxUpdateModelValues(FacesContext context,
                                       UIComponent[] components,
                                       UIViewRoot viewRoot,
                                       String[] submittedComponentIds)
            throws FacesException {
        if (components != null) {
            for (UIComponent component : components) {
                Log.log(context, "start ajaxUpdateModelValues for " + component);
                component.processUpdates(context);
                Log.log(context, "finish ajaxUpdateModelValues for " + component);
            }
        }
        if (submittedComponentIds != null) {
            for (String submittedComponentId : submittedComponentIds) {
                UIComponent submittedComponent = findComponentByPath(viewRoot, submittedComponentId);
                Log.log(context, "start ajaxUpdateModelValues for " + submittedComponent);
                submittedComponent.processUpdates(context);
                Log.log(context, "finish ajaxUpdateModelValues for " + submittedComponent);
            }
        }
    }

    private void ajaxInvokeApplication(FacesContext context,
                                       UIViewRoot viewRoot,
                                       String serverAction,
                                       String serverActionComponentId) {
        if (serverAction == null)
            return;

        if (serverActionComponentId != null) {
            // this is needed for cases when for example Button in a Table needs to know current row's data during action execution
            UIComponent component = findComponentByPath(viewRoot, serverActionComponentId);
            Log.log(context, "start ajaxInvokeApplication for " + component);
        }

        ELContext elContext = context.getELContext();
        MethodExpression methodBinding = context.getApplication().getExpressionFactory().createMethodExpression(
                elContext, "#{" + serverAction + "}", String.class, new Class[]{});
        Log.log(context, "start ajaxInvokeApplication for " + methodBinding);
        methodBinding.invoke(elContext, null);
        Log.log(context, "finish ajaxInvokeApplication for " + methodBinding);
    }

    private void renderPortletsAjaxResponse(FacesContext context) {
        // TODO [sanders] (Apr 1, 2009, 4:56 AM): The below texts are somewhat misleading
        // it probably should have been "This method should be only invoked for portlet Ajax requests".
        if (!AjaxUtil.isAjaxRequest(context)) {
            throw new IllegalStateException("This method should be only invoked for Ajax requests");
        }

        if (!AjaxUtil.isPortletRequest(context)) {
            throw new IllegalStateException("This method should only be invoked for portlet Ajax requests");
        }

//    decreaseSequenceIdForMyFaces(context);
        Integer sequenceId = (EnvironmentUtil.isLiferay(context.getExternalContext().getRequestMap()))
                ? getSequenceIdForMyFaces(context)
                : null;

        Map sessionMap = context.getExternalContext().getSessionMap();
        String componentId = (String) sessionMap.get(AjaxUtil.PARAM_COMPONENT_IDS);
        if (componentId == null) {
            Log.log(context, "CommonAjaxViewRoot.renderPortletsAjaxResponse: " + AjaxUtil.PARAM_COMPONENT_IDS + " == null");
            // Can happen sometimes on simultaneous ajax requests in Portlets.
            // Seems that there's no better way to handle it in Portlets 1.0
            return;
        }
        String[] componentIds = componentId.split(";");
        Map<String, Object> requestMap = context.getExternalContext().getRequestMap();
        requestMap.put(AjaxUtil.KEY_RENDERING_PORTLETS_AJAX_RESPONSE, Boolean.TRUE);
        try {
            UIViewRoot viewRoot = context.getViewRoot();
            assertChildren(viewRoot);

            // clear the style ids set just constructed on the "render" phase in order to avoid warning of repeated style rendering
            StyleUtil.getRenderedStyleElementsIds(context).clear();

            loadBundles(context);

            UIComponent[] components = new UIComponent[componentIds.length];
            for (int i = 0; i < componentIds.length; i++) {
                String component = componentIds[i];
                UIComponent findComponent = findComponentByPath(viewRoot, component);
                if (findComponent == null) {
                    throw new IllegalStateException("Couldn't find component by client id: " + component);
                }
                components[i] = findComponent;
            }
            ExternalContext externalContext = context.getExternalContext();
            RequestFacade request = RequestFacade.getInstance(externalContext.getRequest());
            ResponseFacade response = ResponseFacade.getInstance(externalContext.getResponse());
            try {
                finishProcessAjaxRequest(context, request, response, components, false, sequenceId);
            } catch (IOException e) {
                throw new FacesException(e);
            }
        } finally {
            if (!requestMap.containsKey(AjaxViewHandler.SESSION_EXPIRATION_PROCESSING)) {
                clearPortletSessionParams(context);
            }
            requestMap.remove(AjaxUtil.KEY_RENDERING_PORTLETS_AJAX_RESPONSE);
        }
    }

    private static void clearPortletSessionParams(FacesContext context) {
        Map<String, Object> sessionMap = context.getExternalContext().getSessionMap();
        synchronized (CommonAjaxViewRoot.class) {
            sessionMap.remove(AjaxUtil.PARAM_COMPONENT_IDS);
            sessionMap.remove(AjaxUtil.UPDATE_PORTIONS_SUFFIX);
            sessionMap.remove(AjaxUtil.CUSTOM_JSON_PARAM);
            sessionMap.remove(AjaxUtil.AJAX_REQUEST_MARKER);
        }
    }

    private void finishProcessAjaxRequest(
            FacesContext context,
            RequestFacade request,
            ResponseFacade response,
            UIComponent[] components,
            boolean nonPortletAjaxRequest, Integer sequence) throws IOException {
        AjaxResponse ajaxResponse = ajaxRenderResponse(request, context, components);

        AjaxSavedStateIdxHolder stateIdxHolder = ajaxSaveState(context, request, ajaxResponse, components, sequence);

        if (sequence != null) {
            stateIdxHolder.setJSFSequence(sequence);
        }
        ajaxResponse.setStateIdxHolder(stateIdxHolder);

        ajaxResponse.write(response);
    }

    private AbstractResponseFacade finishSessionExpirationAjaxResponse(FacesContext context,
                                                                       RequestFacade request,
                                                                       UIComponent[] components,
                                                                       boolean nonPortletAjaxRequest) throws IOException {
        AjaxResponse ajaxResponse = null;
        if (!nonPortletAjaxRequest) {
            Map<String, Object> requestMap = context.getExternalContext().getRequestMap();
            requestMap.put(AjaxUtil.KEY_RENDERING_PORTLETS_AJAX_RESPONSE, Boolean.TRUE);
            try {
                // clear the style ids set just constructed on the "render" phase in order to avoid warning of repeated style rendering
                StyleUtil.getRenderedStyleElementsIds(context).clear();
                ajaxResponse = ajaxRenderResponse(request, context, components);
            } finally {
                clearPortletSessionParams(context);
                requestMap.remove(AjaxUtil.KEY_RENDERING_PORTLETS_AJAX_RESPONSE);
            }
        } else {
            ajaxResponse = ajaxRenderResponse(request, context, components);
        }

        ResponseFacade response = ResponseFacade.getInstance(context.getExternalContext().getResponse());
        AbstractResponseFacade responseFacade = new ResponseAdapter(response);
        ajaxResponse.setStateIdxHolder(new AjaxSavedStateIdxHolder());
        if (!nonPortletAjaxRequest) {
            ajaxResponse.setSessoinExpired(Boolean.TRUE.toString());
            ajaxResponse.setSessoinExpiredLocation((String) context.getExternalContext().getRequestMap().get(AjaxViewHandler.LOCATION_HEADER));
            ajaxResponse.write(response);
        } else {
            ajaxResponse.write(responseFacade);
        }
        return responseFacade;
    }

    // TODO [sanders] (Apr 1, 2009, 5:11 AM): Too long name
    private static void finishProccessErrorUnderPortletsDuringAjax(FacesContext context, Throwable e) throws IOException {
        AjaxResponse ajaxResponse = new AjaxResponse();
        Map<String, Object> requestMap = context.getExternalContext().getRequestMap();
        requestMap.put(AjaxUtil.KEY_RENDERING_PORTLETS_AJAX_RESPONSE, Boolean.TRUE);
        ajaxResponse.setStateIdxHolder(new AjaxSavedStateIdxHolder());
        try {
            ajaxResponse.setException(Boolean.TRUE.toString());
            ajaxResponse.setExceptionMessage(e.getMessage());
            ajaxResponse.write(ResponseFacade.getInstance(context.getExternalContext().getResponse()));
        } finally {
            clearPortletSessionParams(context);
            requestMap.remove(AjaxUtil.KEY_RENDERING_PORTLETS_AJAX_RESPONSE);
        }
    }

    private void ajaxPrepareInitializationScripts(
            FacesContext context, AjaxResponse ajaxResponse, List<String> foreignHeadScripts, StringBuilder initializationScripts) {
        StringBuilder tempBuffer = new StringBuilder();
        if (foreignHeadScripts != null) {
            for (String script : foreignHeadScripts) {
                StringInspector scriptInspector = new StringInspector(script);
                boolean substituteDocumentWrite = scriptInspector.indexOfIgnoreCase("document.write") > -1;
                if (substituteDocumentWrite) {
                    tempBuffer.append("O$.substituteDocumentWrite();\n"); // tricky workaround. see ajaxUtil.js for details
                }
                tempBuffer.append(script).append("\n");
                if (substituteDocumentWrite) {
                    tempBuffer.append("O$.restoreDocumentWrite();\n"); // tricky workaround. see ajaxUtil.js for details
                }
            }
        }
        if (tempBuffer.length() > 0) {
            initializationScripts.insert(0, tempBuffer.toString());
        }

        if (initializationScripts.length() > 0) {
            String initScriptsStr = initializationScripts.toString();
            initScriptsStr = initScriptsStr.replaceAll("<!--", "").replaceAll("//-->", "");
            // create special node with runtime js library that contains all initialization scripts
            String uniqueRTLibraryName = ResourceFilter.RUNTIME_INIT_LIBRARY_PATH + AjaxUtil.generateUniqueInitLibraryName();
            String initLibraryUrl = ResourceUtil.getApplicationResourceURL(context, uniqueRTLibraryName);
            ajaxResponse.setInitLibraryName(initLibraryUrl);

            context.getExternalContext().getSessionMap().put(uniqueRTLibraryName, initScriptsStr);
        }
    }

    private AjaxResponse ajaxRenderResponse(
            RequestFacade request,
            FacesContext context,
            UIComponent[] components
    ) throws IOException {

        AjaxResponse ajaxResponse = new AjaxResponse();
        // collect all initialization scripts to buffer to use them in runtime loaded js library
        StringBuilder initializationScripts = new StringBuilder();

        Map<String, Object> requestMap = context.getExternalContext().getRequestMap();
        for (UIComponent component : components) {
            UIForm form = getParentForm(component);
            if (form != null) { // for ButtonRenderer of JSF RI 1.1_02, which requires this parameter to be set by the enclosing form
                requestMap.put(COM_SUN_FACES_FORM_CLIENT_ID_ATTR, form.getClientId(context));
            }
            Log.log(context, "ajaxRenderResponse start for component " + component);
            ajaxRenderRequestedPortions(request, context, component, ajaxResponse, initializationScripts);
            Log.log(context, "ajaxRenderResponse finish for component " + component);
            if (form != null)
                requestMap.remove(COM_SUN_FACES_FORM_CLIENT_ID_ATTR);
        }

        AjaxPluginIncludes availableIncludes = PluginsLoader.getAvailableIncludes(context);
        List<String> foreignHeadScripts = availableIncludes.getScripts();
        ajaxPrepareInitializationScripts(context, ajaxResponse, foreignHeadScripts, initializationScripts);

        //todo: find component with inheader styles declaration and add corresponding functionality to AjaxPlugin(s)

        addJSLibraries(context, ajaxResponse);
        List<String> jsLibraries = availableIncludes.getJsIncludes();
        if (jsLibraries != null)
            addForeignJSLibraries(ajaxResponse, jsLibraries);

        addStyles(context, ajaxResponse, components);
        List<String> cssFiles = availableIncludes.getCssIncludes();
        if (cssFiles != null) {
            addForeignCSSFiles(ajaxResponse, cssFiles);
        }
        return ajaxResponse;
    }

    private UIForm getParentForm(UIComponent component) {
        UIComponent c = component;
        while (c != null && !(c instanceof UIForm))
            c = c.getParent();
        return (UIForm) c;
    }

    private void ajaxRenderRequestedPortions(
            RequestFacade request, FacesContext context, UIComponent component, AjaxResponse ajaxResponse,
            StringBuilder initializationScripts) throws FacesException {
        try {
            render(request, context, component, ajaxResponse, initializationScripts);
        }
        catch (IOException e) {
            throw new FacesException(e.getMessage(), e);
        }
    }

    private void render(
            RequestFacade request,
            FacesContext context,
            UIComponent component,
            AjaxResponse ajaxResponse,
            StringBuilder initializationScripts) throws IOException {
        List<String> updatePortions = AjaxUtil.getAjaxPortionNames(context, request);
        if (updatePortions.isEmpty()) {
            StringWriter wrt = new StringWriter();
            ResponseWriter originalWriter = substituteResponseWriter(context, request, wrt);
            StringBuilder outputBuffer;
            try {
                component.encodeBegin(context);
                component.encodeChildren(context);
                component.encodeEnd(context);

                outputBuffer = new StringBuilder(wrt.toString());
            } finally {
                restoreWriter(context, originalWriter);
            }
            StringBuilder rtLibraryScriptsBuffer = new StringBuilder();
            StringBuilder rawScriptsBuffer = new StringBuilder();
            extractScripts(outputBuffer, rawScriptsBuffer, rtLibraryScriptsBuffer);
            if (rtLibraryScriptsBuffer.length() > 0) {
                initializationScripts.append(rtLibraryScriptsBuffer).append("\n");
            }
            String output = outputBuffer.toString();
            String clientId = component.getClientId(context);
            ajaxResponse.addSimpleUpdate(clientId, output, rawScriptsBuffer.toString());
        } else {
            RenderKitFactory factory = (RenderKitFactory) FactoryFinder.getFactory(FactoryFinder.RENDER_KIT_FACTORY);
            RenderKit renderKit = factory.getRenderKit(context, context.getViewRoot().getRenderKitId());
            Renderer renderer = renderKit.getRenderer(component.getFamily(), component.getRendererType());
            JSONObject customJSONParam = AjaxUtil.getCustomJSONParam(context, request);
            AjaxPortionRenderer ajaxComponentRenderer = (AjaxPortionRenderer) renderer;
            for (String nextId : updatePortions) {
                StringBuilder portionOutput;
                JSONObject responseData;
                StringWriter stringWriter = new StringWriter();
                ResponseWriter originalWriter = substituteResponseWriter(context, request, stringWriter);
                try {
                    responseData = ajaxComponentRenderer.encodeAjaxPortion(context, component, nextId, customJSONParam);
                    portionOutput = new StringBuilder(stringWriter.toString());
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                } finally {
                    restoreWriter(context, originalWriter);
                }

                StringBuilder rawScriptsBuffer = new StringBuilder();
                StringBuilder rtLibraryScriptsBuffer = new StringBuilder();
                extractScripts(portionOutput, rawScriptsBuffer, rtLibraryScriptsBuffer);
                if (rtLibraryScriptsBuffer.length() > 0) {
                    initializationScripts.append(rtLibraryScriptsBuffer).append("\n");
                }
                ajaxResponse.addPortion(nextId, portionOutput.toString(), rawScriptsBuffer.toString(), responseData);
            }
        }
    }

    private void extractScripts(StringBuilder buffer,
                                StringBuilder rawScriptBuffer,
                                StringBuilder rtLibraryScriptBuffer) {
        String scriptStart = "<script";
        String scriptEnd = "/script>";
        while (true) {
            StringInspector bufferInspector = new StringInspector(buffer.toString());
            int fromIndex = bufferInspector.indexOfIgnoreCase(scriptStart);
            if (fromIndex == -1)
                break;

            int toIndex = bufferInspector.indexOfIgnoreCase(scriptEnd, fromIndex);
            if (toIndex == -1)
                break;

            toIndex += scriptEnd.length();
            String rawScript = buffer.substring(fromIndex, toIndex);
            String script = purifyScripts(new StringInspector(rawScript));

            Matcher matcher = JS_VAR_PATTERN.matcher(rawScript);
            boolean varFound = matcher.find();

            buffer.delete(fromIndex, toIndex);

            if (new StringInspector(script).indexOfIgnoreCase("document.write") > -1) {
                if (tempIdCounter == Long.MAX_VALUE) tempIdCounter = 0;
                String someId = "OpenFaces_Ajax_Placeholder:ajax_placeholser_" + tempIdCounter++;
                buffer.insert(fromIndex, "<span id=\"" + someId + "\"></span>");
                script = "O$.substituteDocumentWrite();\n" + script + "\nO$.restoreDocumentWrite('" + someId + "');\n";
            }
            if (varFound) {
                rtLibraryScriptBuffer.append(script).append("\n");
            } else {
                rawScriptBuffer.append(rawScript);
            }
        }
    }

    private String purifyScripts(StringInspector script) {
        StringBuffer result = new StringBuffer();
        int startIdx = script.indexOfIgnoreCase("<script");
        int endIdx = script.indexOfIgnoreCase("</script>");
        if (startIdx == -1 || endIdx == -1) return script.toString();
        int endScriptInit = script.toString().indexOf(">", startIdx + 1);
        if (startIdx > 0) {
            result.append(script.substring(0, startIdx));
            result.append("\n");
            script = script.substring(startIdx);
            // re-read indices
            startIdx = script.indexOfIgnoreCase("<script");
            endIdx = script.indexOfIgnoreCase("</script>");
            if (startIdx != -1)
                endScriptInit = script.toString().indexOf(">", startIdx + 1);
        }
        if (endScriptInit == -1) return script.toString();
        while (startIdx > -1) {
            result.append(script.substring(endScriptInit + 1, endIdx));
            result.append("\n");
            script = script.substring(endIdx + "</script>".length());
            // re-read indices
            startIdx = script.indexOfIgnoreCase("<script");
            endIdx = script.indexOfIgnoreCase("</script>");
            if (startIdx > -1)
                endScriptInit = script.toString().indexOf(">", startIdx + 1);
        }
        if (script.toString().length() > 0) {
            result.append(script);
            result.append("\n");
        }
        return result.toString();
    }

    private void addJSLibraries(FacesContext context, AjaxResponse ajaxResponse) {
        List<String> libraries = (List<String>) context.getExternalContext().getRequestMap().get(ResourceUtil.HEADER_JS_LIBRARIES);
        if (libraries == null) return;
        for (String jsLibrary : libraries) {
            ajaxResponse.addJsLibrary(jsLibrary);
        }
    }

    /**
     * Adds all JS libraries declarations retrieved from third-party JSF components libraries that are currently supported
     * and returned by corresponding plugin.
     */
    private void addForeignJSLibraries(AjaxResponse ajaxResponse, List<String> libraries) {
        for (String library : libraries) {
            ajaxResponse.addJsLibrary(library);
        }
    }

    private void addStyles(FacesContext context, AjaxResponse ajaxResponse, UIComponent[] components) {
        for (UIComponent component : components) {
            List<String> styleClasses = StyleUtil.getAllStyleClassesForComponent(context, component);
            addStyleClasses(ajaxResponse, styleClasses);
            StyleUtil.markStylesRenderedForComponent(context, component);
        }
    }

    private void addStyleClasses(AjaxResponse ajaxResponse, List<String> styleClasses) {
        if (styleClasses == null) return;
        for (String style : styleClasses) {
            ajaxResponse.addStyle(style);
        }
    }

    private void addForeignCSSFiles(AjaxResponse ajaxResponse, List<String> cssFiles) {
        for (String cssFile : cssFiles) {
            ajaxResponse.addCssFile(cssFile);
        }
    }

    /**
     * Save state for processed ajaxs request
     *
     * @param context
     * @param request
     * @param ajaxResponse
     * @param components
     * @param sequence
     * @return If there is server side state saving, return serializid view state. Otherwise, return null.
     *         Serialized view state is used for adjusting "com.sun.faces.VIEW" for RI faces implementation
     * @throws java.io.IOException
     */
    private AjaxSavedStateIdxHolder ajaxSaveState(
            FacesContext context,
            RequestFacade request,
            AjaxResponse ajaxResponse,
            UIComponent[] components, Integer sequence) throws IOException {
        AjaxSavedStateIdxHolder savedStateStructure = new AjaxSavedStateIdxHolder();
        StateManager stateManager = context.getApplication().getStateManager();
        boolean savingStateInClient = stateManager.isSavingStateInClient(context);
        if (savingStateInClient) {
            StringWriter stringWriter = new StringWriter();
            ResponseWriter originalWriter = substituteResponseWriter(context, request, stringWriter);
            try {
                for (UIComponent component : components) {
                    Object state = component.processSaveState(context);
                    String clientId = component.getClientId(context);
                    writeState(context, clientId, state);
                    String stateString = stringWriter.toString();
                    ajaxResponse.addComponentState(clientId, stateString);
                }
            } finally {
                restoreWriter(context, originalWriter);
            }
        } else {
            // TODO [sanders] (Apr 1, 2009, 7:32 AM): Fix deprecation
            Object view = stateManager.saveSerializedView(context);

            if (EnvironmentUtil.isMyFaces() &&
                    !EnvironmentUtil.isRichFacesStateManager(stateManager)
                    && !EnvironmentUtil.isFacelets(context)) {
                obtainViewStateSequenceForMyFaces12(context, request, sequence, savedStateStructure);
            }
            // This case is for MyFaces 1.1.5 viewState sequence updating on client-side
            // We need to get value of viewState sequence and save it for futher processing when response will be rendered to the client
            if (!EnvironmentUtil.isMyFaces() && view instanceof StateManager.SerializedView) {
                obtainViewStateSequence(context, request, view, savedStateStructure);
            } else {
                if (EnvironmentUtil.isMyFaces()
                        && EnvironmentUtil.isRichFacesStateManager(stateManager)
                        && view instanceof StateManager.SerializedView) {
                    obtainViewStateSequence(context, request, view, savedStateStructure);
                } else {
                    if (EnvironmentUtil.isMyFaces() && EnvironmentUtil.isFacelets(context)) {
                        obtainViewStateSequenceForMyFaces12(context, request, sequence, savedStateStructure);
                    }
                }
            }

            if (view instanceof StateManager.SerializedView) {
                StateManager.SerializedView serializedView = (StateManager.SerializedView) view;
                savedStateStructure.setViewStructureId(serializedView.getStructure());
            }
        }
        return savedStateStructure;
    }

    private void obtainViewStateSequence(FacesContext context, RequestFacade request, Object view,
                                         AjaxSavedStateIdxHolder stateIdxHolder) throws IOException {
        StringWriter stringWriter = new StringWriter();
        ResponseWriter originalWriter = substituteResponseWriter(context, request, stringWriter);

        StateManager.SerializedView serializedView = (StateManager.SerializedView) view;
        ResponseStateManager responseStateManager = context.getRenderKit().getResponseStateManager();
        responseStateManager.writeState(context, serializedView);

        restoreWriter(context, originalWriter);

        String stateString = stringWriter.getBuffer().toString();
        // This is necessarry to obtain valid state key for updating it on client side
        parseStateString(stateString, context, stateIdxHolder);
    }

    private void obtainViewStateSequenceForMyFaces12(FacesContext context, RequestFacade request, Integer sequence,
                                                     AjaxSavedStateIdxHolder stateIdxHolder)
            throws IOException {
        StringWriter stringWriter = new StringWriter();
        ResponseWriter originalWriter = substituteResponseWriter(context, request, stringWriter);

        if (!EnvironmentUtil.isFacelets(context)) {
            context.getApplication().getViewHandler().writeState(context);
        } else {
            try {
                ResponseStateManager responseStateManager = context.getRenderKit().getResponseStateManager();
                Method method =
                        responseStateManager.getClass().getMethod("writeState", FacesContext.class, Object.class);
                Object[] state = new Object[2];
                state[0] = Integer.toString(sequence, Character.MAX_RADIX);

                method.invoke(responseStateManager, context, state);
            } catch (NoSuchMethodException e) {
                Log.log(context, e.getMessage(), e);
            } catch (InvocationTargetException e) {
                Log.log(context, e.getMessage(), e);
            } catch (IllegalAccessException e) {
                Log.log(context, e.getMessage(), e);
            }
        }
        restoreWriter(context, originalWriter);

        String stateString = stringWriter.getBuffer().toString();
        // This is necessarry to obtain valid state key for updating it on client side
        parseStateString(stateString, context, stateIdxHolder);
    }

    private void parseStateString(String stateString, FacesContext context, AjaxSavedStateIdxHolder stateIdxHolder) {
        // This method check state string for fields that is used by MyFaces 1.1.5 ,which contains valid state key.
        checkForMyFaces115State(stateString, stateIdxHolder);
        // This method check state string for fields that is used by MyFaces 1.1.3 - 1.1.4, which contains valid state keys.
        checkForMyFacesStateFields(stateString, stateIdxHolder);
    }

    private void checkForMyFacesStateFields(String stateString,
                                            AjaxSavedStateIdxHolder stateIdxHolder) {
        if (stateString != null) {
            int indexOfJsfTree = stateString.indexOf("jsf_tree");
            int indexOfJsfViewId = stateString.indexOf("jsf_viewid");

            if (indexOfJsfTree != -1 && indexOfJsfViewId != -1) {
                int indexOfJsfTreeValue = stateString.indexOf(VALUE_ATTR_STRING, indexOfJsfTree);
                int indexOfJsfViewIdValue = stateString.indexOf(VALUE_ATTR_STRING, indexOfJsfViewId);

                int fiOfJsfTreeValue = indexOfJsfTreeValue + VALUE_ATTR_STRING.length();
                String jsfTreeString = stateString.substring(fiOfJsfTreeValue, stateString.indexOf("\"", fiOfJsfTreeValue));

                int fiOfJsfViewIdValue = indexOfJsfViewIdValue + VALUE_ATTR_STRING.length();
                String jsfViewIdString = stateString.substring(fiOfJsfViewIdValue, stateString.indexOf("\"", fiOfJsfViewIdValue));

                stateIdxHolder.setMyFacesStateId(jsfTreeString);
                stateIdxHolder.setMyFacesViewId(jsfViewIdString);
            }
        }
    }

    private void checkForMyFaces115State(String code,
                                         AjaxSavedStateIdxHolder stateIdxHolder) {
        if (code == null) {
            return;
        }

        int indexOfValue = code.indexOf(VALUE_ATTR_STRING);
        int indexOfViewState = code.indexOf("javax.faces.ViewState");
        if (indexOfValue != -1 && indexOfViewState != -1) {
            int firstIndex = indexOfValue + VALUE_ATTR_STRING.length();
            String viewStateString = code.substring(firstIndex, code.lastIndexOf("\""));

            stateIdxHolder.setViewStateIdentifier(viewStateString);
        }

    }

    private Integer getSequenceIdForMyFaces(FacesContext context) { // see JSFC-1516
        ExternalContext externalContext = context.getExternalContext();
        Object session = externalContext.getSession(false);
        if (session == null)
            return null;

        Map<String, Object> sessionMap = externalContext.getSessionMap();
        Integer sequence = (Integer) sessionMap.get(MYFACES_SEQUENCE_PARAM);
        return sequence;
    }

    private UIComponent findComponentByPath(UIComponent parent, String path) {
        return findComponentByPath(parent, path, false, false);
    }

    private UIComponent findComponentByPath(UIComponent parent,
                                            String path,
                                            boolean preProcessDecodesOnTables,
                                            boolean preRenderResponseOnTables) {
        while (true) {
            if (path == null) {
                return null;
            }

            int separator = path.indexOf(NamingContainer.SEPARATOR_CHAR, 1);
            if (separator == -1)
                return findComponentById(parent, path, true, preProcessDecodesOnTables, preRenderResponseOnTables);

            String id = path.substring(0, separator);
            UIComponent nextParent = findComponentById(parent, id, false, preProcessDecodesOnTables, preRenderResponseOnTables);
            if (nextParent == null) {
                return null;
            }
            parent = nextParent;
            path = path.substring(separator + 1);
        }
    }

    private UIComponent findComponentById(UIComponent parent, String id, boolean isLastComponentInPath,
                                          boolean preProcessDecodesOnTables, boolean preRenderResponseOnTables) {
        if (isIntegerNumber(id) && parent instanceof AbstractTable) {
            AbstractTable table = ((AbstractTable) parent);
            if (!isLastComponentInPath) {
                if (preProcessDecodesOnTables)
                    table.invokeBeforeProcessDecodes(FacesContext.getCurrentInstance());
                if (preRenderResponseOnTables) {
                    table.invokeBeforeRenderResponse(FacesContext.getCurrentInstance());
                    table.setRowIndex(-1); // make the succeding setRowIndex call provide the just-read actual row data through request-scope variables
                }

                int rowIndex = Integer.parseInt(id);
                table.setRowIndex(rowIndex);
            } else {
                int rowIndex = Integer.parseInt(id);
                table.setRowIndex(rowIndex);
            }
            return table;
        } else if (isIntegerNumber(id) && parent instanceof UIData) {
            UIData grid = ((UIData) parent);
            int rowIndex = Integer.parseInt(id);
            grid.setRowIndex(rowIndex);
            return grid;
        } else if (id.charAt(0) == ':' && parent instanceof OUIObjectIterator) {
            id = id.substring(1);
            OUIObjectIterator iterator = (OUIObjectIterator) parent;
            iterator.setObjectId(id);
            return (UIComponent) iterator;
        } else if (isIntegerNumber(id)) {
            try {
                Class clazz = Class.forName("com.sun.facelets.component.UIRepeat");
                if (clazz.isInstance(parent)) {
                    ReflectionUtil.invokeMethod("com.sun.facelets.component.UIRepeat", "setIndex",
                            new Class[]{Integer.TYPE}, new Object[]{Integer.parseInt(id)}, parent);
                    return parent;
                }
            } catch (ClassNotFoundException e) {
                //do nothing - it's ok - not facelets environment
            }

        }
        if (id.equals(parent.getId()))
            return parent;

        Iterator<UIComponent> iterator = parent.getFacetsAndChildren();
        while (iterator.hasNext()) {
            UIComponent child = iterator.next();
            if (child instanceof NamingContainer) {
                if (id.equals(child.getId()))
                    return child;
            } else {
                UIComponent component = findComponentById(child, id,
                        isLastComponentInPath, preProcessDecodesOnTables, preRenderResponseOnTables);
                if (component != null)
                    return component;
            }
        }
        return null;
    }

    private boolean isIntegerNumber(String id) {
        if (id == null || id.length() == 0)
            return false;
        for (int i = 0, length = id.length(); i < length; i++) {
            char c = id.charAt(i);
            if (!Character.isDigit(c))
                return false;
        }
        return true;
    }

    private void writeState(FacesContext context, String clientId, Object state) throws IOException {
        ResponseWriter writer = context.getResponseWriter();
        String stateStr = AjaxUtil.objectToString(state);
        String fieldName = AjaxUtil.getComponentStateFieldName(clientId);
        RenderingUtil.renderHiddenField(writer, fieldName, stateStr);
    }

    private ResponseWriter substituteResponseWriter(FacesContext context, RequestFacade request, Writer innerWriter) {
        ResponseWriter newWriter;
        ResponseWriter responseWriter = context.getResponseWriter();
        if (responseWriter != null) {
            newWriter = responseWriter.cloneWithWriter(innerWriter);
        } else {
            RenderKitFactory factory = (RenderKitFactory) FactoryFinder.getFactory(FactoryFinder.RENDER_KIT_FACTORY);
            RenderKit renderKit = factory.getRenderKit(context, context.getViewRoot().getRenderKitId());
            newWriter = renderKit.createResponseWriter(innerWriter, null, request.getCharacterEncoding());
        }
        context.setResponseWriter(newWriter);
        return responseWriter;
    }

    private void restoreWriter(FacesContext context, ResponseWriter originalWriter) {
        if (originalWriter != null)
            context.setResponseWriter(originalWriter);
    }

    /**
     * This method is copied from MyFaces 1.1.4. The only change is the clearEvents invokation (see comments at the end of
     * the method).
     */
    private void broadcastForPhase(PhaseId phaseId) {
        if (events == null) return;

        boolean abort = false;

        int phaseIdOrdinal = phaseId.getOrdinal();
        for (ListIterator<FacesEvent> listiterator = events.listIterator(); listiterator.hasNext();) {
            FacesEvent event = listiterator.next();
            int ordinal = event.getPhaseId().getOrdinal();
            if (ordinal == PhaseId.ANY_PHASE.getOrdinal() ||
                    ordinal == phaseIdOrdinal) {
                UIComponent source = event.getComponent();
                try {
                    source.broadcast(event);
                }
                catch (AbortProcessingException e) {
                    // abort event processing
                    // Page 3-30 of JSF 1.1 spec: "Throw an AbortProcessingException, to tell the JSF implementation
                    //  that no further broadcast of this event, or any further events, should take place."
                    abort = true;
                    break;
                } finally {

                    try {
                        listiterator.remove();
                    }
                    catch (ConcurrentModificationException cme) {
                        int eventIndex = listiterator.previousIndex();
                        events.remove(eventIndex);
                        listiterator = events.listIterator();
                    }
                }
            }
        }

        if (abort) {
            clearEvents();
        }

        // <MOD> added to the original code from MyFaces since this should always be performed after broadcastForPhase anyway
        FacesContext context = FacesContext.getCurrentInstance();
        if (context.getRenderResponse() || context.getResponseComplete()) {
            clearEvents();
        }
        // </MOD>

    }

    private void clearEvents() {
        events = null;
    }


    public void queueEvent(FacesEvent event) {
        if (event == null) throw new NullPointerException("event");
        if (events == null) {
            events = new ArrayList<FacesEvent>();
        }
        events.add(event);
    }
}