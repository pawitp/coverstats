<html>
<body>
<ol>
    <#list lines as l>
    <li>
        <pre style="color:${l.color}">${l.content}</pre>
    </li>
</#list>
</oll>
</body>
</html>