<%@include file="/include-internal.jsp" %>
<%--@elvariable id="agreements" type="java.util.List"--%>
<%--@elvariable id="externalAgreements" type="java.util.List"--%>
<%--@elvariable id="teamcityPluginResourcesPath" type="java.lang.String"--%>
<c:if test="${fn:length(agreements) + fn:length(externalAgreements) > 0}">
    <bs:linkCSS>
        ${teamcityPluginResourcesPath}/termsOfService.css
    </bs:linkCSS>

    <div id="tsLinks" style="display: none;">
        <c:forEach items="${agreements}" var="agreement">
            <%--@elvariable id="agreement" type="jetbrains.buildServer.termsOfService.TermsOfServiceManager.Agreement"--%>
            <c:url var="url" value="${agreement.link}"/>
            <span class="licenseAgreementLink">
                <a href="${url}" onclick="BS.Util.popupWindow('${url}', 'agreement0'); return false">${agreement.shortName}</a>
            </span>
        </c:forEach>
        <c:forEach items="${externalAgreements}" var="agreement">
            <%--@elvariable id="agreement" type="jetbrains.buildServer.termsOfService.TermsOfServiceManager.ExternalAgreementLink"--%>
            <c:url var="url" value="${agreement.url}"/>
            <span class="licenseAgreementLink">
                <a href="${url}" onclick="BS.Util.popupWindow('${url}', 'agreement0'); return false">${agreement.name}</a>
            </span>
        </c:forEach>
    </div>


    <script type="text/javascript">
        BS.TermsOfServices = {
            updateFooter: function(){
                var element = $j(".column3 .columnContent");
                var html = element.html();
                element.html(html + $j("#tsLinks").html());
            }
        };
        $j(document).ready(function() {
            BS.TermsOfServices.updateFooter();
        });
    </script>
</c:if>
