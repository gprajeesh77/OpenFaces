/*
 * OpenFaces - JSF Component Library 2.0
 * Copyright (C) 2007-2009, TeamDev Ltd.
 * licensing@openfaces.org
 * Unless agreed in writing the contents of this file are subject to
 * the GNU Lesser General Public License Version 2.1 (the "LGPL" License).
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * Please visit http://openfaces.org/licensing/ for more details.
 */

//O$.DateRanges declaration start
O$.DateRanges = function(dateRangesArray,
                         styleClassName,
                         rolloverStyleClassName,
                         disableExcluded,
                         disableIncluded,
                         selectedDayClassName,
                         rolloverSelectedDayClassName) {
  this._dateRanges = dateRangesArray;
  this._styleClassName = styleClassName;
  this._rolloverStyleClassName = rolloverStyleClassName;
  this._disableExcluded = disableExcluded;
  this._disableIncluded = disableIncluded;
  this._selectedDayClass = selectedDayClassName;
  this._rolloverSelectedDayClass = rolloverSelectedDayClassName;
}

O$.DateRanges.prototype.getDateRanges = function() {
  return this._dateRanges;
}

O$.DateRanges.prototype.getSimpleDateRanges = function() {
  if (!this._dateRanges) return undefined;
  var simpleDateRanges = new Array;
  for (var i = 0, count = this._dateRanges.length; i < count; i ++) {
    var dateRange = this._dateRanges[i];
    if (dateRange instanceof O$.SimpleDateRange) {
      simpleDateRanges.push(dateRange);
    }
  }
  return simpleDateRanges;
}
//O$.DateRanges declaration end

//---------------------------------------------------------------

//O$.AbstractDateRange declaration start
O$.AbstractDateRange = function(styleClassName, rolloverStyleClassName,
                                selectedDayStyleClassName, rolloverSelectedDayStyleClassName) {
  this._dates = new Array;
  this._styleClassName = styleClassName;
  this._rolloverStyleClassName = rolloverStyleClassName;
  this._selectedDayStyleClassName = selectedDayStyleClassName;
  this._rolloverSelectedDayStyleClassName = rolloverSelectedDayStyleClassName;
}

O$.AbstractDateRange.prototype.getDates = function() {
  return this._dates;
}
//O$.AbstractDateRange declaration end

//---------------------------------------------------------------

//O$.SimpleDateRange object declaration start
O$.SimpleDateRange = function(fromDate, toDate, styleClassName, rolloverStyleClassName,
                              selectedDayStyleClassName, rolloverSelectedDayStyleClassName) {
  this._fromDate = fromDate;
  this._toDate = toDate;
  O$.AbstractDateRange.apply(this, [styleClassName, rolloverStyleClassName, selectedDayStyleClassName,
    rolloverSelectedDayStyleClassName]);
}
O$.SimpleDateRange.prototype = new O$.AbstractDateRange;

O$.SimpleDateRange.prototype.isDateInRange = function(date) {
  if (!date) return false;
  if (O$._calendar_compareDates(date, this._fromDate) >= 0 && O$._calendar_compareDates(date, this._toDate) <= 0) return true;
  return false;
};
//O$.SimpleDateRange object declaration end