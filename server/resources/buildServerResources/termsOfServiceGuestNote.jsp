<%@include file="/include-internal.jsp" %>
<%--@elvariable id="guestNotice" type="jetbrains.buildServer.termsOfService.TermsOfServiceManager.GuestNotice"--%>
<c:if test="${guestNotice != null}">
    <div id="agreementNote" style="position: fixed; bottom: 0; left: 0; width: 100%; text-align: center; z-index: 4; opacity: 0.9; background-color: #ffa; border: 1px solid #8a8d7f; display: none;">
        <div style="padding: .5em 0;">
            <c:out value="${guestNotice.text}" />
            <input class="btn btn_primary submitButton" style="margin-left: 1.5em;" type="button" value="Review now" onclick="BS.TermsOfServicesGuestNoteDialog.showCentered(); return false;"/>
        </div>
    </div>


    <bs:dialog dialogId="agreementDialog" closeCommand="BS.TermsOfServicesGuestNoteDialog.close();" title="${guestNotice.text}" dialogClass="modalDialog agreementDialog">
        <div style="max-height: 35em; overflow: scroll;" >
            <bs:iframe url="${guestNotice.link}"/>
        </div>

        <div class="popupSaveButtonsBlock">
            <forms:submit label="Accept" onclick="BS.TermsOfServicesGuestNoteDialog.accept();"/>
            <forms:cancel onclick="BS.TermsOfServicesGuestNoteDialog.close();"/>
        </div>
    </bs:dialog>
</c:if>


<script type="text/javascript">
    BS.TermsOfServicesGuestNoteDialog = OO.extend(BS.AbstractModalDialog, {

        getContainer: function () {
            return $('agreementDialog');
        },

        accept: function () {
            BS.Cookie.set("termsOfServiceAccepted", "true", 30);
            BS.Util.hide("agreementNote");
            this.close();
        }
    });

    $j(document).ready(function() {
        if (!BS.Cookie.get("termsOfServiceAccepted")) {
            BS.Util.show("agreementNote");
        }
    });
</script>


<style type="text/css">
    .agreementDialog {
        width: auto;
    }
</style>

