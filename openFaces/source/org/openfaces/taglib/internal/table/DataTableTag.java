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
package org.openfaces.taglib.internal.table;

import org.openfaces.component.table.DataTable;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

/**
 * @author Pavel Kaplin
 */
public class DataTableTag extends AbstractTableTag {
    public String getComponentType() {
        return DataTable.COMPONENT_TYPE;
    }

    public String getRendererType() {
        return "org.openfaces.DataTableRenderer";
    }

    public void setComponentProperties(FacesContext facesContext, UIComponent component) {
        super.setComponentProperties(facesContext, component);

        setPropertyBinding(component, "value");
        setPropertyBinding(component, "rowKey");
        setPropertyBinding(component, "rowDataByKey");
        setIntProperty(component, "pageSize");
        setIntProperty(component, "pageIndex");

        setStringProperty(component, "rowIndexVar");
        setBooleanProperty(component, "paginationKeyboardSupport");
        setPropertyBinding(component, "totalRowCount");
        setBooleanProperty(component, "customDataProviding");
    }
}