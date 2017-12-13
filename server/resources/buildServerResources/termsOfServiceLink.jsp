<%@include file="/include-internal.jsp" %>
<%--@elvariable id="agreementName" type="java.lang.String"--%>
<%--@elvariable id="agreementLink" type="java.lang.String"--%>
<%--@elvariable id="teamcityPluginResourcesPath" type="java.lang.String"--%>
<c:if test="${agreementName != null}">

    <div id="tsLinks" style="display: none;">
            <br/>
            <c:url var="url" value="${agreementLink}"/>
            <a href="${url}" onclick="BS.Util.popupWindow('${url}', 'agreement0'); return false" class="licenseAgreementLink" type="">${agreementName}</a>
    </div>
</c:if>

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