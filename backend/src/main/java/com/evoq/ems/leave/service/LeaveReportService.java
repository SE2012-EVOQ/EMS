package com.evoq.ems.leave.service;

import com.evoq.ems.auth.AccountPrincipal;
import com.evoq.ems.leave.service.LeaveService;
import com.evoq.ems.employee.service.EmployeeAccess;
import com.evoq.ems.common.ModuleReport;
import static com.evoq.ems.common.ModuleReport.text;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.time.LocalDate;
import java.math.BigDecimal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class LeaveReportService {
    private final LeaveService leave;
    private final EmployeeAccess access;
    public LeaveReportService(LeaveService leave, EmployeeAccess access) { this.leave = leave; this.access = access; }
    public ModuleReport report(AccountPrincipal caller,
            LocalDate from, LocalDate to,
            Long employeeId) {
        if (from != null && to != null && from.isAfter(to)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "End date must follow start date");
        // Scope validation occurs before applying client filters, including empty report results.
        if (employeeId != null) access.requireVisible(caller, employeeId);
        var balances = leave.reportBalances(caller);
        var requests = leave.reportRequests(caller);
        var filtered = requests.stream().filter(r -> employeeId == null || employeeId.equals(r.employeeId()))
                .filter(r -> from == null || !r.endDate().isBefore(from)).filter(r -> to == null || !r.startDate().isAfter(to)).toList();
        var selectedBalances = balances.stream().filter(b -> employeeId == null || employeeId.equals(b.employeeId())).toList();
        Map<String, Number> summary = new LinkedHashMap<>();
        summary.put("Leave requests", filtered.size());
        for (String status : List.of("PENDING", "APPROVED", "REJECTED"))
            summary.put(status.charAt(0) + status.substring(1).toLowerCase() + " requests", filtered.stream().filter(r -> status.equals(r.status())).count());
        summary.put("Available days (current balances)", selectedBalances.stream().map(b -> b.balance().availableDays()).reduce(BigDecimal.ZERO, BigDecimal::add));
        summary.put("Used days (current balances)", selectedBalances.stream().map(b -> b.balance().usedDays()).reduce(BigDecimal.ZERO, BigDecimal::add));
        return new ModuleReport(EmployeeAccess.manager(caller) ? "ORGANIZATION" : caller.getRole().equals("SUPERVISOR") ? "SELF_AND_TEAM" : "MINE", summary,
                List.of(new ModuleReport.Table("Requests overlapping selected dates", List.of("Request ID", "Employee ID", "Employee", "Type", "Start", "End", "Calendar days (whole request)", "Status"),
                        filtered.stream().map(r -> List.of(text(r.id()), text(r.employeeId()), text(r.employeeName()), text(r.leaveType()), text(r.startDate()), text(r.endDate()), text(r.days()), text(r.status()))).toList()),
                    new ModuleReport.Table("Current balances (independent of date filter)", List.of("Employee ID", "Employee", "Type", "Total entitlement", "Available days", "Used days"),
                        selectedBalances.stream().map(b -> List.of(text(b.employeeId()), text(b.employeeName()), text(b.balance().leaveType()), text(b.balance().availableDays().add(b.balance().usedDays())), text(b.balance().availableDays()), text(b.balance().usedDays()))).toList())));
    }
}
