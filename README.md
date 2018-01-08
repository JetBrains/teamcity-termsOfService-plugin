# TeamCity Terms Of Services Plugin

By default every TeamCity installation requires the server administrator only to read and accept one main license agreement. 
This plugin allows to specify additional license agreement (terms of services, privacy policy) that should be read and accepted by all other TeamCity users before they can interact with the service any further. 

The plugin can be installed from the very beginning of the server existence or added any time later.
As soon as plugin is installed to the server and configured any user will be redirected to specific page with the text of license agreement before she will be able to perform any further actions.

Plugin configuration should be provided in the <TeamCity Data Directory>/config/termsOfService/settings.xml file. 
    
# Installation
* Donwload the latest plugin build [here](https://teamcity.jetbrains.com/viewType.html?buildTypeId=TeamCityPluginsByJetBrains_TeamcityTermsOfServicePlugin_Build&guest=1)
* [Install the plugin](https://confluence.jetbrains.com/display/TCDL/Installing+Additional+Plugins)
* Configure the plugin as described below
* The plugin is compatible with TeamCity versions starting from 2017.1

# Possible configurations

## Force all users except guest to accept the agreement 

If you want any user (except guest) to be aware of certain agreement you should

* Put the following configuration in `<TeamCity Data Directory>/config/termsOfService/settings.xml` file
```xml
    <terms-of-service>
        <agreement id="privacy_policy"> <!-- Any identifier of the agreement, it's used when persisting the fact of the agreement acceptance by an user-->
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
* Place the agreement HTML in `<TeamCity Data Directory>/config/termsOfService/agreement.html` file 

## Show set of consents for users to agree/disagree 

If you want to ask users to agree with a list of consents you should add 'consents' elements to the 'agreement' element:

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
* Also a special tab will be shown to the user in 'My Settings & Tools' area. On this tab user can review and modify list of accepted consents.

## Show special notice to guest user

If in addition you want to display a special notice to the guest user you should
* Put the following configuration in `<TeamCity Data Directory>/config/termsOfService/settings.xml` file
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
* Place the agreement html in `<TeamCity Data Directory>/config/termsOfService/agreement.html` file 
* Place the guest notice html in `<TeamCity Data Directory>/config/termsOfService/guestNotice.html` file 


# Data related to the agreement acceptance

The data related to the agreement acceptance by a user is saved and can be fetched from the user properties:

* _teamcity.policy.<agreement_id>.acceptedDate_ - date when the agreement was accepted
* _teamcity.policy.<agreement_id>.acceptedFromIP_ - IP address of the request when the user accepts the agreement
* _teamcity.policy.<agreement_id>.acceptedVersion_ - version of the agreement that was accepted by the user. 
* _teamcity.policy.<agreement_id>.consent.<consent_id>.accepted_ - true if the consent was accepted
* _teamcity.policy.<agreement_id>.consent.<consent_id>.acceptedDate_ - date when the consent was accepted
* _teamcity.policy.<agreement_id>.consent.<consent_id>.acceptedFromIP_ - IP address of the request when the user accepts the consent

