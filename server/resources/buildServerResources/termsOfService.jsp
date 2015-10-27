<%@ page import="jetbrains.buildServer.web.openapi.PlaceId" %>
<%@include file="/include-internal.jsp"%>
<%--@elvariable id="pageUrl" type="java.lang.String"--%>
<%--@elvariable id="descriptionFile" type="java.lang.String"--%>
<%--@elvariable id="contentFile" type="java.lang.String"--%>
<%--@elvariable id="termsOfServiceName" type="java.lang.String"--%>
<c:set var="name" value="${termsOfServiceName}"/>
<c:set var="pageTitle" value="${name}"/>
<bs:externalPage>
  <jsp:attribute name="page_title">${pageTitle}</jsp:attribute>
  <jsp:attribute name="head_include">
    <bs:linkCSS>
      /css/forms.css
      /css/initialPages.css
      ${teamcityPluginResourcesPath}/termsOfService.css
    </bs:linkCSS>
  </jsp:attribute>
  <jsp:attribute name="body_include">
    <c:set var="name" value="${termsOfServiceName}"/>
    <div class="licensesPage">
      <c:if test="${not empty descriptionFile}">
        <div class="description">
          <jsp:include page="${descriptionFile}"/>
        </div>
      </c:if>
      <div class="agreement">
        <jsp:include page="${contentFile}"/>
      </div>
    </div>
  </jsp:attribute>
</bs:externalPage>
