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
package org.openfaces.renderkit;

import org.openfaces.component.ComponentWithCaption;
import org.openfaces.component.panel.FoldingPanel;
import org.openfaces.util.ResourceUtil;

import javax.faces.FacesException;
import javax.faces.context.FacesContext;

/**
 * @author Dmitry Pikhulya
 */
public class ExpansionToggleButtonRenderer extends ToggleCaptionButtonRenderer {
    protected void checkContainerType(ComponentWithCaption container) {
        if (!(container instanceof FoldingPanel))
            throw new FacesException("<o:expansionToggleButton> can only be used in <o:foldingPanel> component.");
    }

    protected String getDefaultImageUrl(FacesContext context) {
        return ResourceUtil.getInternalResourceURL(context, ExpansionToggleButtonRenderer.class, "plus_h.gif");
    }

    protected String getDefaultRolloverImageUrl(FacesContext context) {
        return ResourceUtil.getInternalResourceURL(context, ExpansionToggleButtonRenderer.class, "plus_u.gif");
    }

    protected String getDefaultPressedImageUrl(FacesContext context) {
        return ResourceUtil.getInternalResourceURL(context, ExpansionToggleButtonRenderer.class, "plus_d.gif");
    }

    protected String getDefaultToggleImageUrl(FacesContext context) {
        return ResourceUtil.getInternalResourceURL(context, ExpansionToggleButtonRenderer.class, "minus_h.gif");
    }

    protected String getDefaultToggledImageRolloverUrl(FacesContext context) {
        return ResourceUtil.getInternalResourceURL(context, ExpansionToggleButtonRenderer.class, "minus_u.gif");
    }

    protected String getDefaultToggledImagePressedUrl(FacesContext context) {
        return ResourceUtil.getInternalResourceURL(context, ExpansionToggleButtonRenderer.class, "minus_d.gif");
    }

    protected String getDefaultRolloverClass() {
        return null;
    }

    protected String getDefaultPressedClass() {
        return null;
    }

    protected String getInitFunctionName() {
        return "O$._initExpansionToggleButton";
    }
}