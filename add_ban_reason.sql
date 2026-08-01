alter table employees add column if not exists ban_reason text;

update employees set ban_reason = 'منع هيئة موبيل' where emp_code = '429';
update employees set ban_reason = 'منع هيئة' where emp_code = '449';
update employees set ban_reason = 'منع هيئة موبيل' where emp_code = '580';
update employees set ban_reason = 'منع هيئة موبيل' where emp_code = '704';
update employees set ban_reason = 'منع نهائى' where emp_code = '712';
update employees set ban_reason = 'منع هيئة' where emp_code = '878';
update employees set ban_reason = 'منع جنائى' where emp_code = '927';
update employees set ban_reason = 'منع هيئة موبيل' where emp_code = '1027';
update employees set ban_reason = 'منع هيئة موبيل' where emp_code = '1202';