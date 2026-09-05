/**
 * NCM PERMISSION — تنظيف الشيت
 * =============================
 *
 * الكود ده بيلمس الأعمدة دي بس، ومش بيقرب من أي عمود تاني خالص:
 *
 *   تصاريح أفراد :  I (تصريح)  ·  J (Mail Day)
 *   تصاريح مركبات:  D (النوع)  ·  F (تصريح)  ·  G (Mail Day)
 *
 * بيعمل إيه:
 *   1. عمود Mail Day: يشيل كل الألوان ويخليها بيضا، والصف الفاصل الأخضر يفضل أخضر.
 *   2. عمود Mail Day + عمود التصريح: كل التواريخ تتوحّد لصيغة  يوم/شهر/سنة  (08/12/2026).
 *   3. عمود التصريح: أي خلية مش تاريخ ومش "منع" تتكتب فيها "غير محدد موقف".
 *      أي حاجة فيها كلمة منع (منع / منع جنائى / منع نهائى) بتفضل زي ما هي.
 *   4. عمود النوع (المركبات بس): أي خلية فاضية تتكتب فيها "غير محدد".
 *
 * الصفوف الفاصلة (اللي مفيهاش اسم أو رقم سيارة) مش بيتلمس محتواها خالص.
 */

var CONFIG = [
  { name: 'تصاريح أفراد', keyCol: 3, permitCol: 9,  dayCol: 10, typeCol: 0 },
  { name: 'تصاريح مركبات', keyCol: 3, permitCol: 6,  dayCol: 7,  typeCol: 4 }
];

var UNDETERMINED_PERMIT = 'غير محدد موقف';
var UNDETERMINED_TYPE   = 'غير محدد';
var WHITE               = '#ffffff';


/** شغّل ده. */
function cleanPermits() {
  var ss = SpreadsheetApp.getActive();
  var report = [];

  CONFIG.forEach(function (cfg) {
    var sh = findSheet_(ss, cfg.name);
    if (!sh) {
      report.push('⚠️ مالقيتش تاب اسمه: ' + cfg.name);
      return;
    }
    report.push(cleanOneSheet_(sh, cfg));
  });

  SpreadsheetApp.getUi().alert('تم التنظيف\n\n' + report.join('\n\n'));
}


/** التابات ممكن يكون في آخر اسمها مسافة، فبندوّر بالاسم بعد التشذيب. */
function findSheet_(ss, wanted) {
  var sheets = ss.getSheets();
  for (var i = 0; i < sheets.length; i++) {
    if (sheets[i].getName().trim() === wanted.trim()) return sheets[i];
  }
  return null;
}


function cleanOneSheet_(sh, cfg) {
  var lastRow = sh.getLastRow();
  if (lastRow < 2) return sh.getName() + ': مفيش بيانات';

  var n = lastRow - 1;                       // عدد صفوف البيانات (من صف 2)
  var keys = sh.getRange(2, cfg.keyCol, n, 1).getValues();

  var stats = {
    dayDates: 0, dayColoursCleared: 0, greenKept: 0,
    permitDates: 0, permitEmptyFixed: 0, permitTextFixed: 0, banKept: 0,
    typeFixed: 0
  };

  // ---------- 1) عمود Mail Day: الألوان ----------
  var dayBgRange = sh.getRange(2, cfg.dayCol, n, 1);
  var bgs = dayBgRange.getBackgrounds();
  for (var i = 0; i < n; i++) {
    if (isGreen_(bgs[i][0])) {
      stats.greenKept++;                     // الفاصل الأخضر يفضل زي ما هو
    } else {
      if (!isWhiteish_(bgs[i][0])) stats.dayColoursCleared++;
      bgs[i][0] = WHITE;
    }
  }
  dayBgRange.setBackgrounds(bgs);

  // ---------- 2) عمود Mail Day: التواريخ ----------
  var dayVals = sh.getRange(2, cfg.dayCol, n, 1).getValues();
  var dayOut = [];
  for (var i = 0; i < n; i++) {
    var isSep = isBlank_(keys[i][0]);
    var v = dayVals[i][0];
    if (isSep) { dayOut.push([v]); continue; }

    var parsed = parseDate_(v);
    if (parsed) {
      dayOut.push([fmtDate_(parsed.date) + (parsed.note ? ' ' + parsed.note : '')]);
      stats.dayDates++;
    } else {
      dayOut.push([v]);
    }
  }
  // النص لازم يتكتب كنص، وإلا جوجل يعيد يقرا 08/12/2026 حسب لغة الشيت ويقلب اليوم والشهر
  sh.getRange(2, cfg.dayCol, n, 1).setNumberFormat('@').setValues(dayOut);

  // ---------- 3) عمود التصريح ----------
  var pVals = sh.getRange(2, cfg.permitCol, n, 1).getValues();
  var pOut = [];
  for (var i = 0; i < n; i++) {
    var isSep = isBlank_(keys[i][0]);
    var v = pVals[i][0];
    if (isSep) { pOut.push([v]); continue; }

    var s = (v === null || v === undefined) ? '' : String(v).trim();

    if (s.indexOf('منع') !== -1) {           // منع / منع جنائى / منع نهائى
      pOut.push([v]);
      stats.banKept++;
      continue;
    }

    var parsed = parseDate_(v);
    if (parsed) {
      pOut.push([fmtDate_(parsed.date) + (parsed.note ? ' ' + parsed.note : '')]);
      stats.permitDates++;
    } else {
      pOut.push([UNDETERMINED_PERMIT]);
      if (s === '') stats.permitEmptyFixed++; else stats.permitTextFixed++;
    }
  }
  sh.getRange(2, cfg.permitCol, n, 1).setNumberFormat('@').setValues(pOut);

  // ---------- 4) عمود النوع (المركبات بس) ----------
  if (cfg.typeCol) {
    var tVals = sh.getRange(2, cfg.typeCol, n, 1).getValues();
    var tOut = [];
    for (var i = 0; i < n; i++) {
      var isSep = isBlank_(keys[i][0]);
      var v = tVals[i][0];
      if (isSep) { tOut.push([v]); continue; }

      if (isBlank_(v)) {
        tOut.push([UNDETERMINED_TYPE]);
        stats.typeFixed++;
      } else {
        tOut.push([v]);
      }
    }
    sh.getRange(2, cfg.typeCol, n, 1).setValues(tOut);
  }

  return sh.getName() + ':\n'
    + '  • Mail Day — تواريخ اتظبطت: ' + stats.dayDates + '\n'
    + '  • Mail Day — خلايا بقت بيضا: ' + stats.dayColoursCleared + '\n'
    + '  • الفاصل الأخضر اللي فضل زي ما هو: ' + stats.greenKept + '\n'
    + '  • تصريح — تواريخ اتظبطت: ' + stats.permitDates + '\n'
    + '  • تصريح — خانات فاضية بقت "غير محدد موقف": ' + stats.permitEmptyFixed + '\n'
    + '  • تصريح — كلام بقى "غير محدد موقف": ' + stats.permitTextFixed + '\n'
    + '  • تصريح — منع فضل زي ما هو: ' + stats.banKept + '\n'
    + (cfg.typeCol ? '  • النوع — بقى "غير محدد": ' + stats.typeFixed : '');
}


/* ===================== أدوات مساعدة ===================== */

function isBlank_(v) {
  return v === null || v === undefined || String(v).trim() === '';
}

/** أبيض أو بدون لون. */
function isWhiteish_(hex) {
  if (!hex) return true;
  var h = String(hex).toLowerCase();
  return h === '#ffffff' || h === 'white' || h === '';
}

/** أخضر = القناة الخضرا أعلى بوضوح من الحمرا والزرقا. */
function isGreen_(hex) {
  if (!hex) return false;
  var m = String(hex).match(/^#?([0-9a-f]{6})$/i);
  if (!m) return false;
  var v = parseInt(m[1], 16);
  var r = (v >> 16) & 255, g = (v >> 8) & 255, b = v & 255;
  return g > r + 20 && g > b + 20;
}

/**
 * بيرجّع {date, note} أو null.
 * التواريخ في الشيت يوم-أولاً؛ مش بنقلبها لشهر-أولاً غير لما اليوم-أولاً يبقى مستحيل.
 */
function parseDate_(v) {
  if (v === null || v === undefined || v === '') return null;

  if (Object.prototype.toString.call(v) === '[object Date]') {
    return { date: v, note: '' };
  }

  var s = String(v).trim();
  var m = s.match(/^(\d{1,2})\s*[-\/]\s*(\d{1,2})\s*[-\/]\s*(\d{4})/);
  if (!m) return null;

  var a = parseInt(m[1], 10), b = parseInt(m[2], 10), y = parseInt(m[3], 10);
  var day, mon;
  if (b > 12 && a <= 12) { mon = a; day = b; } else { day = a; mon = b; }
  if (mon < 1 || mon > 12 || day < 1 || day > 31) return null;

  var d = new Date(y, mon - 1, day);
  if (d.getMonth() !== mon - 1 || d.getDate() !== day) return null;   // زي 31-02

  var note = s.substring(m[0].length).replace(/^[\s\-\/(),.:;؛]+/, '').replace(/[)\]]+$/, '').trim();
  return { date: d, note: note };
}

function fmtDate_(d) {
  var p = function (x) { return (x < 10 ? '0' : '') + x; };
  return p(d.getDate()) + '/' + p(d.getMonth() + 1) + '/' + d.getFullYear();
}
