BS.UserConsentsForm = OO.extend(BS.AbstractWebForm, {

    formElement: function() {
        return $('editUserConsentsForm');
    },

    setupEventHandlers: function() {
        var that = this;

        this.setUpdateStateHandlers({
            updateState: function() {
                that.saveInSession();
            },
            saveState: function() {
                that.submitConsents();
            }
        });
    },

    saveInSession: function() {
        $("submitUserConsents").value = 'storeInSession';
        BS.FormSaver.save(this, this.formElement().action, BS.StoreInSessionListener);
    },

    submitConsents: function() {
        $("submitUserConsents").value = 'store';

        BS.FormSaver.save(this, this.formElement().action, OO.extend(BS.ErrorsAwareListener, {
            onSuccessfulSave: function() {
                BS.reload(true);
                return false;
            }
        }));
        return false;
    }
});
