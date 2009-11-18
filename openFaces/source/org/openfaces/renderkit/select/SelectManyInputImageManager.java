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

package org.openfaces.renderkit.select;

import org.openfaces.component.select.OUISelectManyInputBase;
import org.openfaces.component.select.SelectItem;
import org.openfaces.util.ResourceUtil;

import javax.faces.context.FacesContext;

/**
 * @author Oleg Marshalenko
 */
public class SelectManyInputImageManager {

    public static boolean hasImages(OUISelectManyInputBase selectManyInputBase) {
        return
            isSpecified(selectManyInputBase.getSelectedImageUrl())
            || isSpecified(selectManyInputBase.getUnselectedImageUrl())
            || isSpecified(selectManyInputBase.getRolloverSelectedImageUrl())
            || isSpecified(selectManyInputBase.getRolloverUnselectedImageUrl())
            || isSpecified(selectManyInputBase.getPressedSelectedImageUrl())
            || isSpecified(selectManyInputBase.getPressedUnselectedImageUrl())
            || isSpecified(selectManyInputBase.getDisabledSelectedImageUrl())
            || isSpecified(selectManyInputBase.getDisabledUnselectedImageUrl())
            ;
    }

    public static String getCurrentImageUrl(FacesContext context, OUISelectManyInputBase selectManyInputBase,
                                            SelectItem selectItem) {
        if (selectManyInputBase.isDisabled() || selectItem.isItemDisabled()) {
            if (selectItem.getItemValue().equals(selectManyInputBase.getValue())) {
                return getDisabledSelectedImageUrl(context, selectManyInputBase);
            } else {
                return getDisabledUnselectedImageUrl(context, selectManyInputBase);
            }
        } else {
            if (selectItem.getItemValue().equals(selectManyInputBase.getValue())) {
                return getSelectedImageUrl(context, selectManyInputBase);
            } else {
                return getUnselectedImageUrl(context, selectManyInputBase);
            }
        }
    }

    public static String getSelectedImageUrl(FacesContext context, OUISelectManyInputBase selectManyInputBase) {
        String imageUrl = selectManyInputBase.getSelectedImageUrl();
        return ResourceUtil.getResourceURL(context, imageUrl, SelectManyInputImageManager.class, null);
    }

    public static String getUnselectedImageUrl(FacesContext context, OUISelectManyInputBase selectManyInputBase) {
        String imageUrl = selectManyInputBase.getUnselectedImageUrl();
        return ResourceUtil.getResourceURL(context, imageUrl, SelectManyInputImageManager.class, null);
    }

    public static String getRolloverSelectedImageUrl(FacesContext context, OUISelectManyInputBase selectManyInputBase) {
        String imageUrl = firstSpecified(
                selectManyInputBase.getRolloverSelectedImageUrl(),
                selectManyInputBase.getSelectedImageUrl());

        return ResourceUtil.getResourceURL(context, imageUrl, SelectManyInputImageManager.class, null);
    }

    public static String getRolloverUnselectedImageUrl(FacesContext context, OUISelectManyInputBase selectManyInputBase) {
        String imageUrl = firstSpecified(
                selectManyInputBase.getRolloverUnselectedImageUrl(),
                selectManyInputBase.getUnselectedImageUrl());

        return ResourceUtil.getResourceURL(context, imageUrl, SelectManyInputImageManager.class, null);
    }

    public static String getPressedSelectedImageUrl(FacesContext context, OUISelectManyInputBase selectManyInputBase) {
        String imageUrl = firstSpecified(
                selectManyInputBase.getPressedSelectedImageUrl(),
                selectManyInputBase.getRolloverSelectedImageUrl(),
                selectManyInputBase.getSelectedImageUrl());

        return ResourceUtil.getResourceURL(context, imageUrl, SelectManyInputImageManager.class, null);
    }

    public static String getPressedUnselectedImageUrl(FacesContext context, OUISelectManyInputBase selectManyInputBase) {
        String imageUrl = firstSpecified(
                selectManyInputBase.getPressedUnselectedImageUrl(),
                selectManyInputBase.getRolloverUnselectedImageUrl(),
                selectManyInputBase.getUnselectedImageUrl());

        return ResourceUtil.getResourceURL(context, imageUrl, SelectManyInputImageManager.class, null);
    }

    public static String getDisabledSelectedImageUrl(FacesContext context, OUISelectManyInputBase selectManyInputBase) {
        String imageUrl = selectManyInputBase.getDisabledSelectedImageUrl();
        return ResourceUtil.getResourceURL(context, imageUrl, SelectManyInputImageManager.class, null);
    }

    public static String getDisabledUnselectedImageUrl(FacesContext context, OUISelectManyInputBase selectManyInputBase) {
        String imageUrl = selectManyInputBase.getDisabledUnselectedImageUrl();
        return ResourceUtil.getResourceURL(context, imageUrl, SelectManyInputImageManager.class, null);
    }

    private static String firstSpecified(String... strings) {
        for (String string : strings) {
            if (isSpecified(string)) {
                return string;
            }
        }

        return null;
    }

    private static boolean isSpecified(String string) {
        return string != null && string.length() > 0;
    }

}