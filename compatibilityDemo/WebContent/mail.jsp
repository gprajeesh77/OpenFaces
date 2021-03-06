<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<%@ taglib uri="http://richfaces.org/a4j" prefix="a4j" %>
<%@ taglib uri="http://richfaces.org/rich" prefix="rich" %>
<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h" %>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f" %>
<%@ taglib uri="http://openfaces.org/" prefix="o" %>

<html xmlns="http://www.w3.org/1999/xhtml">
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <title>Email</title>
    <link rel="shortcut icon" href="favicon.ico" type="image/vnd.microsoft.icon"/>
    <link rel="stylesheet" href="css/content.css" type="text/css" media="screen,projection"/>
    <link rel="stylesheet" href="css/style.css" type="text/css" media="screen,projection"/>
    <link rel="stylesheet" href="css/mail.css" type="text/css" media="screen,projection"/>

    <script type="text/javascript">
        function addSendButton(ed) {
            ed.addButton("sendMail", {
                title: "Send",
                image: "images/mail/send.png",
                onclick: function() {
                    O$("form:editor").onsend();
                }
            });
        }
        function saveMessage(ed) {
            O$("form:newMessage").onsave2();
        }
    </script>
</head>
<body>
<f:view>
<h:form id="form">
<o:window id="newMessage" modal="true" width="50%" height="50%">
    <f:facet name="caption"><h:outputText value="New Message"/></f:facet>
    <table style="width:100%;height:100%">
        <tr>
            <td><h:outputText value="To: "/></td>
            <td width="100%"><h:inputText value="#{EMailBean.messageAddress}" style="width:90%"/></td>
        </tr>
        <tr>
            <td><h:outputText value="Subject: "/></td>
            <td width="100%"><h:inputText value="#{EMailBean.messageSubject}" style="width:90%"/></td>
        </tr>
        <tr>
            <td height="100%" colspan="2">
                <rich:editor id="editor" viewMode="visual" theme="advanced" value="#{EMailBean.messageText}"
                             style="height:100%">
                    <f:param name="plugins" value="save,table"/>

                    <f:param name="theme_advanced_buttons1"
                             value="sendMail,save,|,cut,copy,paste,|,undo,redo,|,link,unlink,anchor,image"/>
                    <f:param name="theme_advanced_buttons2"
                             value="fontselect,fontsizeselect,forecolor,|,bold,italic,underline,|,justifyleft,justifycenter,justifyright,|,bullist,numlist,outdent,indent"/>
                    <f:param name="theme_advanced_buttons3"
                             value="tablecontrols,|,hr,removeformat,visualaid,|,sub,sup"/>
                    <f:param name="theme_advanced_toolbar_location" value="top"/>
                    <f:param name="theme_advanced_toolbar_align" value="left"/>

                    <f:param name="width" value="100%"/>
                    <f:param name="height" value="100%"/>

                    <f:param name="setup" value="addSendButton"/>

                    <f:param name="save_onsavecallback" value="saveMessage"/>
                </rich:editor>
            </td>
        </tr>
    </table>
</o:window>
<o:ajax for="newMessage" listener="#{EMailBean.saveMailListener}" event="save2" render="emailsTreeTable"
        onajaxend="O$('form:newMessage').hide();"/>
<o:ajax for="newMessage" listener="#{EMailBean.sendMailListener}" event="send" render="emailsTreeTable"
        onajaxend="O$('form:newMessage').hide();"/>

<o:borderLayoutPanel>
    <o:sidePanel alignment="top" resizable="false" collapsible="false" size="72px">
        <rich:toolBar height="64px" styleClass="navigation-panel-mail" itemSeparator="none">
            <rich:toolBarGroup style="text-align:left">
                <h:graphicImage url="images/logo.png" alt="OpenFaces"/>
                <h:outputText value="Email" styleClass="caption" style="padding-right:50px"/>
            </rich:toolBarGroup>
            <rich:toolBarGroup styleClass="buttons">
                <h:outputLink value="#" onclick="O$('form:newMessage').showCentered(); return false;">
                    <h:panelGroup layout="block" styleClass="new-message"/>
                    <h:outputText value="New message"/>
                </h:outputLink>
                <h:outputLink value="#">
                    <h:panelGroup layout="block" styleClass="reply"/>
                    <h:outputText value="Reply"/>
                </h:outputLink>
                <h:outputLink value="#">
                    <h:panelGroup layout="block" styleClass="forward"/>
                    <h:outputText value="Forward"/>
                </h:outputLink>
            </rich:toolBarGroup>
            <rich:toolBarGroup location="right" styleClass="link">
                <h:outputLink value="calendar.jsf" styleClass="calendar">
                    <h:graphicImage value="images/titles/navigation-calendar.png"/>
                </h:outputLink>
                <h:outputLink value="tasks.jsf" styleClass="tasks">
                    <h:graphicImage value="images/titles/navigation-tasks.png"/>
                </h:outputLink>
            </rich:toolBarGroup>
        </rich:toolBar>
    </o:sidePanel>
    <o:sidePanel alignment="left" size="230px" contentClass="Sidebar">
        <rich:tree id="emailFolders" switchType="ajax" ajaxSubmitSelection="true" styleClass="email-folders">
            <rich:recursiveTreeNodesAdaptor roots="#{EMailBean.drafts}" var="folder" nodes="#{folder.children}">
                <rich:treeNode data="#{folder}" nodeSelectListener="#{EMailBean.nodeSelectListener}"
                               reRender="emailsTreeTable,emailDetails">
                    <h:outputText value="Drafts"/>
                    <f:facet name="icon">
                        <h:graphicImage url="/images/mail/sidebar-mail-drafts.png" styleClass="icon"/>
                    </f:facet>
                    <f:facet name="iconLeaf">
                        <h:graphicImage url="/images/mail/sidebar-mail-drafts.png" styleClass="icon"/>
                    </f:facet>
                </rich:treeNode>
            </rich:recursiveTreeNodesAdaptor>
            <rich:recursiveTreeNodesAdaptor roots="#{EMailBean.inbox}" var="folder" nodes="#{folder.children}">
                <rich:treeNode data="#{folder}" nodeSelectListener="#{EMailBean.nodeSelectListener}"
                               reRender="emailsTreeTable,emailDetails">
                    <h:outputText value="#{folder.name}"/>
                    <f:facet name="icon">
                        <h:graphicImage url="/images/mail/sidebar-mail-#{folder.subfolder ? 'folder' : 'inbox'}.png"
                                        styleClass="icon"/>
                    </f:facet>
                    <f:facet name="iconLeaf">
                        <h:graphicImage url="/images/mail/sidebar-mail-#{folder.subfolder ? 'folder' : 'inbox'}.png"
                                        styleClass="icon"/>
                    </f:facet>
                </rich:treeNode>
            </rich:recursiveTreeNodesAdaptor>
            <rich:recursiveTreeNodesAdaptor roots="#{EMailBean.sent}" var="folder" nodes="#{folder.children}">
                <rich:treeNode data="#{folder}" nodeSelectListener="#{EMailBean.nodeSelectListener}"
                               reRender="emailsTreeTable,emailDetails">
                    <h:outputText value="Sent"/>
                    <f:facet name="icon">
                        <h:graphicImage url="/images/mail/sidebar-mail-sent.png" styleClass="icon"/>
                    </f:facet>
                    <f:facet name="iconLeaf">
                        <h:graphicImage url="/images/mail/sidebar-mail-sent.png" styleClass="icon"/>
                    </f:facet>
                </rich:treeNode>
            </rich:recursiveTreeNodesAdaptor>
            <f:facet name="iconCollapsed">
                <h:graphicImage url="/images/mail/sidebar-tree-collapsed.png" styleClass="icon-collapsed"/>
            </f:facet>
            <f:facet name="iconExpanded">
                <h:graphicImage url="/images/mail/sidebar-tree-opened.png" styleClass="icon-expanded"/>
            </f:facet>
        </rich:tree>

        <div class="SidebarFooter">
            <a class="ButtonPageSource" onclick="O$('form:pageSource').showCentered(); return false">
                <span>View page source</span>
            </a>

            <div class="Copyright">
                <p>&copy;&nbsp;TeamDev Ltd. | OpenFaces.org</p>
            </div>
        </div>
    </o:sidePanel>
    <h:panelGroup layout="block" style="height: 50%;">
        <o:treeTable id="emailsTreeTable"
                     var="email" columnIdVar="columnId" nodeLevelVar="level"
                     expansionState="allExpanded"
                     sortAscending="#{EMailBean.selection.sortAscending}"
                     sortColumnId="#{EMailBean.selection.sortedColumnId}"
                     style="width: 100%; height: 100%; border: 1px solid white; table-layout: fixed"
                     rolloverRowStyle="background: #b6cfec;"
                     horizontalGridLines="1px solid #eef0f2"
                     headerRowClass="TableHeader"
                     sortedAscendingImageUrl="images/treetable/sort_a.gif"
                     sortedDescendingImageUrl="images/treetable/sort_d.gif"
                     sortedColumnHeaderStyle="background: url('images/treetable/header_selected.gif') repeat-x;"
                     sortedColumnClass="SortedColumn"
                     focusedStyle="border: 1px dotted black !important;"
                     >
            <o:scrolling/>

            <o:row condition="#{level == 0}" style="background: white !important;">
                <o:cell span="6" styleClass="category-name">
                    <h:outputText value="#{email}" style="padding-left: 5px;"/>
                </o:cell>
            </o:row>
            <o:singleNodeSelection
                    style="background:url('images/treetable/selection.gif') repeat-x #168aff !important; color: white !important;"
                    nodeData="#{EMailBean.selectedEMail}">
                <a4j:support event="onchange" process=":form:emailsTreeTable" reRender="emailDetails"/>
            </o:singleNodeSelection>
            <o:dynamicTreeStructure nodeChildren="#{EMailBean.selection.EMailsTreeChildren}"/>
            <o:treeColumn id="importance"
                          expandedToggleImageUrl="images/treetable/expanded4.gif"
                          collapsedToggleImageUrl="images/treetable/collapsed4.gif"
                          width="32px"
                          headerStyle="text-align: right !important;"
                          sortingExpression="#{EMailBean.selection.sortByImportance}"
                          levelIndent="10px">
                <f:facet name="header">
                    <h:graphicImage url="images/treetable/sort_prioity.gif"/>
                </f:facet>
                <h:graphicImage url="#{EMailBean.selection.importanceIcon}"/>
            </o:treeColumn>
            <o:column id="attachment"
                      width="32px"
                      style="text-align: center;"
                      headerStyle="text-align: center !important;"
                      sortingExpression="#{EMailBean.selection.sortByAttachmentExpression}">
                <f:facet name="header">
                    <h:graphicImage url="images/treetable/attachment.gif"/>
                </f:facet>
                <h:graphicImage url="images/treetable/attachment.gif"
                                rendered="#{EMailBean.selection.hasAttachment}"/>
            </o:column>
            <o:column width="16px"
                      style="text-align: center;">
                <f:facet name="header">
                    <h:outputText value=""/>
                </f:facet>
                <h:graphicImage url="images/treetable/letter.gif"/>
            </o:column>
            <o:column id="sender"
                      width="21%"
                      style="padding-left: 5px;"
                      sortingExpression="#{EMailBean.selection.sortBySenderExpression}">
                <f:facet name="header">
                    <h:outputText value="#{EMailBean.folder.incoming ? 'From' : 'To'}"/>
                </f:facet>
                <h:outputText value="#{email.sender}" style="margin-left: 5px;"/>
            </o:column>
            <o:column id="subject"
                      width="61%"
                      bodyStyle="text-align: left;"
                      style="padding-left: 5px;"
                      sortingExpression="#{EMailBean.selection.sortBySubjectExpression}">
                <f:facet name="header">
                    <h:outputText value="Subject"/>
                </f:facet>
                <h:outputText value="#{email.subject}" style="margin-left: 5px;"/>
            </o:column>
            <o:column id="date"
                      width="16%"
                      style="padding-left: 5px;"
                      sortingExpression="#{EMailBean.selection.sortByDateExpression}">
                <f:facet name="header">
                    <h:outputText value="#{EMailBean.folder.incoming ? 'Received' : 'Sent'}"/>
                </f:facet>
                <h:outputText value="#{email.receivedDate}" style="margin-left: 5px;"
                              converter="#{EMailBean.selection.receivedDateConverter}"/>
            </o:column>
        </o:treeTable>
    </h:panelGroup>
    <h:panelGroup layout="block" id="emailDetails" style="height:300px;padding-left:20px">
        <h:panelGroup rendered="#{EMailBean.selectedEMail != null}">
            <h:panelGroup layout="block" style="padding-top:20px">
                <h:outputText value="Subject: " styleClass="email-field email-field-caption"/>
                <h:outputText value="#{EMailBean.selectedEMail.subject}" styleClass="email-field"/>
            </h:panelGroup>
            <h:panelGroup layout="block" style="padding-top:5px">
                <h:outputText value="#{EMailBean.folder.incoming ? 'From' : 'To'}: "
                              styleClass="email-field email-field-caption"/>
                <h:outputText value="#{EMailBean.selectedEMail.sender}" styleClass="email-field"/>
            </h:panelGroup>
            <h:panelGroup layout="block" style="padding-top:40px">
                <h:outputText value="#{EMailBean.selectedEMail.content}" styleClass="email-field" escape="false"/>
            </h:panelGroup>
        </h:panelGroup>
    </h:panelGroup>
</o:borderLayoutPanel>

<o:window id="pageSource" width="70%" height="70%"
          styleClass="SourceView"
          modal="true"
          modalLayerClass="SourceViewModalLayer">
    <f:facet name="caption"><h:outputText value="mail.jsp"/></f:facet>
    <rich:insert src="/mail.jsp" highlight="xhtml"/>
</o:window>

</h:form>
</f:view>
</body>
</html>