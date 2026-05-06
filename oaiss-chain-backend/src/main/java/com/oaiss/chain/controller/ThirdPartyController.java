package com.oaiss.chain.controller;

import com.oaiss.chain.dto.ApiResponse;
import com.oaiss.chain.entity.CarbonReport;
import com.oaiss.chain.entity.ThirdPartyOrg;
import com.oaiss.chain.security.JwtUserDetails;
import com.oaiss.chain.service.ThirdPartyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 第三方监管控制器
 * 提供第三方监管机构数据查询与管理功能
 *
 * @author OAISS Team
 */
@RestController
@RequestMapping("/third-party")
@RequiredArgsConstructor
@PreAuthorize("hasRole('THIRD_PARTY')")
@Tag(name = "15. 第三方监管管理", description = "第三方监管机构数据查询、碳报告审核、监管统计")
public class ThirdPartyController {

    private final ThirdPartyService thirdPartyService;

    @GetMapping("/org-info")
    @Operation(summary = "获取当前机构信息", description = "获取当前登录的第三方监管机构的详细信息")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未授权访问"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "非第三方监管机构用户")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    public ApiResponse<ThirdPartyOrg> getOrgInfo(
            @AuthenticationPrincipal JwtUserDetails currentUser) {
        return ApiResponse.success(thirdPartyService.getCurrentOrg(currentUser));
    }

    @GetMapping("/carbon-reports")
    @Operation(summary = "查询碳报告（监管视角）", description = "第三方监管机构查询所有企业碳报告，支持按企业、状态、关键字筛选")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "参数无效"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未授权访问"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "非第三方监管机构用户")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    public ApiResponse<Page<CarbonReport>> queryCarbonReports(
            @AuthenticationPrincipal JwtUserDetails currentUser,
            @Parameter(description = "企业ID", example = "1") @RequestParam(required = false) Long enterpriseId,
            @Parameter(description = "报告状态：0-草稿, 1-待审核, 2-已通过, 3-已驳回", example = "1") @RequestParam(required = false) Integer status,
            @Parameter(description = "搜索关键字", example = "年度") @RequestParam(required = false) String keyword,
            @Parameter(description = "页码（从1开始）", example = "1") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页大小", example = "10") @RequestParam(defaultValue = "10") Integer size) {
        return thirdPartyService.queryCarbonReports(
                currentUser, enterpriseId, status, keyword, page, size);
    }

    @GetMapping("/statistics")
    @Operation(summary = "获取监管统计数据", description = "获取第三方监管机构管辖范围内的统计数据，包括审核报告数、通过率等")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未授权访问"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "非第三方监管机构用户")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    public ApiResponse<Map<String, Object>> getStatistics(
            @AuthenticationPrincipal JwtUserDetails currentUser) {
        return thirdPartyService.getStatistics(currentUser);
    }

    @PutMapping("/contact")
    @Operation(summary = "更新机构联系方式", description = "更新第三方监管机构的联系人和联系电话")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "更新成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "联系方式无效"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未授权访问"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "非第三方监管机构用户")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    public ApiResponse<Void> updateContact(
            @AuthenticationPrincipal JwtUserDetails currentUser,
            @Parameter(description = "联系人姓名", example = "张三") @RequestParam(required = false) String contactPerson,
            @Parameter(description = "联系电话", example = "13800138000") @RequestParam(required = false) String contactPhone) {
        return thirdPartyService.updateContact(currentUser, contactPerson, contactPhone);
    }
}
