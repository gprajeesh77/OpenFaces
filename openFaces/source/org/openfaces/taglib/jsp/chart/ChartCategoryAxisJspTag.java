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
package org.openfaces.taglib.jsp.chart;

import org.openfaces.taglib.internal.chart.ChartCategoryAxisTag;

import javax.el.ValueExpression;

/**
 * @author Ekaterina Shliakhovetskaya
 */
public class ChartCategoryAxisJspTag extends ChartAxisJspTag {

    public ChartCategoryAxisJspTag() {
        super(new ChartCategoryAxisTag());
    }

    public void setPosition(ValueExpression position) {
        getDelegate().setPropertyValue("position", position);
    }

    public void setLowerMargin(ValueExpression lowerMargin) {
        getDelegate().setPropertyValue("lowerMargin", lowerMargin);
    }

    public void setUpperMargin(ValueExpression upperMargin) {
        getDelegate().setPropertyValue("upperMargin", upperMargin);
    }

    public void setCategoryMargin(ValueExpression categoryMargin) {
        getDelegate().setPropertyValue("categoryMargin", categoryMargin);
    }
}
