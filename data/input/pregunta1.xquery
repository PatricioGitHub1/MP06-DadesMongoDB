declare option output:method "xml";
declare option output:indent "yes";

<posts>{
  for $p in /posts/row[@PostTypeId = "1"]
  let $views := xs:integer($p/@ViewCount)
  order by $views descending
  return <row Name="{data($p/@Title)}" Views="{$views}"/>
}</posts>

