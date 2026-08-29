# תוכנית סופית — מאימות ועד הגשה

**היום:** 29.8 · **דדליין:** 3.9 · **נשארו 5 ימים**

מצב פתיחה: 14 קומיטים, main מסונכרן עם origin, הפייפליין רץ מקצה לקצה,
7 suites / 56 טסטים ירוקים.

---

# שלב A — אימות ב-VS Code

**למה:** זה הסביבה שבה כל העבודה נעשתה. אנחנו מוודאים שהמצב הנקי (בלי קאש,
בלי `target/`) עדיין עובד — כי זה מה שהבודק יקבל.

**מי:** קלוד קוד. שלח לו:

```
Full clean-state verification. Report only — change nothing, commit nothing.

1. Confirm the environment is inherited correctly: print HADOOP_HOME,
   confirm winutils.exe resolves on PATH, print the java version.

2. Delete target/, project/target/, output/ and data/ so nothing is
   reused from a previous run. Do NOT touch anything tracked by git
   other than data/, which the pipeline regenerates.

3. Run `clean compile test` and report: warnings, suite count, test
   count, pass/fail.

4. Run `run` and report: whether data/ was regenerated, whether all six
   directories appear under output/, the SOURCE FILE QUALITY numbers,
   and the row count of output/rejected_lines/part-*.csv.

5. Determinism check: record sha256 of data/transactions.csv, delete
   data/, run again, record sha256 again. The two must be identical.

6. Confirm `git status` shows data/ unmodified after the regeneration —
   if the regenerated file differs from the committed one, that is a
   determinism bug and I need to know.

Report all six results in one message, then stop.
```

**קריטריון מעבר:**
- אפס warnings, 7 suites, 56 טסטים עוברים
- שש תיקיות ב-`output/`, 20 שורות ב-`rejected_lines`
- שני ה-sha256 זהים
- `git status` נקי אחרי הרגנרציה של `data/`

> ⚠️ סעיף 6 הוא החשוב. אם הדאטהסט שנוצר מחדש **שונה** מזה שמגובה ב-git,
> ה-seed לא באמת דטרמיניסטי — וזו טענה מרכזית במצגת שלכם.

---

# שלב B — אימות באינטליג'יי

**למה:** ה-README מורה לבודק לפתוח באינטליג'יי. אם זה לא עובד שם — לא משנה
ש-sbt עובד. חוץ מזה, ה-SDK אצלך מוגדר כרגע ל-JDK 22 בזמן שהקורס דורש 11.

**מי:** אתה, ידנית. אין דרך לעשות את זה מהטרמינל.

1. **סגור את אינטליג'יי לגמרי.** לא רק את החלון — תהליך שרץ מלפני שהוגדר
   `HADOOP_HOME` יירש סביבה ישנה והכתיבה תיפול, בדיוק כמו שקרה קודם
2. פתח מחדש, טען את הפרויקט
3. `File → Project Structure → Project` — הגדר **SDK: 11**
   (מותקן אצלך ב-`C:\Users\ori\.jdks\ms-11.0.32`), ו-Language level: 11
4. `Settings → Build Tools → sbt → JVM` — השאר על ברירת המחדל
5. `File → Invalidate Caches → Invalidate and Restart`
6. חכה שה-import של sbt יסתיים (יכול לקחת דקות)
7. הרץ `com.hit.fp.Main` מתוך ה-IDE
8. הרץ את כל הטסטים: קליק ימני על `src/test/scala` → Run All Tests

**קריטריון מעבר:**
- `Main` רץ עד הסוף ומדפיס את שבעת מקטעי הסיכום
- שש התיקיות נוצרות ב-`output/`
- 56 טסטים ירוקים בחלון הטסטים
- אין קו אדום בעורך

**אם `Main` נופל על winutils:** לא סגרת את אינטליג'יי לגמרי. סגור, ודא
ב-Task Manager שאין תהליך `idea64.exe`, פתח שוב.

---

# שלב C — גיטהאב

**למה:** ההיסטוריה בגיטהאב היא הראיה לעבודת הצוות — 10% מהציון.

**החלטה שצריך לקבל לפני:** האם `submission-prep/` נכנס לרפו?

- **לא (מומלץ):** אלה חומרי עבודה שלנו — פרומפטים, מדריך, תוכניות. הם לא
  חלק מהפרויקט, והבודק שיסתכל ברפו יתבלבל. הוסף `submission-prep/`
  ל-`.gitignore`
- כן: אם אתם רוצים שהם יתועדו כחלק מהעבודה השיתופית

**מי:** קלוד קוד. שלח לו:

```
Repository hygiene before submission. One commit.

1. Add submission-prep/ to .gitignore — it holds working material, not
   project files.

2. The repository root contains three PDFs that are course material,
   not project deliverables:
   "common_rejects_functional_programming - Google Drive.pdf",
   "functional_programming_202607 - Google Docs.pdf",
   "functional_programming_principles - Google Drawings.pdf"
   Report whether they are tracked by git. Do NOT delete them —
   recommend whether they belong in the repository and wait for my call.

3. Commit the .gitignore change as
   `chore(repo): ignore the submission working folder`
   and push to origin/main.

4. Report the final state: commit count, whether main is in sync with
   origin, and the output of `git status`.
```

**וידוא ידני אחריך:** פתח את `github.com/danelbaz2/functional-programming`
בדפדפן וּודא שאתה רואה את כל הקומיטים ואת ה-README מוצג יפה.

---

# שלב D — הסרטון

**החוסם היחיד שנשאר.** התסריט המלא ב-`04-video-script.md`.

1. **סגור הכול, פתח טרמינל חדש** (אחרת winutils)
2. הרץ פעם אחת בשקט כדי שהתלויות בקאש — אחרת ההורדה תבלע 40 שניות מהסרטון
3. `sbt clean` ואז מחק `data/` ו-`output/` — שהסרטון יראה יצירה מאפס
4. הגדל גופן טרמינל ל-16-18pt, סגור התראות
5. חלוקה עם דן: אחד מריץ ומצלם, השני מדבר
6. הקלט לפי התסריט, ~60 שניות
7. העלה ליוטיוב, **Visibility = Unlisted** (לא Private!)
8. בדוק בגלישה פרטית שהלינק נפתח

**חייב להיראות בסרטון:**
מבנה החבילות · ריצה מוצלחת · פלט הקונסולה · **`rejected_lines` עם השורות
הפגומות** · קבצי הפלט על הדיסק

---

# שלב E — הרכבת ה-PDF

לפי `03-pdf-skeleton.md`. שישה מקטעים:

| # | מה | חסר |
|---|---|---|
| 1 | פרטי הסטודנטים | 2 טלפונים + אימות המייל של דן |
| 2 | לינק לסרטון | אחרי שלב D |
| 3 | תיאור טכני / Mapping | ✅ מנוסח |
| 4 | פסקת כלים שיתופיים | חלוקת העבודה בינך לדן |
| 5 | שקפי המצגת | ✅ ייצא את ה-pptx ל-PDF |
| 6 | קוד המקור | להרכיב — סדר מומלץ בשלד |

**לקוד המקור:** גופן חד-רוחבי (Consolas/Courier New) 8-9pt, שוליים צרים,
ואם צריך — לרוחב. **מיושר לשמאל, ללא שורות שבורות.** זו דרישה מפורשת.

---

# שלב F — אריזה והגשה

1. `sbt clean` — מוחק `target/` (3.2MB) ו-`project/target/`
2. ודא ש-`submission-prep/` לא נכנס לארכיון
3. החלט מה עושים עם שלושת ה-PDF-ים של הקורס בשורש
4. `File → Export → Project to Zip File` מאינטליג'יי
5. שם: `ori_kochavi.zip` — **אותיות קטנות, קו תחתון, ללא רווחים**
   *(אם דן הוא מנהל הצוות: `dan_elbaz.zip`)*
6. ה-PDF באותה תבנית בדיוק: `ori_kochavi.pdf`
7. **פתח את ה-zip ובדוק** שהקוד באמת שם ושאין `target/`
8. **פתח את ה-PDF ולחץ על לינק הסרטון** — ודא שהוא לחיץ ונפתח
9. **מנהל הצוות בלבד** מעלה למודל
10. **לפחות 30 דקות לפני הזמן הנקוב** — פערי שעון במודל
11. רענן את דף ההגשה וּודא בעיניים ששני הקבצים עלו

---

# לוח זמנים

| יום | מה |
|---|---|
| **29.8** (היום) | שלבים A + B + C |
| **30.8** | תיקון אחרון במצגת + חזרה על ההצגה עם טיימר |
| **31.8** | שלב D — הסרטון |
| **1.9** | שלב E — ה-PDF |
| **2.9** | שלב F — אריזה, בדיקה סופית |
| **3.9 בוקר** | הגשה |

יום 2.9 הוא רזרבה מכוונת. אל תתכנן להשתמש בו.
