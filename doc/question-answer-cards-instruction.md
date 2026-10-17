# Instructions for AI agents: generating question-answer cards

You will be given a source text, such as an article or part of a book.
Your job is to turn it into a set of question-answer flashcards and save them as a single JSON file.
The user will import that file into a spaced-repetition app
and review the cards over time instead of re-reading the original text.

Read this whole document before generating anything.

## Goal

The cards must let the user recall the important content of the source without opening it again.
After reviewing all cards, the user should remember the key ideas, facts, definitions, arguments,
causes and effects, and conclusions of the source.
Cover the material completely, but leave out filler, anecdotes, and repetition
unless they carry something worth remembering.

## Output format

Produce one JSON document encoded in UTF-8.
Output only the JSON, with no surrounding comments or Markdown code fences, unless you are explicitly asked otherwise.
Do not add any fields that are not described below.

### Top level

```json
{
  "generatedBy": "claude-opus-5-5",
  "cards": { ...root chapter... }
}
```

| Field         | Type   | Required | Meaning                                                                                   |
|---------------|--------|----------|-------------------------------------------------------------------------------------------|
| `generatedBy` | string | yes      | Who generated the cards, for example your model name. It is stored as an attribute on every imported card. |
| `cards`       | object | yes      | The root chapter. There is exactly one root chapter.                                     |

### Chapter

A chapter groups cards and other chapters.
Each chapter becomes a directory on disk, and nested chapters become nested directories.

```json
{
  "chapterName": "Some name",
  "cards": [ ...cards... ],
  "chapters": [ ...sub-chapters... ]
}
```

| Field         | Type             | Required | Meaning                                  |
|---------------|------------------|----------|------------------------------------------|
| `chapterName` | string           | yes      | Name of the chapter and of its directory. |
| `cards`       | array of cards   | no       | Cards that belong directly to this chapter. |
| `chapters`    | array of chapters| no       | Sub-chapters.                            |

Rules for `chapterName`:

- Use only English letters, digits, spaces, `-`, `_` and `.`.
  Every other character is silently removed when the directory name is made,
  so `"Chapter 3: Memory & Sleep"` becomes the directory `Chapter 3 Memory  Sleep`.
- After that removal the name must not be blank, `.` or `..`, otherwise the import fails.
  So a name written entirely in non-Latin script (for example Cyrillic) is invalid.
- Sibling chapters should have different names, otherwise their cards end up in the same directory.
- Prefix every chapter name except the root with an order number,
  even if chapters or sections are not numbered in the source,
  for example `01 Introduction`, `02 Working memory`.
  The app sorts chapters by name, so the number prefixes keep them in the source's order.
- Keep names short.

The root chapter's name should identify the source, for example `Kahneman - Thinking Fast and Slow - Part 1`.
The root chapter doesn't need a number prefix.
The user can rename the root chapter in the import dialog, so this is only a suggestion.

Do not put `null` into the `chapters` or `cards` arrays.

### Question-answer card

```json
{
  "type": "QUESTION_ANSWER",
  "question": "What are the two systems of thinking described by Kahneman?",
  "answer": "System 1: fast, automatic, intuitive.\nSystem 2: slow, deliberate, effortful."
}
```

| Field      | Type   | Required | Meaning                                                  |
|------------|--------|----------|----------------------------------------------------------|
| `type`     | string | yes      | Always exactly `"QUESTION_ANSWER"`.                      |
| `question` | string | yes      | The question. Must not be blank.                         |
| `answer`   | string | yes      | The answer. Must not be blank.                           |

Leading and trailing whitespace is trimmed from all text fields.

Text is shown as plain preformatted text.
Markdown, HTML and LaTeX are not rendered, so do not use them.
Line breaks (`\n`) are preserved, so use them to split longer answers into short lines or simple lists.
Long lines are not wrapped automatically, so keep each line reasonably short (about 120 characters or fewer).

## How the cards are reviewed

Knowing this helps you write good cards.

- Every card is reviewed in one direction:
  the user sees the question and must recall the answer.
  Write each question so that it has exactly one correct answer.
- The user types or thinks of a response and then compares it with the stored text themselves.
  Matching is not exact, so the wording doesn't have to be reproducible letter by letter,
  but the content of the answer must be unambiguous.

## How to write good cards

- **One idea per card.**
  Split a complex idea into several cards instead of writing one card with a long answer.
- **Short answers.**
  A good answer is a word, a phrase, or one or two sentences.
  If an answer is a list, say in the question how many items are expected,
  for example "What are the three stages of memory formation?".
- **Specific questions.**
  Avoid vague questions like "What is important about X?".
  Ask "What does X cause?", "Why does X happen?", "How does X differ from Y?", "What is the definition of X?".
- **Test understanding, not only facts.**
  Besides definitions and facts, include cards about causes, consequences, mechanisms, comparisons, examples,
  and the main arguments and conclusions of the source.
- **Stay faithful to the source.**
  Do not add facts that are not in the source.
  Do not add clarifications of your own.
- **No duplicates.**
  Do not create several cards that ask the same thing in different words.
- **Language.**
  Write the cards in the language of the source unless the user asks for another one.
  Chapter names must still follow the character rules above.

### Examples

Bad: the answer is long and contains several ideas.

```json
{"type": "QUESTION_ANSWER", "question": "What is the spacing effect?",
 "answer": "The spacing effect is when learning is spread out over time, which was discovered by Ebbinghaus in 1885, and it works because of retrieval effort and consolidation during sleep, and it is used by flashcard apps."}
```

Good: the same content split into focused cards.

```json
[
  {"type": "QUESTION_ANSWER",
   "question": "What is the spacing effect in learning?",
   "answer": "Information is remembered better when study sessions are spread out over time rather than massed together."},
  {"type": "QUESTION_ANSWER",
   "question": "Who first described the spacing effect, and when?",
   "answer": "Hermann Ebbinghaus, 1885."},
  {"type": "QUESTION_ANSWER",
   "question": "Name two mechanisms that explain why spaced repetition works.",
   "answer": "1. Greater retrieval effort on each repetition.\n2. Memory consolidation between sessions, e.g. during sleep."}
]
```

## Structuring chapters

- Mirror the structure of the source: one chapter per chapter or major section of the source,
  with sub-chapters for subsections if the source is long.
- Do not nest deeper than needed.
  For a single article, a root chapter with cards directly in it, or with a few sub-chapters, is usually enough.
- Keep cards inside a chapter in the order the material appears in the source.

## Amount of cards

Let the density of the source decide.
As a rough guide, expect about 3 to 10 cards per page of dense non-fiction text, and fewer for light text.
Prefer completeness of important content over brevity, but never pad with trivial cards.

## Complete example

```json
{
  "generatedBy": "claude-opus-5-5",
  "cards": {
    "chapterName": "Make It Stick - Ch 1-2",
    "chapters": [
      {
        "chapterName": "01 Learning is misunderstood",
        "cards": [
          {
            "type": "QUESTION_ANSWER",
            "question": "Why is re-reading a text an ineffective study strategy, according to 'Make It Stick'?",
            "answer": "It creates familiarity with the text, which is mistaken for mastery, but it does not build durable memory."
          },
          {
            "type": "QUESTION_ANSWER",
            "question": "What is the 'illusion of knowing'?",
            "answer": "Mistaking fluency with a text (it feels familiar) for actually knowing the material."
          }
        ]
      },
      {
        "chapterName": "02 To learn retrieve",
        "cards": [
          {
            "type": "QUESTION_ANSWER",
            "question": "What is the testing effect (retrieval practice effect)?",
            "answer": "Retrieving information from memory strengthens that memory more than re-studying it."
          }
        ]
      }
    ]
  }
}
```

## Final checklist

Before returning the result, verify that:

- The output is valid JSON (no trailing commas, no comments, all strings properly escaped, line breaks written as `\n`).
- The top-level object has `cards` with one root chapter, and `generatedBy`.
- Every chapter has a `chapterName` that contains at least one English letter or digit.
- Every chapter except the root has a `chapterName` prefixed with an order number.
- Every card has `"type": "QUESTION_ANSWER"` and non-blank `question` and `answer`.
- No field other than those described in this document is used.
- The important content of the source is covered.
