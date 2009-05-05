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
package org.openfaces.renderkit.window;

import org.openfaces.component.AbstractPopup;
import org.openfaces.component.window.PopupLayer;
import org.openfaces.renderkit.RendererBase;
import org.openfaces.util.RenderingUtil;
import org.openfaces.util.ResourceUtil;
import org.openfaces.util.ScriptBuilder;
import org.openfaces.util.StyleUtil;
import org.openfaces.util.AjaxUtil;
import org.openfaces.util.DefaultStyles;
import org.openfaces.util.EnvironmentUtil;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import java.io.IOException;

/**
 * @author Andrew Palval
 */
public class PopupLayerRenderer extends RendererBase {
    private static final String VISIBLE_HIDDEN_FIELD_SUFFIX = "::visible";
    private static final String LEFT_HIDDEN_FIELD_SUFFIX = "::left";
    private static final String TOP_HIDDEN_FIELD_SUFFIX = "::top";
    private static final String DEFAULT_MODEL_DIV_CLASS = "o_popuplayer_modal_layer";
    public static final String BLOCKING_LAYER_SUFFIX = "::blockingLayer";

    public void encodeBegin(FacesContext context, UIComponent component) throws IOException {
        if (AjaxUtil.getSkipExtraRenderingOnPortletsAjax(context))
            return;

        ResponseWriter writer = context.getResponseWriter();
        PopupLayer popup = (PopupLayer) component;

        String clientId = popup.getClientId(context);
        if (popup.isModal()) {
            writer.startElement("div", popup);
            writer.writeAttribute("id", clientId + BLOCKING_LAYER_SUFFIX, null);
            writer.writeAttribute("name", clientId + BLOCKING_LAYER_SUFFIX, null);
            String modalDivClass = StyleUtil.getCSSClass(context,
                    component, popup.getModalLayerStyle(),
                    popup.getModalLayerClass());
            modalDivClass = StyleUtil.mergeClassNames(DEFAULT_MODEL_DIV_CLASS, modalDivClass);
            writer.writeAttribute("class", modalDivClass, null);
            writer.writeAttribute("style", "position: absolute; display: none;", null);
            writer.endElement("div");
        }

        writer.startElement("div", component);
        writer.writeAttribute("id", clientId, "id");

        String defaultClass = getDefaultClassName() +
                " " + DefaultStyles.getBackgroundColorClass();
        if (popup.isModal())
            defaultClass += " o_popuplayer_modal";

        String styleNames = StyleUtil.getCSSClass(context,
                component, popup.getStyle(), defaultClass, popup.getStyleClass());
        writeAttribute(writer, "class", styleNames);

        writeStandardEvents(writer, popup);
    }

    protected String getDefaultClassName() {
        return "o_popuplayer";
    }

    public void encodeChildren(FacesContext context, UIComponent component) throws IOException {
        if (AjaxUtil.getSkipExtraRenderingOnPortletsAjax(context))
            return;

        PopupLayer popup = (PopupLayer) component;
        RenderingUtil.renderChildren(context, popup);
        encodeCustomContent(context, popup);
    }

    protected void encodeCustomContent(FacesContext context, PopupLayer popupLayer) throws IOException {
    }

    public boolean getRendersChildren() {
        return true;
    }

    public void encodeEnd(FacesContext context, UIComponent component) throws IOException {
        if (AjaxUtil.getSkipExtraRenderingOnPortletsAjax(context))
            return;

        encodeScriptsAndStyles(context, (PopupLayer) component);
        StyleUtil.renderStyleClasses(context, component);

        ResponseWriter writer = context.getResponseWriter();
        writer.endElement("div");
    }

    protected void encodeScriptsAndStyles(FacesContext context, PopupLayer popup) throws IOException {
        ResponseWriter writer = context.getResponseWriter();

        String popupInvokerId = popup.getAnchorElementId();
        if (popupInvokerId != null) {
            UIComponent invokerComponent = popup.findComponent(popupInvokerId);
            if (invokerComponent != null) {
                popupInvokerId = invokerComponent.getClientId(context);
            }
        }

        String clientId = popup.getClientId(context);
        RenderingUtil.renderHiddenField(writer, clientId + VISIBLE_HIDDEN_FIELD_SUFFIX, Boolean.toString(popup.isVisible()));
        RenderingUtil.renderHiddenField(writer, clientId + LEFT_HIDDEN_FIELD_SUFFIX, popup.getLeft());
        RenderingUtil.renderHiddenField(writer, clientId + TOP_HIDDEN_FIELD_SUFFIX, popup.getTop());
//    RenderingUtil.renderHiddenField(writer, clientId + ANCHOR_HIDDEN_FIELD_SUFFIX, popupInvokerId);

        if (popup.getHideOnOuterClick()) {
            ScriptBuilder buf = new ScriptBuilder();
            buf.functionCall("O$._initPopup", clientId).semicolon();
            RenderingUtil.renderInitScript(context, buf, new String[]{
                    ResourceUtil.getUtilJsURL(context),
                    ResourceUtil.getInternalResourceURL(context, AbstractPopup.class, "popup.js"),
            });
        }

        ScriptBuilder sb = new ScriptBuilder();
        sb.initScript(context, popup, "O$._initPopupLayer",
                popup.getLeft(),
                popup.getTop(),
                popup.getWidth(),
                popup.getHeight(),
                RenderingUtil.getRolloverClass(context, popup),
                popup.getHidingTimeout(),
                popup.getDraggable(),
                EnvironmentUtil.isAjax4jsfRequest());

        String onShow = popup.getOnshow();
        if (onShow != null) {
            sb.append("\nO$('").append(clientId).append("').onshow = function (event) {"); // todo: refactor passing events into passing them as a single JSON param to the initialization function
            sb.append(onShow);
            sb.append("};");
        }

        String onHide = popup.getOnhide();
        if (onHide != null) {
            sb.append("\nO$('").append(clientId).append("').onhide = function (event) {");
            sb.append(onHide);
            sb.append("};");
        }

        String ondragstart = popup.getOndragstart();
        if (ondragstart != null) {
            sb.append("\nO$('").append(clientId).append("').ondragstart = function (event) {");
            sb.append(ondragstart);
            sb.append("};");
        }

        String ondragend = popup.getOndragend();
        if (ondragend != null) {
            sb.append("\nO$('").append(clientId).append("').ondragend = function (event) {");
            sb.append(ondragend);
            sb.append("};");
        }

        if (popupInvokerId != null) {
            sb.append("\nO$('");
            sb.append(clientId);
            sb.append("').attachToElement(O$('").append(popupInvokerId).append("'), ");
            sb.append(nullOrJsString(popup.getAnchorX()));
            sb.append(", ");
            sb.append(nullOrJsString(popup.getAnchorY()));
            sb.append(");");
        }

        RenderingUtil.renderInitScript(context, sb, new String[]{
                ResourceUtil.getUtilJsURL(context),
                ResourceUtil.getInternalResourceURL(context, PopupLayerRenderer.class, "popupLayer.js")
        });

    }

    private static String nullOrJsString(String str) { // todo: replace using ScriptBuilder and remove this method
        if (str == null)
            return "null";
        else
            return '\'' + str + '\'';
    }


    public void decode(FacesContext context, UIComponent component) {
        PopupLayer layer = ((PopupLayer) component);

        String visibleKey = component.getClientId(context) + VISIBLE_HIDDEN_FIELD_SUFFIX;
        String visibleValue = context.getExternalContext().getRequestParameterMap().get(visibleKey);
        if (visibleValue != null) {
            boolean visible = Boolean.valueOf(visibleValue);
            if (layer.isVisible() != visible) {
                layer.setVisible(visible);
            }
        }

        String leftKey = component.getClientId(context) + LEFT_HIDDEN_FIELD_SUFFIX;
        String leftValue = context.getExternalContext().getRequestParameterMap().get(leftKey);
        if (leftValue != null && leftValue.length() == 0) {
            leftValue = null;
        }
        String popupLeft = layer.getLeft();
        if (popupLeft != null && !popupLeft.equals(leftValue) || leftValue != null && !leftValue.equals(popupLeft)) {
            layer.setLeft(leftValue);
        }

        String topKey = component.getClientId(context) + TOP_HIDDEN_FIELD_SUFFIX;
        String topValue = context.getExternalContext().getRequestParameterMap().get(topKey);
        if (topValue != null && topValue.length() == 0) {
            topValue = null;
        }
        String popupTop = layer.getTop();
        if (popupTop != null && !popupTop.equals(topValue) || topValue != null && !topValue.equals(popupTop)) {
            layer.setTop(topValue);
        }

    }

}