<%@include file="/include-internal.jsp" %>
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
        <c:set var="name" value="${termsOfServiceName}"/>
        <div class="agreementPage">
            <div class="agreement">
                ${agreementText}
            </div>
        </div>
    </bs:_loginPageDecoration>
  </jsp:attribute>
</bs:externalPage>
