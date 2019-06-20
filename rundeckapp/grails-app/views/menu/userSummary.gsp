    %{--
     - Copyright 2018 Rundeck, Inc. (http://rundeck.com)
     -
     - Licensed under the Apache License, Version 2.0 (the "License");
     - you may not use this file except in compliance with the License.
     - You may obtain a copy of the License at
     -
     -     http://www.apache.org/licenses/LICENSE-2.0
     -
     - Unless required by applicable law or agreed to in writing, software
     - distributed under the License is distributed on an "AS IS" BASIS,
     - WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
     - See the License for the specific language governing permissions and
     - limitations under the License.
     --}%
<html>
<head>
     <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
 <meta name="layout" content="base"/>
 <meta name="tabpage" content="configure"/>
 <meta name="tabtitle" content="${g.message(code:'page.users.title')}"/>
 <title><g:message code="page.users.title"/></title>
</head>

<body>
<div class="container-fluid">
   <div class="card">
       <div class="card-content">
           <div class="row">
               <div class="col-xs-12">
                   <div class="form-group pull-left" style="padding-top:10px">
                       <span class="prompt"><g:message code="page.users.summary"/></span>
                   </div>
                   <div class="form-group pull-right" style="display:inline-block;">
                       <div class="checkbox">
                           <g:checkBox name="logged" value="true" checked="${params.logged=='true'}" class="loggedOnly" id="loggedOnly"
                                onchange="togleLogged()"/>
                           <label for="loggedOnly">
                               Logged users only
                           </label>
                       </div>
                   </div>
               </div>
           </div>
       </div>
   </div>
   <div class="row">
       <div class="col-sm-12">
           <div class="card">
               <div class="card-content">
                   <div class="pageBody" id="userProfilePage">
                       <div class="row">
                           <div class="col-sm-12">
                               <table class="table table-condensed  table-striped">
                                   <tr>
                                       <th class="table-header">
                                           <g:message code="page.users.login.label"/>
                                       </th>
                                       <th class="table-header">
                                           <g:message code="domain.User.email.label"/>
                                       </th>
                                       <th class="table-header">
                                           <g:message code="domain.User.firstName.label"/>
                                       </th>
                                       <th class="table-header">
                                           <g:message code="domain.User.lastName.label"/>
                                       </th>

                                       <th class="table-header">
                                           <g:message code="page.users.created.label"/>
                                       </th>
                                       <th class="table-header">
                                           <g:message code="page.users.updated.label"/>
                                       </th>
                                       <th class="table-header">
                                           <g:message code="page.users.lastjob.label"/>
                                       </th>
                                       <th class="table-header">
                                           <g:message code="page.users.tokens.label"/>
                                           <g:helpTooltip code="page.users.tokens.help" css="text-primary"/>
                                       </th>
                                       <th class="table-header">
                                           LOGGED STATUS
                                       </th>

                                   </tr>
                                   <g:each in="${users}" var="user" status="index">
                                   <tr>
                                       <td>
                                           ${user.value.login}
                                       </td>
                                       <td>
                                           ${user.value.email}
                                           <g:if test="${!user.value.email}">
                                               <span class="text-primary small text-uppercase"><g:message code="not.set" /></span>
                                           </g:if>
                                       </td>
                                       <td>
                                           <g:enc>${user.value.firstName}</g:enc>
                                           <g:if test="${!user.value.firstName}">
                                               <span class="text-primary small text-uppercase"><g:message code="not.set" /></span>
                                           </g:if>
                                       </td>
                                       <td>
                                           <g:enc>${user.value.lastName}</g:enc>

                                           <g:if test="${!user.value.lastName}">
                                               <span class="text-primary small text-uppercase"><g:message code="not.set" /></span>
                                           </g:if>
                                       </td>

                                       <td>
                                           <g:formatDate date="${user.value.created}" format="MM/dd/yyyy hh:mm a"/>
                                       </td>
                                       <td>
                                           <g:formatDate date="${user.value.updated}" format="MM/dd/yyyy hh:mm a"/>
                                       </td>
                                       <td>
                                           <g:if test="${user.value.lastJob}">
                                               <g:formatDate date="${user.value.lastJob}" format="MM/dd/yyyy hh:mm a"/>
                                           </g:if>
                                           <g:else>
                                               <span class="text-primary small text-uppercase"><g:message code="none" /></span>
                                           </g:else>
                                       </td>
                                       <td>

                                           <g:if test="${user.value.tokens}">
                                               <g:enc>${user.value.tokens}</g:enc>
                                           </g:if>
                                           <g:else>
                                               <span class="text-primary small text-uppercase"><g:message code="none" /></span>
                                           </g:else>
                                       </td>
                                       <td>
                                           ${user.value.loggedStatus}
                                       </td>
                                   </tr>
                                   </g:each>
                               </table>
                           </div>

                       </div>

                   </div>
               </div>
           </div>
       </div>
   </div>
</div>
<script type="text/javascript">
function togleLogged() {
    jQuery.ajax({
        url: "${f.createLink(controller:'menu',action:'userSummary')}",
        data: {selectedEncrypter:jQuery("#encrypter").val(), "test":'A'},
        type: 'get',
        success : function (data) { alert(data) }
    })
}
</script>
</body>
</html>