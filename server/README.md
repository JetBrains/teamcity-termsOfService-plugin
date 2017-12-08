## TeamCity Terms Of Services plugin

ATTENTION! Please read carefully the whole text below. Any changes in the plugin can affect __TeamCity Test Drive__ project. 

By default every TeamCity installation requires the server administrator only to read and accept one main license agreement. With this plugin TeamCity installation supports additional license agreement (terms of services, any other information) that should be read and accepted by specific kind of TeamCity server users before they can interact with the service any further. 
The plugin can be installed from the very beginning of the server existence or added any time later.
As soon as plugin is installed to the server any user targeted by plugin configuration will be redirected to specific page with the text of license agreement before she will be able to perform any further actions.

Plugin configuration is provided as a set of `spring` beans in `build-server-plugin-terms-of-service.xml` file. License agreement texts should be added as plugin resources directly to `buildServerResources` folder.
  
## Current Status

For internal use only. External configuration is not supported. 
If you need the plugin for the specific TeamCity installation 
* make a separate branch
* change the plugin configuration according to your needs (examples you can find in latest section of this document)
* add license agreement texts as you need them 
* build the plugin  
* install the plugin to the TeamCity server 

Please note: once the plugin is built there is no options to change plugin configuration.

Attention! Branch 'master' contains plugin configuration and license agreements texts required for __TeamCity Test Drive__ project. Don't change anything until you a re sure the project will not be hurt.


## Under The Hood

You can use the plugin to show any number of terms of services for any (or both) of two user groups
* Any user except guest
* Any project manager

## Possible configurations

* If you want any user (except guest) to be aware of certain terms of services you should
    1) Place the meaningful text in `_text.jspf` file (page formatting and text styles will be applied automatically)
    2) Put the following bean set in `build-server-plugin-terms-of-service.xml` file

```xml
    <bean class="jetbrains.buildServer.termsOfService.TermsOfServiceHandlerInterceptor" lazy-init="false">
        <constructor-arg ref="userServiceTermsManager"/>
    </bean>

    <bean id="userServiceTermsController" class="jetbrains.buildServer.termsOfService.TermsOfServiceController">
        <constructor-arg index="3" ref="userServiceTermsManager"/>
    </bean>

    <bean id="userServiceTermsManager" class="jetbrains.buildServer.termsOfService.PropertyBasedManager">
        <constructor-arg>
            <bean class="jetbrains.buildServer.termsOfService.FileBasedConfig">
                <constructor-arg value=".teamcity.userTermsOfServices.accepted"/>
                <constructor-arg value="User Terms of Service"/>
                <constructor-arg value="/userTerms.html"/>
                <constructor-arg value="_text.jspf"/>
            </bean>
        </constructor-arg>
        <constructor-arg>
            <util:constant static-field="jetbrains.buildServer.termsOfService.TermsOfServiceUtil.ANY_USER_NO_GUEST"/>
        </constructor-arg>
    </bean>

    <bean class="jetbrains.buildServer.termsOfService.TermsOfServicesLink">
        <constructor-arg index="0">
            <util:list>
                <ref bean="userServiceTermsManager"/>                
            </util:list>
        </constructor-arg>
    </bean>
```

* If you want any user (except guest) to accept a certain license agreement and in addition to that you need any project manager to accept another agreemnt you need to 
    1) Place the meaningful text of user agreement in `_text.jspf` file (page formatting and text styles will be applied automatically)
    2) Place the meaningful text of project manager agreement in `_pm_text` file (page formatting and text styles will be applied automatically)
    2) Put the following bean set in `build-server-plugin-terms-of-service.xml` file

```xml
<bean class="jetbrains.buildServer.termsOfService.TermsOfServiceHandlerInterceptor" lazy-init="false">
        <constructor-arg ref="userServiceTermsManager"/>
    </bean>

    <bean class="jetbrains.buildServer.termsOfService.TermsOfServiceHandlerInterceptor" lazy-init="false">
        <constructor-arg ref="pmServiceTermsManager"/>
    </bean>

    <bean id="userServiceTermsController" class="jetbrains.buildServer.termsOfService.TermsOfServiceController">
        <constructor-arg index="3" ref="userServiceTermsManager"/>
    </bean>

    <bean id="pmServiceTermsController" class="jetbrains.buildServer.termsOfService.ProjectAdminTermsOfServiceController">
        <constructor-arg index="3" ref="pmServiceTermsManager"/>
    </bean>

    <bean id="userServiceTermsManager" class="jetbrains.buildServer.termsOfService.PropertyBasedManager">
        <constructor-arg>
            <bean class="jetbrains.buildServer.termsOfService.FileBasedConfig">
                <constructor-arg value=".teamcity.userTermsOfServices.accepted"/>
                <constructor-arg value="User Terms of Service"/>
                <constructor-arg value="/userTerms.html"/>
                <constructor-arg value="_text.jspf"/>
            </bean>
        </constructor-arg>
        <constructor-arg>
            <util:constant static-field="jetbrains.buildServer.termsOfService.TermsOfServiceUtil.ANY_USER_NO_GUEST"/>
        </constructor-arg>
    </bean>


    <bean id="pmServiceTermsManager" class="jetbrains.buildServer.termsOfService.PropertyBasedManager">
        <constructor-arg>
            <bean class="jetbrains.buildServer.termsOfService.FileBasedConfig">
                <constructor-arg value=".teamcity.pmTermsOfServices.accepted"/>
                <constructor-arg value="Project Manager Terms of Service"/>
                <constructor-arg value="/pmTerms.html"/>
                <constructor-arg value="_pm_text.jspf"/>
            </bean>
        </constructor-arg>
        <constructor-arg>
            <util:constant static-field="jetbrains.buildServer.termsOfService.TermsOfServiceUtil.PROJECT_MANAGER_NO_GUEST"/>
        </constructor-arg>
    </bean>


    <bean class="jetbrains.buildServer.termsOfService.TermsOfServicesLink">
        <constructor-arg index="0">
            <util:list>
                <ref bean="userServiceTermsManager"/>
                <ref bean="pmServiceTermsManager"/>
            </util:list>
        </constructor-arg>
    </bean>
```