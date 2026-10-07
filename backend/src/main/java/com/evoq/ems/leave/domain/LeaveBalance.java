package com.evoq.ems.leave.domain;

import com.evoq.ems.employee.domain.Employee;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(
    name = "leave_balance",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_leave_balance_employee_type",
        columnNames = {"employee_id", "leave_type_id"}
    )
)
public class LeaveBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "balance_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "leave_type_id", nullable = false)
    private LeaveType leaveType;

    @Column(name = "available_days", nullable = false, precision = 5, scale = 2)
    private BigDecimal availableDays = BigDecimal.ZERO;

    @Column(name = "used_days", nullable = false, precision = 5, scale = 2)
    private BigDecimal usedDays = BigDecimal.ZERO;

    protected LeaveBalance() {
    }

    public LeaveBalance(Employee employee, LeaveType leaveType, BigDecimal availableDays) {
        this.employee = employee;
        this.leaveType = leaveType;
        this.availableDays = availableDays;
    }

    public Long getId() {
        return id;
    }

    public Employee getEmployee() {
        return employee;
    }

    public LeaveType getLeaveType() {
        return leaveType;
    }

    public BigDecimal getAvailableDays() {
        return availableDays;
    }

    public BigDecimal getUsedDays() {
        return usedDays;
    }

    /** Manual total grant; preserve used history and derive remaining days. */
    public void setEntitlement(BigDecimal total) { availableDays = total.subtract(usedDays); }

    public void approveDays(BigDecimal days) {
        availableDays = availableDays.subtract(days);
        usedDays = usedDays.add(days);
    }
}
