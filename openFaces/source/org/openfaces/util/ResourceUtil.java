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

import javax.faces.application.ViewHandler;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * @author Dmitry Pikhulya
 */
public class ResourceUtil {
    public static final String HEADER_JS_LIBRARIES = "OF:js_file_included";
    public static final String RENDERED_JS_LINKS = "org.openfaces.util.RenderingUtil.renderedJsLinks";
    public static final String JSON_JS_LIB_NAME = "json2.js";

    private static final String OPENFACES_VERSION = "/META-INF/openFacesVersion.txt";
    private static final String VERSION_PLACEHOLDER_STR = "version";

    private ResourceUtil() {
    }

    /**
     * This method returns the URL string ready for rendering into HTML based on the URL specified by the user. If
     * URL is not specified by the user explicitly then URL to a default internal resource is returned instead.
     *
     * @param userSpecifiedUrl             optional resource url as specified by the user. This can be a relative URL, or an absolute URL
     * @param defaultResourceFileName      file name for a resource which should be provided if userSpecifiedUrl is null or empty string
     * @param defaultResourceBaseClassName the class relatively to which defaultResourceFileName is specified
     * @return
     */
    public static String getResourceURL(FacesContext context, String userSpecifiedUrl,
                                        Class defaultResourceBaseClassName, String defaultResourceFileName) {
        return getResourceURL(context, userSpecifiedUrl, defaultResourceBaseClassName, defaultResourceFileName, true);
    }

    /**
     * @param userSpecifiedUrl             optional resource url as specified by the user. This can be a relative URL, or an absolute URL
     * @param defaultResourceFileName      file name for a resource which should be provided if userSpecifiedUrl is null (or empty string).
     *                                     Empty string is also considered as signal for returning the default resource here because null
     *                                     is auto-converted to an empty string when passed through a string binding
     * @param defaultResourceBaseClassName the class relatively to which defaultResourceFileName is specified
     * @param prependContextPath           use true here if you render the attribute yourself, and false if you use pass this URL to HtmlGraphicImage or similar component
     */
    public static String getResourceURL(
            FacesContext context,
            String userSpecifiedUrl,
            Class defaultResourceBaseClassName,
            String defaultResourceFileName,
            boolean prependContextPath) {
        boolean returnDefaultResource = userSpecifiedUrl == null || userSpecifiedUrl.length() == 0;
        String result = returnDefaultResource
                ? getInternalResourceURL(context, defaultResourceBaseClassName, defaultResourceFileName, prependContextPath)
                : (prependContextPath ? getApplicationResourceURL(context, userSpecifiedUrl) : userSpecifiedUrl);
        return result;
    }

    /**
     * Get path to application resource according to contex and resource path
     *
     * @param context      faces contex provided by application
     * @param resourcePath path to resource - either absolute (starting with a slash) in the scope of application context,
     *                     or relative to the current page
     * @return full URL to resource ready for rendering as <code>src</code> or <code>href</code> attribute's value.
     */
    public static String getApplicationResourceURL(FacesContext context, String resourcePath) {
        if (resourcePath == null || resourcePath.length() == 0)
            return "";
        ViewHandler viewHandler = context.getApplication().getViewHandler();
        String resourceUrl = viewHandler.getResourceURL(context, resourcePath);
        String encodedResourceUrl = context.getExternalContext().encodeResourceURL(resourceUrl);
        return encodedResourceUrl;
    }

    public static String getInternalResourceURL(FacesContext context, Class componentClass, String resourceName) {
        return getInternalResourceURL(context, componentClass, resourceName, true);
    }

    /**
     * @param context            Current FacesContext
     * @param componentClass     Class, relative to which the resourcePath is specified
     * @param resourcePath       Path to the resource file
     * @param prependContextPath true means that the resulting url should be prefixed with context root. This is the case
     *                           when the returned URL is rendered without any modifications. Passing false to this
     *                           parameter is required in cases when the returned URL is passed to some component which
     *                           expects application URL, so the component will prepend the URL with context root tself.
     * @return The requested URL
     */
    public static String getInternalResourceURL(
            FacesContext context,
            Class componentClass,
            String resourcePath,
            boolean prependContextPath) {
        if (context == null) throw new NullPointerException("context");
        if (resourcePath == null) throw new NullPointerException("resourcePath");
        if (componentClass == null) throw new NullPointerException("componentClass");

        String packageName = getPackageName(componentClass);
        String packagePath = packageName.replace('.', '/');

        String versionString = getVersionString();
        String urlRelativeToContextRoot = ResourceFilter.INTERNAL_RESOURCE_PATH + packagePath + "/" +
                resourcePath.substring(0, resourcePath.lastIndexOf(".")) + "-" + versionString + resourcePath.substring(resourcePath.lastIndexOf("."));

        if (!prependContextPath)
            return urlRelativeToContextRoot;

        return getApplicationResourceURL(context, urlRelativeToContextRoot);
    }

    private static String versionString;

    /**
     *
     * Return version of OpenFaces
     *
     * @return requested version of OpenFaces
     */
    public static String getVersionString() {
        if (versionString != null)
            return versionString;

        InputStream versionStream = ResourceUtil.class.getResourceAsStream(OPENFACES_VERSION);
        String version = "";
        if (versionStream != null) {
            try {
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(versionStream));
                String buildInfo = bufferedReader.readLine();
                bufferedReader.close();
                if (buildInfo != null) {
                    version = buildInfo.substring(0, buildInfo.indexOf(",")).trim();
                }
            } catch (IOException e) {
                Log.log("Couldn't read version string", e);
            }
        }
        if (VERSION_PLACEHOLDER_STR.equals(version)) {
            long startTime = System.currentTimeMillis() / 1000;
            version = Long.toString(startTime, 36);
        }

        versionString = version;
        return version;
    }

    /**
     *
     * Return URL of util.js file
     *
     * @param context {@link FacesContext} for the current request
     * @return requested URL of util.js file
     */
    public static String getUtilJsURL(FacesContext context) {
        return getInternalResourceURL(context, ResourceUtil.class, "util.js");
    }

    /**
     * Return URL of util.js file. Keep in mind, that  ajaxUtil.js depends on util.js.
     * Don't forget to include util.js as well before including this URL.
     *
     * @param context {@link FacesContext} for the current request
     * @return requested URL of ajaxUtil.js file
     */
    public static String getAjaxUtilJsURL(FacesContext context) {
        return ResourceUtil.getInternalResourceURL(context, ResourceUtil.class, "ajaxUtil.js");
    }

    /**
     *
     * Return URL of json javascript file
     *
     * @param context {@link FacesContext} for the current request
     * @return requested URL of json javascript file
     */
    public static String getJsonJsURL(FacesContext context) {
        return ResourceUtil.getInternalResourceURL(context, ResourceUtil.class, JSON_JS_LIB_NAME);
    }

    /**
     * Return full package name for Class
     *
     * @param aClass The Class object
     * @return full package name for given Class 
     */
    public static String getPackageName(Class aClass) {
        String className = aClass.getName();
        int lastIndexOfDot = className.lastIndexOf('.');
        if (lastIndexOfDot == -1) {
            return "";
        } else {
            return className.substring(0, lastIndexOfDot);
        }
    }

    /**
     *
     * Register javascript library to future adding to response 
     *
     * @param context {@link FacesContext} for the current request
     * @param baseClass Class, relative to which the resourcePath is specified
     * @param relativeJsPath Path to the javascript file
     */
    public static void registerJavascriptLibrary(FacesContext context, Class baseClass, String relativeJsPath) {
        String jsFileUrl = getInternalResourceURL(context, baseClass, relativeJsPath);
        registerJavascriptLibrary(context, jsFileUrl);
    }

    /**
     *
     * Register javascript library to future adding to response
     *
     * @param facesContext {@link FacesContext} for the current request
     * @param jsFileUrl Url for the javascript file
     */
    public static void registerJavascriptLibrary(FacesContext facesContext, String jsFileUrl) {
        Map<String, Object> requestMap = facesContext.getExternalContext().getRequestMap();
        List<String> libraries = (List<String>) requestMap.get(HEADER_JS_LIBRARIES);
        if (libraries == null) {
            libraries = new ArrayList<String>();
            requestMap.put(HEADER_JS_LIBRARIES, libraries);
        }

        if (libraries.contains(jsFileUrl)) return;
        libraries.add(jsFileUrl);

    }

    public static void processHeadResources(FacesContext context) {
        Map<String, Object> requestMap = context.getExternalContext().getRequestMap();

        Class richfacesContextClass = null;
        try {
            richfacesContextClass = Class.forName("org.ajax4jsf.context.AjaxContext");
        } catch (ClassNotFoundException e) {
            // Just checking for class presense. It's normal that a class can be absent.
        }

        String ajax4jsfScriptParameter = (String) ReflectionUtil.getStaticFieldValue(richfacesContextClass, "SCRIPTS_PARAMETER");
        String ajax4jsfStylesParameter = (String) ReflectionUtil.getStaticFieldValue(richfacesContextClass, "STYLES_PARAMETER");

        if (ajax4jsfStylesParameter != null) {
            LinkedHashSet<String> styles = (LinkedHashSet<String>) requestMap.get(ajax4jsfStylesParameter);
            String defaultCssUrl = ((HttpServletRequest) context.getExternalContext().getRequest()).getContextPath()
                    + ResourceFilter.INTERNAL_RESOURCE_PATH + "org/openfaces/renderkit/default" + "-" + getVersionString() + ".css";
            if (styles == null) {
                styles = new LinkedHashSet<String>();
            }
            styles.add(defaultCssUrl);
            requestMap.put(ajax4jsfStylesParameter, styles);
        }

        if (ajax4jsfScriptParameter != null) {
            LinkedHashSet<String> libraries = (LinkedHashSet<String>) requestMap.get(ajax4jsfScriptParameter);
            List<String> ourLibraries = (List<String>) requestMap.get(HEADER_JS_LIBRARIES);

            if (libraries == null) {
                libraries = new LinkedHashSet<String>();
            }

            if (ourLibraries != null) {
                libraries.addAll(ourLibraries);
            }

            requestMap.put(ajax4jsfScriptParameter, libraries);
        }
    }


    /**
     *
     * Register OpenFaces javascript library util.js to future adding to response
     *
     * @param facesContext {@link FacesContext} for the current request
     */
    public static void registerUtilJs(FacesContext facesContext) {
        registerJavascriptLibrary(facesContext, RenderingUtil.class, "util.js");
    }

    public static boolean isHeaderIncludesRegistered(ServletRequest servletRequest) {
        if (AjaxUtil.isAjaxRequest(RequestFacade.getInstance(servletRequest))) return false;
        for (Iterator<String> iterator = StyleUtil.getClassKeyIterator(); iterator.hasNext();) {
            String key = iterator.next();
            if (servletRequest.getAttribute(key) != null) {
                return true;
            }
        }

        return servletRequest.getAttribute(RenderingUtil.ON_LOAD_SCRIPTS_KEY) != null ||
               servletRequest.getAttribute(HEADER_JS_LIBRARIES) != null || 
               servletRequest.getAttribute(StyleUtil.DEFAULT_CSS_REQUESTED) != null;
    }

    //  public static void registerJavascriptLibrary(RequestFacade servletRequest, String relativeJsPath) {
//
//    List libraries = (List) servletRequest.getAttribute(ResourceFilter.HEADER_JS_LIBRARIES);
//    if (libraries == null) {
//      libraries = new ArrayList();
//      libraries.add(relativeJsPath);
//    }

/*
    if (AjaxUtil.isAjax4jsfRequest((HttpServletRequest) facesContext.getExternalContext().getRequest())) {
      ResponseWriter writer = facesContext.getResponseWriter();
      try {
        writer.startElement("script", null);
        writer.writeAttribute("type", "text/javascript", null);
        writer.writeAttribute("src", jsFileUrl, null);
        writer.endElement("script");
      } catch (IOException e) {
        e.printStackTrace();
      }
    } else {
*/
//    if (libraries.contains(relativeJsPath)) return;
//    libraries.add(relativeJsPath);
//
//  }

    /**
     * Render javascript file link, if not rendered early
     *
     * @param jsFile Javascript file to include 
     * @param context {@link FacesContext} for the current request 
     * @throws IOException if an input/output error occurs
     */
    public static void renderJSLinkIfNeeded(String jsFile, FacesContext context) throws IOException {
        ResponseWriter writer = context.getResponseWriter();
        List<String> renderedJsLinks = getRenderedJsLinks(context);
        if (renderedJsLinks.contains(jsFile)) {
            return;
        }

        if (AjaxUtil.isAjaxRequest(context)) {
            registerJavascriptLibrary(context, jsFile);
        } else if (AjaxUtil.isAjax4jsfRequest()) {
            registerJavascriptLibrary(context, jsFile);
        } else {
            writer.startElement("script", null);
            writer.writeAttribute("type", "text/javascript", null);
            writer.writeAttribute("src", jsFile, null);
            // write white-space to avoid creating self-closing <script/> tags
            // under certain servers, which are not correctly interpreted by browsers (JSFC-2303)
            writer.writeText(" ", null);
            writer.endElement("script");
        }

        renderedJsLinks.add(jsFile);
    }

    /**
     *
     * Return list of already rendered javascript links
     *
     * @param context {@link FacesContext} for the current request
     * @return list of already rendered javascript links
     */
    public static List<String> getRenderedJsLinks(FacesContext context) {
        Map<String, Object> requestMap = context.getExternalContext().getRequestMap();
        List<String> renderedJsLinks = (List<String>) requestMap.get(RENDERED_JS_LINKS);
        if (renderedJsLinks == null) {
            renderedJsLinks = new ArrayList<String>();
            requestMap.put(RENDERED_JS_LINKS, renderedJsLinks);
        }
        return renderedJsLinks;
    }

    /**
     * Return URL to clear.gif image
     * @param context {@link FacesContext} for the current request
     * @return URL to clear.gif image
     */
    public static String getClearGif(FacesContext context) {
        return ResourceUtil.getInternalResourceURL(context, ResourceUtil.class, "clear.gif");
    }
}