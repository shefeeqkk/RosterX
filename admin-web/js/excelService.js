/**
 * RosterX - Excel (.xlsx) Export & Import Service
 * 
 * Powered by ExcelJS for rich cell styling, shift color-coding, borders,
 * sticky frozen panes, and SheetJS for high-speed import parsing.
 * 
 * Shift Palette Colors:
 * - FS   : Soft Mint (#bbf7d0, text #14532d)
 * - FG   : Forest Green (#3f6212, text #ffffff)
 * - G    : Periwinkle Blue (#93c5fd, text #1e3a8a)
 * - SG   : Terracotta Orange (#c2410c, text #ffffff)
 * - SS   : Salmon / Peach (#fdba74, text #7c2d12)
 * - NS   : Slate Grey (#64748b, text #ffffff)
 * - OFF  : Bright Yellow (#fef08a, text #713f12)
 * - SW[Name]: Light Slate (#e2e8f0, text #334155, dashed border) - Treated as OFF Category
 * - L    : Red (#ef4444, text #ffffff)
 */

import { SHIFTS, sortEngineersByWeekOffs, getEngineerSwapCode } from './constants.js?v=1.0';

// Official Shift Colors formatted for ExcelJS (ARGB Hex) - High Contrast Scheme
const SHIFT_PALETTE = {
  FS: {
    bg: 'FFBBF7D0',
    font: 'FF064E3B',
    name: 'First Shift',
    timing: '07:30 AM - 04:30 PM',
    desc: 'Morning operational shift'
  },
  FG: {
    bg: 'FF166534',
    font: 'FFFFFFFF',
    name: 'First General Shift',
    timing: '08:30 AM - 05:30 PM',
    desc: 'Daytime primary general shift'
  },
  G: {
    bg: 'FFBAE6FD',
    font: 'FF0C4A6E',
    name: 'General Shift',
    timing: '09:30 AM - 06:30 PM',
    desc: 'Core daytime overlap shift'
  },
  SG: {
    bg: 'FFEA580C',
    font: 'FFFFFFFF',
    name: 'Second General Shift',
    timing: '11:00 AM - 08:00 PM',
    desc: 'Mid-day to evening overlap shift'
  },
  SS: {
    bg: 'FFFED7AA',
    font: 'FF7C2D12',
    name: 'Second Shift',
    timing: '01:00 PM - 10:00 PM',
    desc: 'Evening operational shift'
  },
  NS: {
    bg: 'FF1E293B',
    font: 'FFFFFFFF',
    name: 'Night Shift',
    timing: '09:00 PM - 07:30 AM',
    desc: 'Overnight 24/7 monitoring shift'
  },
  OFF: {
    bg: 'FFFEF08A',
    font: 'FF713F12',
    name: 'Weekly Off',
    timing: 'Rest Day',
    desc: 'Standard scheduled rest day'
  },
  L: {
    bg: 'FFDC2626',
    font: 'FFFFFFFF',
    name: 'Approved Leave',
    timing: 'Leave of Absence',
    desc: 'Annual or approved leave absence'
  },
  CUSTOM: {
    bg: 'FFF3E8FF',
    font: 'FF581C87',
    name: 'Custom Shift',
    timing: 'Operational Directive',
    desc: 'Custom configured shift'
  }
};

const THIN_BORDER = {
  top: { style: 'thin', color: { argb: 'FFCBD5E1' } },
  left: { style: 'thin', color: { argb: 'FFCBD5E1' } },
  bottom: { style: 'thin', color: { argb: 'FFCBD5E1' } },
  right: { style: 'thin', color: { argb: 'FFCBD5E1' } }
};

export class ExcelService {
  /**
   * Helper to trigger download of a generated ExcelJS workbook buffer
   */
  static async triggerFileDownload(workbook, fileName) {
    if (typeof workbook.xlsx !== 'undefined') {
      const buffer = await workbook.xlsx.writeBuffer();
      const blob = new Blob([buffer], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = fileName;
      document.body.appendChild(a);
      a.click();
      setTimeout(() => {
        document.body.removeChild(a);
        URL.revokeObjectURL(url);
      }, 500);
    }
  }

  /**
   * Build a beautifully styled enterprise Excel workbook
   */
  static async buildStyledWorkbook({ periodLabel, year, month, totalDays, engineers, constraints = {}, isTemplate = false }) {
    const dayShortNames = ['SUN', 'MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT'];

    // Ensure engineers are sorted in Weekoffs Day Order (Sun-Mon rank 1, Mon-Tue rank 2, etc.)
    if (Array.isArray(engineers)) {
      engineers = sortEngineersByWeekOffs(engineers);
    }

    // Check if ExcelJS is loaded
    if (typeof ExcelJS !== 'undefined') {
      const wb = new ExcelJS.Workbook();
      wb.creator = 'RosterX';
      wb.created = new Date();

      // Main Roster Worksheet with Frozen Panes (Sticky Header & Names)
      const ws = wb.addWorksheet(`Roster-${periodLabel}`, {
        views: [{ state: 'frozen', xSplit: 2, ySplit: 2 }],
        properties: { defaultRowHeight: 22 }
      });

      // 1. Header Row 1: Day Numbers
      const headerRow1Values = ['ENGG NAMES', periodLabel];
      for (let d = 1; d <= totalDays; d++) headerRow1Values.push(d);
      headerRow1Values.push('WRK', 'NS', 'OFF', 'STATUS');

      const r1 = ws.addRow(headerRow1Values);
      r1.height = 26;

      // Style Header 1
      r1.eachCell((cell, colNumber) => {
        cell.alignment = { vertical: 'middle', horizontal: 'center' };
        cell.border = THIN_BORDER;

        if (colNumber === 1) {
          // Engineer Name Header
          cell.fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: 'FF1E293B' } };
          cell.font = { name: 'Calibri', size: 11, bold: true, color: { argb: 'FFFFFFFF' } };
          cell.alignment = { vertical: 'middle', horizontal: 'left', indent: 1 };
        } else if (colNumber === 2) {
          // Period Label (OCT-26, NOV-26)
          cell.fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: 'FFF59E0B' } };
          cell.font = { name: 'Calibri', size: 11, bold: true, color: { argb: 'FF000000' } };
        } else if (colNumber <= totalDays + 2) {
          // Day Numbers
          cell.fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: 'FF1E293B' } };
          cell.font = { name: 'Calibri', size: 11, bold: true, color: { argb: 'FFFFFFFF' } };
        } else {
          // Summary Stats Headers (WRK, NS, OFF, STATUS)
          cell.fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: 'FF334155' } };
          cell.font = { name: 'Calibri', size: 11, bold: true, color: { argb: 'FFFFFFFF' } };
        }
      });

      // 2. Header Row 2: Days of the Week
      const headerRow2Values = [`(Total: ${engineers.length})`, 'DAY'];
      for (let d = 1; d <= totalDays; d++) {
        const dow = new Date(year, month - 1, d).getDay();
        headerRow2Values.push(dayShortNames[dow]);
      }
      headerRow2Values.push('STATS', '', '', 'LIFECYCLE');

      const r2 = ws.addRow(headerRow2Values);
      r2.height = 22;

      // Style Header 2
      r2.eachCell((cell, colNumber) => {
        cell.alignment = { vertical: 'middle', horizontal: 'center' };
        cell.border = THIN_BORDER;

        if (colNumber === 1) {
          cell.fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: 'FFF1F5F9' } };
          cell.font = { name: 'Calibri', size: 10, bold: true, color: { argb: 'FF64748B' } };
          cell.alignment = { vertical: 'middle', horizontal: 'left', indent: 1 };
        } else if (colNumber === 2) {
          cell.fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: 'FFE2E8F0' } };
          cell.font = { name: 'Calibri', size: 10, bold: true, color: { argb: 'FF475569' } };
        } else if (colNumber <= totalDays + 2) {
          const dayName = String(cell.value || '');
          const isWeekend = dayName === 'SUN' || dayName === 'SAT';
          cell.fill = {
            type: 'pattern',
            pattern: 'solid',
            fgColor: { argb: isWeekend ? 'FFDC2626' : 'FF475569' } // Weekend badge
          };
          cell.font = { name: 'Calibri', size: 10, bold: true, color: { argb: 'FFFFFFFF' } };
        } else {
          cell.fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: 'FFE2E8F0' } };
          cell.font = { name: 'Calibri', size: 10, bold: true, color: { argb: 'FF475569' } };
        }
      });

      // 3. Engineer Shift Rows
      engineers.forEach((eng) => {
        const rowValues = [eng.name, ''];
        let wrk = 0;
        let ns = 0;
        let off = 0;

        for (let d = 0; d < totalDays; d++) {
          let code = '';
          if (isTemplate) {
            code = ''; // BLANK DATA FOR SAMPLE TEMPLATE
          } else if (Array.isArray(eng.shifts)) {
            code = eng.shifts[d] || 'OFF';
          } else if (eng.shifts && typeof eng.shifts === 'object') {
            code = eng.shifts[d + 1] || 'OFF';
          } else {
            code = 'OFF';
          }

          rowValues.push(code);

          if (!isTemplate) {
            if (code === 'OFF' || (code && code.startsWith('SW'))) {
              off++;
            } else if (code === 'NS') {
              ns++;
              wrk++;
            } else if (code !== 'L' && code !== '') {
              wrk++;
            }
          }
        }

        rowValues.push(isTemplate ? '' : wrk, isTemplate ? '' : ns, isTemplate ? '' : off, 'Active');
        const dataRow = ws.addRow(rowValues);
        dataRow.height = 24;

        // Apply Shift-specific formatting
        dataRow.eachCell((cell, colNumber) => {
          cell.border = THIN_BORDER;

          if (colNumber === 1) {
            // Engineer Name
            cell.alignment = { vertical: 'middle', horizontal: 'left', indent: 1 };
            cell.fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: 'FFF8FAFC' } };
            cell.font = { name: 'Calibri', size: 11, bold: true, color: { argb: 'FF0F172A' } };
          } else if (colNumber === 2) {
            // Empty index col
            cell.fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: 'FFFFFFFF' } };
          } else if (colNumber <= totalDays + 2) {
            // Shift Code Cell
            const rawVal = cell.value ? String(cell.value).toUpperCase().trim() : '';
            if (rawVal && rawVal.startsWith('SW')) {
              // Standardized Swap Code -> Treated as OFF Category
              cell.fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: 'FFE2E8F0' } };
              cell.font = { name: 'Calibri', size: 11, bold: true, color: { argb: 'FF334155' } };
            } else if (rawVal && SHIFT_PALETTE[rawVal]) {
              const palette = SHIFT_PALETTE[rawVal];
              cell.fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: palette.bg } };
              cell.font = { name: 'Calibri', size: 11, bold: true, color: { argb: palette.font } };
            } else {
              // Blank template cell ready for input
              cell.fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: 'FFFFFFFF' } };
              cell.font = { name: 'Calibri', size: 11, color: { argb: 'FF0F172A' } };
            }
            cell.alignment = { vertical: 'middle', horizontal: 'center' };
          } else if (colNumber <= totalDays + 5) {
            // WRK, NS, OFF stats
            cell.fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: 'FFF1F5F9' } };
            cell.font = { name: 'Calibri', size: 10, bold: true, color: { argb: 'FF1E293B' } };
            cell.alignment = { vertical: 'middle', horizontal: 'center' };
          } else {
            // STATUS Pill (Active only)
            cell.fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: 'FFD1FAE5' } };
            cell.font = { name: 'Calibri', size: 10, bold: true, color: { argb: 'FF065F46' } };
            cell.alignment = { vertical: 'middle', horizontal: 'center' };
          }
        });
      });

      // 4. Coverage Footers: Day Shift & Night Shift only (Apprentice Engineers Excluded)
      const minDayStaff = constraints.daytimeOverlapMin || 3;
      const nightHeadcount = constraints.nightHeadcount || 1;

      // Blank separator row
      ws.addRow([]);

      const footersConfig = [
        { label: `Coverage: Day Shift (Combined Excl. NS)`, type: 'DAY', target: `Target: ≥${minDayStaff}` },
        { label: `Coverage: Night Shift (NS == ${nightHeadcount})`, type: 'NIGHT', target: `Target: ${nightHeadcount}` }
      ];

      footersConfig.forEach(cfg => {
        const footRowValues = [cfg.label, '—'];
        for (let d = 0; d < totalDays; d++) {
          let count = 0;
          if (!isTemplate) {
            engineers.forEach((eng) => {
              // NOTE: Apprentice engineers shifts are strictly NOT considered in coverage
              if (eng.role === 'apprentice') return;

              let code = 'OFF';
              if (Array.isArray(eng.shifts)) {
                code = eng.shifts[d] || 'OFF';
              } else if (eng.shifts && typeof eng.shifts === 'object') {
                code = eng.shifts[d + 1] || 'OFF';
              }

              if (cfg.type === 'NIGHT') {
                if (code === 'NS') count++;
              } else if (cfg.type === 'DAY') {
                // Day Shift: active operational shifts excluding Night Shift and Rest/Leave/Swap
                if (code && code !== 'NS' && code !== 'OFF' && !code.startsWith('SW') && code !== 'L') {
                  count++;
                }
              }
            });
          }
          footRowValues.push(isTemplate ? '' : count);
        }
        footRowValues.push(cfg.target, '', '', '');

        const fRow = ws.addRow(footRowValues);
        fRow.height = 20;
        fRow.eachCell((cell, colNumber) => {
          cell.border = THIN_BORDER;
          if (colNumber === 1) {
            cell.fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: 'FFE2E8F0' } };
            cell.font = { name: 'Calibri', size: 10, bold: true, color: { argb: 'FF334155' } };
            cell.alignment = { vertical: 'middle', horizontal: 'left', indent: 1 };
          } else {
            cell.fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: 'FFF8FAFC' } };
            cell.font = { name: 'Calibri', size: 10, bold: true, color: { argb: 'FF1E293B' } };
            cell.alignment = { vertical: 'middle', horizontal: 'center' };
          }
        });
      });

      // Set Column Widths for Optimal View
      const columnsConfig = [
        { width: 22 }, // Engineer Name
        { width: 10 }  // Period
      ];
      for (let d = 1; d <= totalDays; d++) {
        columnsConfig.push({ width: 6.5 }); // Day columns
      }
      columnsConfig.push(
        { width: 8 },  // WRK
        { width: 8 },  // NS
        { width: 8 },  // OFF
        { width: 15 }  // STATUS
      );
      ws.columns = columnsConfig;

      // 5. Reference Guide Sheet: Shift Codes Guide
      const wsGuide = wb.addWorksheet('Shift_Codes_Guide', {
        properties: { defaultRowHeight: 24 }
      });

      const guideHeaders = ['Shift Code', 'Shift Name', 'Reference Timings', 'Color Theme Badge', 'Operational Description'];
      const gHeaderRow = wsGuide.addRow(guideHeaders);
      gHeaderRow.height = 26;
      gHeaderRow.eachCell(cell => {
        cell.fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: 'FF1E293B' } };
        cell.font = { name: 'Calibri', size: 11, bold: true, color: { argb: 'FFFFFFFF' } };
        cell.alignment = { vertical: 'middle', horizontal: 'center' };
        cell.border = THIN_BORDER;
      });

      Object.entries(SHIFT_PALETTE).forEach(([code, info]) => {
        const gRow = wsGuide.addRow([code, info.name, info.timing, `[ ${code} ]`, info.desc]);
        gRow.height = 24;

        gRow.eachCell((cell, colNumber) => {
          cell.border = THIN_BORDER;
          if (colNumber === 1 || colNumber === 4) {
            cell.fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: info.bg } };
            cell.font = { name: 'Calibri', size: 11, bold: true, color: { argb: info.font } };
            cell.alignment = { vertical: 'middle', horizontal: 'center' };
          } else {
            cell.alignment = { vertical: 'middle', horizontal: 'left', indent: 1 };
            cell.font = { name: 'Calibri', size: 10 };
          }
        });
      });

      // Add Standardized Swap Row in Reference Guide
      const swRow = wsGuide.addRow([
        'SW[Name]',
        'Swap Code (Treated as OFF)',
        'Rest Day / Peer Covers Shift',
        '[ SWM / SWSH ]',
        'Assigned engineer is OFF on that day; peer identified by initial(s) takes shift'
      ]);
      swRow.height = 24;
      swRow.eachCell((cell, colNumber) => {
        cell.border = THIN_BORDER;
        if (colNumber === 1 || colNumber === 4) {
          cell.fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: 'FFE2E8F0' } };
          cell.font = { name: 'Calibri', size: 11, bold: true, color: { argb: 'FF334155' } };
          cell.alignment = { vertical: 'middle', horizontal: 'center' };
        } else {
          cell.alignment = { vertical: 'middle', horizontal: 'left', indent: 1 };
          cell.font = { name: 'Calibri', size: 10 };
        }
      });

      wsGuide.columns = [
        { width: 14 },
        { width: 24 },
        { width: 26 },
        { width: 20 },
        { width: 40 }
      ];

      return wb;
    }

    // Fallback using SheetJS (XLSX) if ExcelJS is unavailable
    if (typeof XLSX !== 'undefined') {
      const headerRow1 = ['ENGG NAMES', periodLabel];
      for (let d = 1; d <= totalDays; d++) headerRow1.push(d);
      headerRow1.push('WRK', 'NS', 'OFF', 'STATUS');

      const headerRow2 = [`(Total: ${engineers.length})`, 'DAY'];
      for (let d = 1; d <= totalDays; d++) {
        const dow = new Date(year, month - 1, d).getDay();
        headerRow2.push(dayShortNames[dow]);
      }
      headerRow2.push('STATS', '', '', 'LIFECYCLE');

      const dataRows = [];

      engineers.forEach((eng) => {
        const row = [eng.name, ''];
        let wrk = 0, ns = 0, off = 0;

        for (let d = 0; d < totalDays; d++) {
          let code = '';
          if (isTemplate) {
            code = '';
          } else if (Array.isArray(eng.shifts)) {
            code = eng.shifts[d] || 'OFF';
          } else if (eng.shifts && typeof eng.shifts === 'object') {
            code = eng.shifts[d + 1] || 'OFF';
          } else {
            code = 'OFF';
          }
          row.push(code);
          if (!isTemplate) {
            if (code === 'OFF' || (code && code.startsWith('SW'))) off++;
            else if (code === 'NS') { ns++; wrk++; }
            else if (code !== 'L' && code !== '') wrk++;
          }
        }
        row.push(isTemplate ? '' : wrk, isTemplate ? '' : ns, isTemplate ? '' : off, eng.status || 'Active');
        dataRows.push(row);
      });

      const wsData = [headerRow1, headerRow2, ...dataRows];
      const ws = XLSX.utils.aoa_to_sheet(wsData);
      const wb = XLSX.utils.book_new();
      XLSX.utils.book_append_sheet(wb, ws, `Roster-${periodLabel}`);
      return wb;
    }

    throw new Error('Neither ExcelJS nor SheetJS library is loaded.');
  }

  /**
   * Export the active roster grid with full cell color-coding and statistics
   */
  static async exportRosterToExcel(gridRoster, year, month, totalDays, constraints = {}) {
    const monthNames = ['JAN', 'FEB', 'MAR', 'APR', 'MAY', 'JUN', 'JUL', 'AUG', 'SEP', 'OCT', 'NOV', 'DEC'];
    const periodLabel = `${monthNames[month - 1]}-${String(year).slice(-2)}`;
    const fileName = `NOC_Shift_Roster_${periodLabel}.xlsx`;

    const wb = await ExcelService.buildStyledWorkbook({
      periodLabel,
      year,
      month,
      totalDays,
      engineers: gridRoster,
      constraints,
      isTemplate: false
    });

    if (typeof ExcelJS !== 'undefined' && wb instanceof ExcelJS.Workbook) {
      await ExcelService.triggerFileDownload(wb, fileName);
    } else if (typeof XLSX !== 'undefined') {
      XLSX.writeFile(wb, fileName);
    }
  }

  /**
   * Build the AppSheet Master Data Workbook matching Shift_Data_Master.xlsx
   * Contains:
   * 1. Sheet "Roster_AppSheet_Final" (Tab Color: Green) with columns:
   *    Date | MonthKey | EngineerName | ShiftCode | SwapStatus | SwapWith
   * 2. Sheet "REF DATA" (Tab Color: Red) with columns:
   *    EngineerName Values | ShiftCode Values | SwapStatus Values
   */
  static async buildAppSheetWorkbook({ year, month, totalDays, engineers, manualSwaps = [], allEngineers = [] }) {
    const monthNames = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
    const monthKey = `${monthNames[month - 1]}-${year}`;

    // Ensure engineers are sorted in Weekoffs Day Order
    const sortedEngineers = Array.isArray(engineers) ? sortEngineersByWeekOffs(engineers) : [];

    // Reference pool of all engineers
    const engineerPool = Array.isArray(allEngineers) && allEngineers.length > 0 ? allEngineers : sortedEngineers;

    // Helper to find swap partner from shift code (e.g. SWM -> MANOJ, SWSH -> SHEFEEQ, SWSI -> SILAMBARASAN)
    const findPartnerBySwapCode = (code, currentEngName, pool) => {
      if (!code || !code.startsWith('SW')) return null;
      const cleanCode = code.toUpperCase();
      if (cleanCode === 'SW' || cleanCode === 'SWOFF') return null;

      // 1. Direct match with standard getEngineerSwapCode
      for (const peer of pool) {
        if (peer.name === currentEngName) continue;
        const expectedCode = getEngineerSwapCode(peer.name, pool);
        if (expectedCode === cleanCode) {
          return peer.name;
        }
      }

      // 2. Secondary prefix match against first name initials
      const suffix = cleanCode.replace(/^SW/, '');
      for (const peer of pool) {
        if (peer.name === currentEngName) continue;
        const fName = peer.name.trim().toUpperCase().split(/[\s\W]+/)[0];
        if (fName.startsWith(suffix)) {
          return peer.name;
        }
      }
      return null;
    };

    // Build normalized rows
    const rows = [];
    for (let day = 1; day <= totalDays; day++) {
      const dIdx = day - 1;
      const dateStr = `${String(day).padStart(2, '0')}-${String(month).padStart(2, '0')}-${year}`;

      // Map to store swap info for this specific day: engineerName -> { status, shiftCode, swapWith }
      const daySwaps = new Map();

      // 1. Detect swaps strictly from actual grid cell codes starting with SW
      sortedEngineers.forEach(eng => {
        const s = String((eng.shifts && eng.shifts[dIdx]) || '').trim().toUpperCase();
        if (s.startsWith('SW')) {
          let partner = findPartnerBySwapCode(s, eng.name, engineerPool);
          // Fallback to manualSwaps if code is generic SW or SWOFF and needs partner resolution
          if (!partner && Array.isArray(manualSwaps)) {
            const match = manualSwaps.find(ms => 
              (Number(ms.day1) === day && ms.engineer_a === eng.name) ||
              (Number(ms.day2) === day && ms.engineer_b === eng.name)
            );
            if (match) {
              partner = match.engineer_a === eng.name ? match.engineer_b : match.engineer_a;
            }
          }

          daySwaps.set(eng.name, {
            status: 'Given OFF',
            shiftCode: 'SWOFF',
            swapWith: partner || ''
          });

          // If a partner is identified, mark them as Covering Swap only if they don't have an SW code themselves
          if (partner && !daySwaps.has(partner)) {
            const partnerEng = sortedEngineers.find(e => e.name === partner);
            const partnerRawShift = partnerEng && partnerEng.shifts ? String(partnerEng.shifts[dIdx] || '').trim().toUpperCase() : '';
            if (!partnerRawShift.startsWith('SW')) {
              // Covering partner works their assigned shift (or default shift if marked OFF)
              const coveringShift = (partnerRawShift && partnerRawShift !== 'OFF') ? partnerRawShift : 'NS';
              daySwaps.set(partner, {
                status: 'Covering Swap',
                shiftCode: coveringShift,
                swapWith: eng.name
              });
            }
          }
        }
      });

      // Generate rows in current sorted engineer order
      sortedEngineers.forEach(eng => {
        const rawShift = String((eng.shifts && eng.shifts[dIdx]) || '').trim().toUpperCase();
        let shiftCode = rawShift || 'OFF';
        let swapStatus = 'Normal';
        let swapWith = '';

        if (daySwaps.has(eng.name)) {
          const swapInfo = daySwaps.get(eng.name);
          shiftCode = swapInfo.shiftCode;
          swapStatus = swapInfo.status;
          swapWith = swapInfo.swapWith;
        } else if (shiftCode.startsWith('SW')) {
          shiftCode = 'SWOFF';
          swapStatus = 'Given OFF';
        }

        rows.push({
          date: dateStr,
          monthKey: monthKey,
          engineerName: eng.name,
          shiftCode: shiftCode,
          swapStatus: swapStatus,
          swapWith: swapWith
        });
      });
    }

    // Build with ExcelJS
    if (typeof ExcelJS !== 'undefined') {
      const wb = new ExcelJS.Workbook();
      wb.creator = 'RosterX';
      wb.created = new Date();

      // Sheet 1: Roster_AppSheet_Final
      const wsAppSheet = wb.addWorksheet('Roster_AppSheet_Final', {
        views: [{ state: 'frozen', xSplit: 0, ySplit: 1 }],
        properties: { 
          defaultRowHeight: 20,
          tabColor: { argb: 'FF2E7D32' } // Green Tab Color
        }
      });

      // Headers Row 1
      const headers = ['Date', 'MonthKey', 'EngineerName', 'ShiftCode', 'SwapStatus', 'SwapWith'];
      const hRow = wsAppSheet.addRow(headers);
      hRow.height = 26;

      const headerFill = { type: 'pattern', pattern: 'solid', fgColor: { argb: 'FF274E13' } }; // Dark Forest Green
      const headerFont = { name: 'Calibri', size: 11, bold: true, color: { argb: 'FFFFFFFF' } };

      hRow.eachCell((cell, colNumber) => {
        cell.fill = headerFill;
        cell.font = headerFont;
        cell.border = THIN_BORDER;
        cell.alignment = (colNumber === 3 || colNumber === 6)
          ? { vertical: 'middle', horizontal: 'left', indent: 1 }
          : { vertical: 'middle', horizontal: 'center' };
      });

      // Populate Data Rows
      rows.forEach(r => {
        const dataRow = wsAppSheet.addRow([
          r.date,
          r.monthKey,
          r.engineerName,
          r.shiftCode,
          r.swapStatus,
          r.swapWith
        ]);
        dataRow.height = 20;
        dataRow.eachCell((cell, colNumber) => {
          cell.font = { name: 'Calibri', size: 11 };
          cell.border = THIN_BORDER;
          cell.alignment = (colNumber === 3 || colNumber === 6)
            ? { vertical: 'middle', horizontal: 'left', indent: 1 }
            : { vertical: 'middle', horizontal: 'center' };
        });
      });

      wsAppSheet.columns = [
        { width: 14 }, // Date
        { width: 14 }, // MonthKey
        { width: 24 }, // EngineerName
        { width: 14 }, // ShiftCode
        { width: 18 }, // SwapStatus
        { width: 24 }  // SwapWith
      ];

      // Sheet 2: REF DATA
      const wsRef = wb.addWorksheet('REF DATA', {
        views: [{ state: 'frozen', xSplit: 0, ySplit: 1 }],
        properties: { 
          defaultRowHeight: 20,
          tabColor: { argb: 'FFD32F2F' } // Red Tab Color
        }
      });

      const refHeaders = ['EngineerName Values', 'ShiftCode Values', 'SwapStatus Values'];
      const refHRow = wsRef.addRow(refHeaders);
      refHRow.height = 26;
      refHRow.eachCell((cell) => {
        cell.fill = headerFill;
        cell.font = headerFont;
        cell.border = THIN_BORDER;
        cell.alignment = { vertical: 'middle', horizontal: 'left', indent: 1 };
      });

      // Reference values matching Image 2
      const engNameValues = engineerPool.map(e => e.name);
      const shiftCodeValues = ['FS', 'FG', 'G', 'SG', 'SS', 'NS', 'OFF', 'SWOFF', 'L'];
      const swapStatusValues = ['Normal', 'Covering Swap', 'Given OFF'];

      const maxRefRows = Math.max(engNameValues.length, shiftCodeValues.length, swapStatusValues.length);
      for (let i = 0; i < maxRefRows; i++) {
        const refRow = wsRef.addRow([
          engNameValues[i] || '',
          shiftCodeValues[i] || '',
          swapStatusValues[i] || ''
        ]);
        refRow.height = 20;
        refRow.eachCell((cell) => {
          cell.font = { name: 'Calibri', size: 10 };
          cell.border = THIN_BORDER;
          cell.alignment = { vertical: 'middle', horizontal: 'left', indent: 1 };
        });
      }

      wsRef.columns = [
        { width: 24 },
        { width: 18 },
        { width: 20 }
      ];

      return wb;
    }

    // Fallback using SheetJS (XLSX) if ExcelJS is unavailable
    if (typeof XLSX !== 'undefined') {
      const wb = XLSX.utils.book_new();

      const appSheetData = [
        ['Date', 'MonthKey', 'EngineerName', 'ShiftCode', 'SwapStatus', 'SwapWith'],
        ...rows.map(r => [r.date, r.monthKey, r.engineerName, r.shiftCode, r.swapStatus, r.swapWith])
      ];
      const ws1 = XLSX.utils.aoa_to_sheet(appSheetData);
      XLSX.utils.book_append_sheet(wb, ws1, 'Roster_AppSheet_Final');

      const engNameValues = engineerPool.map(e => e.name);
      const shiftCodeValues = ['FS', 'FG', 'G', 'SG', 'SS', 'NS', 'OFF', 'SWOFF', 'L'];
      const swapStatusValues = ['Normal', 'Covering Swap', 'Given OFF'];
      const maxRefRows = Math.max(engNameValues.length, shiftCodeValues.length, swapStatusValues.length);
      const refDataRows = [['EngineerName Values', 'ShiftCode Values', 'SwapStatus Values']];
      for (let i = 0; i < maxRefRows; i++) {
        refDataRows.push([engNameValues[i] || '', shiftCodeValues[i] || '', swapStatusValues[i] || '']);
      }
      const ws2 = XLSX.utils.aoa_to_sheet(refDataRows);
      XLSX.utils.book_append_sheet(wb, ws2, 'REF DATA');

      return wb;
    }

    throw new Error('Neither ExcelJS nor SheetJS library is loaded.');
  }

  /**
   * Export the active roster in normalized AppSheet Data format (Shift_Data_Master.xlsx)
   */
  static async exportRosterToAppSheet(gridRoster, year, month, totalDays, manualSwaps = [], allEngineers = []) {
    const monthNames = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
    const periodLabel = `${monthNames[month - 1]}_${year}`;
    const fileName = `Shift_Data_Master_${periodLabel}.xlsx`;

    const wb = await ExcelService.buildAppSheetWorkbook({
      year,
      month,
      totalDays,
      engineers: gridRoster,
      manualSwaps,
      allEngineers
    });

    if (typeof ExcelJS !== 'undefined' && wb instanceof ExcelJS.Workbook) {
      await ExcelService.triggerFileDownload(wb, fileName);
    } else if (typeof XLSX !== 'undefined') {
      XLSX.writeFile(wb, fileName);
    }
  }

  /**
   * Download a pre-formatted, color-coded sample Excel template for the selected month
   */
  static async downloadSampleTemplate(year, month, engineers, constraints = {}) {
    const totalDays = new Date(year, month, 0).getDate();
    const monthNames = ['JAN', 'FEB', 'MAR', 'APR', 'MAY', 'JUN', 'JUL', 'AUG', 'SEP', 'OCT', 'NOV', 'DEC'];
    const periodLabel = `${monthNames[month - 1]}-${String(year).slice(-2)}`;
    const fileName = `RosterX_Template_${periodLabel}.xlsx`;

    // Exclude Resigned engineers from the template
    const activeEngineers = engineers.filter(e => e.status !== 'Resigned');

    const wb = await ExcelService.buildStyledWorkbook({
      periodLabel,
      year,
      month,
      totalDays,
      engineers: activeEngineers,
      constraints,
      isTemplate: true
    });

    if (typeof ExcelJS !== 'undefined' && wb instanceof ExcelJS.Workbook) {
      await ExcelService.triggerFileDownload(wb, fileName);
    } else if (typeof XLSX !== 'undefined') {
      XLSX.writeFile(wb, fileName);
    }
  }

  /**
   * Import and parse an uploaded Excel spreadsheet file
   */
  static async parseExcelRoster(file, targetTotalDays, currentEngineers) {
    if (typeof XLSX === 'undefined') {
      throw new Error('SheetJS library is not loaded. Please ensure you have internet access or the library is included.');
    }

    return new Promise((resolve, reject) => {
      const reader = new FileReader();

      reader.onload = (e) => {
        try {
          const data = new Uint8Array(e.target.result);
          const workbook = XLSX.read(data, { type: 'array' });
          if (!workbook.SheetNames || workbook.SheetNames.length === 0) {
            throw new Error('Excel workbook contains no sheets.');
          }

          const firstSheet = workbook.Sheets[workbook.SheetNames[0]];
          const rows = XLSX.utils.sheet_to_json(firstSheet, { header: 1, defval: '' });

          if (!rows || rows.length < 2) {
            throw new Error('Spreadsheet has insufficient rows. Please ensure it contains headers and engineer rows.');
          }

          // 1. Locate the row containing day numbers (1, 2, 3...)
          let headerRowIndex = -1;
          let firstDayColIndex = -1;

          for (let r = 0; r < Math.min(rows.length, 10); r++) {
            const row = rows[r];
            if (!row || !Array.isArray(row)) continue;

            for (let c = 0; c < row.length; c++) {
              const val = row[c];
              if (val === 1 || val === '1' || String(val).trim() === '1') {
                const nextVal = row[c + 1];
                if (nextVal === 2 || nextVal === '2' || String(nextVal).trim() === '2') {
                  headerRowIndex = r;
                  firstDayColIndex = c;
                  break;
                }
              }
            }
            if (headerRowIndex !== -1) break;
          }

          if (headerRowIndex === -1) {
            throw new Error('Could not detect the Day 1 column in the spreadsheet. Please ensure headers include day numbers (1, 2, 3...).');
          }

          let detectedDays = 0;
          const dayRow = rows[headerRowIndex];
          for (let c = firstDayColIndex; c < dayRow.length; c++) {
            const v = dayRow[c];
            if (v !== '' && !isNaN(parseInt(v, 10)) && parseInt(v, 10) === detectedDays + 1) {
              detectedDays++;
            } else if (detectedDays >= 28) {
              break;
            }
          }

          if (detectedDays === 0) detectedDays = targetTotalDays;

          const warnings = [];
          const parsedEngineers = [];
          const validShiftCodes = ['FS', 'FG', 'G', 'SG', 'SS', 'NS', 'OFF', 'L', 'CUSTOM'];
          const nameColIndex = 0;

          let startRow = headerRowIndex + 1;
          const nextRow = rows[startRow];
          if (nextRow && (
            String(nextRow[firstDayColIndex]).toUpperCase().includes('MON') ||
            String(nextRow[firstDayColIndex]).toUpperCase().includes('SUN') ||
            String(nextRow[firstDayColIndex]).toUpperCase().includes('DAY') ||
            String(nextRow[nameColIndex]).toLowerCase().includes('total')
          )) {
            startRow++;
          }

          for (let r = startRow; r < rows.length; r++) {
            const row = rows[r];
            if (!row || !row[nameColIndex]) continue;

            const rawName = String(row[nameColIndex]).trim();
            if (!rawName) continue;

            const upperName = rawName.toUpperCase();
            if (
              upperName.startsWith('COVERAGE') ||
              upperName.startsWith('TOTAL') ||
              upperName.startsWith('MORNING') ||
              upperName.startsWith('GENERAL') ||
              upperName.startsWith('EVENING') ||
              upperName.startsWith('NIGHT') ||
              upperName.startsWith('STATS')
            ) {
              break;
            }

            const matchedEng = currentEngineers.find(e => 
              e.name.trim().toUpperCase() === rawName.toUpperCase() ||
              (e.full_name && e.full_name.trim().toUpperCase() === rawName.toUpperCase()) ||
              rawName.toUpperCase().includes(e.name.trim().toUpperCase())
            );

            if (!matchedEng) {
              warnings.push(`Engineer "${rawName}" not found in current directory; loaded as active.`);
            }

            const shifts = [];
            for (let d = 0; d < targetTotalDays; d++) {
              const colIdx = firstDayColIndex + d;
              let rawCode = row[colIdx] ? String(row[colIdx]).trim().toUpperCase() : 'OFF';

              if (rawCode === 'O' || rawCode === 'WO' || rawCode === 'W/O') rawCode = 'OFF';
              if (rawCode === 'N' || rawCode === 'NIGHT') rawCode = 'NS';
              if (rawCode === 'LEAVE') rawCode = 'L';

              if (rawCode.startsWith('SW')) {
                // Accepted as valid standardized swap code
              } else if (!validShiftCodes.includes(rawCode)) {
                rawCode = 'OFF';
              }
              shifts.push(rawCode);
            }

            parsedEngineers.push({
              id: matchedEng ? matchedEng.id : `eng_${rawName.toLowerCase().replace(/[\s\W]+/g, '_')}`,
              name: matchedEng ? matchedEng.name : rawName,
              shifts: shifts,
              status: matchedEng ? matchedEng.status : 'Active',
              role: matchedEng ? matchedEng.role : 'engineer'
            });
          }

          if (parsedEngineers.length === 0) {
            throw new Error('No valid engineer shift rows were found in the uploaded file.');
          }

          resolve({
            engineers: parsedEngineers,
            detectedDays,
            totalDays: targetTotalDays,
            warnings
          });

        } catch (err) {
          reject(err);
        }
      };

      reader.onerror = () => reject(new Error('Failed to read Excel file from disk.'));
      reader.readAsArrayBuffer(file);
    });
  }
}

// Global window exposure for instant access & fallback
if (typeof window !== 'undefined') {
  window.ExcelService = ExcelService;
}
