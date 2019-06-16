<html>
<body>
Commits:
<ul>
    <#list commits as c>
        <li>
            ${c.commitId} ${c.message}
            <#if c.report??>
                <a href="/repos/${repo.scm}/${repo.name}/commits/${c.commitId}/files/">${c.report.coveredStatements} / ${c.report.missedStatements}</a>
            <#else>
                No Report
            </#if>
        </li>
    </#list>
</ul>
<#if isAdmin>
    <p>Token for upload: <tt>${repo.uploadToken}</tt></p>
    <p>Command for upload (Kotlin): <tt>curl -f -F token=${repo.uploadToken} -F commit=$(git rev-parse HEAD) -F report=@build/reports/jacoco/test/jacocoTestReport.xml ${uploadUrl}</tt></p>
</#if>
</body>
</html>