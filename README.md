
# TeamCity Terms Of Services Plugin [![official JetBrains project](http://jb.gg/badges/official.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub)

This TeamCity plugin allows displaying an additional agreement (terms of services, privacy policy) to all the TeamCity server users and requiring users to accept the agreement before they can continue to interact with the service. The plugin can be useful for implementing GDPR requirements for a TeamCity installation.

As soon as the plugin is installed and configured correctly, all the newly logged in users will be redirected to the page with the text of the license agreement before they are able to perform any further actions. 
This doesn't occur for the users who are currently using Web UI.

The plugin is compatible with TeamCity 2017.1 and later.

# Installation
* Download the  [latest plugin build](https://teamcity.jetbrains.com/viewType.html?buildTypeId=TeamCityPluginsByJetBrains_TeamcityTermsOfServicePlugin_Build&guest=1)
* [Install the plugin](https://confluence.jetbrains.com/display/TCDL/Installing+Additional+Plugins)

# Configuration
The plugin is configured using the settings file `<TeamCity Data Directory>/config/termsOfService/settings.xml`. The file is automatically reloaded while the server is running. In case the file is invalid, teamcity-server.log gets related warnings of the "TermsOfService" category.

Upon the first plugin installation, the default configuration is created in the "termsOfService" directory. This configuration is disabled and does not change any behavior.

To activate the settings, do the following:

* modify the settings.xml file based on your needs (see possible configurations below). You can modify [Data Directory](https://confluence.jetbrains.com/display/TCD10/TeamCity+Data+Directory) content in the browser,
* replace the content of the agreement.html and/or guestNotice.html files with your agreements,
* change the values of 'enabled' attributes from 'false' to 'true' in the settings.xml file. 


##### Force all users except guest to accept the agreement 

If you want any user (except guest) to be aware of a certain agreement, do the following:

* Add the following configuration to the `<TeamCity Data Directory>/config/termsOfService/settings.xml` file
```xml
    <terms-of-service>
        <!-- id: any identifier of the agreement, it's used when persisting the fact of the agreement acceptance by a user-->
        <!-- enabled: optional attribute, can be used to disable the agreement-->
        <agreement id="privacy_policy" enabled="true"> 
            <parameters>
              <param name="content-file" value="agreement.html"/>  <!-- Path to the file containing agreement html, relative to the <TeamCity Data Directory>/config/termsOfService/ directory  -->
              <param name="short-name" value="Privacy Policy"/>  <!-- Name of the link to the agreement in footer -->
              <param name="full-name" value="Privacy Policy for TeamCity"/>	<!-- Title of the agreement shown on the agreement page-->
              
              <!-- 
                  Optional parameter. By default all the sessions which were active when the agreement was introduced don't have to accept it. 
                  This parameter can be used to force all the active sessions to accept the agreement after the specified date.
              -->
              <param name="enforcement-date" value="2018-01-13T00:00+0000"/>  
              
              <param name="version" value="2017.6"/>  <!-- Current version of the agreement. When changed all users will have to accept it again. -->
              
              <!-- 
                Optional parameter. When defined it will be displayed on the 'Accept agreement' page in a note describing why the user have to accept the agreement. 
                When not defined default note will be shown.   
              -->
              <param name="new-user-note" value="You have to accept the Privacy Policy agreement before you can continue to use TeamCity.
                                                    Review the terms and click 'I agree' when you're ready to proceed."/>  
              
              <!-- 
                Optional parameter. When defined it will be displayed on the 'Accept agreement' page in a note describing why the user have to accept the agreement again. 
                When not defined default note will be shown.  
              -->
              <param name="new-version-note" value="We've updated the Privacy Policy agreement.
                                                    Review the terms and click 'I agree' when you're ready to continue using TeamCity."/>  
            </parameters>
        </agreement>
    </terms-of-service>
```
* Place the agreement HTML in the `<TeamCity Data Directory>/config/termsOfService/agreement.html` file 

##### Test the agreement - show it to a single user only 

If you want check how the agreement works, you can enable it for a single user only:

* Add 'user-filter' attribute to the 'agreement' element:
```xml
    <terms-of-service>
        <agreement id="privacy_policy" enabled="true" user-filter="username:admin"> 
            <parameters>
              <param name="content-file" value="agreement.html"/> 
              <param name="short-name" value="Privacy Policy"/>  
              <param name="full-name" value="Privacy Policy for TeamCity"/>	
              <param name="version" value="2017.6"/>  
            </parameters>
        </agreement>
    </terms-of-service>
```
* With the specified configuration the agreement will be shown to the 'admin' user only.

##### Show set of consents for users to agree/disagree 

If you want to ask users to agree with a list of consents, add the 'consents' elements to the 'agreement' element:

* `<TeamCity Data Directory>/config/termsOfService/settings.xml`:
```xml
    <terms-of-service>
        <agreement id="privacy_policy">
            <parameters>
              <param name="content-file" value="agreement.html"/>  
              <param name="short-name" value="Privacy Policy"/>  
              <param name="full-name" value="Privacy Policy for TeamCity"/>	
              <param name="version" value="2017.6"/>  
            </parameters>
            <consents>
                <consent id="newsletter" text="Yes please, I'd like to receive emails about offers and services" default="true"/>
                <consent id="thirdPartyData" text="Yes, I allow to share my personal data with third parties" default="true"/>
            </consents>
        </agreement>
    </terms-of-service>
```
* If you need to add HTML to the consent text, use the 'file' attribute instead of 'text':
     ```xml
     <consent id="newsletter" file="newsletterConsent.html" default="true"/>
     ```
     Put the consent HTML in the <TeamCity Data Directory>/config/termsOfService/newsletterConsent.html file.
      
* With such configuration a user will be asked to agree with two optional consents on the agreement page. 
* A special tab will also be shown to the user in the 'My Settings & Tools' area. On this tab users can review and modify the list of accepted consents.

##### Show special notice to guest user

If you want to additionally display a special notice to the guest user, do the following:
* Add the following configuration to the `<TeamCity Data Directory>/config/termsOfService/settings.xml` file
```xml
<terms-of-service>
    <!-- enabled: optional attribute, can be used to disable the notice-->
    <guest-notice enabled="true">
        <parameters>
            <param name="content-file" value="guestNotice.html"/> <!-- Path to the file containing notice html, relative to the <TeamCity Data Directory>/config/termsOfService/ directory  -->
            <param name="text" value="A privacy reminder from JetBrains"/>  <!-- Short text to be shown in the notice-->
            <param name="accepted-cookie-name" value="guest-notice-accepted"/> <!-- The name of the cookie where the fact of acceptance is saved -->
            <param name="accepted-cookie-max-age-days" value="30"/> <!-- The cookie's expiration interval. After the specified number of days the user will be asked to confirm the notice again. -->
        </parameters>
    </guest-notice>
</terms-of-service>
```
* Place the guest notice in the `<TeamCity Data Directory>/config/termsOfService/guestNotice.html` file 


# Data related to the agreement acceptance

The data related to the agreement acceptance by a user is saved and can be fetched from the user properties:

* _teamcity.termsOfService.<agreement_id>.acceptedDate_ - the date when the agreement was accepted
* _teamcity.termsOfService.<agreement_id>.acceptedFromIP_ - the IP address of the request when the user accepts the agreement
* _teamcity.termsOfService.<agreement_id>.acceptedVersion_ - the version of the agreement that was accepted by the user 
* _teamcity.termsOfService.<agreement_id>.consent.<consent_id>.accepted_ - true if the consent was accepted
* _teamcity.termsOfService.<agreement_id>.consent.<consent_id>.acceptedDate_ - the date when the consent was accepted
* _teamcity.termsOfService.<agreement_id>.consent.<consent_id>.acceptedFromIP_ - the IP address of the request when the user accepts the consent

