# TeamCity Terms Of Services Plugin

This TeamCity plugin allows displaying an additional license agreement (terms of services, privacy policy) to all the TeamCity server users and requiring users to accept the agreement before they can continue to interact with the service. The plugin can be useful for implementing GDPR requirements for a TeamCity installation.

As soon as the plugin is installed and configured correctly, all the users will be redirected to the page with the text of the license agreement before they are able to perform any further actions. This occurrs on the next action in the web UI for the current users or right after the login for the newly logging in users.

The plugin is compatible with TeamCity 2017.1 and later.

# Installation
* Download the  [latest plugin build](https://teamcity.jetbrains.com/viewType.html?buildTypeId=TeamCityPluginsByJetBrains_TeamcityTermsOfServicePlugin_Build&guest=1)
* [Install the plugin](https://confluence.jetbrains.com/display/TCDL/Installing+Additional+Plugins)

# Configuration
The plugin is configured using the settings file `<TeamCity Data Directory>/config/termsOfService/settings.xml`. Until a valid file is found in the location, the plugin does not change any behavior. The file is automatically reloaded while the server is running. In case the file is invalid, teamcity-server.log gets related warnings of the "TermsOfService" category.
Upon the first plugin installation, the "termsOfService" directory and a file in it need to be created manually.

## Force all users except guest to accept the agreement 

If you want any user (except guest) to be aware of a certain agreement, do the following:

* Add the following configuration to the `<TeamCity Data Directory>/config/termsOfService/settings.xml` file
```xml
    <terms-of-service>
        <agreement id="privacy_policy"> <!-- Any identifier of the agreement, it's used when persisting the fact of the agreement acceptance by a user-->
            <parameters>
              <param name="content-file" value="agreement.html"/>  <!-- Path to the file containing agreement html, relative to the <TeamCity Data Directory>/config/termsOfService/ directory  -->
              <param name="short-name" value="Privacy Policy"/>  <!-- Name of the link to the agreement in footer -->
              <param name="full-name" value="Privacy Policy for Hosted TeamCity (https://teamcity.jetbrains.com)"/>	<!-- Title of the agreement shown on the agreement page-->
              <param name="version" value="2017.6"/>  <!-- Current version of the agreement. When changed all users will have to accept it again. -->
              <param name="last-updated" value="08 January 2018"/>  <!-- Optional parameter. When defined it will be displayed in a note describing why the user have to accept the agreement again. -->
            </parameters>
        </agreement>
    </terms-of-service>
```
* Place the agreement HTML in the `<TeamCity Data Directory>/config/termsOfService/agreement.html` file 

## Show set of consents for users to agree/disagree 

If you want to ask users to agree with a list of consents, add the 'consents' elements to the 'agreement' element:

* `<TeamCity Data Directory>/config/termsOfService/settings.xml`:
```xml
    <terms-of-service>
        <agreement id="privacy_policy">
            <parameters>
              <param name="content-file" value="agreement.html"/>  
              <param name="short-name" value="Privacy Policy"/>  
              <param name="full-name" value="Privacy Policy for Hosted TeamCity (https://teamcity.jetbrains.com)"/>	
              <param name="version" value="2017.6"/>  
            </parameters>
            <consents>
                <consent id="newsletter" text="Yes please, I'd like to receive emails about offers and services" default="true"/>
                <consent id="thirdPartyData" text="Yes, I allow to share my personal data with third parties" default="true"/>
            </consents>
        </agreement>
    </terms-of-service>
```

* With such configuration a user will be asked to agree with two optional consents on the agreement page. 
* A special tab will also be shown to the user in the 'My Settings & Tools' area. On this tab users can review and modify the list of accepted consents.

## Show special notice to guest user

If you want to additionally display a special notice to the guest user, do the following:
* Add the following configuration to the `<TeamCity Data Directory>/config/termsOfService/settings.xml` file
```xml
<terms-of-service>
    <agreement id="privacy_policy"> 
        <parameters>
            <param name="content-file" value="agreement.html"/>  
            <param name="short-name" value="Privacy Policy"/>  
            <param name="full-name" value="Privacy Policy for Hosted TeamCity (https://teamcity.jetbrains.com)"/>	
            <param name="version" value="2017.6"/>  
        </parameters>
    </agreement>
    <guest-notice>
        <parameters>
            <param name="content-file" value="guestNotice.html"/> <!-- Path to the file containing notice html, relative to the <TeamCity Data Directory>/config/termsOfService/ directory  -->
            <param name="text" value="A privacy reminder from JetBrains"/>  <!-- Short text to be shown in the notice-->
            <param name="accepted-cookie-name" value="guest-notice-accepted"/> <!-- The name of the cookie where the fact of acceptance is saved -->
            <param name="accepted-cookie-max-age-days" value="30"/> <!-- The cookie's expiration interval. After the specified number of days the user will be asked to confirm the notice again. -->
        </parameters>
    </guest-notice>
</terms-of-service>
```
* Place the agreement in the `<TeamCity Data Directory>/config/termsOfService/agreement.html` file 
* Place the guest notice in the `<TeamCity Data Directory>/config/termsOfService/guestNotice.html` file 


# Data related to the agreement acceptance

The data related to the agreement acceptance by a user is saved and can be fetched from the user properties:

* _teamcity.policy.<agreement_id>.acceptedDate_ - the date when the agreement was accepted
* _teamcity.policy.<agreement_id>.acceptedFromIP_ - the IP address of the request when the user accepts the agreement
* _teamcity.policy.<agreement_id>.acceptedVersion_ - the version of the agreement that was accepted by the user. 
* _teamcity.policy.<agreement_id>.consent.<consent_id>.accepted_ - true if the consent was accepted
* _teamcity.policy.<agreement_id>.consent.<consent_id>.acceptedDate_ - the date when the consent was accepted
* _teamcity.policy.<agreement_id>.consent.<consent_id>.acceptedFromIP_ - the IP address of the request when the user accepts the consent

