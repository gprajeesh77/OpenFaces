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

package org.openfaces.testapp.jBossSeam.treetable;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Out;
import org.jboss.seam.annotations.Scope;
import org.openfaces.util.FacesUtil;
import org.openfaces.component.table.AllNodesCollapsed;
import org.openfaces.component.table.AllNodesExpanded;
import org.openfaces.component.table.ExpansionState;
import org.openfaces.component.table.SeveralLevelsExpanded;
import org.openfaces.component.table.TextFilterCriterion;

import javax.ejb.Remove;
import javax.ejb.Stateful;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import static javax.persistence.PersistenceContextType.EXTENDED;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * @author Tatyana Matveyeva
 */

@Stateful
@Scope(ScopeType.CONVERSATION)
@Name("seamTreeTable")
public class SeamTreeTableBean implements Serializable, SeamTreeTable {

    @PersistenceContext(type = EXTENDED)
    private EntityManager em;

    @In
    @Out
    private SeamTestTree seamtesttree;

    private TreeTableItem singleSelectionItem;
    private List<TreeTableItem> multipleSelectionItems;


    private ExpansionState forumTreeTableExpansionState1 = new SeveralLevelsExpanded(1);
    private ExpansionState forumTreeTableExpansionState2 = new AllNodesExpanded();
    private ExpansionState forumTreeTableExpansionState3 = new AllNodesCollapsed();
    private TextFilterCriterion filterValue = new TextFilterCriterion("2006");

    public List getNodeChildren() {
        TreeTableItem item = (TreeTableItem) FacesUtil.getRequestMapValue("item");
        return item != null ? item.getItems() :
                em.createQuery("select item from TreeTableItem item where item.parent is null").getResultList();
    }


    public ExpansionState getForumTreeTableExpansionState1() {
        return forumTreeTableExpansionState1;
    }

    public void setForumTreeTableExpansionState1(ExpansionState forumTreeTableExpansionState1) {
        this.forumTreeTableExpansionState1 = forumTreeTableExpansionState1;
    }

    public ExpansionState getForumTreeTableExpansionState2() {
        return forumTreeTableExpansionState2;
    }

    public void setForumTreeTableExpansionState2(ExpansionState forumTreeTableExpansionState2) {
        this.forumTreeTableExpansionState2 = forumTreeTableExpansionState2;
    }

    public ExpansionState getForumTreeTableExpansionState3() {
        return forumTreeTableExpansionState3;
    }

    public void setForumTreeTableExpansionState3(ExpansionState forumTreeTableExpansionState3) {
        this.forumTreeTableExpansionState3 = forumTreeTableExpansionState3;
    }

    public String getDateCategory() {
        TreeTableItem item = (TreeTableItem) FacesUtil.getRequestMapValue("item");
        Date date = item.getDate();
        return formatDate(date);
    }

    private String formatDate(Date date) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy");
        return simpleDateFormat.format(date);
    }

    public TextFilterCriterion getFilterValue() {
        return filterValue;
    }

    public void setFilterValue(TextFilterCriterion filterValue) {
        this.filterValue = filterValue;
    }


    public TreeTableItem getSingleSelectionItem() {
        return singleSelectionItem;
    }

    public void setSingleSelectionItem(TreeTableItem singleSelectionItem) {
        this.singleSelectionItem = singleSelectionItem;
    }

    public List<TreeTableItem> getMultipleSelectionItems() {
        return multipleSelectionItems;
    }

    public void setMultipleSelectionItems(List<TreeTableItem> multipleSelectionItems) {
        this.multipleSelectionItems = multipleSelectionItems;
    }

    public String sortByFirstColumn() {
        seamtesttree.getTreeTable().setSortColumnId("subject");
        seamtesttree.getTreeTable().setSortAscending(false);
        return null;
    }

    @Remove
    @Destroy
    public void destroy() {
    }
}