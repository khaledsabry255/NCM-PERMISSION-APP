/**
 * NCM WORK SHEET — استقبال الشيت من الكمبيوتر وتحديث الشيت اللي التطبيق بيقراه.
 *
 * الكمبيوتر بيبعت ملف الإكسل هنا، والكود ده بيحطه في نفس الشيت — من غير ما
 * لينك الشيت يتغير، وده المهم: التطبيق بيقرا الـ ID ده وهو مكتوب في الكود.
 *
 * التابات بتتنسخ بالاسم. الاسم "تصاريح أفراد " فيه مسافة في الآخر — دي جزء
 * من الاسم ولازم تفضل زي ما هي.
 */

var TARGET_ID = '1ZceJtmQMpW7Ky3Ysgz0mDcmE6Uxr600OuO2KOMHJnWE';
// The shared secret is NOT kept here: this repository is public, and a value
// on this line is a value anyone can use to write to the sheet. The live one
// lives in .ncm-secrets\apps-script-upload-secret.txt and is pasted into the
// Apps Script editor by hand, where it stays private.
var SECRET    = 'PASTE-THE-SECRET-HERE';
var TABS      = ['تصاريح أفراد ', 'تصاريح مركبات', 'ASE Letters'];

function doPost(e) {
  try {
    if (!e || !e.parameter || e.parameter.secret !== SECRET) {
      return reply_({ ok: false, error: 'unauthorized' });
    }

    var bytes = Utilities.base64Decode(e.postData.contents);
    var blob  = Utilities.newBlob(bytes,
                  'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
                  'incoming.xlsx');

    // Convert the upload into a throwaway Sheet we can read tab by tab.
    var tmp = Drive.Files.insert(
      { title: 'ncm-sync-temp', mimeType: MimeType.GOOGLE_SHEETS }, blob);

    var src    = SpreadsheetApp.openById(tmp.id);
    var target = SpreadsheetApp.openById(TARGET_ID);
    var report = {};

    try {
      // Every wanted tab has to be present before anything is replaced, so a
      // half-finished import can never leave the live sheet gutted.
      TABS.forEach(function (name) {
        if (!src.getSheetByName(name)) throw new Error('tab missing in upload: ' + name);
      });

      TABS.forEach(function (name) {
        var incoming = src.getSheetByName(name).copyTo(target);
        var old = target.getSheetByName(name);
        if (old) target.deleteSheet(old);
        incoming.setName(name);
        report[name] = incoming.getLastRow();
      });

      // A brand new Sheet keeps its own empty first tab; drop it if it survived.
      var leftover = target.getSheetByName('Sheet1');
      if (leftover && target.getSheets().length > TABS.length) target.deleteSheet(leftover);

    } finally {
      Drive.Files.remove(tmp.id);
    }

    return reply_({ ok: true, rows: report, at: new Date().toISOString() });

  } catch (err) {
    return reply_({ ok: false, error: String(err) });
  }
}

function reply_(obj) {
  return ContentService.createTextOutput(JSON.stringify(obj))
    .setMimeType(ContentService.MimeType.JSON);
}
