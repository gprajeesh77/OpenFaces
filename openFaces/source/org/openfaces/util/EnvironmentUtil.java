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
package org.openfaces.util;

import org.openfaces.ajax.AjaxViewHandler;
import org.openfaces.ajax.ViewHandlerWrapper;

import javax.faces.application.Application;
import javax.faces.application.StateManager;
import javax.faces.application.ViewHandler;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

// TODO [sanders] (Apr 1, 2009, 8:36 AM): rename to Environment
/**
 * @author Eugene Goncharov
 */
public class EnvironmentUtil {
    public static final String TEAMDEV_JSF_ENVIRONMENT_TRINIDAD_SUPPORT = "org.openfaces.environment.trinidadSupport";

    private static final String KEY_UNDEFINED_BROWSER = "undefined_browser";
    private static final String KEY_EXPLORER_BROWSER = "explorer_browser";
    private static final String KEY_OPERA_BROWSER = "opera_browser";
    private static final String KEY_MOZILLA_BROWSER = "mozilla_browser";
    private static final String KEY_SAFARI_BROWSER = "safari_browser";

    private static final String XHTML_CONTENT_TYPE = "application/xhtml+xml";

    private static final String TEAMDEV_JSF_FACELETS = "org.openfaces.facelets";

    private EnvironmentUtil() {
    }


    public static boolean isLiferay(Map requestMap) {
        boolean isLiferayRequest;
        isLiferayRequest = (requestMap != null) && requestMap.containsKey("com.liferay.portal.kernel.servlet.PortletServletRequest");

        if (!isLiferayRequest) {
            FacesContext context = FacesContext.getCurrentInstance();
            ExternalContext externalContext = context.getExternalContext();
            Object session = externalContext.getSession(false);
            isLiferayRequest = session != null &&
                    session.getClass().getName().equalsIgnoreCase("com.liferay.portlet.PortletSessionImpl");
        }


        return isLiferayRequest;
    }

    public static boolean isRI() {
        boolean jsfRI;
        try {
            Class.forName("com.sun.faces.application.ApplicationImpl");
            jsfRI = true;
        } catch (ClassNotFoundException e) {
            jsfRI = false;
        }
        return jsfRI;
    }

    public static boolean isFacelets(FacesContext context) {
        Map<String, Object> applicationMap = context.getExternalContext().getApplicationMap();
        String isFaceletsKey = AjaxViewHandler.class.getName() + ".isFacelets";
        Boolean faceletsFlag = (Boolean) applicationMap.get(isFaceletsKey);
        if (faceletsFlag == null) {
            boolean facelets = isFacelets_internal(context);
            faceletsFlag = facelets ? Boolean.TRUE : Boolean.FALSE;
            applicationMap.put(isFaceletsKey, faceletsFlag);
        }
        return faceletsFlag;
    }

    private static boolean isFacelets_internal(FacesContext context) {
        Class faceletsViewHandlerClass;
        try {
            faceletsViewHandlerClass = Class.forName("com.sun.facelets.FaceletViewHandler");
        } catch (ClassNotFoundException e) {
            return false;
        }

        String faceletsParameter = context.getExternalContext().getInitParameter(TEAMDEV_JSF_FACELETS);
        if (faceletsParameter != null && faceletsParameter.equalsIgnoreCase("true")) {
            return true;
        }

        // the presense of the facelets.ui.Repeat class doesn't yet mean that this application really uses facelets
        // e.g. JBoss Portal has facelets in classpath, which makes it possible to create the facelets repeat component
        Application application = context.getApplication();
        ViewHandler handler = application.getViewHandler();
        while (true) {
            if (handler instanceof ViewHandlerWrapper) {
                handler = ((ViewHandlerWrapper) handler).getDelegate();
                continue;
            }
            break;
        }
        Class handlerClass = handler.getClass();
        String actualViewHandlerClassName = handlerClass.getName();
        boolean faceletsViewHandler =
                actualViewHandlerClassName.equals("com.sun.facelets.FaceletViewHandler") ||
                        actualViewHandlerClassName.equals("org.jboss.seam.ui.facelet.SeamFaceletViewHandler");

        if (!faceletsViewHandler) {
            faceletsViewHandler = isFaceletsViewHandlerInUse(handler, faceletsViewHandlerClass);
        }

        return faceletsViewHandler;
    }

    private static boolean isFaceletsViewHandlerInUse(Object handler, Class faceletsViewHandlerClass) {
        boolean faceletsViewHandler = false;

        Object resultHandler = getWrappedHandler(handler);

        if (resultHandler != null) {
            faceletsViewHandler = isFaceletsViewHandler(resultHandler, faceletsViewHandlerClass);

            if (!faceletsViewHandler && resultHandler instanceof ViewHandlerWrapper) {
                resultHandler = ((ViewHandlerWrapper) resultHandler).getDelegate();
                faceletsViewHandler = isFaceletsViewHandler(resultHandler, faceletsViewHandlerClass);
                if (!faceletsViewHandler) {
                    isFaceletsViewHandlerInUse(resultHandler, faceletsViewHandlerClass);
                }
            }
        }
        return faceletsViewHandler;
    }

    private static boolean isFaceletsViewHandler(Object resultHandler, Class faceletsViewHandlerClass) {
        return faceletsViewHandlerClass.isAssignableFrom(resultHandler.getClass());
    }

    private static Object getWrappedHandler(Object handler) {
        if (handler.getClass().getName().equalsIgnoreCase("org.apache.portals.bridges.jsf.PortletViewHandlerImpl")) {
            try {
                Class portletHandlerClass = handler.getClass();
                Field handlerField = portletHandlerClass.getDeclaredField("handler");
                handlerField.setAccessible(true);
                return handlerField.get(handler);

            } catch (IllegalAccessException e) {
                return null;
            } catch (NoSuchFieldException e) {
                return null;
            }
        }
        return null;
    }

    public static boolean isMyFaces() {
        boolean myFaces;
        try {
            Class.forName("org.apache.myfaces.application.ApplicationImpl");
            myFaces = true;
        } catch (ClassNotFoundException e) {
            myFaces = false;
        }
        return myFaces;
    }

    public static boolean isAjax4jsfRequest() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        ExternalContext externalContext = facesContext.getExternalContext();
        try {
            Class ajax4jsfContextClazz = Class.forName("org.ajax4jsf.context.AjaxContext");
            Method currentInstanceMethod = ajax4jsfContextClazz.getMethod("getCurrentInstance", FacesContext.class);
            Object currentInstance = currentInstanceMethod.invoke(null, facesContext);
            if (currentInstance == null)
                return false;

            boolean isFacesContextInUse = true;
            Method isAjaxRequestMethod = null;
            try {
                isAjaxRequestMethod = ajax4jsfContextClazz.getMethod("isAjaxRequest", FacesContext.class);
            } catch (NoSuchMethodException e) {
                // We will try to search for this method without FacesContext in params
            } catch (SecurityException e) {
                //
            }
            if (isAjaxRequestMethod == null) {
                isAjaxRequestMethod = ajax4jsfContextClazz.getMethod("isAjaxRequest");
                isFacesContextInUse = false;
            }
            Boolean result = (isFacesContextInUse)
                    ? (Boolean) isAjaxRequestMethod.invoke(currentInstance, facesContext)
                    : (Boolean) isAjaxRequestMethod.invoke(currentInstance);

            return result != null && result;
        } catch (ClassNotFoundException e) {
            return false;
        } catch (NoSuchMethodException e) {
            externalContext.log("Ajax4jsf support is disabled because getCurrentInstance method was not " +
                    "found in org.ajax4jsf.context.AjaxContext.", e);
            return false;
        } catch (IllegalAccessException e) {
            externalContext.log("Ajax4jsf support is disabled because exception was thrown during " +
                    "execution of getCurrentInstance method in org.ajax4jsf.context.AjaxContext", e);
            return false;
        } catch (InvocationTargetException e) {
            externalContext.log("Ajax4jsf support is disabled because exception was thrown during " +
                    "execution of getCurrentInstance method in org.ajax4jsf.context.AjaxContext", e);
            return false;
        }
    }

    public static boolean isRichFacesStateManager(StateManager stateManager) {
        return stateManager.getClass().getName().equalsIgnoreCase("org.ajax4jsf.application.AjaxStateManager");
    }

    public static boolean isExoPortal() {
        try {
            Class.forName("org.exoplatform.portlet.faces.component.UIExoViewRoot");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static boolean isTrinidad() {
        Map applicationMap = FacesContext.getCurrentInstance().getExternalContext().getApplicationMap();
        Boolean trinidadSupport = (Boolean) applicationMap.get(TEAMDEV_JSF_ENVIRONMENT_TRINIDAD_SUPPORT);
        return trinidadSupport != null && trinidadSupport.equals(Boolean.TRUE);
    }

    public static boolean isExplorer() {
        Boolean isExplorer = (Boolean) getSessionMap().get(KEY_EXPLORER_BROWSER);
        if (isExplorer == null) {
            isExplorer = checkBrowser("msie") && !isOpera();
            getSessionMap().put(KEY_EXPLORER_BROWSER, isExplorer);
        }
        return isExplorer;
    }

    public static boolean isMozilla() {
        Boolean isMozilla = (Boolean) getSessionMap().get(KEY_MOZILLA_BROWSER);
        if (isMozilla == null) {
            isMozilla = checkBrowser("mozilla") && !isExplorer() && !isSafari() && !isOpera();
            getSessionMap().put(KEY_MOZILLA_BROWSER, isMozilla);
        }
        return isMozilla;
    }

    private static Map<String, Object> getSessionMap() {
        return FacesContext.getCurrentInstance().getExternalContext().getSessionMap();
    }

    public static boolean isOpera() {
        Boolean isOpera = (Boolean) getSessionMap().get(KEY_OPERA_BROWSER);
        if (isOpera == null) {
            isOpera = checkBrowser("opera");
            getSessionMap().put(KEY_OPERA_BROWSER, isOpera);
        }
        return isOpera;
    }

    public static boolean isSafari() {
        Boolean isSafari = (Boolean) getSessionMap().get(KEY_SAFARI_BROWSER);
        if (isSafari == null) {
            isSafari = checkBrowser("safari");
            getSessionMap().put(KEY_SAFARI_BROWSER, isSafari);
        }
        return isSafari;
    }

    public static boolean isChrome() {
        Boolean isSafari = (Boolean) getSessionMap().get(KEY_SAFARI_BROWSER);
        if (isSafari == null) {
            isSafari = checkBrowser("chrome");
            getSessionMap().put(KEY_SAFARI_BROWSER, isSafari);
        }
        return isSafari;
    }

    public static boolean isXhtmlPlusXmlContentType(FacesContext context) {
        String contentType;
        try {
            contentType = context.getResponseWriter().getContentType();
        } catch (UnsupportedOperationException e) {
            // this is the case for eXo Portal: org.exoplatform.faces.context.HtmlResponseWriter.getContentType throws UnsupportedOperationException
            return false;
        }
        boolean result = XHTML_CONTENT_TYPE.equals(contentType);
        return result;
    }

    public static boolean isMozillaXhtmlPlusXmlContentType(FacesContext context) {
        boolean result = isMozilla() && isXhtmlPlusXmlContentType(context);
        return result;
    }

    /**
     * Browser cannot be detected under certain configurations (such as Liferay Portal). So we should provide decent
     * behavior even in such situations.
     *
     * @return true, when the browser cannot be detected due to specific configuration (e.g. Liferay Portal doesn't allow
     *         retrieving user-agent). Note that this doesn't mean that the request comes from the unsupported browser.
     */
    public static boolean isUndefinedBrowser() {
        Boolean isUndefined = (Boolean) getSessionMap().get(KEY_SAFARI_BROWSER);
        if (isUndefined == null) {
            isUndefined = checkBrowser(KEY_UNDEFINED_BROWSER);
            getSessionMap().put(KEY_SAFARI_BROWSER, isUndefined);
        }
        return isUndefined;
    }

    // TODO [sanders] (Apr 1, 2009, 8:48 AM): consider renaming to isBrowser
    public static boolean checkBrowser(String browserName) {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        String userAgent = getUserAgent(facesContext);
        if (userAgent == null)
            userAgent = KEY_UNDEFINED_BROWSER;

        return userAgent.toLowerCase().contains(browserName.toLowerCase());
    }

    public static boolean isMozillaFF2(FacesContext context) {//todo review this method
        String userAgent = getUserAgent(context);
        if (userAgent == null)
            userAgent = " ";
        return userAgent.contains("Firefox/2.");
    }

    private static String getUserAgent(FacesContext context) {
        RequestFacade request = RequestFacade.getInstance(context.getExternalContext().getRequest());
        String userAgent = request.getHeader("user-agent");
        return userAgent;
    }

}