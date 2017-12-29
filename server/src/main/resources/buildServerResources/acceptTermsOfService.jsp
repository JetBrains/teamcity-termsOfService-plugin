<%@ page import="jetbrains.buildServer.web.openapi.PlaceId" %>
<%@include file="/include-internal.jsp"%>
<%--@elvariable id="pageUrl" type="java.lang.String"--%>
<%--@elvariable id="agreementText" type="java.lang.String"--%>
<%--@elvariable id="termsOfServiceName" type="java.lang.String"--%>
<c:set var="name" value="${termsOfServiceName}"/>
<c:set var="pageTitle" value="${name}"/>
<bs:externalPage>
  <jsp:attribute name="page_title">${pageTitle}</jsp:attribute>
  <jsp:attribute name="head_include">
    <bs:linkCSS>
      /css/forms.css
      /css/initialPages.css
      /css/maintenance-initialPages-common.css
      ${teamcityPluginResourcesPath}/termsOfService.css
    </bs:linkCSS>
  </jsp:attribute>
  <jsp:attribute name="body_include">
    <bs:_loginPageDecoration id="agreementPage" title="${name}">
    <div class="agreementPage">
      <div class="agreement">
        ${agreementText}
      </div>
      <div class="agreementForm clearfix">
        <form action="${pageUrl}" method="post" onsubmit="if (!this.accept.checked) { alert('Please accept the ${name}'); return false; };">
          <c:forEach var="consent" items="${consents}">
            <%--@elvariable id="consent" type="jetbrains.buildServer.termsOfService.TermsOfServiceManager.Consent"--%>
            <p><forms:checkbox name="${consent.id}" checked="${consent.checkedByDefault}"/><label for="${consent.id}" class="rightLabel">${consent.text}</label></p>
          </c:forEach>
          <ext:includeExtensions placeId="<%=PlaceId.ACCEPT_LICENSE_SETTING%>"/>
          <div class="continueBlock">
            <forms:submit label="I agree"/>
            &nbsp;
            <c:url value="/ajax.html?logout=1" var="logoutUrl"/>
            <a class="logout" href="#" onclick="BS.Logout('${logoutUrl}'); return false;">Log out</a>
          </div>
        </form>
      </div>
    </div>
    </bs:_loginPageDecoration>
  </jsp:attribute>
</bs:externalPage>
