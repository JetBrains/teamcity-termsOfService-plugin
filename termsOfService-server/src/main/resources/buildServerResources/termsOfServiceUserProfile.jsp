<%@include file="/include-internal.jsp" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%--@elvariable id="form" type="jetbrains.buildServer.termsOfService.TermsOfServiceUserProfileExtension.Form"--%>
<%--@elvariable id="teamcityPluginResourcesPath" type="java.lang.String"--%>

<bs:linkScript>
    ${teamcityPluginResourcesPath}/termsOfServiceUserProfile.js
</bs:linkScript>

<bs:messages key="consentsSaved"/>

<form action="<c:url value='/userProfileTermsOfService.html'/>" onsubmit="return BS.UserConsentsForm.submitConsents();" method="post"
      id="editUserConsentsForm">
    <input type="hidden" id="submitUserConsents" name="submitUserConsents" value="store"/>

    <div style="margin: 1em 0 1em 0;">
        <c:url var="url" value="${form.agreement.link}"/>
        <a href="${url}" onclick="BS.Util.popupWindow('${url}', 'agreement0'); return false">${form.agreement.fullName}</a>
    </div>

    <div class="settingsBlock">
        <div class="settingsBlockTitle">Consents to use personal data</div>
        <div class="settingsBlockContent">
            <c:forEach var="consent" items="${form.agreement.consents}">
                <div class="general-property">
                    <forms:checkbox name="${consent.id}" checked="${form.consentStates[consent.id]}"/><label for="${consent.id}" class="rightLabel">${consent.html}</label>
                </div>
            </c:forEach>
        </div>
    </div>

    <forms:modified onSave="return BS.UserConsentsForm.submitConsents();"/>

    <div class="saveButtonsBlock">
        <forms:submit id="submitConsents" name="submitConsents" label="Save" onclick="return BS.UserConsentsForm.submitConsents();"/>
        <forms:saving/>
    </div>
</form>

<script type="text/javascript">
    $j(document).ready(function() {
        BS.UserConsentsForm.setupEventHandlers();
        BS.UserConsentsForm.setModified(${form.stateModified});
    });
</script>