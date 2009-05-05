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

import org.openfaces.component.ComponentWithCaption;
import org.openfaces.component.window.AbstractWindow;
import org.openfaces.util.ResourceUtil;
import org.openfaces.renderkit.ToggleCaptionButtonRenderer;

import javax.faces.FacesException;
import javax.faces.context.FacesContext;
import java.util.List;

/**
 * @author Dmitry Pikhulya
 */
public class MaximizeWindowButtonRenderer extends ToggleCaptionButtonRenderer {
    protected void checkContainerType(ComponentWithCaption container) {
        if (!(container instanceof AbstractWindow))
            throw new FacesException("<o:maximizeWindowButton> can only be used in <o:window> and <o:confirmation> components.");
    }

    protected String getDefaultImageUrl(FacesContext context) {
        return ResourceUtil.getInternalResourceURL(context, AbstractWindowRenderer.class, "maximize.gif");
    }

    protected String getDefaultToggleImageUrl(FacesContext context) {
        return ResourceUtil.getInternalResourceURL(context, AbstractWindowRenderer.class, "restore.gif");
    }

    protected String getInitFunctionName() {
        return "O$._initMaximizeWindowButton";
    }

    protected List<String> getJsLibraries(FacesContext context) {
        List<String> libraries = super.getJsLibraries(context);
        libraries.add(AbstractWindowRenderer.getWindowJs(context));
        return libraries;
    }

}