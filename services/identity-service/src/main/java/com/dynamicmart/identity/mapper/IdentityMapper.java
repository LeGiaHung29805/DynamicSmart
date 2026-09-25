package com.dynamicmart.identity.mapper;

import com.dynamicmart.identity.auth.domain.UserAccount;
import com.dynamicmart.identity.dto.response.AddressResponse;
import com.dynamicmart.identity.dto.response.AdminUserResponse;
import com.dynamicmart.identity.dto.response.ProfileResponse;
import com.dynamicmart.identity.dto.response.UserAuditResponse;
import com.dynamicmart.identity.entity.Address;
import com.dynamicmart.identity.entity.UserManagementAudit;
import java.util.List;

public final class IdentityMapper {
    private IdentityMapper() { }
    public static ProfileResponse profile(UserAccount user) {
        return new ProfileResponse(user.getId(), user.getEmail(), user.getFullName(), user.getPhone(),
                user.getRole().name(), user.getStatus().name(), user.getLastLoginAt(), user.getCreatedAt(), user.getUpdatedAt());
    }
    public static AddressResponse address(Address value) {
        return new AddressResponse(value.getId(), value.getRecipientName(), value.getPhone(), value.getAddressLine(),
                value.getProvinceId(), value.getWardId(), value.getProvinceName(), value.getWardName(),
                value.isDefaultAddress(), value.getStatus().name(), value.getUpdatedAt());
    }
    public static UserAuditResponse audit(UserManagementAudit value) {
        return new UserAuditResponse(value.getId(), value.getAction(), value.getOldRole(), value.getNewRole(),
                value.getOldStatus(), value.getNewStatus(), value.getReason(), value.getCreatedAt());
    }
    public static AdminUserResponse adminUser(UserAccount user, List<UserAuditResponse> audits) {
        return new AdminUserResponse(user.getId(), user.getEmail(), user.getFullName(), user.getPhone(),
                user.getRole().name(), user.getStatus().name(), user.getLastLoginAt(), user.getCreatedAt(), audits);
    }
}
