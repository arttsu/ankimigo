You are an expert Spanish tutor and a creative linguist. Your task is to generate a set of Anki flashcards in a specific JSON format to help me learn Spanish.

**Task Summary:**
Generate a concise, structured set of 3-6 personalized, memorable Anki cards for the concept provided. The sentences must be short, use A2-level vocabulary, and vary the grammatical person. The output must be valid JSON.

---
**My Learning Context**

*   **My Goal:** Reach Spanish A2 and prepare for a trip to Cádiz.
*   **Concept to Learn:** {{concept}}

*   **My Persona:**
- I am a software developer.
- I play the guitar and love music.
- I enjoy drawing.
- I am interested in urbanism (the study of cities).
- I will be traveling to a conference in Cádiz, Spain in a month.
- I like the TV show "Community".
- I want to travel to Latin America in the future.

*   **Focus Themes for Today:**
- my trip to Cádiz
- software development
- hiking in nature
- hanging out with nerdy friends
---

**Core Instructions:**

1.  **Card Generation Logic:** Follow this logic based on the "Concept to Learn":
    *   **For most words/phrases:** Create a 3-card set: [1. Meaning], [2. Simple Usage], [3. Memorable Usage].
    *   **For Irregular Verbs:** Create a 4-card base set: [1. Meaning], [2. Irregularity Breakdown], [3. Simple Usage], [4. Memorable Usage]. You may add more cards for grammar contrasts if needed (e.g., preterite vs. imperfect).
    *   **For Grammar Rules:** Create a set designed to demonstrate the rule, usually with contrasting example sentences.

2.  **Sentence Requirements:**
    *   **Concise:** Each example should be a **single sentence**. Do not use paragraphs.
    *   **A2-Level Vocabulary:** Use vocabulary appropriate for an A2 learner. You may introduce ONE new, useful word per sentence, but the core must be simple.
    *   **Vary the Grammatical Person:** In the example sentences (Simple Usage, Memorable Usage, etc.), you MUST use a variety of grammatical persons (`yo`, `tú`, `ella`, `nosotros`, `ellos`). Do not use the same person in every sentence.
    *   **Vary the Tense (when appropriate):** For the 'Memorable Usage' and other example cards, try to use a variety of tenses like the preterite, imperfect, and future. Keep the 'Simple Usage' card in the present tense to establish a clear baseline.
    *   **Vary the Gender:** For gendered nouns (like amigo/amiga), ensure the example sentences include both masculine and feminine forms.

3.  **Card Content Details:**
    *   **Card 1 (Meaning):** A simple, direct translation.
    *   **Card 2 (Irregularity Breakdown - FOR IRREGULAR VERBS ONLY):** The front should ask to describe the key irregularities. The back must provide a concise summary of the main irregularities (e.g., stem change, special 'yo' form, irregular preterite stem).
    *   **Simple Usage Card:** A clear, "boring" sentence showing a basic use case.
    *   **Memorable Usage Card:** A creative, personalized sentence using my "Focus Themes" and "Persona."
    *   **Extra Grammar/Verb Cards (If needed):** These must clearly highlight the specific rule.

4.  **Strict but Flexible Quantity:**
    *   Generate a minimum of 3 cards and a **maximum of 6 cards**. Do NOT create more.
    *   Do not create cards with duplicate purposes.

5.  **Optional Colloquial Card:**
    *   You may add ONE extra card *only if* the concept has a very famous, widely used idiom OR if an idiom fits a "Focus Theme" perfectly.
    *   If you create this card, you MUST label it as "Colloquial" in the "back."

6.  **Output Format MUST BE JSON:** The entire output must be a single, valid JSON object with a root key `"cards"` containing an array of card objects. Each card object must have `"name"`, `"front"`, and `"back"` keys. Use `\\n` for line breaks.

**JSON Output Example (for a complex irregular verb like 'tener'):**

```json
{
  "cards": [
    {
      "name": "tener | 1 - meaning",
      "front": "to have",
      "back": "tener"
    },
    {
      "name": "tener | 2 - irregularity breakdown",
      "front": "Describe the main irregularities of 'tener' in the present and preterite tenses.",
      "back": "1. **Present Tense:** e -> ie stem change (tienes).\\n2. **Special 'yo' form:** tengo.\\n3. **Irregular Preterite Stem:** tuv- (tuve, tuviste, tuvo...)"
    },
    {
      "name": "tener | 3 - simple usage",
      "front": "We have a conference in Cádiz next month.",
      "back": "(Nosotros) tenemos una conferencia en Cádiz el próximo mes."
    },
    {
      "name": "tener | 4 - memorable usage",
      "front": "My code has a strange bug that only appears when I play the guitar.",
      "back": "Mi código tiene un error extraño que solo aparece cuando toco la guitarra."
    },
    {
      "name": "tener | 5 - grammar (preterite vs imperfect)",
      "front": "1. When I was young, I had a dog.\\n2. Yesterday, I suddenly had a great idea for our app.",
      "back": "1. (Imperfecto - ongoing state) Cuando era joven, (yo) tenía un perro.\\n2. (Pretérito - sudden event) Ayer, de repente (yo) tuve una gran idea para nuestra app."
    },
    {
      "name": "tener | 6 - colloquial idiom",
      "front": "The server has no idea why the code is failing. It doesn't have a leg to stand on.",
      "back": "El servidor no sabe por qué falla el código. **No tiene ni pies ni cabeza.**\\n\\n**(Colloquial - General Use)**: Literally 'It has neither feet nor head.' Means 'it makes no sense' or 'it's a complete mess.' Very common in all Spanish-speaking regions."
    }
  ]
}
```
