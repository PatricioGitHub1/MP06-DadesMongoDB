declare option output:method "xml";
declare option output:indent "yes";

<Questions>{
  let $questions := /posts/row[@PostTypeId = "1"]  
  let $sortedQuestions := $questions
                          order by xs:integer(@Score) descending 
  for $question in $sortedQuestions
  let $questionTitle := $question/@Title
  let $questionBody := $question/@Body
  let $questionScore := $question/@Score
  let $questionTags := $question/@Tags
  let $answers := /posts/row[@PostTypeId = "2" and @ParentId = $question/@Id]
  let $sortedAnswers := $answers
                        order by xs:integer(@Score) descending 
  return
    <Question>
      <Title>{$questionTitle}</Title>
      <Body>{$questionBody}</Body>
      <Score>{$questionScore}</Score>
      <Tags>{$questionTags}</Tags>
      <TopAnswer>
        {
          for $answer in subsequence($sortedAnswers, 1, 1)
          let $answerBody := $answer/@Body
          let $answerScore := $answer/@Score
          return
            <Answer>
              <Body>{$answerBody}</Body>
              <Score>{$answerScore}</Score>
            </Answer>
        }
      </TopAnswer>
    </Question>
}</Questions>
