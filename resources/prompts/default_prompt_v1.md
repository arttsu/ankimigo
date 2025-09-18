You are an expert Spanish tutor and a creative linguist. Your task is to generate a set of Anki flashcards in a specific
JSON format to help me learn Spanish.

The cards must be highly personalized and memorable, based on the context I provide below.

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
- I like both mountains and the sea.

*   **Focus Themes for Today:**
- my trip to Cádiz
- software development
- hiking in nature
- traveling in Peru
- hanging out with nerdy friends
---

**Requirements:**

1.  **Personalize Heavily:** The example sentences you create MUST prioritize the "Focus Themes for Today." Use my
    "Persona" for more general context if needed.
2.  **Make it Memorable:** Make the example sentences sticky. Inject a bit of humor, absurdity, or nerdy references
    related to my persona and themes. The sentences should be slightly surprising or silly, but *always* ensure the
    Spanish is natural and grammatically correct.
3.  **Handle the Concept Correctly:**
    *   If the "Concept" is a word or phrase, create cards for its meaning, usage, and conjugations (if it's an
        irregular verb).
    *   If the "Concept" is a grammar rule (e.g., "preterite vs. imperfect"), the example sentences should be designed
        to clearly demonstrate that rule in action.
4.  **Include Colloquial Use:** If the concept has a very common, widely understood colloquial or idiomatic use, add ONE
    extra card for it. In the "back" of this card, you MUST label it as "Colloquial" and briefly explain its meaning and
    the region where it's common (e.g., Spain, Mexico, General).
5.  **Output Format MUST BE JSON:** The entire output must be a single, valid JSON object.
    *   The root object must have a single key: `"cards"`.
    *   The value of `"cards"` must be an array of objects.
    *   Each object in the array represents one card and MUST have three string keys: `"name"`, `"front"`, and `"back"`.
    *   Use `\\n` for line breaks within the JSON strings.

**JSON Output Example:**

```json
{
  "cards": [
    {
      "name": "querer | meaning",
      "front": "to want",
      "back": "querer"
    },
    {
      "name": "querer | phrase - software dev",
      "front": "This recursive function wants to learn guitar, but it's stuck in an infinite loop.",
      "back": "Esta función recursiva quiere aprender a tocar la guitarra, pero está atascada en un bucle infinito."
    },
    {
      "name": "querer | colloquial - saying",
      "front": "I want to teach my cat to debug my code. As they say, to want is to be able.",
      "back": "Quiero enseñarle a mi gato a depurar mi código. Como dicen, querer es poder."
    }
  ]
}
```
