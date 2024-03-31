declare option output:method "xml";
declare option output:indent "yes";

<users>{
  for $user in /posts/row[@PostTypeId = "1"]
  group by $userId := $user/@OwnerUserId 
  let $userName := /users/row[@Id = $userId]/@DisplayName 
  let $questionCount := count($user)         
  order by $questionCount descending           
  return <User Name="{$userName}" Questions="{data($questionCount)}"/> 

}</users>
