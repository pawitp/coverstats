<html>
<body>
Directory: ${report.path} (${report.coveredStatements} / ${report.missedStatements})

<h2>Subdirectories</h2>
<ul>
<#list children as c>
    <li><a href="${baseUrl}${c.path}">${c.path?remove_beginning(report.path)?remove_beginning("/")}</a> (${c.coveredStatements} / ${c.missedStatements})</li>
</#list>
</ul>
</body>
</html>