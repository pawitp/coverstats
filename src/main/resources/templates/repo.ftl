<html>
<body>
Commits:
<ul>
    <#list commits as c>
        <li>
            <a href="/repos/github/${repo.name}/commits/${c.commitId}">${c.commitId} ${c.message}</a>
            <#if c.report??>
                ${c.report.coveredStatements} / ${c.report.missedStatements}
            <#else>
                No Report
            </#if>
        </li>
    </#list>
</ul>
<#if isAdmin>
    <p>Token for upload: <tt>${repo.uploadToken}</tt></p>
    <p>Command for upload (Kotlin): <tt>curl -F token=${repo.uploadToken} -F commit=$(git rev-parse HEAD) -F report=@build/reports/jacoco/test/jacocoTestReport.xml ${uploadUrl}</tt></p>
</#if>
</body>
</html>