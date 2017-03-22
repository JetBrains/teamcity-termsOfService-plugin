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
      /css/maintenance-initialPages-common.css
      ${teamcityPluginResourcesPath}/termsOfService.css
    </bs:linkCSS>
  </jsp:attribute>
  <jsp:attribute name="body_include">
    <bs:_loginPageDecoration id="agreementPage" title="${name}">
    <div class="agreementPage">
      <div class="agreement">
        <c:if test="${not empty descriptionFile}">
          <div class="description">
            <jsp:include page="${descriptionFile}"/>
          </div>
        </c:if>
        <jsp:include page="${contentFile}"/>
      </div>
      <div class="agreementForm clearfix">
        <p>You must accept the ${name} to proceed.</p>
        <form action="${pageUrl}" method="post" onsubmit="if (!this.accept.checked) { alert('Please accept the ${name}'); return false; };">
          <p><forms:checkbox name="accept"/><label for="accept" class="rightLabel">Accept ${name}</label></p>
          <ext:includeExtensions placeId="<%=PlaceId.ACCEPT_LICENSE_SETTING%>"/>
          <div class="continueBlock">
            <forms:submit label="Continue &raquo;" name="Continue" disabled="${true}"/>
            &nbsp;
            <c:url value="/ajax.html?logout=1" var="logoutUrl"/>
            <a class="logout" href="#" onclick="BS.Logout('${logoutUrl}'); return false;">Log out</a>
          </div>
          <script>
              (function () {
                  var $submit = $j('.submitButton');
                  var $accept = $j('#accept');

                  function enableSubmitIfLicenseAccepted() {
                      $submit.prop('disabled', !$accept.prop('checked'));
                  }
                  $accept.on('click', enableSubmitIfLicenseAccepted);
                  enableSubmitIfLicenseAccepted();
              })();
          </script>
        </form>
      </div>
    </div>
    </bs:_loginPageDecoration>
  </jsp:attribute>
</bs:externalPage>
