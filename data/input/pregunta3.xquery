declare option output:method "xml";
declare option output:indent "yes";

<tags>{
  let $tags := /tags/row
  for $t in $tags
  order by xs:integer($t/@Count) descending
  return <row TagName="{$t/@TagName}" TagCount="{$t/@Count}"/>
}</tags>