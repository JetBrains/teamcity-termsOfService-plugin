<%@include file="/include-internal.jsp" %>
<%--@elvariable id="termsOfServices" type="java.util.List<jetbrains.buildServer.termsOfService.TermsOfServiceManager>"--%>
<%--@elvariable id="teamcityPluginResourcesPath" type="java.lang.String"--%>
<div id="tsLinks" style="display: none;">
    <c:forEach items="${termsOfServices}" var="ts" varStatus="tsStatus">
        <br/>
        <c:url var="url" value="${entryPointPrefix}${ts.config.path}"/>
        <a href="${url}" onclick="BS.Util.popupWindow('${url}', 'agreement${tsStatus.index}'); return false" class="licenseAgreementLink" type="">${ts.config.name}</a>
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