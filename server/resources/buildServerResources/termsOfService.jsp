<%@ page import="jetbrains.buildServer.web.openapi.PlaceId" %>
<%@include file="/include-internal.jsp" %>
<%--@elvariable id="pageUrl" type="java.lang.String"--%>
<%--@elvariable id="descriptionFile" type="java.lang.String"--%>
<%--@elvariable id="contentFile" type="java.lang.String"--%>
<%--@elvariable id="termsOfServiceName" type="java.lang.String"--%>
<c:set var="name" value="${termsOfServiceName}"/>
<c:set var="pageTitle" value="${name}"/>
<bs:externalPage>
    <jsp:attribute name="page_title">${pageTitle}</jsp:attribute>
    <jsp:attribute name="head_include">
        <c:if test="${intprop:getBoolean('teamcity.plugin.testDrive.googleAnalytics.enabled')}">
            <!-- Google Tag Manager -->
            <!-- NOTE! THE SAME SCRIPT IS USED ON OTHER PAGES ON TEAMCITY.JETBRAINS.COM USING STATIC UI EXTENSIONS -->
            <script>
                (function (w, d, s, l, i) {
                    w[l] = w[l] || [];
                    w[l].push({
                        'gtm.start': new Date().getTime(), event: 'gtm.js'
                    });
                    var f = d.getElementsByTagName(s)[0],
                        j = d.createElement(s), dl = l != 'dataLayer' ? '&l=' + l : '';
                    j.async = true;
                    j.src =
                        'https://www.googletagmanager.com/gtm.js?id=' + i + dl;
                    f.parentNode.insertBefore(j, f);
                })(window, document, 'script', 'dataLayer', 'GTM-5P98');
            </script>
            <!-- End Google Tag Manager -->
        </c:if>
    <bs:linkCSS>
      /css/forms.css
      /css/initialPages.css
      /css/maintenance-initialPages-common.css
        ${teamcityPluginResourcesPath}/termsOfService.css
    </bs:linkCSS>
  </jsp:attribute>
    <jsp:attribute name="body_include">
        <c:if test="${intprop:getBoolean('teamcity.plugin.testDrive.googleAnalytics.enabled')}">
            <!-- Google Tag Manager (noscript)-->
            <!-- NOTE! THE SAME SCRIPT IS USED ON OTHER PAGES ON TEAMCITY.JETBRAINS.COM USING STATIC UI EXTENSIONS -->
                <noscript><iframe src="https://www.googletagmanager.com/ns.html?id=GTM-5P98"
                                  height="0" width="0" style="display:none;visibility:hidden"></iframe></noscript>
            <!-- End Google Tag Manager (noscript)-->
        </c:if>
    <bs:_loginPageDecoration id="agreementPage" title="${name}">
    <c:set var="name" value="${termsOfServiceName}"/>
    <div class="agreementPage">
        <div class="agreement">
        <c:if test="${not empty descriptionFile}">
          <div class="description">
              <jsp:include page="${descriptionFile}"/>
          </div>
        </c:if>
            <jsp:include page="${contentFile}"/>
        </div>
    </div>
    </bs:_loginPageDecoration>
  </jsp:attribute>
</bs:externalPage>
