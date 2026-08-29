# שלד קובץ ה-PDF להגשה

**שם הקובץ:** `first_last.pdf` — שם פרטי ומשפחה של **מנהל הצוות**, אותיות קטנות, קו תחתון, ללא רווחים.

מלא את השדות המסומנים `<< >>`. שמור על הסדר — הבודק סורק לפי המבנה הזה.

---

## 1. פרטי הסטודנטים

> מנהל הצוות ראשון.

**מנהל הצוות**
- שם מלא: Ori Kochavi
- ת"ז: 305345837
- אימייל: oriko9@gmail.com
- טלפון: << >>

**שותף**
- שם מלא: Dan Elbaz
- ת"ז: 209712389
- אימייל: danelbaz3@gmail.co  ⚠️ **לאמת — נראה שחסרה ה-m בסוף**
- טלפון: << >>

---

## 2. קישור לסרטון ההדגמה

**YouTube (Unlisted):** << הדבק לינק לחיץ >>

אורך: כ-60 שניות. מציג ריצה מלאה של הפייפליין בליווי הסבר קולי.

> ⚠️ ודא שהלינק **לחיץ** ב-PDF הסופי, לא טקסט. בדוק בפועל.

---

## 3. תיאור טכני — Mapping

> פסקה קצרה: אילו פעולות Spark יושמו, היכן ממוקמות הטכניקות הפונקציונליות, ובאילו מחלקות/פונקציות.

**נוסח מוצע — ערוך לפי טעמך:**

הפייפליין בנוי כליבה פונקציונלית טהורה עטופה במעטפת אימפרטיבית. חבילת `com.hit.fp.core` אינה מבצעת קלט/פלט כלל, וכל הלוגיקה העסקית נבדקת ב-ScalaTest ללא הפעלת Spark.

**פעולות Spark — שש, כאשר ארבע נדרשות:**

| פעולה | מיקום | תפקיד |
|---|---|---|
| `joinWith` | `TransactionJobs.enrich` | חיבור טיפוסי בין הטרנזקציות לקטלוג המוצרים |
| `map` | `TransactionJobs.enrich`, `orderSizeDistribution` | החלת פונקציות טהורות על כל רשומה |
| `filter` | `TransactionJobs.keepSignificant` | סינון לפי סף הכנסה |
| `groupByKey` | `revenueByCategory`, `spendingByCustomer`, `orderSizeDistribution` | קיבוץ לפני אגרגציה |
| `reduceByKey` | `TransactionJobs.revenueByCountry` | אגרגציה על **RDD API** עם פונקציה אסוציאטיבית |
| `flatMap` | `io.DataSource` | פענוח שורות תוך הפרדת תקינות מנדחות |

**חמש הטכניקות הפונקציונליות המתקדמות — נדרשות שלוש, מומשו חמש.** כל אחת מסומנת בקוד בהערה `ADVANCED FUNCTIONAL PROGRAMMING TECHNIQUE N`:

| # | טכניקה | מחלקה / פונקציה |
|---|---|---|
| 1 | **Custom Combinator** | `core.Transform` — טיפוס פונקציה עצמאי עם שלושה קומבינטורים: `~>` (שרשור), `zip` (הרצה מקבילה), `filterK` (סינון ל-`Option`). נבנה מאפס, **לא** מבוסס על `Function1.andThen` |
| 2 | **Functional Error Handling** | `core.ParseError` — `sealed trait` עם ארבעה מקרים (`WrongFieldCount`, `NotANumber`, `OutOfRange`, `MissingField`). `CsvParsing.parseTransaction` מחזיר `Either[ParseError, Transaction]` דרך for-comprehension שמקצרת בראשון שנכשל. אפס exceptions |
| 3 | **Tail Recursion** | `PureAnalytics.totalRevenue`, `PureAnalytics.paretoOf`, ושלוש פונקציות ב-`DataGeneration` — כולן עם `@tailrec` |
| 4 | **Pattern Matching + ADT** | `PureAnalytics.classifyOrder` — התאמה עם guards המייצרת `case object` מתוך `sealed trait OrderSize`. `describeSize` ממצה על כל המקרים, כך שהקומפיילר יתריע אם ייווסף סיווג חדש |
| 5 | **Closures ב-Spark** | `TransactionJobs.keepSignificant` — הפעלה חלקית של `PureAnalytics.atLeastRevenue` בונה closure שסוגר על סף שחי ב-driver; Spark מסדרל אותו ושולח לכל executor |

**נוסף על כך:** Currying (`atLeastRevenue`, `shippedTo`, `topBy`, ושני ה-validators ב-`CsvParsing`), Higher-Order Functions (`topBy` גנרי עם פונקציית ניקוד), ומחולל אקראיות פונקציונלי טהור (`core.Rng`, state passing) המבטיח שאותו seed מייצר את אותו דאטהסט בדיוק בכל מכונה.

**Immutability:** אפס מופעים של `var` בכל `src/main`. כל מבני הנתונים הם `case class`, `List`, `Vector`, `Map`.

---

## 4. סיכום כלים שיתופיים

> עד 100 מילים.

**נוסח מוצע — ערוך לפי מה שבאמת עשיתם:**

עבדנו על מאגר GitHub משותף בזרימת ענפים: כל תיקון בענף נפרד, קומיט אחד לכל שינוי לוגי, ויומן שינויים ב-`docs/BUGFIX_LOG.md` המתעד סימפטום, סיבה שורשית, תיקון ואימות. השתמשנו ב-Claude כ-Pair Programmer בתוך VS Code — לא לכתיבת קוד עיוורת, אלא לסקירת קוד מול דרישות הקורס, לאיתור פערי תיעוד וטסטים, ולאימות עמידות הפייפליין בנתונים פגומים. כל הצעה של ה-AI נבדקה מול `sbt test` לפני מיזוג. חלוקת העבודה: << מי עשה מה >>.

*(ספירת מילים: ~85)*

---

## 5. שקפי המצגת

> העתק של השקפים שיוצגו בהרצאת הסיום.

מקור התוכן: `PRESENTATION.md` בשורש הפרויקט — חמישה שקפים מוכנים:

1. **E-Commerce Analytics, the Functional Way** — הבעיה והדאטהסט
2. **Architecture: Functional Core, Imperative Shell** — הפרדת core מ-io/spark
3. **Functional Error Handling: Errors Are Data** — `Either` + ה-ADT `ParseError`
4. **Advanced FP Techniques** — חמש הטכניקות ומיקומן
5. **Business Results & Conclusion** — התובנות מהדוחות

המצגת בנויה: `submission-prep/functional_programming_presentation.pptx` — ייצא ל-PDF או הדבק את השקפים כתמונות.

---

## 6. קוד המקור

> ⚠️ **מיושר לשמאל, ללא שורות שבורות.** אל תדביק ישירות מ-IntelliJ עם עימוד — השתמש בגופן חד-רוחבי (Consolas / Courier New) בגודל 8-9pt, שוליים צרים, ואם צריך — לרוחב (landscape).

סדר מומלץ — מהליבה החוצה, כך שהבודק רואה קודם את הטהור:

1. `core/Transform.scala` — הקומבינטור המותאם (טכניקה 1)
2. `core/ParseError.scala` — ה-ADT של השגיאות (טכניקה 2)
3. `core/CsvParsing.scala` — פענוח דרך `Either` (טכניקה 2 בפעולה)
4. `core/PureAnalytics.scala` — הליבה האנליטית (טכניקות 3 ו-4)
5. `core/Rng.scala` — מחולל אקראיות טהור
6. `model/OrderSize.scala` — ה-ADT של הסיווג
7. `spark/TransactionJobs.scala` — כל פעולות Spark (טכניקה 5)
8. `io/DataSource.scala` — הפרדת תקינות מנדחות
9. `Main.scala` — המעטפת האימפרטיבית
10. דוגמה מייצגת מהטסטים: `core/CsvParsingSpec.scala`

> `DataGeneration.scala` (373 שורות) ארוך — הדבק ממנו רק את `corrupt` ואת הפונקציות עם `@tailrec`, וציין שהשאר בקוד המצורף.

---

## צ'קליסט לפני ייצוא ה-PDF

- [ ] שם הקובץ בתבנית `first_last.pdf`
- [ ] מנהל הצוות מופיע ראשון
- [ ] לינק הסרטון לחיץ ונבדק
- [ ] כל 6 המקטעים נוכחים
- [ ] הקוד מיושר לשמאל, אין שורות שנשברו באמצע
- [ ] אין טקסט מציין-מקום `<< >>` שנשכח
