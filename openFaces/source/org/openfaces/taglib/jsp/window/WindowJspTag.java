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
package org.openfaces.taglib.jsp.window;

import org.openfaces.taglib.internal.window.WindowTag;
import org.openfaces.taglib.jsp.AbstractWindowJspTag;

import javax.el.ValueExpression;

/**
 * @author Dmitry Pikhulya
 */
public class WindowJspTag extends AbstractWindowJspTag {
    public WindowJspTag() {
        super(new WindowTag());
    }

    public void setResizeable(ValueExpression resizeable) {
        getDelegate().setPropertyValue("resizeable", resizeable);
    }

    public void setWidth(ValueExpression width) {
        getDelegate().setPropertyValue("minWidth", width);
    }

    public void setHeight(ValueExpression height) {
        getDelegate().setPropertyValue("minHeight", height);
    }

}