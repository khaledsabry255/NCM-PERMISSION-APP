/**
 * NCM Permission — Google Sheet cleanup.
 *
 * Touches ONLY:
 *   تصاريح أفراد  : column I (تصريح) and column J (Mali Day)
 *   تصاريح مركبات : column D (النوع), column F (تصريح), column G (Mail Day)
 * Every other column, and every separator row, is left completely untouched.
 *
 * Run order:
 *   1) Select "previewChanges" from the function dropdown above and click Run.
 *      Nothing is written — it only logs what WOULD change.
 *   2) Check the log (View > Logs, or the "Execution log" panel).
 *   3) If the numbers look right, select "applyChanges" and click Run.
 */

var UNDETERMINED = 'غير محدد موقف';
var UNDETERMINED_TYPE = 'غير محدد';
var WHITE = '#ffffff';

function previewChanges() { runAll_(true); }
function applyChanges()   { runAll_(false); }

function runAll_(dryRun) {
  var ss = SpreadsheetApp.getActive();
  var ind = findSheet_(ss, 'أفراد');
  var veh = findSheet_(ss, 'مركبات');

  var indStats = processSheet_(ind, { nameCol: 3, permitCol: 9, dayCol: 10, typeCol: null }, dryRun);
  var vehStats = processSheet_(veh, { nameCol: 3, permitCol: 6, dayCol: 7, typeCol: 4 }, dryRun);

  Logger.log((dryRun ? '=== PREVIEW (nothing written) ===' : '=== APPLIED ==='));
  Logger.log('تصاريح أفراد : ' + JSON.stringify(indStats));
  Logger.log('تصاريح مركبات: ' + JSON.stringify(vehStats));
}

function findSheet_(ss, keyword) {
  var sheets = ss.getSheets();
  for (var i = 0; i < sheets.length; i++) {
    if (sheets[i].getName().indexOf(keyword) !== -1) return sheets[i];
  }
  throw new Error('Sheet containing "' + keyword + '" not found. Sheet names: ' +
    sheets.map(function (s) { return s.getName(); }).join(', '));
}

function processSheet_(sh, cfg, dryRun) {
  var lastRow = sh.getLastRow();
  var stats = {
    mailday_whitened: 0, mailday_dates_normalised: 0,
    permit_kept_ban: 0, permit_dates_normalised: 0, permit_to_undetermined: 0,
    type_filled: 0, separator_rows_skipped: 0
  };
  if (lastRow < 2) return stats;

  var n = lastRow - 1;
  var nameRange = sh.getRange(2, cfg.nameCol, n, 1);
  var names = nameRange.getValues();

  var permitRange = sh.getRange(2, cfg.permitCol, n, 1);
  var permitValues = permitRange.getValues();
  var permitFormats = permitRange.getNumberFormats();

  var dayRange = sh.getRange(2, cfg.dayCol, n, 1);
  var dayValues = dayRange.getValues();
  var dayFormats = dayRange.getNumberFormats();
  var dayBackgrounds = dayRange.getBackgrounds();

  var typeRange = cfg.typeCol ? sh.getRange(2, cfg.typeCol, n, 1) : null;
  var typeValues = typeRange ? typeRange.getValues() : null;

  var newPermitValues = [], newPermitFormats = [];
  var newDayValues = [], newDayFormats = [], newDayBackgrounds = [];
  var newTypeValues = typeRange ? [] : null;

  for (var i = 0; i < n; i++) {
    var isSeparator = isBlank_(names[i][0]);

    if (isSeparator) {
      stats.separator_rows_skipped++;
      newDayValues.push([dayValues[i][0]]);
      newDayFormats.push([dayFormats[i][0]]);
      newDayBackgrounds.push([dayBackgrounds[i][0]]); // green kept as-is
      newPermitValues.push([permitValues[i][0]]);
      newPermitFormats.push([permitFormats[i][0]]);
      if (newTypeValues) newTypeValues.push([typeValues[i][0]]);
      continue;
    }

    // ---- Mail Day: every data-row cell goes white; date text normalised ----
    var md = parseDate_(dayValues[i][0]);
    if (md) {
      newDayValues.push([formatDate_(md)]);
      newDayFormats.push(['@']);
      stats.mailday_dates_normalised++;
    } else {
      newDayValues.push([dayValues[i][0]]);
      newDayFormats.push([dayFormats[i][0]]);
    }
    newDayBackgrounds.push([WHITE]);
    stats.mailday_whitened++;

    // ---- Permit: بان يفضل زي ما هو، الباقي تاريخ أو "غير محدد موقف" ----
    var praw = permitValues[i][0];
    var ptext = (praw === null || praw === undefined) ? '' : String(praw).trim();
    if (ptext.indexOf('منع') !== -1) {
      newPermitValues.push([praw]);
      newPermitFormats.push([permitFormats[i][0]]);
      stats.permit_kept_ban++;
    } else {
      var pd = parseDate_(praw);
      if (pd) {
        newPermitValues.push([formatDate_(pd)]);
        newPermitFormats.push(['@']);
        stats.permit_dates_normalised++;
      } else {
        newPermitValues.push([UNDETERMINED]);
        newPermitFormats.push(['@']);
        stats.permit_to_undetermined++;
      }
    }

    // ---- Vehicle type: blank -> "غير محدد" ----
    if (newTypeValues) {
      var traw = typeValues[i][0];
      var ttext = (traw === null || traw === undefined) ? '' : String(traw).trim();
      if (ttext === '') {
        newTypeValues.push([UNDETERMINED_TYPE]);
        stats.type_filled++;
      } else {
        newTypeValues.push([traw]);
      }
    }
  }

  if (!dryRun) {
    dayRange.setNumberFormats(newDayFormats);
    dayRange.setValues(newDayValues);
    dayRange.setBackgrounds(newDayBackgrounds);

    permitRange.setNumberFormats(newPermitFormats);
    permitRange.setValues(newPermitValues);

    if (typeRange) typeRange.setValues(newTypeValues);
  }

  return stats;
}

function isBlank_(v) {
  return v === null || v === undefined || String(v).trim() === '';
}

/**
 * Returns a JS Date or null. Day-first (dd/mm/yyyy), matching the app —
 * only reads month-first when day-first is impossible (day > 12).
 */
function parseDate_(v) {
  if (v === null || v === undefined || v === '') return null;
  if (Object.prototype.toString.call(v) === '[object Date]') return v;
  var s = String(v).trim();
  var m = s.match(/^(\d{1,2})[-\/](\d{1,2})[-\/](\d{4})/);
  if (!m) return null;
  var a = parseInt(m[1], 10), b = parseInt(m[2], 10), y = parseInt(m[3], 10);
  var day, month;
  if (b > 12 && a <= 12) { month = a; day = b; }
  else { day = a; month = b; }
  if (month < 1 || month > 12 || day < 1 || day > 31) return null;
  var d = new Date(y, month - 1, day);
  if (d.getMonth() !== month - 1 || d.getDate() !== day) return null;
  return d;
}

function formatDate_(d) {
  var dd = ('0' + d.getDate()).slice(-2);
  var mm = ('0' + (d.getMonth() + 1)).slice(-2);
  return dd + '/' + mm + '/' + d.getFullYear();
}
