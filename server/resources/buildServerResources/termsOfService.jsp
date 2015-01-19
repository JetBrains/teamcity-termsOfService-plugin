<%@include file="/include-internal.jsp"%>
<c:set var="pageTitle" value="Terms Of Services"/>
<%@ taglib prefix="tos" tagdir=""%>
<bs:externalPage>
  <jsp:attribute name="page_title">${pageTitle}</jsp:attribute>
  <jsp:attribute name="head_include">
    <bs:linkCSS>
      /css/initialPages.css
      serviceTerms.css
    </bs:linkCSS>
  </jsp:attribute>
  <jsp:attribute name="body_include">
    <div class="licensesPage">
      <div class="agreement">
        <jsp:include page="${contentFile}"/>
      </div>
    </div>
  </jsp:attribute>
</bs:externalPage>
