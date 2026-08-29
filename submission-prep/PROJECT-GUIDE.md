# מדריך מקיף — פרויקט הגמר בתכנות פונקציונלי

**מטרת המדריך:** שתבין את הפרויקט לעומק, תוכל להסביר כל שורה בו, ותוכל להגן עליו בעל פה מול המרצה.

**איך לקרוא:** פרקים 1-3 הם התמונה הגדולה — קרא ברצף. פרק 4 הוא צלילה למודולים. פרק 5 מסביר את מושגי ה-FP. פרק 6 הוא הכנה להצגה. פרק 7 — שאלות שאתה עלול להישאל ותשובות מוכנות.

---

# פרק 1 — מה הפרויקט עושה, במשפט אחד

> הפרויקט מייצר דאטהסט סינתטי של 15,000 טרנזקציות מסחר אלקטרוני ו-200 מוצרים, קורא אותו מהדיסק, מחבר בין השניים, מסנן, מבצע חמש אגרגציות שונות באמצעות Apache Spark, וכותב שישה דוחות לדיסק — כשכל הלוגיקה העסקית כתובה כפונקציות טהורות שנבדקות בלי Spark בכלל.

זהו. אם תזכור רק את זה — זה מספיק לפתיחת המצגת.

## למה דווקא הפייפליין הזה

הדרישה הייתה "צינור עיבוד נתונים מקצה לקצה עם לפחות 10,000 רשומות". מסחר אלקטרוני נבחר כי הוא מייצר באופן טבעי בדיוק את סוגי השאלות שאגרגציות עונות עליהן: מי הלקוחות הכי רווחיים, אילו קטגוריות מייצרות את רוב ההכנסה, איך מתפלגים גדלי ההזמנות. זה הופך את הדוחות למשמעותיים ולא לתרגיל טכני ריק.

## הבחירה שמגדירה את כל הפרויקט

**Functional Core, Imperative Shell.**

זו לא סיסמה — זה עיקרון ארכיטקטוני עם שם, והוא הדבר שהמרצה יחפש. הרעיון:

- **הליבה** (`com.hit.fp.core`) מכילה את כל הלוגיקה העסקית כפונקציות טהורות. היא לא קוראת קבצים, לא כותבת קבצים, לא מדפיסה, לא בודקת שעון, לא מגרילה מספרים ממקור גלובלי. פונקציה בליבה מקבלת ערכים ומחזירה ערכים. נקודה.
- **המעטפת** (`com.hit.fp.io`, `com.hit.fp.spark`, `Main`) עושה את כל הדברים ה"מלוכלכים": פותחת קבצים, מדפיסה, מנהלת את חיבור ה-Spark. אבל היא לא מקבלת שום החלטה עסקית — היא רק מחליטה **מתי** ו**איפה** להפעיל את פונקציות הליבה.

**למה זה חשוב מעשית, ולא רק תיאורטית:** בגלל ההפרדה הזו, אפשר לבדוק את כל הלוגיקה האנליטית של הפרויקט ב-ScalaTest רגיל, בלי להרים SparkSession, בלי קובץ אחד על הדיסק, בשניות. זו התשובה לשאלה "למה טרחתם".

---

# פרק 2 — מודל הנתונים

לפני שמבינים את הזרימה, צריך להכיר את הטיפוסים. כולם `final case class` — בלתי-משתנים לחלוטין.

## הקלט

**`Transaction`** — שורה מקובץ הטרנזקציות, 7 שדות:
```
orderId, customerId, productId, quantity, unitPrice, country, orderDate
T0000000, C00404,   P0075,     5,        368.16,    Germany, 2024-12-31
```

**`Product`** — שורה מקטלוג המוצרים, 4 שדות:
```
productId, productName,      category,    basePrice
P0000,     Deluxe Jacket 0,  Electronics, 217.92
```

## הטיפוס שנוצר מהחיבור

**`EnrichedTransaction`** — התוצאה של ה-join. לוקח את הטרנזקציה, מוסיף לה את שם המוצר והקטגוריה מהקטלוג, ומחשב את שדה ה-`revenue` (כמות × מחיר יחידה, מעוגל לשתי ספרות).

זה הטיפוס שכל האגרגציות עובדות עליו.

## טיפוסי הפלט (`model/Reports.scala`)

| טיפוס | שדות | עונה על השאלה |
|---|---|---|
| `CategoryRevenue` | category, revenue, orders, unitsSold | כמה כל קטגוריה מכניסה |
| `CountryRevenue` | country, revenue | כמה כל מדינה מכניסה |
| `CustomerSpending` | customerId, totalSpent, orders | כמה כל לקוח הוציא |
| `OrderSizeCount` | size, count | כמה הזמנות בכל סיווג גודל |
| `ParetoLine` | category, revenue, cumulativeShare | האם מעט קטגוריות מייצרות רוב ההכנסה |
| `RejectedLine` | reason, line | אילו שורות נדחו ולמה |

## שני ה-ADTs

**ADT** = Algebraic Data Type. בפועל: `sealed trait` עם רשימה סגורה של מימושים. הקומפיילר יודע את הרשימה המלאה, ולכן יכול להתריע אם `match` שכח מקרה.

**`OrderSize`** — סיווג גודל הזמנה:
```scala
sealed trait OrderSize { def label: String }
case object Small   extends OrderSize   // מתחת 50$
case object Medium  extends OrderSize   // 50-200$
case object Large   extends OrderSize   // 200-1000$
case object Premium extends OrderSize   // מעל 1000$
```

**`ParseError`** — למה שורה נדחתה:
```scala
sealed trait ParseError { def line: String; def reason: String }
final case class WrongFieldCount(line, expected, found)  extends ParseError
final case class NotANumber(line, field, value)          extends ParseError
final case class OutOfRange(line, field, value)          extends ParseError
final case class MissingField(line, field)               extends ParseError
```

שים לב: השגיאה היא **ערך**, לא exception. היא נושאת את השורה המקורית ותיאור קריא. זה הלב של טכניקה 2.

---

# פרק 3 — הפייפליין, שלב אחר שלב

הכול מתנהל מ-`Main.scala`. חמישה שלבים.

## שלב 0 — הקונפיגורציה

```scala
val config = PipelineConfig.default
```

`PipelineConfig` הוא case class אחד שמרכז **כל** נתיב וכל סף בפרויקט:

| שדה | ערך | משמעות |
|---|---|---|
| `dataDirectory` | `"data"` | לאן נכתב הקלט המיוצר |
| `outputDirectory` | `"output"` | לאן נכתבים הדוחות |
| `transactionCount` | 15000 | כמה טרנזקציות לייצר |
| `productCount` | 200 | כמה מוצרים |
| `seed` | `20260816L` | ה-seed שמבטיח שחזור מלא |
| `minimalRevenue` | 20.0 | סף הכנסה לסינון |
| `topCustomerCount` | 20 | כמה לקוחות בדוח |
| `paretoThreshold` | 80.0 | אחוז ההכנסה לניתוח פארטו |

**למה זה חשוב:** אף job לא מכיל נתיב או סף מקודדים בקוד. טסט יכול להריץ את כל הפייפליין על תיקייה זמנית פשוט ע"י בניית `PipelineConfig` אחר. זו לא קוסמטיקה — זו הסיבה ש-`TransactionJobsSpec` יכול לקום.

## שלב 1 — ייצור הדאטהסט

```scala
val generated = DatasetWriter.generateIfMissing(config)
```

אם `data/` ריקה, `DataGeneration` (טהור!) בונה רשימות של `Product` ו-`Transaction` ומרנדר אותן כשורות CSV. `DatasetWriter` (מלוכלך) כותב אותן לדיסק.

**שלוש נקודות שכדאי להכיר:**

**א. המחולל הוא פונקציה טהורה.** הוא לא משתמש ב-`scala.util.Random`. הוא משתמש ב-`core.Rng` — מחולל אקראיות פונקציונלי:

```scala
final case class Rng(private val seed: Long) {
  def nextLong: (Long, Rng) = {
    val next = seed * multiplier + increment
    (next, Rng(next))          // מחזיר את המספר *ואת המצב הבא*
  }
}
```

זה הטריק: במקום לשנות מצב פנימי, הפונקציה **מחזירה את המצב הבא כערך**. הקורא חייב להעביר אותו הלאה. התוצאה: אותו seed → אותו רצף → אותו דאטהסט, בכל מכונה, תמיד. זה מה שמאפשר לבודק להריץ אצלו ולקבל בדיוק את המספרים שבמצגת שלך.

**ב. התפלגות Zipf.** המוצרים לא נבחרים באחידות. `DataGeneration` בונה משקלי Zipf — מוצר בדירוג r מקבל משקל פרופורציוני ל-1/r^exponent. זה מדמה מציאות: מעט מוצרים נמכרים הרבה, הרבה מוצרים נמכרים מעט. בלי זה, ניתוח פארטו היה מחזיר תוצאה משעממת ואחידה.

**ג. השחתה מכוונת.** זו הנקודה הכי חשובה בפרויקט מבחינת המרצה:

```scala
private val corruptionPeriod: Int = 977

private def corrupt(line: String, position: Int): String =
  if (position % corruptionPeriod == 0) {
    fields.updated(4, "N/A").mkString(",")      // המחיר הופך למילה
  } else if (position % (corruptionPeriod * 3) == 1) {
    line.split(",", -1).dropRight(1).mkString(",")  // שדה נעלם
  } else line
```

**המספרים המדויקים:** מתוך 15,000 שורות —
- **15 שורות** עם `N/A` במקום מחיר → `NotANumber`
- **5 שורות** עם 6 שדות במקום 7 → `WrongFieldCount`
- **סה"כ 20 נדחות, 14,980 תקינות**

תדע את המספרים האלה בעל פה. הם יופיעו בקונסולה ובסרטון.

## שלב 2 — קריאה ופענוח

```scala
val transactions = DataSource.loadTransactions(spark, config.transactionsPath)
val products     = DataSource.loadProducts(spark, config.productsPath)
val rejected     = DataSource.loadRejectedTransactions(spark, config.transactionsPath)
```

הנה איך `DataSource` מפריד תקין מפגום — וזה יפה:

```scala
def readLines(spark, path): Dataset[String] =
  spark.read.textFile(path)
    .filter(line => line.nonEmpty && !CsvParsing.isHeader(line))

def loadTransactions(spark, path): Dataset[Transaction] =
  readLines(spark, path)
    .flatMap(line => CsvParsing.parseTransaction(line).toOption)   // רק ה-Right

def loadRejectedTransactions(spark, path): Dataset[RejectedLine] =
  readLines(spark, path).flatMap { line =>
    CsvParsing.parseTransaction(line).left.toOption                // רק ה-Left
      .map(error => RejectedLine(error.reason, error.line))
  }
```

**אותה פונקציית פענוח בדיוק, פעמיים.** פעם אחת לוקחים ממנה את ההצלחות, פעם אחת את הכישלונות. `flatMap` על `Option` זורק את ה-`None` אוטומטית. אין `try`, אין `catch`, אין דגלים.

**איך הפענוח עצמו עובד** (`CsvParsing.parseTransaction`):

```scala
val text     = nonEmptyField(line) _      // הפעלה חלקית: validator לשורה הזו
val positive = positiveNumber(line) _

for {
  orderId    <- text("orderId", fields(0)).right
  customerId <- text("customerId", fields(1)).right
  quantity   <- positive("quantity", fields(3)).right
  unitPrice  <- positive("unitPrice", fields(4)).right
  ...
} yield Transaction(...)
```

זו **for-comprehension על `Either`**. הכלל: ברגע שאחד הצעדים מחזיר `Left`, כל השרשרת מקצרת ומחזירה את אותו `Left`. אף שדה אחריו לא מנוסה. אין exception, אין קפיצה בזרימה — רק ערכים.

## שלב 3 — העיבוד המבוזר

חמישה jobs ב-`TransactionJobs`, וכולם חולקים אותו כלל: **מקבלים Dataset, מחזירים Dataset, לא משנים כלום, לא נוגעים בדיסק.**

### 3.1 `enrich` — ה-join

```scala
transactions
  .joinWith(products, transactions("productId") === products("productId"))
  .map { case (transaction, product) => PureAnalytics.enrich(transaction, product) }
```

`joinWith` הוא join **טיפוסי** — התוצאה היא `Dataset[(Transaction, Product)]`, לא `DataFrame` של שורות אנונימיות. ה-`map` מפרק את הזוג ב-pattern matching ומעביר לפונקציה הטהורה `PureAnalytics.enrich`.

שים לב לחלוקת האחריות: Spark מחליט איך לחבר; `PureAnalytics` מחליט מה זה אומר לחבר.

### 3.2 `keepSignificant` — הסינון (וטכניקה 5)

```scala
val isSignificant: EnrichedTransaction => Boolean =
  PureAnalytics.atLeastRevenue(minimalRevenue)   // הפעלה חלקית
enriched.filter(isSignificant)
```

`atLeastRevenue` מוגדרת עם שתי רשימות פרמטרים:
```scala
def atLeastRevenue(threshold: Double)(transaction: EnrichedTransaction): Boolean =
  transaction.revenue >= threshold
```

כשמפעילים רק את הראשונה, מקבלים פונקציה שכבר "סגורה" על הסף — **closure**. Spark מסדרל (serialize) את הפונקציה הזו ושולח אותה לכל executor בקלאסטר. הסף חי ב-driver; ה-closure נוסע אליו ברשת.

זו טכניקה 5, וזה גם currying בפעולה. שתי דרישות בשורה אחת.

### 3.3 `revenueByCategory` — אגרגציה עם Dataset API

```scala
enriched
  .groupByKey(t => t.category)
  .mapGroups { (category, rows) => PureAnalytics.foldCategory(category, rows) }
```

ו-`foldCategory` היא טהורה:
```scala
val folded = transactions.foldLeft(RevenueAccumulator.empty)(accumulate)
```

`RevenueAccumulator` הוא case class עם שלושה שדות (revenue, orders, units). כל קריאה ל-`accumulate` **בונה מופע חדש** — אין שינוי במקום. זו צבירה בלי mutation.

### 3.4 `revenueByCountry` — ה-job היחיד ב-RDD API

```scala
enriched.rdd
  .map(t => (t.country, t.revenue))
  .reduceByKey((left, right) => left + right)
  .map { case (country, revenue) => CountryRevenue(country, roundMoney(revenue)) }
```

**למה יש כאן RDD בכלל?** כדי להראות שליטה בשני ה-APIs. זה גם הזוג הקלאסי של Spark: `map` שיוצר זוג (מפתח, ערך), ואז `reduceByKey` שמקפל את כל הערכים של כל מפתח עם פונקציה אסוציאטיבית.

**למה הפונקציה חייבת להיות אסוציאטיבית?** כי `reduceByKey` מקפל חלקית על כל מחיצה לפני ה-shuffle. אם הסדר היה משנה, התוצאה הייתה תלויה במספר המחיצות. חיבור אסוציאטיבי — אז לא.

### 3.5 `orderSizeDistribution` — הקומבינטור בפעולה

```scala
val readRevenue = Transform.lift((t: EnrichedTransaction) => t.revenue)
val classify    = Transform.lift((r: Double) => PureAnalytics.classifyOrder(r))
val labelOf     = Transform.lift((s: OrderSize) => s.label)

val pipeline = readRevenue ~> classify ~> labelOf     // ← הקומבינטור המותאם

enriched.map(t => pipeline.run(t))
  .groupByKey(label => label)
  .count()
  .map { case (label, howMany) => OrderSizeCount(label, howMany) }
```

שלוש טרנספורמציות נפרדות, כל אחת בת שורה, מולחמות לאחת בעזרת `~>`. זה בדיוק מה שקומבינטור אמור לעשות.

### 3.6 `topCustomers` — היברידי, ובכוונה

```scala
val everyCustomer = spendingByCustomer(enriched).collect().toList
val best = PureAnalytics.topBy(everyCustomer, count) { c => c.totalSpent }
```

הקיבוץ נעשה מבוזר (`spendingByCustomer`), אבל **הבחירה** של ה-top-N נעשית ב-driver, עם פונקציה טהורה וגנרית:

```scala
def topBy[A](items: List[A], count: Int)(score: A => Double): List[A] =
  items.sortBy(item => -score(item)).take(count)
```

שים לב ל-`[A]` — היא עובדת על **כל** טיפוס, כי הקורא מספק את פונקציית הניקוד. זו Higher-Order Function גנרית, והיא גם curried.

**אם ישאלו למה collect:** מספר הלקוחות הייחודיים הוא סדר גודל של אלפים — נכנס בנוחות לזיכרון ה-driver. עבור top-N על מיליארדי מפתחות היינו משתמשים ב-`takeOrdered` המבוזר. זו החלטה מודעת, לא עצלות.

### 3.7 `paretoByCategory`

מקבל את דוח הקטגוריות, אוסף ל-driver (יש רק כמה קטגוריות), ומעביר ל-`PureAnalytics.paretoOf` — שהיא tail-recursive וצוברת את האחוז המצטבר.

## שלב 4 — כתיבה לדיסק

```scala
DataSink.writeCsv(categories, config.categoryReportPath)
// ... × 6
```

`DataSink` הוא היחיד שמדבר עם `SaveMode.Overwrite`. שישה דוחות → שש תיקיות תחת `output/`.

## שלב 5 — הדפסה

```scala
ConsoleReport.section("REVENUE BY CATEGORY", ReportFormatting.categoryLines(categoryList))
```

**חלוקה עדינה שכדאי להצביע עליה:** `ReportFormatting` **טהור** — הוא מקבל נתונים ומחזיר `List[String]`. `ConsoleReport` **מלוכלך** — הוא לוקח `List[String]` ומדפיס. אפילו העיצוב של הפלט נבדק ביחידה (`ReportFormattingSpec`), כי הוא מחזיר מחרוזות במקום להדפיס אותן.

## ניהול משאבים

```scala
val enriched = TransactionJobs.enrich(...).persist(StorageLevel.MEMORY_AND_DISK)
...
enriched.unpersist()
```

`enriched` משמש בחמישה jobs. בלי `persist` הוא היה מחושב מחדש חמש פעמים, כולל ה-join.

וב-`main`:
```scala
try { run(spark, config) } finally { spark.stop() }
```

ה-session נסגר גם אם נזרקה שגיאה. שאלה קלאסית של בודק.

---

# פרק 4 — מפת המודולים

טבלה אחת. אם מישהו שואל "איפה X" — כאן.

| מודול | קובץ | טהור? | תפקיד |
|---|---|---|---|
| **core** | `PureAnalytics.scala` | ✅ | כל הלוגיקה האנליטית: enrich, סיווג, צבירה, פארטו, top-N |
| | `DataGeneration.scala` | ✅ | בניית הדאטהסט הסינתטי + ההשחתה המכוונת |
| | `CsvParsing.scala` | ✅ | פענוח שורות ל-`Either[ParseError, T]` |
| | `Transform.scala` | ✅ | הקומבינטור המותאם `~>` / `zip` / `filterK` |
| | `Rng.scala` | ✅ | מחולל אקראיות פונקציונלי (state passing) |
| | `ParseError.scala` | ✅ | ADT של שגיאות פענוח |
| | `ReportFormatting.scala` | ✅ | הפיכת נתונים למחרוזות |
| **spark** | `TransactionJobs.scala` | ✅* | חמישה jobs מבוזרים. לא נוגע בדיסק |
| | `SparkSessionProvider.scala` | ❌ | פתיחת ה-session, `local[*]`, 8 מחיצות shuffle |
| **io** | `DataSource.scala` | ❌ | קריאת קבצים, הפרדת תקין מפגום |
| | `DataSink.scala` | ❌ | כתיבת דוחות |
| | `DatasetWriter.scala` | ❌ | כתיבת הדאטהסט המיוצר |
| | `ConsoleReport.scala` | ❌ | הדפסה למסך |
| | `PipelineConfig.scala` | ✅ | קונפיגורציה בלתי-משתנה |
| **model** | `Transaction`, `Product`, `EnrichedTransaction` | ✅ | טיפוסי הליבה |
| | `Reports.scala` | ✅ | חמישה טיפוסי פלט |
| | `OrderSize.scala` | ✅ | ADT של סיווג גודל |
| | `RejectedLine.scala` | ✅ | שורה שנדחתה + סיבה |
| **root** | `Main.scala` | ❌ | המעטפת: מתזמר את חמשת השלבים |

\* `TransactionJobs` לא מבצע I/O ולא משנה כלום, אבל הוא כן תלוי ב-SparkSession. לכן "טהור" במובן שאין לו side effects, אך לא ניתן להריצו בלי Spark.

## הטסטים

| Suite | בודק |
|---|---|
| `CsvParsingSpec` | פענוח תקין + כל סוגי ה-`ParseError` |
| `PureAnalyticsSpec` | חישובי הכנסה, סיווג, פארטו, top-N |
| `TransformSpec` | הקומבינטורים `~>`, `zip`, `filterK`, `chain` |
| `DataGenerationSpec` | דטרמיניזם של ה-seed, מספר הרשומות, ההשחתה |
| `ReportFormattingSpec` | פורמט הפלט |
| `TransactionJobsSpec` | ה-jobs המבוזרים על SparkSession מקומי |

**חמישה מתוך שישה רצים בלי Spark בכלל.** זה הפרס על ההפרדה.

---

# פרק 5 — מושגי התכנות הפונקציונלי, בהסבר מלא

זה הפרק שמכין אותך לשאלות "מה זה בעצם".

## 5.1 פונקציה טהורה (Pure Function)

**הגדרה:** פונקציה שמקיימת שני תנאים —
1. התוצאה תלויה **אך ורק** בארגומנטים. אותו קלט → אותו פלט, תמיד.
2. אין לה **תופעות לוואי**: לא כותבת לקובץ, לא מדפיסה, לא משנה משתנה חיצוני, לא קוראת שעון.

**בפרויקט:**
```scala
def revenueOf(transaction: Transaction): Double =
  roundMoney(transaction.quantity * transaction.unitPrice)
```

**למה זה שווה משהו:** אפשר להחליף כל קריאה לפונקציה בערך שהיא מחזירה בלי לשנות את התנהגות התוכנית (זה נקרא *referential transparency*). מזה נובע: אפשר לבדוק אותה בבידוד, אפשר להריץ אותה במקביל בלי נעילות, ואפשר להסיק עליה בראש.

**זו בדיוק הסיבה ש-Spark דורש את זה:** פונקציה שנשלחת ל-executor רצה על מכונה אחרת, בסדר לא ידוע, אולי פעמיים (retry). אם היא לא טהורה — התוצאה לא צפויה.

## 5.2 אי-שינויות (Immutability)

**הגדרה:** מבנה נתונים שלא ניתן לשנות אחרי יצירתו. "שינוי" = יצירת עותק חדש.

**בפרויקט:** אפס `var` ב-`src/main`. כל הטיפוסים הם `final case class`. הצבירה:
```scala
def accumulate(acc: RevenueAccumulator, t: EnrichedTransaction): RevenueAccumulator =
  RevenueAccumulator(
    revenue = acc.revenue + t.revenue,     // מופע חדש
    orders  = acc.orders + 1L,
    units   = acc.units + t.quantity
  )
```

**אם ישאלו "זה לא בזבזני?":** ה-JVM מצוין באובייקטים קצרי-חיים; ה-GC הדורי מטפל בהם כמעט בחינם. בתמורה מקבלים בטיחות בריבוי תהליכים בלי נעילה אחת. ב-Spark זה לא אופציה אלא תנאי.

## 5.3 פונקציות מסדר גבוה (Higher-Order Functions)

**הגדרה:** פונקציה שמקבלת פונקציה כפרמטר, או מחזירה פונקציה.

**בפרויקט:**
```scala
def topBy[A](items: List[A], count: Int)(score: A => Double): List[A] =
  items.sortBy(item => -score(item)).take(count)
```

`score` הוא פרמטר שהוא פונקציה. זה מה שהופך את `topBy` לגנרית — היא לא יודעת מה זה לקוח, היא רק יודעת למיין לפי ניקוד.

## 5.4 Currying והפעלה חלקית

**Currying:** הגדרת פונקציה עם כמה רשימות פרמטרים, כך שאפשר לספק אותן בשלבים.

```scala
def atLeastRevenue(threshold: Double)(t: EnrichedTransaction): Boolean =
  t.revenue >= threshold
```

**הפעלה חלקית:** מספקים רק את הרשימה הראשונה ומקבלים פונקציה חדשה:
```scala
val isSignificant = PureAnalytics.atLeastRevenue(20.0)
// עכשיו isSignificant: EnrichedTransaction => Boolean
```

**בפרויקט:** `atLeastRevenue`, `shippedTo`, `topBy`, ושני ה-validators ב-`CsvParsing`:
```scala
val text = nonEmptyField(line) _    // validator שכבר "יודע" את השורה
text("orderId", fields(0))          // ומשתמשים בו לכל שדה
```

## 5.5 Closure

**הגדרה:** פונקציה שלוכדת ("סוגרת על") משתנה מהסביבה שבה הוגדרה, ונושאת אותו איתה.

```scala
val minimalRevenue = 20.0                              // חי ב-driver
val isSignificant = atLeastRevenue(minimalRevenue)     // ה-closure לכד אותו
enriched.filter(isSignificant)                         // Spark שולח אותו לכל executor
```

**למה זה מעניין ב-Spark ספציפית:** ה-executors רצים בתהליכים אחרים, אולי במכונות אחרות. Spark חייב **לסדרל** את ה-closure ולשלוח אותו ברשת. כל מה שהוא לכד חייב להיות serializable — ולכן `Transform` מוגדר `extends Serializable`. אם היה לוכד משהו שלא ניתן לסדרול, הריצה הייתה נכשלת ב-`Task not serializable`.

## 5.6 רקורסיית זנב (Tail Recursion)

**הגדרה:** רקורסיה שבה הקריאה הרקורסיבית היא **הביטוי האחרון** בפונקציה — אין שום חישוב אחריה.

```scala
@tailrec
def loop(remaining: List[EnrichedTransaction], sum: Double): Double =
  remaining match {
    case Nil          => sum
    case head :: tail => loop(tail, sum + head.revenue)   // ← אחרון
  }
```

**למה זה חשוב:** הקומפיילר מזהה את הדפוס והופך את הרקורסיה ללולאה. אין גדילה של מחסנית הקריאות → אין `StackOverflowError` גם על רשימה של מיליון איברים.

**מה עושה `@tailrec`:** לא מבצע את האופטימיזציה — **מוודא** אותה. אם כתבת רקורסיה שאינה זנבית, הקוד לא יתקמפל. זו רשת ביטחון.

**דוגמה לרקורסיה שאינה זנבית:**
```scala
def sum(l: List[Double]): Double = l match {
  case Nil => 0.0
  case h :: t => h + sum(t)     // ✗ אחרי הקריאה יש חיבור
}
```

**בפרויקט:** 5 מופעים — `PureAnalytics.totalRevenue`, `PureAnalytics.paretoOf`, ושלוש ב-`DataGeneration`.

## 5.7 Pattern Matching ו-ADT

**Pattern matching** הוא לא switch. הוא פירוק מבנה + התאמה + כריכת משתנים, בביטוי אחד.

```scala
def classifyOrder(revenue: Double): OrderSize =
  revenue match {
    case amount if amount < 50.0   => Small      // התאמה עם guard
    case amount if amount < 200.0  => Medium
    case amount if amount < 1000.0 => Large
    case _                         => Premium
  }
```

ופירוק זוגות:
```scala
.map { case (transaction, product) => PureAnalytics.enrich(transaction, product) }
```

**מה נותן ה-`sealed`:** הקומפיילר יודע שרשימת המימושים של `OrderSize` סגורה לקובץ הזה. לכן ב-
```scala
def describeSize(size: OrderSize): String = size match {
  case Small => ...; case Medium => ...; case Large => ...; case Premium => ...
}
```
אם מחר יתווסף `case object Enterprise extends OrderSize` ותשכח אותו כאן — **הקומפיילר יתריע**. זו בדיקת מיצוי (exhaustiveness), וזה ההבדל המהותי בין ADT לבין ירושה רגילה.

## 5.8 טיפול פונקציונלי בשגיאות

**הרעיון:** שגיאה היא **ערך מוחזר**, לא זריקה שמשנה את זרימת התוכנית.

| כלי | מתי |
|---|---|
| `Option[A]` | יש ערך או שאין — בלי סיבה |
| `Either[E, A]` | הצליח (`Right`) או נכשל (`Left`) **עם תיאור** |
| `Try[A]` | עוטף קוד שעלול לזרוק exception |

**בפרויקט משתמשים בכל השלושה, כל אחד במקומו:**

`Try` — לגישור בין עולם שזורק לעולם שלא:
```scala
Try(value.toDouble) match {
  case Failure(_)                      => Left(NotANumber(line, field, value))
  case Success(n) if n <= 0.0           => Left(OutOfRange(line, field, value))
  case Success(n)                       => Right(n)
}
```
`toDouble` זורק `NumberFormatException`. `Try` תופס אותו והופך אותו לערך. משם והלאה אין exceptions.

`Either` — לשרשור שמקצר בכישלון הראשון (for-comprehension, ראה פרק 3).

`Option` — לסינון:
```scala
.flatMap(line => CsvParsing.parseTransaction(line).toOption)
```

**מה להגיד למרצה:** "בחרנו ב-`Either` ולא ב-`Option` בשכבת הפענוח כי `None` היה מאבד את **הסיבה**. `ParseError` נושא את השורה המקורית ואת התיאור, ולכן יכולנו לייצר דוח `rejected_lines` שאומר לאיש העסקים מה בדיוק היה שבור — לא רק שמשהו נזרק."

## 5.9 קומבינטור

**הגדרה:** ערך שיודע להיות מורכב עם ערך אחר מאותו סוג, כדי לייצר ערך גדול יותר מאותו סוג.

```scala
trait Transform[A, B] extends Serializable {
  def run(input: A): B

  def ~>[C](next: Transform[B, C]): Transform[A, C] = {      // שרשור
    val self = this
    new Transform[A, C] { def run(input: A): C = next.run(self.run(input)) }
  }

  def zip[C](other: Transform[A, C]): Transform[A, (B, C)]    // מקבילי
  def filterK(predicate: B => Boolean): Transform[A, Option[B]]  // סינון
}
```

ובאובייקט הנלווה:
```scala
def lift[A, B](f: A => B): Transform[A, B]        // הרמת פונקציה רגילה
def identity[A]: Transform[A, A]                  // האיבר הנייטרלי
def chain[A](steps: List[Transform[A, A]]): Transform[A, A] =
  steps.foldLeft(identity[A])((combined, step) => combined ~> step)
```

**נקודה חשובה להגנה:** ההערה בקוד מציינת במפורש שזה **לא** מבוסס על `Function1.andThen`, כי שימוש חוזר ב-`Function1` אינו נחשב מימוש קומבינטור מותאם. בנינו את הטיפוס מאפס.

**`identity` היא האיבר הנייטרלי של `~>`** — `identity ~> f` שווה ל-`f`. זה מה שמאפשר ל-`chain` לעבוד גם על רשימה ריקה. אם מישהו ישאל אם זה מונואיד — התשובה: `Transform[A, A]` עם `~>` ו-`identity` אכן מקיים את חוקי המונואיד.

---

# פרק 6 — ההצגה בכיתה (3-5 דקות)

## מבנה מוצע

| זמן | שקף | מסר יחיד |
|---|---|---|
| 0:00-0:40 | 1. הבעיה | 15,000 טרנזקציות, שישה דוחות, והנתונים לא מושלמים |
| 0:40-1:40 | 2. הארכיטקטורה | Functional Core, Imperative Shell — **וזה השקף החשוב** |
| 1:40-2:30 | 3. שגיאות כערכים | 20 שורות פגומות, אפס קריסות |
| 2:30-3:30 | 4. חמש הטכניקות | נדרשו שלוש, מימשנו חמש |
| 3:30-4:00 | 5. תוצאות | מה למדנו על החנות + מה למדנו על FP |

## שלושת המסרים שחייבים לעבור

**1. בידדנו את ה-I/O בכוונה, וזה השתלם.**
> "כל הלוגיקה העסקית יושבת ב-core ואינה נוגעת בדיסק. התוצאה: חמישה מתוך שישה קבצי הטסטים רצים בלי להרים SparkSession בכלל. אנחנו בודקים את חוקי העסק בשניות, לא בדקות."

**2. שגיאות הן נתונים.**
> "המחולל שלנו משחית בכוונה 20 שורות מתוך 15,000. הפייפליין לא זורק אף exception — כל שורה פגומה חוזרת כ-Left עם ADT שמתאר את הסיבה, ונכתבת לדוח נפרד. בעולם האמיתי נתונים לעולם אינם מושלמים, ותוכנית שקורסת על שורה אחת פגומה היא תוכנית לא שמישה."

**3. השתמשנו ב-AI בעיניים פקוחות.**
> "עבדנו עם Claude כשותף פיתוח בתוך VS Code. לא לקבלת קוד מוכן — לסקירת הקוד מול דרישות הקורס, לאיתור פערי תיעוד וטסטים, ולאימות עמידות הפייפליין. כל הצעה נבדקה מול sbt test לפני מיזוג, ואת הארכיטקטורה בחרנו אנחנו."

זו נקודה שהמרצה ציין במפורש שהוא מחפש. אל תדלג עליה.

## המודל המנטלי — איך לענות אם ישאלו "למה פונקציונלי"

אל תגיד "כי זה נדרש בקורס". תגיד:

> "עיבוד נתונים מבוזר הוא בדיוק המקום שבו התכנות הפונקציונלי משתלם. Spark שולח את הקוד שלנו לרוץ על מכונות אחרות, בסדר שאנחנו לא שולטים בו, לפעמים פעמיים בגלל retry. פונקציה עם מצב משתנה או תופעת לוואי פשוט תיתן תוצאה שגויה בתנאים האלה. פונקציה טהורה — לא. אז ההפרדה שעשינו היא לא תרגיל אקדמי: היא התנאי לכך שהתוצאות נכונות."

---

# פרק 7 — שאלות שאתה עלול להישאל, ותשובות מוכנות

**ש: למה Scala 2 ולא Scala 3?**
ת: Spark 3.3.0 אינו נתמך במלואו על Scala 3. ה-encoders של Dataset API מסתמכים על מקרו-ים שלא הועברו במלואם. בחרנו 2.12.19 שהיא הגרסה שעליה Spark 3.3 נבנה.

**ש: מה ההבדל בין RDD, DataFrame ו-Dataset, ולמה השתמשתם בשניים?**
ת: RDD הוא אוסף מבוזר של אובייקטים — טיפוסי אבל בלי אופטימיזציה. DataFrame הוא `Dataset[Row]` — יש אופטימיזציה דרך Catalyst אבל אין בטיחות טיפוסים בזמן קומפילציה. Dataset משלב את שניהם. השתמשנו ב-Dataset ברוב הפייפליין כי אנחנו רוצים ש-`EnrichedTransaction` יהיה טיפוס אמיתי, וב-RDD ב-`revenueByCountry` כדי להדגים שליטה גם ב-API הנמוך.

**ש: למה `groupByKey` ולא `reduceByKey` בכל מקום? `groupByKey` יקר יותר.**
ת: נכון — `reduceByKey` מקפל חלקית לפני ה-shuffle ומעביר פחות נתונים. השתמשנו ב-`groupByKey().mapGroups` היכן שהאגרגציה צריכה לראות את כל הקבוצה כדי לבנות רשומה מורכבת (הכנסה + מספר הזמנות + כמות יחידות), וב-`reduceByKey` ב-`revenueByCountry` שם האגרגציה היא חיבור פשוט. בסקאלה של 15,000 רשומות ההפרש זניח; בסקאלה גדולה היינו שוקלים `Aggregator` טיפוסי.

**ש: הדאטהסט מיוצר ולא אמיתי — זה לא רמאות?**
ת: מסמך הדרישות מאפשר "בחירה **או יצירה**" של דאטהסט. יצירה נותנת יתרון: הפרויקט עומד בפני עצמו בלי הורדות, וה-seed מבטיח שהבודק יקבל בדיוק את המספרים שבמצגת. הוספנו התפלגות Zipf כדי שהנתונים יתנהגו כמו נתוני אמת, והשחתה מכוונת כדי שיהיה מה לטפל בו.

**ש: איפה הוכחת ה-immutability?**
ת: `grep -rn "\bvar \b" src/main` מחזיר ריק. אפס משתנים משתנים בכל קוד הייצור.

**ש: מה קורה אם קובץ הקלט חסר לגמרי?**
ת: `DatasetWriter.generateIfMissing` מייצר אותו. אם הוא קיים אבל פגום — הפענוח מחזיר `Left` לכל שורה, והן נכתבות לדוח הנדחות. (**זו נקודה שכדאי שתבדוק בפועל לפני ההצגה** — הרץ עם קובץ ריק וראה מה קורה.)

**ש: `collect()` מביא הכול ל-driver — זה לא מסוכן?**
ת: כן, על דאטה גדול. השתמשנו בו רק היכן שהתוצאה כבר מצומצמת: מספר הקטגוריות הוא חד-ספרתי, מספר הלקוחות הייחודיים סדר גודל של אלפים. ל-top-N על מיליארדי מפתחות היינו משתמשים ב-`takeOrdered`.

**ש: למה `persist` על `enriched`?**
ת: הוא נצרך על ידי חמישה jobs שונים. בלי persist, כל job היה מפעיל מחדש את כל שרשרת החישוב כולל ה-join. `MEMORY_AND_DISK` נבחר כדי שלא ניפול אם הזיכרון לא מספיק.

**ש: מה זה `Rng` ולמה לא `scala.util.Random`?**
ת: `scala.util.Random` מחזיק מצב פנימי משתנה — קריאה אליו היא תופעת לוואי, והיא הופכת כל פונקציה שמשתמשת בו ללא-טהורה ולא ניתנת לבדיקה דטרמיניסטית. `Rng` שלנו מחזיר זוג: המספר **והמחולל הבא**. המצב עובר כערך. התוצאה: אותו seed מייצר את אותו דאטהסט תמיד, וניתן לבדוק את זה בטסט.

**ש: הראה לי איפה יש Function Composition.**
ת: `Transform.~>` ב-`core/Transform.scala`, והשימוש ב-`TransactionJobs.orderSizeDistribution`:
`readRevenue ~> classify ~> labelOf`.

**ש: איך היית מרחיב את זה למיליארד רשומות?**
ת: הליבה לא הייתה משתנה בכלל — היא כבר טהורה וניתנת להרצה במקביל. הייתי מחליף את ה-`collect()` ב-`takeOrdered` מבוזר, מחליף `groupByKey().mapGroups` ב-`Aggregator` טיפוסי כדי לצמצם shuffle, ומכוונן את `spark.sql.shuffle.partitions` (כרגע 8, מתאים ל-`local[*]`). זה הפרס על ההפרדה: ההתאמות הן כולן בשכבת ה-Spark.

---

# נספח — פקודות שכדאי שיהיו לך בראש

```bash
sbt run                # מייצר דאטהסט אם חסר, ואז מריץ את הפייפליין
sbt test               # כל שישה ה-suites
sbt clean compile      # בנייה נקייה
sbt "testOnly com.hit.fp.core.CsvParsingSpec"    # suite בודד

grep -rn "\bvar \b" src/main          # הוכחת immutability — מחזיר ריק
grep -rn "TECHNIQUE" src/main         # מיקום חמש הטכניקות
wc -l data/transactions.csv           # 15001 (כולל header)
```

## מספרים לזכור בעל פה

| נתון | ערך |
|---|---|
| טרנזקציות | 15,000 |
| מוצרים | 200 |
| שורות תקינות | 14,980 |
| שורות נדחות | 20 (15 `NotANumber` + 5 `WrongFieldCount`) |
| דוחות פלט | 6 |
| פעולות Spark | 6 (נדרשו 4) |
| טכניקות מתקדמות | 5 (נדרשו 3) |
| מופעי `var` | 0 |
| seed | `20260816L` |
| סף הכנסה מינימלי | 20.0 |
