/*
 * OpenFaces - JSF Component Library 2.0
 * Copyright (C) 2007-2011, TeamDev Ltd.
 * licensing@openfaces.org
 * Unless agreed in writing the contents of this file are subject to
 * the GNU Lesser General Public License Version 2.1 (the "LGPL" License).
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * Please visit http://openfaces.org/licensing/ for more details.
 */
package org.openfaces.taglib.jsp.timetable;

import org.openfaces.taglib.internal.AbstractComponentTag;
import org.openfaces.taglib.jsp.AbstractComponentJspTag;

import javax.el.ValueExpression;

/**
 * @author Roman Porotnikov
 */
public abstract class AbstractStyledEventContentJspTag extends AbstractComponentJspTag {

    public AbstractStyledEventContentJspTag(AbstractComponentTag delegate) {
        super(delegate);
    }

    public void setStyle(ValueExpression style) {
        getDelegate().setPropertyValue("style", style);
    }

    public void setStyleClass(ValueExpression styleClass) {
        getDelegate().setPropertyValue("styleClass", styleClass);
    }

}
