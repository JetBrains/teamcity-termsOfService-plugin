<%@ page import="jetbrains.buildServer.web.openapi.PlaceId" %>
<%@include file="/include-internal.jsp"%>
<c:set var="pageTitle" value="Terms Of Services"/>
<bs:externalPage>
  <jsp:attribute name="page_title">${pageTitle}</jsp:attribute>
  <jsp:attribute name="head_include">
    <bs:linkCSS>
      /css/forms.css
      /css/initialPages.css
      serviceTerms.css
    </bs:linkCSS>
  </jsp:attribute>
  <jsp:attribute name="body_include">
    <c:set var="name" value="Terms of Service"/>
    <div class="licensesPage">
      <div class="agreement">
        <jsp:include page="_text.jspf"/>
      </div>
      <div class="agreementForm clearfix">
        <p>You must accept the ${name} to proceed.</p>
        <form action="${pageUrl}" method="post" onsubmit="if (!this.accept.checked) { alert('Please accept the ${name}'); return false; };">
          <p><forms:checkbox name="accept"/><label for="accept" class="rightLabel">Accept ${name}</label></p>
          <ext:includeExtensions placeId="<%=PlaceId.ACCEPT_LICENSE_SETTING%>"/>
          <p class="continueBlock">
            <input class="btn" type="submit" name="Continue" value="Continue &raquo;"/>
          </p>
        </form>
      </div>
    </div>
  </jsp:attribute>
</bs:externalPage>
