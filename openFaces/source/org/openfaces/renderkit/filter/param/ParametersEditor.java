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

package org.openfaces.renderkit.filter.param;

import org.openfaces.component.filter.CompositeFilter;
import org.openfaces.component.filter.FilterProperty;
import org.openfaces.component.filter.OperationType;
import org.openfaces.component.filter.criterion.NamedPropertyLocator;
import org.openfaces.component.filter.criterion.PropertyFilterCriterion;
import org.openfaces.renderkit.filter.FilterRow;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import java.util.List;
import java.util.Map;

public abstract class ParametersEditor {

    protected PropertyFilterCriterion criterion = new PropertyFilterCriterion(); // todo: try to avoid data duplication with FilterRow's criterion
    protected FilterProperty filterProperty;

    public ParametersEditor() {
    }

    protected ParametersEditor(FilterProperty filterProperty, OperationType operation) {
        this.filterProperty = filterProperty;
        criterion.setPropertyLocator(new NamedPropertyLocator(filterProperty.getName()));
        criterion.setOperation(operation);
    }

    public void prepare(FacesContext context, CompositeFilter compositeFilter, FilterRow filterRow, UIComponent container){
        criterion.setInverse(filterRow.isInverse());
    }

    public abstract void update(FacesContext context, CompositeFilter compositeFilter, FilterRow filterRow, UIComponent container);

    protected void clearContainer(UIComponent container) {
        List<UIComponent> children = container.getChildren();
        children.clear();
    }

    public PropertyFilterCriterion getCriterion() {
        return criterion;
    }

    public void setParameters(Map<String, Object> parameters) {
        this.criterion.getParameters().clear();
        this.criterion.getParameters().putAll(parameters);
    }

    public static enum ParameterEditorType {
        DROP_DOWN_PARAMETERS_EDITOR,
        DATE_CHOOSER_PARAMETERS_EDITOR,
        TWO_DATE_CHOOSER_PARAMETERS_EDITOR,
        SPINNER_PARAMETRS_EDITOR,
        TWO_SPINNER_PARAMETRS_EDITOR,
        INPUT_TEXT_PARAMETRS_EDITOR
    }

    public static ParameterEditorType getParameterEditorType(FilterProperty filterProperty, OperationType operation) {
        switch (operation) {
            case LE:
            case GE:
            case GT:
            case LT:
                switch (filterProperty.getType()) {
                    case DATE:
                        return ParameterEditorType.DATE_CHOOSER_PARAMETERS_EDITOR;
                    case NUMBER:
                        return ParameterEditorType.SPINNER_PARAMETRS_EDITOR;
                    default:
                        return ParameterEditorType.INPUT_TEXT_PARAMETRS_EDITOR;
                }
            case BETWEEN:
                switch (filterProperty.getType()) {
                    case DATE:
                        return ParameterEditorType.TWO_DATE_CHOOSER_PARAMETERS_EDITOR;
                    case NUMBER:
                        return ParameterEditorType.TWO_SPINNER_PARAMETRS_EDITOR;
                    default:
                        throw new UnsupportedOperationException();
                }
            case EQUALS: {

                switch (filterProperty.getType()) {
                    case DATE:
                        return ParameterEditorType.DATE_CHOOSER_PARAMETERS_EDITOR;
                    case NUMBER:
                        return ParameterEditorType.SPINNER_PARAMETRS_EDITOR;
                    case SELECT:
                        return ParameterEditorType.DROP_DOWN_PARAMETERS_EDITOR;
                    default:
                        if (filterProperty.getDataProvider() != null) {
                            return ParameterEditorType.DROP_DOWN_PARAMETERS_EDITOR;
                        } else {
                            return ParameterEditorType.INPUT_TEXT_PARAMETRS_EDITOR;
                        }
                }
            }
            case CONTAINS:
            case BEGINS:
            case ENDS:
            default:
                return ParameterEditorType.INPUT_TEXT_PARAMETRS_EDITOR;

        }
    }

    public static ParametersEditor getInstance(ParameterEditorType type, FilterProperty filterProperty,
                                               OperationType operation, Map<String, Object> parameters) {

        ParametersEditor result;
        switch (type) {
            case DROP_DOWN_PARAMETERS_EDITOR:
                result = new DropDownParametersEditor(filterProperty, operation);
                break;
            case DATE_CHOOSER_PARAMETERS_EDITOR:
                result = new DateChooserParametersEditor(filterProperty, operation);
                break;
            case TWO_DATE_CHOOSER_PARAMETERS_EDITOR:
                result = new TwoDateChooserParametersEditor(filterProperty, operation);
                break;
            case SPINNER_PARAMETRS_EDITOR:
                result = new SpinnerParametersEditor(filterProperty, operation);
                break;
            case TWO_SPINNER_PARAMETRS_EDITOR:
                result = new TwoSpinnerParametersEditor(filterProperty, operation);
                break;
            case INPUT_TEXT_PARAMETRS_EDITOR:
                result = new InputTextParametersEditor(filterProperty, operation);
                break;
            default:
                throw new UnsupportedOperationException();
        }
        if (parameters != null)
            result.setParameters(parameters);
        return result;
    }
}
