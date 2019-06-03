<html>
<body>
Commits:
<ul>
    <#list commits as c>
        <li><a href="/repos/github/${repoName}/commits/${c.commitId}">${c.message}</a></li>
    </#list>
</ul>
</body>
</html>