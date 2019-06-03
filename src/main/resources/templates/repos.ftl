<html>
<body>
Repositories:
<ul>
    <#list session.repositories as repo, permission>
        <li><a href="/repos/github/${repo}">${repo}</a></li>
    </#list>
</ul>
</body>
</html>