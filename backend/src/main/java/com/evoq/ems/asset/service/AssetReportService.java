package com.evoq.ems.asset.service;

import com.evoq.ems.auth.AccountPrincipal;
import com.evoq.ems.asset.service.AssetService;
import com.evoq.ems.asset.service.AssetAssignmentService;
import com.evoq.ems.employee.service.EmployeeAccess;
import com.evoq.ems.common.ModuleReport;
import static com.evoq.ems.common.ModuleReport.text;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

@Service
@Transactional(readOnly = true)
public class AssetReportService {
    private final AssetService assets;
    private final AssetAssignmentService assignments;
    public AssetReportService(AssetService assets, AssetAssignmentService assignments) { this.assets = assets; this.assignments = assignments; }
    public ModuleReport report(AccountPrincipal caller, Long employeeId) {
        EmployeeAccess.requireCaller(caller);
        if (employeeId != null) EmployeeAccess.requireOwnOrManager(caller, employeeId);
        boolean manager = EmployeeAccess.manager(caller);
        var history = employeeId != null || !manager ? assignments.getAssignmentsByEmployee(employeeId == null ? caller.getEmployeeId() : employeeId) : assignments.getAllAssignments();
        var inventory = assets.getAllAssets(caller).stream().filter(a -> employeeId == null || history.stream().anyMatch(h -> h.getAssetId().equals(a.getAssetId()) && "ASSIGNED".equals(h.getAssignmentStatus()))).toList();
        Map<String, Number> summary = new LinkedHashMap<>();
        summary.put(manager && employeeId == null ? "Registered assets" : "Currently assigned assets", inventory.size());
        if (manager && employeeId == null) {
            summary.put("Available assets", inventory.stream().filter(a -> "AVAILABLE".equals(a.getStatus())).count());
            summary.put("Other asset statuses", inventory.stream().filter(a -> !"AVAILABLE".equals(a.getStatus()) && !"ASSIGNED".equals(a.getStatus())).count());
        }
        summary.put("Active assignments", history.stream().filter(a -> "ASSIGNED".equals(a.getAssignmentStatus())).count());
        summary.put("Returned assignments", history.stream().filter(a -> "RETURNED".equals(a.getAssignmentStatus())).count());
        return new ModuleReport(manager ? employeeId == null ? "ORGANIZATION" : "SELECTED_EMPLOYEE" : "MINE", summary,
                List.of(new ModuleReport.Table("Current assets", List.of("Asset ID", "Name", "Type", "Serial number", "Status"),
                    inventory.stream().map(a -> List.of(text(a.getAssetId()), text(a.getAssetName()), text(a.getAssetType()), text(a.getSerialNumber()), text(a.getStatus()))).toList()),
                    new ModuleReport.Table("Assignment history", List.of("Assignment ID", "Asset ID", "Employee ID", "Assigned date", "Returned date", "Status"),
                    history.stream().map(a -> List.of(text(a.getAssignmentId()), text(a.getAssetId()), text(a.getEmployeeId()), text(a.getAssignedDate()), text(a.getReturnedDate()), text(a.getAssignmentStatus()))).toList())));
    }
}
