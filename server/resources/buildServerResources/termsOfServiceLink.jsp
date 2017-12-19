<%@include file="/include-internal.jsp" %>
<%--@elvariable id="agreements" type="java.util.List"--%>
<%--@elvariable id="teamcityPluginResourcesPath" type="java.lang.String"--%>
<c:if test="${fn:length(agreements) > 0}">

    <div id="tsLinks" style="display: none;">
        <c:forEach items="${agreements}" var="agreement">
            <%--@elvariable id="agreement" type="jetbrains.buildServer.termsOfService.TermsOfServiceManager.Agreement"--%>
            <br/>
            <c:url var="url" value="${agreement.link}"/>
            <a href="${url}" onclick="BS.Util.popupWindow('${url}', 'agreement0'); return false" class="licenseAgreementLink" type="">${agreement.shortName}</a>
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
