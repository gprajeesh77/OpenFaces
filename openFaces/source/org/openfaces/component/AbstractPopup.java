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
package org.openfaces.component;

import org.openfaces.util.RenderingUtil;
import org.openfaces.util.ResourceUtil;
import org.openfaces.util.Script;
import org.openfaces.util.ScriptBuilder;

import javax.faces.component.UIPanel;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import java.io.IOException;

/**
 * @author Kharchenko
 */
public abstract class AbstractPopup extends UIPanel {
    public static final String COMPONENT_TYPE = "org.openfaces.Popup";
    public static final String COMPONENT_FAMILY = "org.openfaces.Popup";

    private static final String POPUP_STYLE = "position: absolute; visibility: hidden;";

    protected AbstractPopup() {
    }

    public String getFamily() {
        return COMPONENT_FAMILY;
    }

    public void encodeBegin(FacesContext context) throws IOException {
        if (!isRendered()) return;
        encodeOpeningTags(context);
    }

    protected void encodeOpeningTags(FacesContext context) throws IOException {
        ResponseWriter writer = context.getResponseWriter();
        writer.startElement("div", this);
        writer.writeAttribute("id", getClientId(context), "id");
        writer.writeAttribute("style", getPopupStyle(), null);
    }

    protected String getPopupStyle() {
        return POPUP_STYLE;
    }

    public void encodeChildren(FacesContext context) throws IOException {
        if (!isRendered()) return;
        encodeContent(context);
    }

    public void encodeEnd(FacesContext context) throws IOException {
        if (!isRendered()) return;
        encodeClosingTags(context);

        renderInitScript(context);
    }

    protected void encodeClosingTags(FacesContext context) throws IOException {
        ResponseWriter writer = context.getResponseWriter();
        writer.endElement("div");
    }

    /**
     * Abstract method for popup's content rendering
     */
    protected abstract void encodeContent(FacesContext context) throws IOException;


    public boolean getRendersChildren() {
        return true;
    }

    private void renderInitScript(FacesContext context) throws IOException {
        boolean useDisplayNoneByDefault = getUseDisplayNoneByDefault();
        Script initScript = new ScriptBuilder().initScript(context, this, "O$._initPopup",
                useDisplayNoneByDefault).semicolon();
        RenderingUtil.renderInitScript(context, initScript, new String[]{
                ResourceUtil.getUtilJsURL(context),
                ResourceUtil.getInternalResourceURL(context, AbstractPopup.class, "popup.js")}
        );
    }

    protected boolean getUseDisplayNoneByDefault() {
        return false;
    }

}