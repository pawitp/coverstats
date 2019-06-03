<html>
<body>
Commits:
<ul>
    <#list commits as c>
        <li><a href="/repos/github/${repo.name}/commits/${c.commitId}">${c.message}</a></li>
    </#list>
</ul>
<#if isAdmin>
    <p>Token for upload: <tt>${repo.token}</tt></p>
</#if>
</body>
</html>