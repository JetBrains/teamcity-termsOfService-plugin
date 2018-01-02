<%@include file="/include-internal.jsp" %>
<%--@elvariable id="guestNotice" type="jetbrains.buildServer.termsOfService.TermsOfServiceManager.GuestNotice"--%>
<c:if test="${guestNotice != null}">
    <bs:linkCSS>
        ${teamcityPluginResourcesPath}/termsOfService.css
    </bs:linkCSS>

    <div id="guestNotice" style="display: none;">
        <c:out value="${guestNotice.text}" />
        <input class="btn btn_primary submitButton" style="margin-left: 1.5em;" type="button" value="Review now" onclick="BS.TermsOfServicesGuestNoteDialog.showCentered(); return false;"/>
    </div>


    <bs:dialog dialogId="agreementDialog" closeCommand="BS.TermsOfServicesGuestNoteDialog.close();" title="${guestNotice.text}" dialogClass="modalDialog agreementDialog">
        <div style="max-height: 35em; overflow: scroll;" >
            ${guestNotice.html}
        </div>

        <div class="popupSaveButtonsBlock">
            <forms:submit label="Accept" onclick="BS.TermsOfServicesGuestNoteDialog.accept();"/>
            <forms:cancel onclick="BS.TermsOfServicesGuestNoteDialog.close();"/>
        </div>
    </bs:dialog>


    <script type="text/javascript">
        BS.TermsOfServicesGuestNoteDialog = OO.extend(BS.AbstractModalDialog, {

            getContainer: function () {
                return $('agreementDialog');
            },

            accept: function () {
                BS.Cookie.set("termsOfServiceAccepted", "true", 30);
                BS.Util.hide("guestNotice");
                this.close();
            }
        });

        $j(document).ready(function() {
            if (!BS.Cookie.get("termsOfServiceAccepted")) {
                BS.Util.show("guestNotice");
            }
        });
    </script>
</c:if>




