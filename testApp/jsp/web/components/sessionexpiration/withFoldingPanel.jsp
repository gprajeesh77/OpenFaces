<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h" %>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f" %>
<%@ taglib uri="http://openfaces.org/" prefix="o" %>
<%@ taglib uri="http://richfaces.org/a4j" prefix="a4j"%>
<%@ taglib uri="http://richfaces.org/rich" prefix="rich"%>
<%@ taglib uri="http://myfaces.apache.org/tomahawk" prefix="t"%>

<html>
<head>
  <title>FoldingPanel with session expiration</title>
  <link rel="STYLESHEET" type="text/css" href="../../main.css"/>
  <script type="text/javascript" src="../functionalTesting/funcTestsUtil.js"></script>
</head>

<body>
<f:view>
  <h:form id="fn">
   <%@ include file="withFoldingPanel_core.xhtml" %>
  </h:form>
</f:view>

</body>
</html>