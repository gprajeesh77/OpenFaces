<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h" %>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f" %>
<%@ taglib uri="http://openfaces.org/" prefix="o" %>
<%@ taglib uri="http://richfaces.org/a4j" prefix="a4j" %>
<%@ taglib uri="http://richfaces.org/rich" prefix="rich" %>
<%@ taglib uri="http://myfaces.apache.org/tomahawk" prefix="t" %>

<html>
<head>
  <title>DataTable with Ajax</title>
</head>
<script type="text/javascript" src="../../funcTestsUtil.js"></script>
<link rel="STYLESHEET" type="text/css" href="../../main.css"/>
<body>
<f:view>
  <h:form id="form1">
    <%@ include file="dataTableAjax_core.xhtml" %>
  </h:form>
  <h:form id="form2">
    <%@ include file="dataTableAjax2_core.xhtml" %>
  </h:form>
</f:view>

</body>
</html>