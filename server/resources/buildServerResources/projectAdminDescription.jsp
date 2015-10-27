<%@include file="/include-internal.jsp"%>
<%--@elvariable id="projects" type="java.util.List<jetbrains.buildServer.serverSide.SProject>"--%>
<c:set var="projectsCount" value="${fn:length(projects)}"/>
<c:set var="maxInList" value="5"/>
<c:set var="projectsList">
    <c:forEach items="${projects}" var="p" varStatus="pStatus">
        <c:if test="${pStatus.index <= maxInList}">
            <div>${p.fullName}</div>
        </c:if>
    </c:forEach>
    <c:if test="${projectsCount > maxInList}">
        <div>and ${projectsCount-maxInList} more project<bs:s val="${projectsCount-maxInList}"/></div>
    </c:if>
</c:set>
You have permission <span class="roleName">EDIT_PROJECT</span> in <c:choose>
    <c:when test="${projectsCount == 1}"><span class="projectName"><c:out value="${projects[0].fullName}"/></span> project</c:when>
    <c:when test="${projectsCount > 1}"><bs:popup_static controlId="projectsListPoopup" linkOpensPopup="false" popup_options="delay: 0, shift: {x: 0, y: 20}">
          <jsp:attribute name="content">
              <div>${projectsList}</div>
          </jsp:attribute>
        <jsp:body>${projectsCount} projects</jsp:body>
    </bs:popup_static>
    </c:when>
</c:choose>
