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
package org.openfaces.renderkit.ajax;

import org.openfaces.component.ajax.AjaxSettings;
import org.openfaces.component.ajax.SessionExpiration;
import org.openfaces.org.json.JSONObject;
import org.openfaces.renderkit.AjaxPortionRenderer;
import org.openfaces.util.ResourceUtil;
import org.openfaces.renderkit.window.ConfirmationRenderer;
import org.openfaces.util.AjaxUtil;
import org.openfaces.util.RenderingUtil;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import java.io.IOException;

/**
 * @author Eugene Goncharov
 */
public class AjaxSettingsRenderer extends AbstractSettingsRenderer implements AjaxPortionRenderer {

    public void encodeBegin(FacesContext context, UIComponent component) throws IOException {
        AjaxSettings ajaxSettings = (AjaxSettings) component;
        if (!(AjaxUtil.isAjaxRequest(context))) {
            processOnsessionexpired(context, ajaxSettings);
            ResourceUtil.renderJSLinkIfNeeded(ResourceUtil.getUtilJsURL(context), context);
            ResourceUtil.renderJSLinkIfNeeded(ResourceUtil.getInternalResourceURL(context,
                    ConfirmationRenderer.class, ConfirmationRenderer.JS_SCRIPT_URL), context);
        }

        processOnerror(context, ajaxSettings);

        if (!(AjaxUtil.isAjaxRequest(context))) {
            processOnajaxstart(context, ajaxSettings);
            processOnajaxend(context, ajaxSettings);
        }
    }

    public JSONObject encodeAjaxPortion(FacesContext context, UIComponent component, String portionName, JSONObject jsonParam) throws IOException {
        encodeBegin(context, component);
        encodeChildren(context, component);
        return null;
    }

    public void encodeChildren(FacesContext context, UIComponent component) throws IOException {
        AjaxSettings ajaxSettings = (AjaxSettings) component;
        if (isAjaxSessionExpirationProcessing(context)) {

            SessionExpiration expirationFacet = ajaxSettings.getSessionExpiration();
            expirationFacet.encodeAll(context);
        }

        if (isAjaxErrorProcessing(context)) {

            // TODO [Eugene Goncharov]: add error processing here
        }

        processAjaxProgressMessage(context, (AjaxSettings) component);
    }

    private void processAjaxProgressMessage(FacesContext context, AjaxSettings ajaxSettings) throws IOException {
        UIComponent progressMessage = ajaxSettings.getProgressMessage();
        if (progressMessage != null) {
            progressMessage.encodeAll(context);
        }
    }

    public boolean getRendersChildren() {
        return true;
    }

    private void processOnsessionexpired(FacesContext context, AjaxSettings component) throws IOException {
        if (component.getOnsessionexpired() == null)
            return;

        processEvent(context, component, "onsessionexpired", component.getOnsessionexpired());
    }


    private void processOnerror(FacesContext context, AjaxSettings component) throws IOException {
        if (component.getOnerror() == null)
            return;

        processEvent(context, component, "onerror", component.getOnerror());
    }


    private void processOnajaxstart(FacesContext context, AjaxSettings component) throws IOException {
        if (component.getOnajaxstart() == null)
            return;

        processEvent(context, component, "onajaxstart", component.getOnajaxstart());
    }

    private void processOnajaxend(FacesContext context, AjaxSettings component) throws IOException {
        if (component.getOnajaxend() == null)
            return;

        processEvent(context, component, "onajaxend", component.getOnajaxend());
    }


}