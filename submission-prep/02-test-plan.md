# תוכנית בדיקה מלאה — לפני הגשה

**דדליין:** 3 בספטמבר. להגיש **לפחות 30 דקות** לפני השעה הנקובה (פערי שעון במודל).
**קנס איחור:** 2 נקודות לכל שעה עגולה.

סמן `[x]` כשסיימת. כל סבב עומד בפני עצמו — אפשר להריץ בסדר הזה בדיוק.

---

## מטריצת דרישות — סטטוס נוכחי

| # | דרישה | סטטוס | ראיה |
|---|---|---|---|
| 1 | ≥10,000 רשומות | ✅ | 15,000 טרנזקציות + 200 מוצרים |
| 2 | פורמט CSV / JSON | ✅ | שני קבצי CSV ב-`data/` |
| 3 | פלט לקובץ מקומי | ✅ | 6 דוחות ל-`output/` (`Main.scala:92-97`) |
| 4 | הפרדת Pure ↔ I/O | ✅ | `core/` מול `io/`+`spark/` |
| 5 | Scala 2.12.19 | ✅ | `build.sbt:20,26` |
| 6 | JDK 11 | ⚠️ | לא נעול ב-`scalacOptions` — ראה P6 |
| 7 | IntelliJ / SBT | ✅ | `.idea/` + `build.sbt` |
| 8 | RDD / DataFrame / Dataset | ✅ | Dataset בכל ה-jobs + RDD ב-`revenueByCountry` |
| 9 | ≥4 פעולות Spark | ✅ **6** | joinWith, map, filter, groupByKey, reduceByKey, flatMap |
| 10 | פונקציות טהורות | ✅ | כל `core/` |
| 11 | Immutability / ללא var | ✅ | **0** מופעי `var` ב-`src/main` |
| 12 | Higher-Order Functions | ✅ | `topBy`, `Transform.lift`, `chain` |
| 13 | Currying | ✅ | `atLeastRevenue`, `shippedTo`, `topBy`, `nonEmptyField`, `positiveNumber` |
| 14 | Function Composition | ✅ | `~>`, `zip`, `filterK` ב-`Transform` |
| 15 | טכניקה 1 — Custom Combinator | ✅ | `Transform.scala:7` |
| 16 | טכניקה 2 — Functional Error Handling | ✅ | `ParseError.scala:7` |
| 17 | טכניקה 3 — Tail-recursion | ✅ | `PureAnalytics.scala:31` (5 × `@tailrec`) |
| 18 | טכניקה 4 — Pattern Matching + ADT | ✅ | `PureAnalytics.scala:32`, `OrderSize.scala` |
| 19 | טכניקה 5 — Closures ב-Spark | ✅ | `TransactionJobs.scala:38` |
| 20 | ScalaTest | ✅ | 6 suites |
| 21 | ScalaDoc אנגלית | ⚠️ | פערים ב-8 קבצים — ראה P3 |
| 22 | Traits ולא Interfaces | ✅ | `ParseError`, `Transform`, `OrderSize` |
| 23 | קובץ PDF | ❌ | לא קיים |
| 24 | סרטון YouTube | ❌ | לא קיים |
| 25 | שקפים | ⚠️ | `PRESENTATION.md` מוכן, לא הומר לשקפים |
| 26 | עבודה בזוג | ❓ | **דורש החלטה — 10 נקודות** |
| 27 | שמות קבצים `first_last.zip/.pdf` | ❌ | טרם |

**נדרש 3 מתוך 5 טכניקות מתקדמות — יש 5 מתוך 5.** נקודת חוזק להדגיש.

---

## סבב A — בנייה נקייה

- [ ] `sbt clean compile` — לתעד כל warning (קובץ + שורה)
- [ ] `sbt test` — לתעד **מספר הטסטים** ומצב כל suite. זהו ה-baseline
- [ ] כל 6 ה-suites ירוקים: CsvParsingSpec, DataGenerationSpec, PureAnalyticsSpec, ReportFormattingSpec, TransformSpec, TransactionJobsSpec
- [ ] אין warning על deprecation שנוצר מקוד שלנו (של Spark זה בסדר)

**קריטריון מעבר:** אפס טסטים נכשלים, אפס warnings מקוד הפרויקט.

---

## סבב B — ריצה מקצה לקצה

- [ ] `rm -rf output data` (או מחיקה ידנית)
- [ ] `sbt run`
- [ ] הודעה `Dataset generated in data` מודפסת
- [ ] `data/transactions.csv` = 15,001 שורות (כולל header), `data/products.csv` = 201
- [ ] נוצרו 6 תיקיות דוח ב-`output/`: `revenue_by_category`, `revenue_by_country`, `top_customers`, `order_sizes`, `pareto_categories`, `rejected_lines`
- [ ] כל אחת מכילה קובץ `part-*.csv` לא ריק
- [ ] הודפסו 7 מקטעי קונסולה: SOURCE FILE QUALITY, REVENUE BY CATEGORY, REVENUE BY COUNTRY, TOP CUSTOMERS, ORDER SIZE DISTRIBUTION, PARETO ANALYSIS, TOTAL
- [ ] אין stack trace, אין exception

**קריטריון מעבר:** ריצה נקייה מקצה לקצה. זו בדיוק הריצה שתצולם לסרטון.

---

## סבב C — דטרמיניזם (ה-seed)

- [ ] `rm -rf data && sbt run` → לחשב hash: `sha256sum data/transactions.csv`
- [ ] שוב `rm -rf data && sbt run` → לחשב hash שוב
- [ ] **שני ה-hash זהים**

**למה זה חשוב:** ה-`Rng` הוא מחולל אקראיות פונקציונלי טהור (state passing, ללא `scala.util.Random` גלובלי) עם seed `20260816L` ב-`PipelineConfig.default`. אם ה-hash שונה — יש אי-דטרמיניזם נסתר. זו נקודה שהמרצה עשוי לשאול עליה.

---

## סבב D — עמידות לנתונים פגומים ⭐

זו **נקודת השיא** שהמרצה ביקש להדגיש. המחולל כבר משחית בכוונה 20 שורות מתוך 15,000:

| סוג השחתה | כמות | ParseError שנוצר | דוגמה |
|---|---|---|---|
| מחיר → `N/A` | 15 | `NotANumber` | שורה 978: `...,3,N/A,Italy,2024-05-15` |
| שדה חסר (6 במקום 7) | 5 | `WrongFieldCount` | שורה 2933: `...,4,81.42,USA` |
| **סה"כ נדחות** | **20** | | |
| **תקינות** | **14,980** | | |

- [ ] `output/rejected_lines/part-*.csv` מכיל **בדיוק 20 שורות** (ללא header)
- [ ] כל שורה נושאת `reason` קריא: `field unitPrice is not a number: 'N/A'` / `expected 7 fields but found 6`
- [ ] בקונסולה, SOURCE FILE QUALITY מציג 14,980 תקינות מול 20 נדחות
- [ ] **ה-pipeline לא קרס** — אף exception לא נזרק
- [ ] בדיקה ידנית: הזרקת שורה פגומה נוספת ל-`data/transactions.csv` (למשל `X,,P0001,0,abc,,`) והרצה חוזרת → מספר הנדחות עולה ב-1, אין קריסה
- [ ] לאחר הבדיקה — להחזיר את `data/` למצב המקורי (`git checkout data/`)

**קריטריון מעבר:** 20 נדחות מזוהות ומסווגות, אפס exceptions. זה מדגים את `Either` + ה-ADT `ParseError` בפעולה.

---

## סבב E — בדיקות תוכן ואיכות

- [ ] כל ארבעת סוגי ה-`ParseError` מכוסים בטסט: `NotANumber`, `WrongFieldCount`, `MissingField`, `OutOfRange`
- [ ] `git status` נקי אחרי תיקון ה-CRLF
- [ ] README מסתדר עם `project/build.properties` (גרסת sbt)
- [ ] ScalaDoc מלא בכל הצהרה פומבית
- [ ] אין `var` בשום מקום ב-`src/main`: `grep -rn "\bvar \b" src/main` → ריק
- [ ] הוחלט מה עושים עם `docs/index.html`
- [ ] תגי `ADVANCED FUNCTIONAL PROGRAMMING TECHNIQUE 1..5` שלמים ולא שונו

---

## סבב F — הגשה

- [ ] `sbt clean` — מחיקת `target/` (חוסך עשרות MB ב-zip)
- [ ] מחיקת תיקיית `submission-prep/` מהארכיון (זה חומר עבודה, לא חלק מההגשה)
- [ ] החלטה: לכלול או לא לכלול את `output/` ו-`data/` ב-zip (`data/` כן — הוא מבטיח שחזור; `output/` לא — נבנה מחדש)
- [ ] יצוא הפרויקט מ-IntelliJ ל-zip
- [ ] שם הקובץ: `first_last.zip` — **אותיות קטנות, ללא רווחים, קו תחתון**
- [ ] קובץ PDF בשם `first_last.pdf` — אותה תבנית בדיוק
- [ ] הסרטון עלה ל-YouTube במצב **Unlisted**
- [ ] הלינק ב-PDF **לחיץ** — לבדוק בפועל בפתיחת ה-PDF
- [ ] הסרטון נפתח בגלישה פרטית (אימות שהוא באמת נגיש)
- [ ] ה-PDF מכיל את כל 6 התכולות (ראה `03-pdf-skeleton.md`)
- [ ] קוד המקור ב-PDF **מיושר לשמאל, ללא שורות שבורות**
- [ ] **מנהל הצוות בלבד** מעלה למודל
- [ ] העלאה בוצעה **≥30 דקות** לפני הדדליין
- [ ] אישור העלאה במודל נצפה בעיניים

---

## לוח זמנים מוצע (7 ימים)

| יום | משימה |
|---|---|
| 27-28.8 | תיקוני קוד P2-P6 דרך קלוד קוד + סבבים A-C |
| 29.8 | סבב D + השלמת פערי טסטים |
| 30.8 | הפיכת `PRESENTATION.md` לשקפים + חזרה על ההצגה |
| 31.8 | צילום והעלאת הסרטון |
| 1.9 | הרכבת ה-PDF על 6 תכולותיו |
| 2.9 | סבבי E-F, בנייה נקייה, יצוא zip |
| **3.9** | **הגשה — לא אחרי הצהריים** |
