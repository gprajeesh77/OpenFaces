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
package org.openfaces.taglib.facelets.select;

import org.openfaces.taglib.facelets.AbstractFaceletsComponentHandler;
import org.openfaces.taglib.internal.select.SelectBooleanCheckboxTag;

import com.sun.facelets.tag.jsf.ComponentConfig;

/**
 * @author Roman Porotnikov
 */
public class SelectBooleanCheckboxTagHandler extends AbstractFaceletsComponentHandler {

    public SelectBooleanCheckboxTagHandler(ComponentConfig componentConfig) {
        super(componentConfig, new SelectBooleanCheckboxTag());
    }
}
