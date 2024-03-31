declare option output:method "xml";
declare option output:indent "yes";

<posts>{
  let $topTags := (
    for $tag in /tags/row
    order by xs:integer($tag/@TagCount) descending
    return $tag/@TagName
  )[position() <= 10]

  for $post in /posts/row[@PostTypeId = "1"]
  where some $tag in tokenize($post/@Tags, '&lt;|&gt;') satisfies $tag = $topTags
  order by xs:integer($post/@ViewCount) descending
  return $post[position() <= 100]
}</posts>